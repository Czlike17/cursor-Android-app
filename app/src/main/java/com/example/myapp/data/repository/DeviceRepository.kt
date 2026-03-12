package com.example.myapp.data.repository

import com.example.myapp.data.local.dao.DeviceDao
import com.example.myapp.data.local.entity.Device
import com.example.myapp.domain.base.BaseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 设备 Repository
 * 处理设备相关的业务逻辑，所有方法强制传入 username 实现数据隔离
 */
@Singleton
class DeviceRepository @Inject constructor(
    private val deviceDao: DeviceDao
) : BaseRepository() {

    /**
     * 添加设备
     */
    suspend fun addDevice(device: Device): Result<Long> = safeDatabaseCall {
        deviceDao.insert(device)
    }

    /**
     * 插入设备
     */
    suspend fun insertDevice(device: Device): Result<Long> = safeDatabaseCall {
        deviceDao.insert(device)
    }

    /**
     * 更新设备
     */
    suspend fun updateDevice(device: Device): Result<Int> = safeDatabaseCall {
        deviceDao.update(device)
    }

    /**
     * 删除设备
     */
    suspend fun deleteDevice(device: Device): Result<Int> = safeDatabaseCall {
        deviceDao.delete(device)
    }

    /**
     * 根据 ID 获取设备
     */
    suspend fun getDeviceById(deviceId: Long, username: String): Result<Device?> = safeDatabaseCall {
        deviceDao.getDeviceById(deviceId, username)
    }

    /**
     * 根据 ID 获取设备（Flow）
     */
    fun getDeviceByIdFlow(deviceId: Long, username: String): Flow<Device?> {
        return deviceDao.getDeviceByIdFlow(deviceId, username)
    }

    /**
     * 获取所有设备
     */
    fun getAllDevices(username: String): Flow<List<Device>> {
        return deviceDao.getAllDevices(username)
    }

    /**
     * 根据用户名获取设备列表（Flow，自动监听变化）
     */
    fun getDevicesByUsername(username: String): Flow<List<Device>> {
        return deviceDao.getAllDevices(username)
    }

    /**
     * 根据用户名获取设备列表（一次性）
     */
    suspend fun getDevicesByUsernameOnce(username: String): Result<List<Device>> = safeDatabaseCall {
        deviceDao.getAllDevicesOnce(username)
    }

    /**
     * 获取在线设备
     */
    fun getOnlineDevices(username: String): Flow<List<Device>> {
        return deviceDao.getOnlineDevices(username)
    }

    /**
     * 获取离线设备
     */
    fun getOfflineDevices(username: String): Flow<List<Device>> {
        return deviceDao.getOfflineDevices(username)
    }

    /**
     * 根据房间获取设备
     */
    fun getDevicesByRoom(roomId: Long, username: String): Flow<List<Device>> {
        return deviceDao.getDevicesByRoom(roomId, username)
    }

    /**
     * 根据类型获取设备
     */
    fun getDevicesByType(deviceType: String, username: String): Flow<List<Device>> {
        return deviceDao.getDevicesByType(deviceType, username)
    }

    /**
     * 更新设备在线状态
     */
    suspend fun updateOnlineStatus(deviceId: Long, isOnline: Boolean, username: String): Result<Int> = safeDatabaseCall {
        deviceDao.updateOnlineStatus(deviceId, isOnline, username)
    }

    /**
     * 更新设备状态
     */
    suspend fun updateDeviceStatus(deviceId: Long, status: String, username: String): Result<Int> = safeDatabaseCall {
        deviceDao.updateDeviceStatus(deviceId, status, username)
    }

    /**
     * 搜索设备
     */
    fun searchDevicesByName(keyword: String, username: String): Flow<List<Device>> {
        return deviceDao.searchDevicesByName(keyword, username)
    }

    /**
     * 获取设备数量
     */
    suspend fun getDeviceCount(username: String): Result<Int> = safeDatabaseCall {
        deviceDao.getDeviceCount(username)
    }

    /**
     * 删除指定设备
     */
    suspend fun deleteDeviceById(deviceId: Long, username: String): Result<Int> = safeDatabaseCall {
        deviceDao.deleteById(deviceId, username)
    }
}


