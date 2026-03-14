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

    suspend fun logUserOperation(log: UserHabitLog): Result<Long> = safeDatabaseCall {
        userHabitLogDao.insert(log)
    }

    fun getLogsByDevice(deviceId: Long, username: String): Flow<List<UserHabitLog>> {
        return userHabitLogDao.getLogsByDevice(deviceId, username)
    }

    fun getLogsByTimeRange(startTime: Long, endTime: Long, username: String): Flow<List<UserHabitLog>> {
        return userHabitLogDao.getLogsByTimeRangeFlow(startTime, endTime, username)
    }

    /**
     * 【AI引擎专用】：一次性获取时间范围内的所有操作日志
     */
    suspend fun getLogsByTimeRangeOnce(startTime: Long, endTime: Long, username: String): Result<List<UserHabitLog>> = safeDatabaseCall {
        userHabitLogDao.getLogsByTimeRange(startTime, endTime, username)
    }

    suspend fun getLogsForLearning(
        deviceId: Long,
        startTime: Long,
        endTime: Long,
        username: String
    ): Result<List<UserHabitLog>> = safeDatabaseCall {
        userHabitLogDao.getLogsForLearning(deviceId, startTime, endTime, username)
    }

    suspend fun cleanOldLogs(beforeTimestamp: Long, username: String): Result<Int> = safeDatabaseCall {
        userHabitLogDao.deleteLogsBeforeTime(beforeTimestamp, username)
    }

    // ==================== 习惯模型相关 ====================

    suspend fun saveHabit(habit: UserHabit): Result<Long> = safeDatabaseCall {
        userHabitDao.insert(habit)
    }

    fun getAllHabits(username: String): Flow<List<UserHabit>> {
        return userHabitDao.getAllHabits(username)
    }

    fun getEnabledHabits(username: String): Flow<List<UserHabit>> {
        return userHabitDao.getEnabledHabits(username)
    }

    fun getHabitsByDevice(deviceId: Long, username: String): Flow<List<UserHabit>> {
        return userHabitDao.getHabitsByDevice(deviceId, username)
    }

    suspend fun getTriggableHabits(weekType: Int, username: String): Result<List<UserHabit>> = safeDatabaseCall {
        userHabitDao.getTriggableHabits(weekType, username)
    }

    suspend fun updateHabitEnabled(habitId: Long, isEnabled: Boolean, username: String): Result<Int> = safeDatabaseCall {
        userHabitDao.updateHabitEnabled(habitId, isEnabled, username)
    }

    suspend fun updateHabitConfidence(habitId: Long, confidence: Double, username: String): Result<Int> = safeDatabaseCall {
        userHabitDao.updateHabitConfidence(habitId, confidence, System.currentTimeMillis(), username)
    }

    fun getHighConfidenceHabits(minConfidence: Double, username: String): Flow<List<UserHabit>> {
        return userHabitDao.getHighConfidenceHabits(minConfidence, username)
    }

    suspend fun deleteLowConfidenceHabits(minConfidence: Double, username: String): Result<Int> = safeDatabaseCall {
        userHabitDao.deleteLowConfidenceHabits(minConfidence, username)
    }

    suspend fun deleteHabitsByDevice(deviceId: Long, username: String): Result<Int> = safeDatabaseCall {
        userHabitDao.deleteHabitsByDevice(deviceId, username)
    }

    suspend fun addHabit(habit: UserHabit): Result<Long> = safeDatabaseCall {
        userHabitDao.insert(habit)
    }

    suspend fun deleteHabit(habit: UserHabit): Result<Int> = safeDatabaseCall {
        userHabitDao.delete(habit)
    }

    suspend fun getHabitsByUsernameOnce(username: String): Result<List<UserHabit>> = safeDatabaseCall {
        userHabitDao.getAllHabitsOnce(username)
    }

    /**
     * 【AI引擎专用】：智能合并与保存 AI 提取的习惯规则 (草稿状态)
     */
    suspend fun mergeAndSaveAIHabits(newHabits: List<UserHabit>, username: String): Result<Unit> = safeDatabaseCall {
        for (newHabit in newHabits) {
            val existingHabit = userHabitDao.getHabitByDeviceAndAction(newHabit.deviceId, newHabit.actionCommand, username)

            if (existingHabit == null) {
                // 不存在该动作的规律：作为全新草稿插入（引擎内部已设 isEnabled = false）
                userHabitDao.insert(newHabit)
            } else {
                // 已存在该动作的规律：仅平滑更新周期、环境和置信度，绝不覆盖用户自定义的名字和开关状态
                val updatedHabit = existingHabit.copy(
                    timeWindow = newHabit.timeWindow,
                    weekType = newHabit.weekType,
                    environmentThreshold = newHabit.environmentThreshold,
                    confidence = newHabit.confidence
                )
                userHabitDao.update(updatedHabit)
            }
        }
    }
}