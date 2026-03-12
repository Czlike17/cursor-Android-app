package com.example.myapp.data.repository

import com.example.myapp.data.local.dao.EnvironmentCacheDao
import com.example.myapp.data.local.entity.EnvironmentCache
import com.example.myapp.domain.base.BaseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 环境数据 Repository
 * 处理环境数据缓存相关的业务逻辑
 */
@Singleton
class EnvironmentRepository @Inject constructor(
    private val environmentCacheDao: EnvironmentCacheDao
) : BaseRepository() {

    /**
     * 保存环境数据
     */
    suspend fun saveEnvironmentData(cache: EnvironmentCache): Result<Long> = safeDatabaseCall {
        environmentCacheDao.insert(cache)
    }

    /**
     * 批量保存环境数据
     */
    suspend fun saveEnvironmentDataBatch(caches: List<EnvironmentCache>): Result<List<Long>> = safeDatabaseCall {
        environmentCacheDao.insertAll(caches)
    }

    /**
     * 获取设备最新的环境数据
     */
    suspend fun getLatestEnvironmentData(deviceId: Long, username: String): Result<EnvironmentCache?> = safeDatabaseCall {
        environmentCacheDao.getLatestCache(deviceId, username)
    }

    /**
     * 获取设备最新的环境数据（Flow）
     */
    fun getLatestEnvironmentDataFlow(deviceId: Long, username: String): Flow<EnvironmentCache?> {
        return environmentCacheDao.getLatestCacheFlow(deviceId, username)
    }

    /**
     * 获取设备的环境数据
     */
    fun getEnvironmentDataByDevice(deviceId: Long, username: String): Flow<List<EnvironmentCache>> {
        return environmentCacheDao.getCachesByDevice(deviceId, username)
    }

    /**
     * 获取时间范围内的环境数据
     */
    fun getEnvironmentDataByTimeRange(
        startTime: Long,
        endTime: Long,
        username: String
    ): Flow<List<EnvironmentCache>> {
        return environmentCacheDao.getCachesByTimeRange(startTime, endTime, username)
    }

    /**
     * 获取设备在时间范围内的环境数据
     */
    suspend fun getEnvironmentDataByDeviceAndTimeRange(
        deviceId: Long,
        startTime: Long,
        endTime: Long,
        username: String
    ): Result<List<EnvironmentCache>> = safeDatabaseCall {
        environmentCacheDao.getCachesByDeviceAndTimeRange(deviceId, startTime, endTime, username)
    }

    /**
     * 获取最近的环境数据
     */
    fun getRecentEnvironmentData(limit: Int, username: String): Flow<List<EnvironmentCache>> {
        return environmentCacheDao.getRecentCaches(limit, username)
    }

    /**
     * 获取设备的平均温度
     */
    suspend fun getAverageTemperature(deviceId: Long, username: String): Result<Float?> = safeDatabaseCall {
        environmentCacheDao.getAverageTemperature(deviceId, username)
    }

    /**
     * 获取设备的平均湿度
     */
    suspend fun getAverageHumidity(deviceId: Long, username: String): Result<Float?> = safeDatabaseCall {
        environmentCacheDao.getAverageHumidity(deviceId, username)
    }

    /**
     * 清理旧的环境数据
     */
    suspend fun cleanOldEnvironmentData(beforeTimestamp: Long, username: String): Result<Int> = safeDatabaseCall {
        environmentCacheDao.deleteCachesBeforeTime(beforeTimestamp, username)
    }

    /**
     * 删除设备的环境数据
     */
    suspend fun deleteEnvironmentDataByDevice(deviceId: Long, username: String): Result<Int> = safeDatabaseCall {
        environmentCacheDao.deleteCachesByDevice(deviceId, username)
    }
}


