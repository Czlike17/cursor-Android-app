package com.example.myapp.data.local.dao

import androidx.room.*
import com.example.myapp.data.local.entity.AutoControlLog
import kotlinx.coroutines.flow.Flow

/**
 * 自动控制日志 DAO
 * 所有查询方法强制传入 username 参数，实现多账号数据隔离
 */
@Dao
interface AutoControlLogDao {

    /**
     * 插入自动控制日志
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AutoControlLog): Long

    /**
     * 批量插入自动控制日志
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<AutoControlLog>): List<Long>

    /**
     * 更新自动控制日志
     */
    @Update
    suspend fun update(log: AutoControlLog): Int

    /**
     * 删除自动控制日志
     */
    @Delete
    suspend fun delete(log: AutoControlLog): Int

    /**
     * 根据 ID 查询日志
     */
    @Query("SELECT * FROM auto_control_log WHERE id = :id AND username = :username LIMIT 1")
    suspend fun getLogById(id: Long, username: String): AutoControlLog?

    /**
     * 获取指定用户的所有自动控制日志
     */
    @Query("SELECT * FROM auto_control_log WHERE username = :username ORDER BY execute_time DESC")
    fun getAllLogs(username: String): Flow<List<AutoControlLog>>

    /**
     * 获取指定设备的自动控制日志
     */
    @Query("SELECT * FROM auto_control_log WHERE device_id = :deviceId AND username = :username ORDER BY execute_time DESC")
    fun getLogsByDevice(deviceId: Long, username: String): Flow<List<AutoControlLog>>

    /**
     * 获取指定习惯触发的日志
     */
    @Query("SELECT * FROM auto_control_log WHERE habit_id = :habitId AND username = :username ORDER BY execute_time DESC")
    fun getLogsByHabit(habitId: Long, username: String): Flow<List<AutoControlLog>>

    /**
     * 获取指定时间范围内的日志
     */
    @Query("""
        SELECT * FROM auto_control_log 
        WHERE execute_time BETWEEN :startTime AND :endTime 
        AND username = :username 
        ORDER BY execute_time DESC
    """)
    fun getLogsByTimeRange(startTime: Long, endTime: Long, username: String): Flow<List<AutoControlLog>>

    /**
     * 获取成功执行的日志
     */
    @Query("SELECT * FROM auto_control_log WHERE is_success = 1 AND username = :username ORDER BY execute_time DESC")
    fun getSuccessLogs(username: String): Flow<List<AutoControlLog>>

    /**
     * 获取失败的日志
     */
    @Query("SELECT * FROM auto_control_log WHERE is_success = 0 AND username = :username ORDER BY execute_time DESC")
    fun getFailedLogs(username: String): Flow<List<AutoControlLog>>

    /**
     * 获取指定设备成功执行的日志
     */
    @Query("""
        SELECT * FROM auto_control_log 
        WHERE device_id = :deviceId 
        AND is_success = 1 
        AND username = :username 
        ORDER BY execute_time DESC
    """)
    fun getSuccessLogsByDevice(deviceId: Long, username: String): Flow<List<AutoControlLog>>

    /**
     * 获取指定习惯的成功率
     */
    @Query("""
        SELECT 
            CAST(SUM(CASE WHEN is_success = 1 THEN 1 ELSE 0 END) AS FLOAT) / COUNT(*) 
        FROM auto_control_log 
        WHERE habit_id = :habitId 
        AND username = :username
    """)
    suspend fun getSuccessRateByHabit(habitId: Long, username: String): Float?

    /**
     * 获取最近 N 条日志
     */
    @Query("SELECT * FROM auto_control_log WHERE username = :username ORDER BY execute_time DESC LIMIT :limit")
    fun getRecentLogs(limit: Int, username: String): Flow<List<AutoControlLog>>

    /**
     * 获取日志总数
     */
    @Query("SELECT COUNT(*) FROM auto_control_log WHERE username = :username")
    suspend fun getLogCount(username: String): Int

    /**
     * 获取成功日志数量
     */
    @Query("SELECT COUNT(*) FROM auto_control_log WHERE is_success = 1 AND username = :username")
    suspend fun getSuccessLogCount(username: String): Int

    /**
     * 获取失败日志数量
     */
    @Query("SELECT COUNT(*) FROM auto_control_log WHERE is_success = 0 AND username = :username")
    suspend fun getFailedLogCount(username: String): Int

    /**
     * 获取指定设备的日志数量
     */
    @Query("SELECT COUNT(*) FROM auto_control_log WHERE device_id = :deviceId AND username = :username")
    suspend fun getLogCountByDevice(deviceId: Long, username: String): Int

    /**
     * 删除指定时间之前的日志（数据清理）
     */
    @Query("DELETE FROM auto_control_log WHERE execute_time < :timestamp AND username = :username")
    suspend fun deleteLogsBeforeTime(timestamp: Long, username: String): Int

    /**
     * 删除指定设备的所有日志
     */
    @Query("DELETE FROM auto_control_log WHERE device_id = :deviceId AND username = :username")
    suspend fun deleteLogsByDevice(deviceId: Long, username: String): Int

    /**
     * 删除指定习惯的所有日志
     */
    @Query("DELETE FROM auto_control_log WHERE habit_id = :habitId AND username = :username")
    suspend fun deleteLogsByHabit(habitId: Long, username: String): Int

    /**
     * 删除指定用户的所有日志
     */
    @Query("DELETE FROM auto_control_log WHERE username = :username")
    suspend fun deleteAllByUsername(username: String): Int
    
    /**
     * 获取指定习惯的最新日志
     */
    @Query("SELECT * FROM auto_control_log WHERE habit_id = :habitId AND username = :username ORDER BY execute_time DESC LIMIT 1")
    suspend fun getLatestLogByHabit(habitId: Long, username: String): AutoControlLog?
}


