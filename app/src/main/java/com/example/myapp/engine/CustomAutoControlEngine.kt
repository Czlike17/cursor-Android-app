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
 * 条件同时满足且习惯启用时，生成指令发布到 MQTT
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
    
    // 引擎运行状态
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    
    /**
     * 启动自动控制引擎
     */
    fun start() {
        if (_isRunning.value) {
            Timber.w("AutoControlEngine: Already running")
            return
        }
        
        _isRunning.value = true
        Timber.i("AutoControlEngine: Starting")
        
        scope.launch {
            val username = preferencesManager.getUsernameSync() ?: return@launch
            
            // 创建四个数据流
            val timeFlow = createTimeFlow()
            val environmentFlow = createEnvironmentFlow(username)
            val userStateFlow = createUserStateFlow()
            val habitFlow = createHabitFlow(username)
            
            // 合并四个数据流
            combine(timeFlow, environmentFlow, userStateFlow, habitFlow) { time, environment, userState, habits ->
                ControlContext(time, environment, userState, habits)
            }.collect { context ->
                processControlContext(context, username)
            }
        }
    }
    
    /**
     * 停止自动控制引擎
     */
    fun stop() {
        _isRunning.value = false
        scope.coroutineContext.cancelChildren()
        Timber.i("AutoControlEngine: Stopped")
    }
    
    /**
     * 记录手动操作（暂停该习惯1小时）
     */
    suspend fun recordManualOperation(deviceId: Long, action: String) {
        val username = preferencesManager.getUsernameSync() ?: return
        
        // 查找对应的习惯
        val habit = userHabitDao.getHabitByDeviceAndAction(deviceId, action, username)
        
        if (habit != null) {
            val pauseUntil = System.currentTimeMillis() + pauseDurationMs
            manualPauseMap[habit.id] = pauseUntil
            Timber.d("AutoControlEngine: Manual operation detected, pausing habit ${habit.id} for 1 hour")
        }
    }
    
    /**
     * 创建时间数据流（每分钟更新）
     */
    private fun createTimeFlow(): Flow<TimeContext> = flow {
        while (currentCoroutineContext().isActive) {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val weekType = calendar.get(Calendar.DAY_OF_WEEK)
            
            emit(TimeContext(hour, minute, weekType))
            delay(60 * 1000) // 每分钟更新
        }
    }
    
    /**
     * 创建环境数据流（每5分钟更新）
     */
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
            delay(5 * 60 * 1000) // 每5分钟更新
        }
    }
    
    /**
     * 创建用户状态数据流（每分钟更新）
     */
    private fun createUserStateFlow(): Flow<UserStateContext> = flow {
        while (currentCoroutineContext().isActive) {
            val isAtHome = preferencesManager.getAtHomeMode().first()
            
            emit(UserStateContext(isAtHome = isAtHome))
            delay(60 * 1000) // 每分钟更新
        }
    }
    
    /**
     * 创建习惯模型数据流（实时监听数据库变化）
     */
    private fun createHabitFlow(username: String): Flow<List<UserHabit>> {
        return userHabitDao.getEnabledHabitsFlow(username)
    }
    
    /**
     * 处理控制上下文
     */
    private suspend fun processControlContext(context: ControlContext, username: String) {
        for (habit in context.habits) {
            // 检查是否被手动操作暂停
            val pauseUntil = manualPauseMap[habit.id]
            if (pauseUntil != null && System.currentTimeMillis() < pauseUntil) {
                continue // 仍在暂停期内
            } else if (pauseUntil != null) {
                manualPauseMap.remove(habit.id) // 暂停期结束
            }
            
            // 检查习惯是否满足触发条件
            if (checkHabitConditions(habit, context)) {
                // 检查是否已经执行过（避免重复执行）
                val lastLog = autoControlLogDao.getLatestLogByHabit(habit.id, username)
                val now = System.currentTimeMillis()
                
                if (lastLog != null && (now - lastLog.executeTime) < 10 * 60 * 1000) {
                    continue // 10分钟内已执行过，跳过
                }
                
                // 执行自动控制
                executeAutoControl(habit, username)
            }
        }
    }
    
    /**
     * 检查习惯触发条件
     */
    private fun checkHabitConditions(habit: UserHabit, context: ControlContext): Boolean {
        try {
            val triggerCondition = gson.fromJson(habit.triggerCondition, Map::class.java) as? Map<String, Any>
                ?: return false
            
            // 检查时间条件
            val timeWindow = triggerCondition["time"] as? String
            if (timeWindow != null && !checkTimeWindow(timeWindow, context.time)) {
                return false
            }
            
            // 检查星期条件
            val weekType = (triggerCondition["weekType"] as? Double)?.toInt()
            if (weekType != null && weekType != 0 && weekType != context.time.weekType) {
                return false
            }
            
            // 检查环境条件
            val environmentStr = triggerCondition["environment"] as? String
            if (environmentStr != null && !checkEnvironmentCondition(environmentStr, context.environment)) {
                return false
            }
            
            // 检查用户状态（在家模式）
            if (!context.userState.isAtHome) {
                return false // 外出模式下不执行自动控制
            }
            
            return true
            
        } catch (e: Exception) {
            Timber.e(e, "AutoControlEngine: Failed to check habit conditions")
            return false
        }
    }
    
    /**
     * 检查时间窗口
     */
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
     * 检查环境条件
     */
    private fun checkEnvironmentCondition(environmentStr: String, environment: EnvironmentContext): Boolean {
        try {
            val condition = gson.fromJson(environmentStr, Map::class.java) as? Map<String, Any>
                ?: return true
            
            // 检查温度
            val tempCondition = condition["temperature"] as? Map<String, Any>
            if (tempCondition != null && environment.temperature != null) {
                val minTemp = (tempCondition["min"] as? Double)?.toFloat() ?: Float.MIN_VALUE
                val maxTemp = (tempCondition["max"] as? Double)?.toFloat() ?: Float.MAX_VALUE
                
                if (environment.temperature !in minTemp..maxTemp) {
                    return false
                }
            }
            
            // 检查湿度
            val humidityCondition = condition["humidity"] as? Map<String, Any>
            if (humidityCondition != null && environment.humidity != null) {
                val minHumidity = (humidityCondition["min"] as? Double)?.toFloat() ?: Float.MIN_VALUE
                val maxHumidity = (humidityCondition["max"] as? Double)?.toFloat() ?: Float.MAX_VALUE
                
                if (environment.humidity !in minHumidity..maxHumidity) {
                    return false
                }
            }
            
            return true
            
        } catch (e: Exception) {
            return true // 解析失败时默认通过
        }
    }
    
    /**
     * 执行自动控制
     */

    private suspend fun executeAutoControl(habit: UserHabit, username: String) {
        try {
            // 获取设备信息
            val device = deviceDao.getDeviceById(habit.deviceId, username) ?: return

            // 【关键闭环 2】：Mock 模式下拦截网络层，直接驱动 Room 数据库
            if (com.example.myapp.BuildConfig.IS_MOCK_MODE) {
                delay(500) // 模拟网络延迟

                // 尝试提取现有状态和指令并合并
                val currentStatusMap = try {
                    gson.fromJson(device.status, MutableMap::class.java) as? MutableMap<String, Any> ?: mutableMapOf()
                } catch (e: Exception) {
                    mutableMapOf<String, Any>()
                }

                val commandMap = try {
                    gson.fromJson(habit.actionCommand, Map::class.java) as? Map<String, Any> ?: emptyMap()
                } catch (e: Exception) {
                    // 容错：如果 action 不是 JSON 格式，默认当作电源开关处理
                    mapOf("power" to habit.actionCommand)
                }

                currentStatusMap.putAll(commandMap)
                val updatedDevice = device.copy(status = gson.toJson(currentStatusMap))

                // 直接更新数据库，这会触发 HomeFragment 里的 Flow，实现首页自动翻转开关！
                deviceDao.update(updatedDevice)

                Timber.i("AutoControlEngine: [MOCK] Executed habit ${habit.habitName}, UI will auto-refresh.")
            } else {
                // 生产环境：发送真实的 MQTT 指令
                val payload = habit.actionCommand
                mqttClientManager.publish(
                    topic = device.publishTopic,
                    payload = payload,
                    qos = 1
                )
            }

            // 记录自动控制日志
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

            // 记录失败日志
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
    
    /**
     * 控制上下文
     */
    private data class ControlContext(
        val time: TimeContext,
        val environment: EnvironmentContext,
        val userState: UserStateContext,
        val habits: List<UserHabit>
    )
    
    /**
     * 时间上下文
     */
    private data class TimeContext(
        val hour: Int = 0,
        val minute: Int = 0,
        val weekType: Int = 0
    )
    
    /**
     * 环境上下文
     */
    private data class EnvironmentContext(
        val temperature: Float? = null,
        val humidity: Float? = null,
        val lightIntensity: Float? = null,
        val pm25: Float? = null
    )
    
    /**
     * 用户状态上下文
     */
    private data class UserStateContext(
        val isAtHome: Boolean = true
    )
}

