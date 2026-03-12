package com.example.myapp.mock

import com.example.myapp.data.local.dao.UserHabitLogDao
import com.example.myapp.data.local.entity.UserHabitLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Mock 测试数据生成器
 * 
 * 简洁、高效的测试数据生成工具
 * 用于快速生成测试数据以体验 App 功能
 */
@Singleton
class MockTestDataGenerator @Inject constructor(
    private val userHabitLogDao: UserHabitLogDao
) {
    
    /**
     * 生成 10 条测试习惯日志
     * 
     * @param username 用户名
     * @param deviceId 设备 ID
     * @return 生成的日志数量
     */
    suspend fun generateTestHabitLogs(
        username: String,
        deviceId: Long
    ): Int = withContext(Dispatchers.IO) {
        try {
            Timber.d("[MOCK] Generating test habit logs for device: $deviceId")
            
            val logs = mutableListOf<UserHabitLog>()
            val now = System.currentTimeMillis()
            val oneDayMillis = 24 * 60 * 60 * 1000L
            
            // 生成 10 条测试数据（最近 3 天）
            for (i in 0 until 10) {
                val dayOffset = i / 3  // 每天 3-4 条
                val hourOffset = (i % 3) * 6 + 7  // 7:00, 13:00, 19:00
                
                val timestamp = now - (dayOffset * oneDayMillis) + (hourOffset * 60 * 60 * 1000L)
                val action = if (hourOffset < 12) "turn_on" else if (hourOffset < 20) "turn_on" else "turn_off"
                val temp = 20f + Random.nextFloat() * 5f
                val humidity = 50f + Random.nextFloat() * 10f
                
                logs.add(createHabitLog(username, deviceId, timestamp, action, temp, humidity))
            }
            
            // 批量插入
            logs.forEach { log ->
                userHabitLogDao.insert(log)
            }
            
            Timber.i("[MOCK] Generated ${logs.size} test habit logs")
            logs.size
            
        } catch (e: Exception) {
            Timber.e(e, "[MOCK] Failed to generate test habit logs")
            throw e
        }
    }
    
    /**
     * 创建习惯日志
     */
    private fun createHabitLog(
        username: String,
        deviceId: Long,
        timestamp: Long,
        action: String,
        temperature: Float,
        humidity: Float
    ): UserHabitLog {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        val weekType = if (dayOfWeek == java.util.Calendar.SUNDAY) 7 else dayOfWeek - 1
        
        val environmentData = """{"temperature":$temperature,"humidity":$humidity,"light":${Random.nextFloat() * 300 + 200}}"""
        
        return UserHabitLog(
            username = username,
            deviceId = deviceId,
            action = action,
            operateTime = timestamp,
            weekType = weekType,
            environmentData = environmentData,
            isManualCancel = false
        )
    }
    
    /**
     * 清除所有测试数据
     */
    suspend fun clearAllTestData(username: String): Int = withContext(Dispatchers.IO) {
        try {
            val count = userHabitLogDao.deleteAllByUsername(username)
            Timber.i("[MOCK] Cleared $count test habit logs")
            count
        } catch (e: Exception) {
            Timber.e(e, "[MOCK] Failed to clear test data")
            throw e
        }
    }
}














