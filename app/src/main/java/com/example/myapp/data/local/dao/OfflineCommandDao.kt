package com.example.myapp.data.local.dao

import androidx.room.*
import com.example.myapp.data.local.entity.OfflineCommand
import kotlinx.coroutines.flow.Flow

/**
 * 离线指令 DAO
 * * 架构更新说明：
 * 1. [数据盲盒修复] 将所有修改/删除操作的隐式返回值 Unit 强制升级为 Int / List<Long>。
 * 2. [并发安全] 上层 Repository 现可根据返回的“受影响行数”精准拦截并发重发与幽灵数据。
 */
@Dao
interface OfflineCommandDao {

    /**
     * 插入离线指令
     * @return 插入的新记录 ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(command: OfflineCommand): Long

    /**
     * 批量插入离线指令
     * 【安全修复】：返回值从 Unit 修改为 List<Long>，确保每条指令的插入状态都可被追踪
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(commands: List<OfflineCommand>): List<Long>

    /**
     * 删除离线指令
     * 【安全修复】：返回值从 Unit 修改为 Int，返回受影响的行数
     */
    @Delete
    suspend fun delete(command: OfflineCommand): Int

    /**
     * 根据 ID 删除离线指令
     * 【安全修复】：返回值从 Unit 修改为 Int，返回受影响的行数
     */
    @Query("DELETE FROM offline_command WHERE id = :commandId")
    suspend fun deleteById(commandId: Long): Int

    /**
     * 获取指定用户的所有离线指令
     */
    @Query("SELECT * FROM offline_command WHERE username = :username ORDER BY create_time ASC")
    suspend fun getAllByUsername(username: String): List<OfflineCommand>

    /**
     * 获取指定用户的所有离线指令（Flow）
     */
    @Query("SELECT * FROM offline_command WHERE username = :username ORDER BY create_time ASC")
    fun getAllByUsernameFlow(username: String): Flow<List<OfflineCommand>>

    /**
     * 获取指定设备的离线指令
     */
    @Query("SELECT * FROM offline_command WHERE device_id = :deviceId AND username = :username ORDER BY create_time ASC")
    suspend fun getByDeviceId(deviceId: Long, username: String): List<OfflineCommand>

    /**
     * 更新重试次数
     * 【安全修复】：返回值从 Unit 修改为 Int，用于判断是否真的触碰到了该条指令
     */
    @Query("UPDATE offline_command SET retry_count = :retryCount WHERE id = :commandId")
    suspend fun updateRetryCount(commandId: Long, retryCount: Int): Int

    /**
     * 删除指定用户的所有离线指令
     * 【安全修复】：返回值从 Unit 修改为 Int，用于日志统计究竟清理了多少条历史包袱
     */
    @Query("DELETE FROM offline_command WHERE username = :username")
    suspend fun deleteAllByUsername(username: String): Int

    /**
     * 获取离线指令数量
     */
    @Query("SELECT COUNT(*) FROM offline_command WHERE username = :username")
    suspend fun getCountByUsername(username: String): Int
}