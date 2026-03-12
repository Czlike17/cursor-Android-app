package com.example.myapp.data.repository

import com.example.myapp.data.local.dao.UserHabitDao
import com.example.myapp.data.local.dao.UserHabitLogDao
import com.example.myapp.data.local.entity.UserHabit
import com.example.myapp.data.local.entity.UserHabitLog
import com.example.myapp.domain.base.BaseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户习惯 Repository
 * 处理用户习惯学习和操作日志相关的业务逻辑
 */
@Singleton
class UserHabitRepository @Inject constructor(
    private val userHabitDao: UserHabitDao,
    private val userHabitLogDao: UserHabitLogDao
) : BaseRepository() {

    // ==================== 操作日志相关 ====================

    /**
     * 记录用户操作
     */
    suspend fun logUserOperation(log: UserHabitLog): Result<Long> = safeDatabaseCall {
        userHabitLogDao.insert(log)
    }

    /**
     * 获取设备的操作日志
     */
    fun getLogsByDevice(deviceId: Long, username: String): Flow<List<UserHabitLog>> {
        return userHabitLogDao.getLogsByDevice(deviceId, username)
    }

    /**
     * 获取时间范围内的操作日志
     */
    fun getLogsByTimeRange(startTime: Long, endTime: Long, username: String): Flow<List<UserHabitLog>> {
        return userHabitLogDao.getLogsByTimeRangeFlow(startTime, endTime, username)
    }

    /**
     * 获取用于习惯学习的日志数据
     */
    suspend fun getLogsForLearning(
        deviceId: Long,
        startTime: Long,
        endTime: Long,
        username: String
    ): Result<List<UserHabitLog>> = safeDatabaseCall {
        userHabitLogDao.getLogsForLearning(deviceId, startTime, endTime, username)
    }

    /**
     * 清理旧日志
     */
    suspend fun cleanOldLogs(beforeTimestamp: Long, username: String): Result<Int> = safeDatabaseCall {
        userHabitLogDao.deleteLogsBeforeTime(beforeTimestamp, username)
    }

    // ==================== 习惯模型相关 ====================

    /**
     * 保存或更新习惯
     */
    suspend fun saveHabit(habit: UserHabit): Result<Long> = safeDatabaseCall {
        userHabitDao.insert(habit)
    }

    /**
     * 获取所有习惯
     */
    fun getAllHabits(username: String): Flow<List<UserHabit>> {
        return userHabitDao.getAllHabits(username)
    }

    /**
     * 获取已启用的习惯
     */
    fun getEnabledHabits(username: String): Flow<List<UserHabit>> {
        return userHabitDao.getEnabledHabits(username)
    }

    /**
     * 获取设备的习惯
     */
    fun getHabitsByDevice(deviceId: Long, username: String): Flow<List<UserHabit>> {
        return userHabitDao.getHabitsByDevice(deviceId, username)
    }

    /**
     * 获取可触发的习惯
     */
    suspend fun getTriggableHabits(
        weekType: Int,
        username: String
    ): Result<List<UserHabit>> = safeDatabaseCall {
        userHabitDao.getTriggableHabits(weekType, username)
    }

    /**
     * 更新习惯启用状态
     */
    suspend fun updateHabitEnabled(
        habitId: Long,
        isEnabled: Boolean,
        username: String
    ): Result<Int> = safeDatabaseCall {
        userHabitDao.updateHabitEnabled(habitId, isEnabled, username)
    }

    /**
     * 更新习惯置信度
     */
    suspend fun updateHabitConfidence(
        habitId: Long,
        confidence: Double,
        username: String
    ): Result<Int> = safeDatabaseCall {
        userHabitDao.updateHabitConfidence(habitId, confidence, System.currentTimeMillis(), username)
    }

    /**
     * 获取高置信度习惯
     */
    fun getHighConfidenceHabits(minConfidence: Double, username: String): Flow<List<UserHabit>> {
        return userHabitDao.getHighConfidenceHabits(minConfidence, username)
    }

    /**
     * 删除低置信度习惯
     */
    suspend fun deleteLowConfidenceHabits(minConfidence: Double, username: String): Result<Int> = safeDatabaseCall {
        userHabitDao.deleteLowConfidenceHabits(minConfidence, username)
    }

    /**
     * 删除设备的所有习惯
     */
    suspend fun deleteHabitsByDevice(deviceId: Long, username: String): Result<Int> = safeDatabaseCall {
        userHabitDao.deleteHabitsByDevice(deviceId, username)
    }
    
    /**
     * 添加习惯
     */
    suspend fun addHabit(habit: UserHabit): Result<Long> = safeDatabaseCall {
        userHabitDao.insert(habit)
    }
    
    /**
     * 删除习惯
     */
    suspend fun deleteHabit(habit: UserHabit): Result<Int> = safeDatabaseCall {
        userHabitDao.delete(habit)
    }
    
    /**
     * 获取用户的所有习惯（一次性查询）
     */
    suspend fun getHabitsByUsernameOnce(username: String): Result<List<UserHabit>> = safeDatabaseCall {
        userHabitDao.getAllHabitsOnce(username)
    }
}


