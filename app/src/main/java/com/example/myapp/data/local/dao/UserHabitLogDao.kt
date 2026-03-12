package com.example.myapp.data.local.dao

import androidx.room.*
import com.example.myapp.data.local.entity.UserHabitLog
import kotlinx.coroutines.flow.Flow

/**
 * 用户操作日志 DAO
 * 所有查询方法强制传入 username 参数，实现多账号数据隔离
 */
@Dao
interface UserHabitLogDao {

    /**
     * 插入操作日志
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: UserHabitLog): Long

    /**
     * 批量插入操作日志
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<UserHabitLog>): List<Long>

    /**
     * 更新操作日志
     */
    @Update
    suspend fun update(log: UserHabitLog): Int

    /**
     * 删除操作日志
     */
    @Delete
    suspend fun delete(log: UserHabitLog): Int

    /**
     * 根据 ID 查询日志
     */
    @Query("SELECT * FROM user_habit_log WHERE id = :id AND username = :username LIMIT 1")
    suspend fun getLogById(id: Long, username: String): UserHabitLog?

    /**
     * 获取指定用户的所有操作日志
     */
    @Query("SELECT * FROM user_habit_log WHERE username = :username ORDER BY operate_time DESC")
    fun getAllLogs(username: String): Flow<List<UserHabitLog>>

    /**
     * 获取指定设备的操作日志
     */
    @Query("SELECT * FROM user_habit_log WHERE device_id = :deviceId AND username = :username ORDER BY operate_time DESC")
    fun getLogsByDevice(deviceId: Long, username: String): Flow<List<UserHabitLog>>

    /**
     * 获取指定设备和动作的操作日志
     */
    @Query("SELECT * FROM user_habit_log WHERE device_id = :deviceId AND action = :action AND username = :username ORDER BY operate_time DESC")
    fun getLogsByDeviceAndAction(deviceId: Long, action: String, username: String): Flow<List<UserHabitLog>>

    /**
     * 获取指定时间范围内的操作日志（同步方法，用于习惯提取）
     */
    @Query("SELECT * FROM user_habit_log WHERE operate_time BETWEEN :startTime AND :endTime AND username = :username ORDER BY operate_time DESC")
    suspend fun getLogsByTimeRange(startTime: Long, endTime: Long, username: String): List<UserHabitLog>

    /**
     * 获取指定时间范围内的操作日志（Flow）
     */
    @Query("SELECT * FROM user_habit_log WHERE operate_time BETWEEN :startTime AND :endTime AND username = :username ORDER BY operate_time DESC")
    fun getLogsByTimeRangeFlow(startTime: Long, endTime: Long, username: String): Flow<List<UserHabitLog>>

    /**
     * 获取指定设备在时间范围内的操作日志（用于习惯学习）
     */
    @Query("""
        SELECT * FROM user_habit_log 
        WHERE device_id = :deviceId 
        AND operate_time BETWEEN :startTime AND :endTime 
        AND username = :username 
        ORDER BY operate_time ASC
    """)
    suspend fun getLogsForLearning(deviceId: Long, startTime: Long, endTime: Long, username: String): List<UserHabitLog>

    /**
     * 获取指定星期类型的操作日志
     */
    @Query("SELECT * FROM user_habit_log WHERE week_type = :weekType AND username = :username ORDER BY operate_time DESC")
    fun getLogsByWeekType(weekType: Int, username: String): Flow<List<UserHabitLog>>

    /**
     * 获取指定设备、动作和星期类型的日志（用于习惯分析）
     */
    @Query("""
        SELECT * FROM user_habit_log 
        WHERE device_id = :deviceId 
        AND action = :action 
        AND week_type = :weekType 
        AND username = :username 
        ORDER BY operate_time ASC
    """)
    suspend fun getLogsForHabitAnalysis(
        deviceId: Long, 
        action: String, 
        weekType: Int, 
        username: String
    ): List<UserHabitLog>

    /**
     * 获取非手动取消的操作日志
     */
    @Query("""
        SELECT * FROM user_habit_log 
        WHERE device_id = :deviceId 
        AND is_manual_cancel = 0 
        AND username = :username 
        ORDER BY operate_time DESC
    """)
    fun getNonCancelledLogs(deviceId: Long, username: String): Flow<List<UserHabitLog>>

    /**
     * 获取最近 N 条操作日志
     */
    @Query("SELECT * FROM user_habit_log WHERE username = :username ORDER BY operate_time DESC LIMIT :limit")
    fun getRecentLogs(limit: Int, username: String): Flow<List<UserHabitLog>>

    /**
     * 获取操作日志总数
     */
    @Query("SELECT COUNT(*) FROM user_habit_log WHERE username = :username")
    suspend fun getLogCount(username: String): Int

    /**
     * 删除指定时间之前的日志（数据清理）
     */
    @Query("DELETE FROM user_habit_log WHERE operate_time < :timestamp AND username = :username")
    suspend fun deleteLogsBeforeTime(timestamp: Long, username: String): Int

    /**
     * 删除指定设备的所有日志
     */
    @Query("DELETE FROM user_habit_log WHERE device_id = :deviceId AND username = :username")
    suspend fun deleteLogsByDevice(deviceId: Long, username: String): Int

    /**
     * 删除指定用户的所有日志
     */
    @Query("DELETE FROM user_habit_log WHERE username = :username")
    suspend fun deleteAllByUsername(username: String): Int
}


