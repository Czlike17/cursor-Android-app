package com.example.myapp.engine

import android.content.Context
import com.example.myapp.data.local.AppDatabase
import com.example.myapp.data.local.entity.AutoControlLog
import com.example.myapp.data.local.entity.UserHabit
import com.example.myapp.data.mqtt.MqttClientManager
import com.example.myapp.util.PreferencesManager
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 自动控制引擎
 * 使用 Kotlin Flow 合并时间、环境、用户状态、习惯模型四个数据流
 * 条件同时满足且习惯启用时，生成指令发布到 MQTT 或 Mock 数据库
 */
@Singleton
class CustomAutoControlEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mqttClientManager: MqttClientManager,
    private val preferencesManager: PreferencesManager
) {

    private val database = AppDatabase.getDatabase(context)
    private val userHabitDao = database.userHabitDao()
    private val environmentCacheDao = database.environmentCacheDao()
    private val autoControlLogDao = database.autoControlLogDao()
    private val deviceDao = database.deviceDao()

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // 手动操作暂停记录 <habitId, pauseUntilTime>
    private val manualPauseMap = mutableMapOf<Long, Long>()
    private val pauseDurationMs = 60 * 60 * 1000L // 1小时

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    fun start() {
        if (_isRunning.value) {
            Timber.w("AutoControlEngine: Already running")
            return
        }

        _isRunning.value = true
        Timber.i("AutoControlEngine: Starting")

        scope.launch {
            val username = preferencesManager.getUsernameSync() ?: return@launch

            val timeFlow = createTimeFlow()
            val environmentFlow = createEnvironmentFlow(username)
            val userStateFlow = createUserStateFlow()
            val habitFlow = createHabitFlow(username)

            combine(timeFlow, environmentFlow, userStateFlow, habitFlow) { time, environment, userState, habits ->
                ControlContext(time, environment, userState, habits)
            }.collect { context ->
                processControlContext(context, username)
            }
        }
    }

    fun stop() {
        _isRunning.value = false
        scope.coroutineContext.cancelChildren()
        Timber.i("AutoControlEngine: Stopped")
    }

    suspend fun recordManualOperation(deviceId: Long, action: String) {
        val username = preferencesManager.getUsernameSync() ?: return
        val habit = userHabitDao.getHabitByDeviceAndAction(deviceId, action, username)

        if (habit != null) {
            val pauseUntil = System.currentTimeMillis() + pauseDurationMs
            manualPauseMap[habit.id] = pauseUntil
            Timber.d("AutoControlEngine: Manual operation detected, pausing habit ${habit.id} for 1 hour")
        }
    }

    private fun createTimeFlow(): Flow<TimeContext> = flow {
        while (currentCoroutineContext().isActive) {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val weekType = calendar.get(Calendar.DAY_OF_WEEK)

            emit(TimeContext(hour, minute, weekType))
            delay(60 * 1000)
        }
    }

    private fun createEnvironmentFlow(username: String): Flow<EnvironmentContext> = flow {
        while (currentCoroutineContext().isActive) {
            val latestEnv = environmentCacheDao.getLatestByUsername(username)
            val envContext = if (latestEnv != null) {
                EnvironmentContext(
                    temperature = latestEnv.temperature,
                    humidity = latestEnv.humidity,
                    lightIntensity = latestEnv.lightIntensity,
                    pm25 = latestEnv.pm25
                )
            } else {
                EnvironmentContext()
            }
            emit(envContext)
            delay(5 * 60 * 1000)
        }
    }

    private fun createUserStateFlow(): Flow<UserStateContext> = flow {
        while (currentCoroutineContext().isActive) {
            val isAtHome = preferencesManager.getAtHomeMode().first()
            emit(UserStateContext(isAtHome = isAtHome))
            delay(60 * 1000)
        }
    }

    private fun createHabitFlow(username: String): Flow<List<UserHabit>> {
        return userHabitDao.getEnabledHabitsFlow(username)
    }

    private suspend fun processControlContext(context: ControlContext, username: String) {
        for (habit in context.habits) {
            val pauseUntil = manualPauseMap[habit.id]
            if (pauseUntil != null && System.currentTimeMillis() < pauseUntil) continue
            else if (pauseUntil != null) manualPauseMap.remove(habit.id)

            if (checkHabitConditions(habit, context)) {
                val lastLog = autoControlLogDao.getLatestLogByHabit(habit.id, username)
                val now = System.currentTimeMillis()

                // 【防抖保护】：避免高频传感器反复触发，同一习惯 10 分钟内冷却
                if (lastLog != null && (now - lastLog.executeTime) < 10 * 60 * 1000) continue

                executeAutoControl(habit, username)
            }
        }
    }

    /**
     * 【企业级修复：核心裁决器】
     * 完美适配独立的字段与 Bitmask 位运算，杜绝旧版 JSON 解析导致的不触发
     */
    private fun checkHabitConditions(habit: UserHabit, context: ControlContext): Boolean {
        try {
            // 1. 检查星期 (Bitmask 位运算：提取当前星期对应的位，判断是否为 1)
            if (habit.weekType != 0) {
                val currentDay = context.time.weekType
                val isDayMatched = (habit.weekType and (1 shl currentDay)) != 0
                if (!isDayMatched) return false
            }

            // 2. 检查时间窗口
            if (!habit.timeWindow.isNullOrEmpty()) {
                if (!checkTimeWindow(habit.timeWindow, context.time)) return false
            }

            // 3. 检查环境条件
            if (!habit.environmentThreshold.isNullOrEmpty()) {
                if (!checkEnvironmentCondition(habit.environmentThreshold, context.environment)) return false
            }

            // 4. 检查用户状态
            if (!context.userState.isAtHome) {
                return false
            }

            return true

        } catch (e: Exception) {
            Timber.e(e, "AutoControlEngine: Failed to check habit conditions")
            return false
        }
    }

    private fun checkTimeWindow(timeWindow: String, time: TimeContext): Boolean {
        try {
            val parts = timeWindow.split("-")
            if (parts.size != 2) return false

            val startParts = parts[0].split(":")
            val endParts = parts[1].split(":")

            val startHour = startParts[0].toInt()
            val endHour = endParts[0].toInt()

            val currentMinutes = time.hour * 60 + time.minute
            val startMinutes = startHour * 60
            val endMinutes = endHour * 60 + 59

            return currentMinutes in startMinutes..endMinutes
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * 【企业级修复：环境数据安全断言】
     * 如果设置了环境条件但传感器无数据，宁可不触发，绝不误触。
     */
    private fun checkEnvironmentCondition(environmentStr: String, environment: EnvironmentContext): Boolean {
        try {
            val condition = gson.fromJson(environmentStr, Map::class.java) as? Map<String, Any>
                ?: return true

            val tempCondition = condition["temperature"] as? Map<String, Double>
            if (tempCondition != null) {
                val currentTemp = environment.temperature ?: return false // 没采到数据则不触发
                val minTemp = tempCondition["min"]?.toFloat() ?: Float.MIN_VALUE
                val maxTemp = tempCondition["max"]?.toFloat() ?: Float.MAX_VALUE

                if (currentTemp !in minTemp..maxTemp) return false
            }

            val humidityCondition = condition["humidity"] as? Map<String, Double>
            if (humidityCondition != null) {
                val currentHumidity = environment.humidity ?: return false
                val minHumidity = humidityCondition["min"]?.toFloat() ?: Float.MIN_VALUE
                val maxHumidity = humidityCondition["max"]?.toFloat() ?: Float.MAX_VALUE

                if (currentHumidity !in minHumidity..maxHumidity) return false
            }

            return true
        } catch (e: Exception) {
            Timber.e(e, "Check environment failed")
            return false
        }
    }

    private suspend fun executeAutoControl(habit: UserHabit, username: String) {
        try {
            val device = deviceDao.getDeviceById(habit.deviceId, username) ?: return

            if (com.example.myapp.BuildConfig.IS_MOCK_MODE) {
                delay(500)

                val currentStatusMap = try {
                    gson.fromJson(device.status, MutableMap::class.java) as? MutableMap<String, Any> ?: mutableMapOf()
                } catch (e: Exception) {
                    mutableMapOf<String, Any>()
                }

                val commandMap = try {
                    gson.fromJson(habit.actionCommand, Map::class.java) as? Map<String, Any> ?: emptyMap()
                } catch (e: Exception) {
                    mapOf("power" to habit.actionCommand)
                }

                currentStatusMap.putAll(commandMap)
                val updatedDevice = device.copy(status = gson.toJson(currentStatusMap))

                // 直接更新数据库，Flow 会将变化推送到 HomeFragment
                deviceDao.update(updatedDevice)

                Timber.i("AutoControlEngine: [MOCK] Executed habit ${habit.habitName}, UI will auto-refresh.")
            } else {
                val payload = habit.actionCommand
                mqttClientManager.publish(
                    topic = device.publishTopic,
                    payload = payload,
                    qos = 1
                )
            }

            val log = AutoControlLog(
                habitId = habit.id,
                deviceId = habit.deviceId,
                action = habit.actionCommand,
                triggerReason = "习惯触发: ${habit.habitName}",
                executeTime = System.currentTimeMillis(),
                isSuccess = true,
                resultMessage = "执行成功",
                environmentData = null,
                username = username
            )
            autoControlLogDao.insert(log)

            Timber.i("AutoControlEngine: Executed habit ${habit.habitName} for device ${device.deviceName}")

        } catch (e: Exception) {
            Timber.e(e, "AutoControlEngine: Failed to execute habit ${habit.habitName}")

            val log = AutoControlLog(
                habitId = habit.id,
                deviceId = habit.deviceId,
                action = habit.actionCommand,
                triggerReason = "习惯触发: ${habit.habitName}",
                executeTime = System.currentTimeMillis(),
                isSuccess = false,
                resultMessage = "执行失败: ${e.message}",
                environmentData = null,
                username = username
            )
            autoControlLogDao.insert(log)
        }
    }

    private data class ControlContext(
        val time: TimeContext,
        val environment: EnvironmentContext,
        val userState: UserStateContext,
        val habits: List<UserHabit>
    )

    private data class TimeContext(
        val hour: Int = 0,
        val minute: Int = 0,
        val weekType: Int = 0
    )

    private data class EnvironmentContext(
        val temperature: Float? = null,
        val humidity: Float? = null,
        val lightIntensity: Float? = null,
        val pm25: Float? = null
    )

    private data class UserStateContext(
        val isAtHome: Boolean = true
    )
}