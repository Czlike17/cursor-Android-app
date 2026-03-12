package com.example.myapp.data.local.dao

import androidx.room.*
import com.example.myapp.data.local.entity.EnvironmentCache
import kotlinx.coroutines.flow.Flow

/**
 * 环境数据缓存 DAO
 * 所有查询方法强制传入 username 参数，实现多账号数据隔离
 */
@Dao
interface EnvironmentCacheDao {

    /**
     * 插入环境数据
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cache: EnvironmentCache): Long

    /**
     * 批量插入环境数据
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(caches: List<EnvironmentCache>): List<Long>

    /**
     * 更新环境数据
     */
    @Update
    suspend fun update(cache: EnvironmentCache): Int

    /**
     * 删除环境数据
     */
    @Delete
    suspend fun delete(cache: EnvironmentCache): Int

    /**
     * 根据 ID 查询环境数据
     */
    @Query("SELECT * FROM environment_cache WHERE id = :id AND username = :username LIMIT 1")
    suspend fun getCacheById(id: Long, username: String): EnvironmentCache?

    /**
     * 获取指定用户的所有环境数据
     */
    @Query("SELECT * FROM environment_cache WHERE username = :username ORDER BY timestamp DESC")
    fun getAllCaches(username: String): Flow<List<EnvironmentCache>>

    /**
     * 获取指定设备的环境数据
     */
    @Query("SELECT * FROM environment_cache WHERE device_id = :deviceId AND username = :username ORDER BY timestamp DESC")
    fun getCachesByDevice(deviceId: Long, username: String): Flow<List<EnvironmentCache>>

    /**
     * 获取指定设备最新的环境数据
     */
    @Query("SELECT * FROM environment_cache WHERE device_id = :deviceId AND username = :username ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestCache(deviceId: Long, username: String): EnvironmentCache?

    /**
     * 获取指定设备最新的环境数据（Flow）
     */
    @Query("SELECT * FROM environment_cache WHERE device_id = :deviceId AND username = :username ORDER BY timestamp DESC LIMIT 1")
    fun getLatestCacheFlow(deviceId: Long, username: String): Flow<EnvironmentCache?>

    /**
     * 获取指定时间范围内的环境数据
     */
    @Query("""
        SELECT * FROM environment_cache 
        WHERE timestamp BETWEEN :startTime AND :endTime 
        AND username = :username 
        ORDER BY timestamp DESC
    """)
    fun getCachesByTimeRange(startTime: Long, endTime: Long, username: String): Flow<List<EnvironmentCache>>

    /**
     * 获取指定设备在时间范围内的环境数据
     */
    @Query("""
        SELECT * FROM environment_cache 
        WHERE device_id = :deviceId 
        AND timestamp BETWEEN :startTime AND :endTime 
        AND username = :username 
        ORDER BY timestamp ASC
    """)
    suspend fun getCachesByDeviceAndTimeRange(
        deviceId: Long, 
        startTime: Long, 
        endTime: Long, 
        username: String
    ): List<EnvironmentCache>

    /**
     * 获取最近 N 条环境数据
     */
    @Query("SELECT * FROM environment_cache WHERE username = :username ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentCaches(limit: Int, username: String): Flow<List<EnvironmentCache>>

    /**
     * 获取指定设备最近 N 条环境数据
     */
    @Query("SELECT * FROM environment_cache WHERE device_id = :deviceId AND username = :username ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentCachesByDevice(deviceId: Long, limit: Int, username: String): Flow<List<EnvironmentCache>>

    /**
     * 获取环境数据总数
     */
    @Query("SELECT COUNT(*) FROM environment_cache WHERE username = :username")
    suspend fun getCacheCount(username: String): Int

    /**
     * 获取指定设备的环境数据数量
     */
    @Query("SELECT COUNT(*) FROM environment_cache WHERE device_id = :deviceId AND username = :username")
    suspend fun getCacheCountByDevice(deviceId: Long, username: String): Int

    /**
     * 删除指定时间之前的环境数据（数据清理）
     */
    @Query("DELETE FROM environment_cache WHERE timestamp < :timestamp AND username = :username")
    suspend fun deleteCachesBeforeTime(timestamp: Long, username: String): Int

    /**
     * 删除指定设备的所有环境数据
     */
    @Query("DELETE FROM environment_cache WHERE device_id = :deviceId AND username = :username")
    suspend fun deleteCachesByDevice(deviceId: Long, username: String): Int

    /**
     * 删除指定用户的所有环境数据
     */
    @Query("DELETE FROM environment_cache WHERE username = :username")
    suspend fun deleteAllByUsername(username: String): Int

    /**
     * 获取指定设备的平均温度
     */
    @Query("""
        SELECT AVG(temperature) FROM environment_cache 
        WHERE device_id = :deviceId 
        AND username = :username 
        AND temperature IS NOT NULL
    """)
    suspend fun getAverageTemperature(deviceId: Long, username: String): Float?

    /**
     * 获取指定设备的平均湿度
     */
    @Query("""
        SELECT AVG(humidity) FROM environment_cache 
        WHERE device_id = :deviceId 
        AND username = :username 
        AND humidity IS NOT NULL
    """)
    suspend fun getAverageHumidity(deviceId: Long, username: String): Float?
    
    /**
     * 获取指定用户最新的环境数据
     */
    @Query("SELECT * FROM environment_cache WHERE username = :username ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestByUsername(username: String): EnvironmentCache?
    
    /**
     * 删除指定时间之前的旧数据
     */
    @Query("DELETE FROM environment_cache WHERE timestamp < :expireTime AND username = :username")
    suspend fun deleteOldData(expireTime: Long, username: String): Int
}


