package com.example.myapp.data.local.dao

import androidx.room.*
import com.example.myapp.data.local.entity.Device
import kotlinx.coroutines.flow.Flow

/**
 * 设备 DAO
 * 所有查询方法强制传入 username 参数，实现多账号数据隔离
 */
@Dao
interface DeviceDao {

    /**
     * 插入设备
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: Device): Long

    /**
     * 批量插入设备
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(devices: List<Device>): List<Long>

    /**
     * 更新设备
     */
    @Update
    suspend fun update(device: Device): Int

    /**
     * 删除设备
     */
    @Delete
    suspend fun delete(device: Device): Int

    /**
     * 根据设备 ID 和用户名查询设备
     */
    @Query("SELECT * FROM device WHERE device_id = :deviceId AND username = :username LIMIT 1")
    suspend fun getDeviceById(deviceId: Long, username: String): Device?

    /**
     * 根据设备 ID 和用户名查询设备（Flow）
     */
    @Query("SELECT * FROM device WHERE device_id = :deviceId AND username = :username LIMIT 1")
    fun getDeviceByIdFlow(deviceId: Long, username: String): Flow<Device?>

    /**
     * 获取指定用户的所有设备
     */
    @Query("SELECT * FROM device WHERE username = :username ORDER BY create_time DESC")
    fun getAllDevices(username: String): Flow<List<Device>>

    /**
     * 获取指定用户的所有设备（一次性查询）
     */
    @Query("SELECT * FROM device WHERE username = :username ORDER BY create_time DESC")
    suspend fun getAllDevicesOnce(username: String): List<Device>

    /**
     * 获取指定用户在线的设备
     */
    @Query("SELECT * FROM device WHERE username = :username AND is_online = 1 ORDER BY create_time DESC")
    fun getOnlineDevices(username: String): Flow<List<Device>>

    /**
     * 获取指定用户离线的设备
     */
    @Query("SELECT * FROM device WHERE username = :username AND is_online = 0 ORDER BY create_time DESC")
    fun getOfflineDevices(username: String): Flow<List<Device>>

    /**
     * 根据房间 ID 获取设备
     */
    @Query("SELECT * FROM device WHERE room_id = :roomId AND username = :username ORDER BY create_time DESC")
    fun getDevicesByRoom(roomId: Long, username: String): Flow<List<Device>>

    /**
     * 根据设备类型获取设备
     */
    @Query("SELECT * FROM device WHERE device_type = :deviceType AND username = :username ORDER BY create_time DESC")
    fun getDevicesByType(deviceType: String, username: String): Flow<List<Device>>

    /**
     * 更新设备在线状态
     */
    @Query("UPDATE device SET is_online = :isOnline WHERE device_id = :deviceId AND username = :username")
    suspend fun updateOnlineStatus(deviceId: Long, isOnline: Boolean, username: String): Int

    /**
     * 更新设备状态
     */
    @Query("UPDATE device SET status = :status WHERE device_id = :deviceId AND username = :username")
    suspend fun updateDeviceStatus(deviceId: Long, status: String, username: String): Int

    /**
     * 批量更新设备在线状态
     */
    @Query("UPDATE device SET is_online = :isOnline WHERE username = :username")
    suspend fun updateAllOnlineStatus(isOnline: Boolean, username: String): Int

    /**
     * 获取指定用户的设备数量
     */
    @Query("SELECT COUNT(*) FROM device WHERE username = :username")
    suspend fun getDeviceCount(username: String): Int

    /**
     * 根据设备名称搜索
     */
    @Query("SELECT * FROM device WHERE device_name LIKE '%' || :keyword || '%' AND username = :username ORDER BY create_time DESC")
    fun searchDevicesByName(keyword: String, username: String): Flow<List<Device>>

    /**
     * 删除指定用户的所有设备
     */
    @Query("DELETE FROM device WHERE username = :username")
    suspend fun deleteAllByUsername(username: String): Int

    /**
     * 根据设备 ID 删除
     */
    @Query("DELETE FROM device WHERE device_id = :deviceId AND username = :username")
    suspend fun deleteById(deviceId: Long, username: String): Int
}


