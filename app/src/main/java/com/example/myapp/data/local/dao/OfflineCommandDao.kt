package com.example.myapp.data.local.dao

import androidx.room.*
import com.example.myapp.data.local.entity.OfflineCommand
import kotlinx.coroutines.flow.Flow

/**
 * 离线指令 DAO
 */
@Dao
interface OfflineCommandDao {
    
    /**
     * 插入离线指令
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(command: OfflineCommand): Long
    
    /**
     * 批量插入离线指令
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(commands: List<OfflineCommand>)
    
    /**
     * 删除离线指令
     */
    @Delete
    suspend fun delete(command: OfflineCommand)
    
    /**
     * 根据 ID 删除离线指令
     */
    @Query("DELETE FROM offline_command WHERE id = :commandId")
    suspend fun deleteById(commandId: Long)
    
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
     */
    @Query("UPDATE offline_command SET retry_count = :retryCount WHERE id = :commandId")
    suspend fun updateRetryCount(commandId: Long, retryCount: Int)
    
    /**
     * 删除指定用户的所有离线指令
     */
    @Query("DELETE FROM offline_command WHERE username = :username")
    suspend fun deleteAllByUsername(username: String)
    
    /**
     * 获取离线指令数量
     */
    @Query("SELECT COUNT(*) FROM offline_command WHERE username = :username")
    suspend fun getCountByUsername(username: String): Int
}

















