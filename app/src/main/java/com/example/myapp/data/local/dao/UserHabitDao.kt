package com.example.myapp.data.local.dao

import androidx.room.*
import com.example.myapp.data.local.entity.UserHabit
import kotlinx.coroutines.flow.Flow

/**
 * 用户习惯模型 DAO
 * 所有查询方法强制传入 username 参数，实现多账号数据隔离
 */
@Dao
interface UserHabitDao {

    /**
     * 插入习惯
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habit: UserHabit): Long

    /**
     * 批量插入习惯
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(habits: List<UserHabit>): List<Long>

    /**
     * 更新习惯
     */
    @Update
    suspend fun update(habit: UserHabit): Int

    /**
     * 删除习惯
     */
    @Delete
    suspend fun delete(habit: UserHabit): Int

    /**
     * 根据 ID 查询习惯
     */
    @Query("SELECT * FROM user_habit WHERE habit_id = :habitId AND username = :username LIMIT 1")
    suspend fun getHabitById(habitId: Long, username: String): UserHabit?

    /**
     * 根据 ID 查询习惯（Flow）
     */
    @Query("SELECT * FROM user_habit WHERE habit_id = :habitId AND username = :username LIMIT 1")
    fun getHabitByIdFlow(habitId: Long, username: String): Flow<UserHabit?>

    /**
     * 获取指定用户的所有习惯
     */
    @Query("SELECT * FROM user_habit WHERE username = :username ORDER BY confidence DESC")
    fun getAllHabits(username: String): Flow<List<UserHabit>>
    
    /**
     * 获取指定用户的所有习惯（一次性查询）
     */
    @Query("SELECT * FROM user_habit WHERE username = :username ORDER BY confidence DESC")
    suspend fun getAllHabitsOnce(username: String): List<UserHabit>

    /**
     * 获取指定用户已启用的习惯
     */
    @Query("SELECT * FROM user_habit WHERE is_enabled = 1 AND username = :username ORDER BY confidence DESC")
    fun getEnabledHabits(username: String): Flow<List<UserHabit>>

    /**
     * 获取指定设备的习惯
     */
    @Query("SELECT * FROM user_habit WHERE device_id = :deviceId AND username = :username ORDER BY confidence DESC")
    fun getHabitsByDevice(deviceId: Long, username: String): Flow<List<UserHabit>>

    /**
     * 获取指定设备已启用的习惯
     */
    @Query("""
        SELECT * FROM user_habit 
        WHERE device_id = :deviceId 
        AND is_enabled = 1 
        AND username = :username 
        ORDER BY confidence DESC
    """)
    fun getEnabledHabitsByDevice(deviceId: Long, username: String): Flow<List<UserHabit>>

    /**
     * 获取指定设备和动作指令的习惯
     */
    @Query("""
        SELECT * FROM user_habit 
        WHERE device_id = :deviceId 
        AND action_command = :actionCommand 
        AND week_type = :weekType 
        AND username = :username 
        LIMIT 1
    """)
    suspend fun getHabitByCondition(
        deviceId: Long, 
        actionCommand: String, 
        weekType: Int, 
        username: String
    ): UserHabit?

    /**
     * 获取指定星期类型的习惯
     */
    @Query("SELECT * FROM user_habit WHERE week_type = :weekType AND username = :username ORDER BY confidence DESC")
    fun getHabitsByWeekType(weekType: Int, username: String): Flow<List<UserHabit>>

    /**
     * 获取指定时间窗口内可能触发的习惯
     * 注意: time_window 格式为 "HH:mm-HH:mm"，这里简化查询，实际使用时需要在代码中解析
     */
    @Query("""
        SELECT * FROM user_habit 
        WHERE is_enabled = 1 
        AND username = :username 
        AND (week_type = :weekType OR week_type = 0)
        ORDER BY confidence DESC
    """)
    suspend fun getTriggableHabits(
        weekType: Int, 
        username: String
    ): List<UserHabit>

    /**
     * 更新习惯启用状态
     */
    @Query("UPDATE user_habit SET is_enabled = :isEnabled WHERE habit_id = :habitId AND username = :username")
    suspend fun updateHabitEnabled(habitId: Long, isEnabled: Boolean, username: String): Int

    /**
     * 更新习惯置信度
     */
    @Query("UPDATE user_habit SET confidence = :confidence, update_time = :updateTime WHERE habit_id = :habitId AND username = :username")
    suspend fun updateHabitConfidence(habitId: Long, confidence: Double, updateTime: Long, username: String): Int

    /**
     * 获取高置信度的习惯（置信度 >= 阈值）
     */
    @Query("""
        SELECT * FROM user_habit 
        WHERE confidence >= :minConfidence 
        AND is_enabled = 1 
        AND username = :username 
        ORDER BY confidence DESC
    """)
    fun getHighConfidenceHabits(minConfidence: Double, username: String): Flow<List<UserHabit>>

    /**
     * 获取习惯总数
     */
    @Query("SELECT COUNT(*) FROM user_habit WHERE username = :username")
    suspend fun getHabitCount(username: String): Int

    /**
     * 获取已启用的习惯数量
     */
    @Query("SELECT COUNT(*) FROM user_habit WHERE is_enabled = 1 AND username = :username")
    suspend fun getEnabledHabitCount(username: String): Int

    /**
     * 删除指定设备的所有习惯
     */
    @Query("DELETE FROM user_habit WHERE device_id = :deviceId AND username = :username")
    suspend fun deleteHabitsByDevice(deviceId: Long, username: String): Int

    /**
     * 删除低置信度的习惯
     */
    @Query("DELETE FROM user_habit WHERE confidence < :minConfidence AND username = :username")
    suspend fun deleteLowConfidenceHabits(minConfidence: Double, username: String): Int

    /**
     * 删除指定用户的所有习惯
     */
    @Query("DELETE FROM user_habit WHERE username = :username")
    suspend fun deleteAllByUsername(username: String): Int
    
    /**
     * 根据设备ID和动作指令获取习惯
     */
    @Query("SELECT * FROM user_habit WHERE device_id = :deviceId AND action_command = :actionCommand AND username = :username LIMIT 1")
    suspend fun getHabitByDeviceAndAction(deviceId: Long, actionCommand: String, username: String): UserHabit?
    
    /**
     * 获取已启用的习惯（Flow）
     */
    @Query("SELECT * FROM user_habit WHERE is_enabled = 1 AND username = :username ORDER BY confidence DESC")
    fun getEnabledHabitsFlow(username: String): Flow<List<UserHabit>>
}
