package com.example.myapp.data.repository

import com.example.myapp.data.local.dao.AutoControlLogDao
import com.example.myapp.data.local.entity.AutoControlLog
import com.example.myapp.domain.base.BaseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 自动控制日志 Repository
 * 处理自动控制日志相关的业务逻辑
 */
@Singleton
class AutoControlLogRepository @Inject constructor(
    private val autoControlLogDao: AutoControlLogDao
) : BaseRepository() {

    /**
     * 记录自动控制日志
     */
    suspend fun logAutoControl(log: AutoControlLog): Result<Long> = safeDatabaseCall {
        autoControlLogDao.insert(log)
    }

    /**
     * 获取所有自动控制日志
     */
    fun getAllLogs(username: String): Flow<List<AutoControlLog>> {
        return autoControlLogDao.getAllLogs(username)
    }

    /**
     * 获取设备的自动控制日志
     */
    fun getLogsByDevice(deviceId: Long, username: String): Flow<List<AutoControlLog>> {
        return autoControlLogDao.getLogsByDevice(deviceId, username)
    }

    /**
     * 获取习惯触发的日志
     */
    fun getLogsByHabit(habitId: Long, username: String): Flow<List<AutoControlLog>> {
        return autoControlLogDao.getLogsByHabit(habitId, username)
    }

    /**
     * 获取时间范围内的日志
     */
    fun getLogsByTimeRange(startTime: Long, endTime: Long, username: String): Flow<List<AutoControlLog>> {
        return autoControlLogDao.getLogsByTimeRange(startTime, endTime, username)
    }

    /**
     * 获取成功的日志
     */
    fun getSuccessLogs(username: String): Flow<List<AutoControlLog>> {
        return autoControlLogDao.getSuccessLogs(username)
    }

    /**
     * 获取失败的日志
     */
    fun getFailedLogs(username: String): Flow<List<AutoControlLog>> {
        return autoControlLogDao.getFailedLogs(username)
    }

    /**
     * 获取习惯的成功率
     */
    suspend fun getSuccessRateByHabit(habitId: Long, username: String): Result<Float?> = safeDatabaseCall {
        autoControlLogDao.getSuccessRateByHabit(habitId, username)
    }

    /**
     * 获取最近的日志
     */
    fun getRecentLogs(limit: Int, username: String): Flow<List<AutoControlLog>> {
        return autoControlLogDao.getRecentLogs(limit, username)
    }

    /**
     * 获取统计数据
     */
    suspend fun getStatistics(username: String): Result<ControlStatistics> = safeDatabaseCall {
        val totalCount = autoControlLogDao.getLogCount(username)
        val successCount = autoControlLogDao.getSuccessLogCount(username)
        val failedCount = autoControlLogDao.getFailedLogCount(username)
        val successRate = if (totalCount > 0) successCount.toFloat() / totalCount else 0f

        ControlStatistics(
            totalCount = totalCount,
            successCount = successCount,
            failedCount = failedCount,
            successRate = successRate
        )
    }

    /**
     * 清理旧日志
     */
    suspend fun cleanOldLogs(beforeTimestamp: Long, username: String): Result<Int> = safeDatabaseCall {
        autoControlLogDao.deleteLogsBeforeTime(beforeTimestamp, username)
    }

    /**
     * 删除设备的日志
     */
    suspend fun deleteLogsByDevice(deviceId: Long, username: String): Result<Int> = safeDatabaseCall {
        autoControlLogDao.deleteLogsByDevice(deviceId, username)
    }
}

/**
 * 控制统计数据
 */
data class ControlStatistics(
    val totalCount: Int,
    val successCount: Int,
    val failedCount: Int,
    val successRate: Float
)


