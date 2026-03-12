package com.example.myapp.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapp.data.local.AppDatabase
import com.example.myapp.data.local.entity.UserHabit
import com.example.myapp.util.PreferencesManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Calendar

/**
 * 习惯提取 Worker
 * 每日凌晨2点运行，分析近30天操作日志，提取高频稳定操作
 */
@HiltWorker
class HabitExtractionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val preferencesManager: PreferencesManager
) : CoroutineWorker(appContext, workerParams) {

    private val database = AppDatabase.getDatabase(appContext)
    private val userHabitLogDao = database.userHabitLogDao()
    private val userHabitDao = database.userHabitDao()

    companion object {
        private const val DAYS_TO_ANALYZE = 30
        private const val MIN_FREQUENCY = 5 // 最少出现5次
        private const val MIN_CONFIDENCE = 0.8 // 最低可信度0.8
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val username = preferencesManager.getUsernameSync() ?: return@withContext Result.failure()
            
            Timber.d("HabitExtraction: Starting for user $username")
            
            // 计算30天前的时间戳
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -DAYS_TO_ANALYZE)
            val startTime = calendar.timeInMillis
            val endTime = System.currentTimeMillis()
            
            // 获取近30天的操作日志
            val logs = userHabitLogDao.getLogsByTimeRange(startTime, endTime, username)
            
            if (logs.isEmpty()) {
                Timber.d("HabitExtraction: No logs found")
                return@withContext Result.success()
            }
            
            // 按"设备+操作"分组
            val groupedLogs = logs.groupBy { "${it.deviceId}_${it.action}" }
            
            // 分析每个分组
            for ((key, groupLogs) in groupedLogs) {
                if (groupLogs.size < MIN_FREQUENCY) {
                    continue // 频率太低，跳过
                }
                
                val deviceId = groupLogs.first().deviceId
                val action = groupLogs.first().action
                
                // 分析时间模式
                val timePattern = analyzeTimePattern(groupLogs)
                
                // 分析环境条件
                val environmentCondition = analyzeEnvironmentCondition(groupLogs)
                
                // 计算可信度
                val confidence = calculateConfidence(groupLogs, timePattern)
                
                if (confidence < MIN_CONFIDENCE) {
                    continue // 可信度太低，跳过
                }
                
                // 检查是否已存在该习惯
                val existingHabit = userHabitDao.getHabitByDeviceAndAction(deviceId, action, username)
                
                if (existingHabit == null) {
                    // 创建新习惯
                    val habit = UserHabit(
                        deviceId = deviceId,
                        habitName = generateHabitName(action),
                        triggerCondition = buildTriggerCondition(timePattern, environmentCondition),
                        actionCommand = action,
                        weekType = timePattern.weekType,
                        timeWindow = timePattern.timeWindow,
                        environmentThreshold = environmentCondition,
                        confidence = confidence,
                        isEnabled = confidence >= MIN_CONFIDENCE, // 可信度>=0.8自动启用
                        username = username
                    )
                    
                    userHabitDao.insert(habit)
                    Timber.d("HabitExtraction: Created habit for device $deviceId, action $action, confidence $confidence")
                } else {
                    // 更新现有习惯
                    val updatedHabit = existingHabit.copy(
                        triggerCondition = buildTriggerCondition(timePattern, environmentCondition),
                        weekType = timePattern.weekType,
                        timeWindow = timePattern.timeWindow,
                        environmentThreshold = environmentCondition,
                        confidence = confidence,
                        isEnabled = if (confidence >= MIN_CONFIDENCE) existingHabit.isEnabled else false
                    )
                    
                    userHabitDao.update(updatedHabit)
                    Timber.d("HabitExtraction: Updated habit for device $deviceId, action $action, confidence $confidence")
                }
            }
            
            Timber.d("HabitExtraction: Completed successfully")
            Result.success()
            
        } catch (e: Exception) {
            Timber.e(e, "HabitExtraction: Failed")
            Result.failure()
        }
    }
    
    /**
     * 分析时间模式
     */
    private fun analyzeTimePattern(logs: List<com.example.myapp.data.local.entity.UserHabitLog>): TimePattern {
        // 统计星期分布
        val weekTypeCount = logs.groupingBy { it.weekType }.eachCount()
        val dominantWeekType = weekTypeCount.maxByOrNull { it.value }?.key ?: 0
        
        // 统计时间分布（小时）
        val hourList = logs.map { log ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = log.operateTime
            calendar.get(Calendar.HOUR_OF_DAY)
        }
        
        val avgHour = hourList.average().toInt()
        val stdDev = calculateStdDev(hourList.map { it.toDouble() })
        
        // 时间窗口：平均时间 ± 标准差
        val timeWindow = "${(avgHour - stdDev.toInt()).coerceIn(0, 23)}:00-${(avgHour + stdDev.toInt()).coerceIn(0, 23)}:59"
        
        return TimePattern(dominantWeekType, timeWindow, avgHour)
    }
    
    /**
     * 分析环境条件
     */
    private fun analyzeEnvironmentCondition(logs: List<com.example.myapp.data.local.entity.UserHabitLog>): String {
        val environmentDataList = logs.mapNotNull { it.environmentData }
        
        if (environmentDataList.isEmpty()) {
            return "{}"
        }
        
        // 解析环境数据并计算平均值
        val tempList = mutableListOf<Float>()
        val humidityList = mutableListOf<Float>()
        
        for (envData in environmentDataList) {
            try {
                val json = org.json.JSONObject(envData)
                if (json.has("temperature")) {
                    tempList.add(json.getDouble("temperature").toFloat())
                }
                if (json.has("humidity")) {
                    humidityList.add(json.getDouble("humidity").toFloat())
                }
            } catch (e: Exception) {
                // 忽略解析错误
            }
        }
        
        val condition = mutableMapOf<String, Any>()
        
        if (tempList.isNotEmpty()) {
            val avgTemp = tempList.average().toFloat()
            condition["temperature"] = mapOf(
                "min" to (avgTemp - 2).toInt(),
                "max" to (avgTemp + 2).toInt()
            )
        }
        
        if (humidityList.isNotEmpty()) {
            val avgHumidity = humidityList.average().toFloat()
            condition["humidity"] = mapOf(
                "min" to (avgHumidity - 10).toInt(),
                "max" to (avgHumidity + 10).toInt()
            )
        }
        
        return com.google.gson.Gson().toJson(condition)
    }
    
    /**
     * 计算可信度
     */
    private fun calculateConfidence(
        logs: List<com.example.myapp.data.local.entity.UserHabitLog>,
        timePattern: TimePattern
    ): Double {
        // 频率得分（0-0.4）
        val frequencyScore = (logs.size.toDouble() / DAYS_TO_ANALYZE).coerceAtMost(0.4)
        
        // 时间稳定性得分（0-0.3）
        val hourList = logs.map { log ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = log.operateTime
            calendar.get(Calendar.HOUR_OF_DAY).toDouble()
        }
        val stdDev = calculateStdDev(hourList)
        val timeStabilityScore = (1.0 - (stdDev / 12.0)).coerceIn(0.0, 0.3)
        
        // 周期规律性得分（0-0.3）
        val weekTypeCount = logs.groupingBy { it.weekType }.eachCount()
        val maxWeekTypeCount = weekTypeCount.maxOfOrNull { it.value } ?: 0
        val weekRegularityScore = (maxWeekTypeCount.toDouble() / logs.size * 0.3).coerceAtMost(0.3)
        
        return frequencyScore + timeStabilityScore + weekRegularityScore
    }
    
    /**
     * 计算标准差
     */
    private fun calculateStdDev(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }
    
    /**
     * 生成习惯名称
     */
    private fun generateHabitName(action: String): String {
        return when {
            action.contains("light") && action.contains("on") -> "开灯习惯"
            action.contains("light") && action.contains("off") -> "关灯习惯"
            action.contains("ac") && action.contains("on") -> "开空调习惯"
            action.contains("ac") && action.contains("off") -> "关空调习惯"
            else -> "自动控制习惯"
        }
    }
    
    /**
     * 构建触发条件
     */
    private fun buildTriggerCondition(timePattern: TimePattern, environmentCondition: String): String {
        val condition = mutableMapOf<String, Any>()
        condition["time"] = timePattern.timeWindow
        condition["weekType"] = timePattern.weekType
        
        if (environmentCondition.isNotEmpty() && environmentCondition != "{}") {
            condition["environment"] = environmentCondition
        }
        
        return com.google.gson.Gson().toJson(condition)
    }
    
    /**
     * 时间模式数据类
     */
    private data class TimePattern(
        val weekType: Int,
        val timeWindow: String,
        val avgHour: Int
    )
}

