package com.example.myapp.data.local.dao

import androidx.room.*
import com.example.myapp.data.local.entity.User
import kotlinx.coroutines.flow.Flow

/**
 * 用户 DAO
 * 提供用户账号的数据库操作
 */
@Dao
interface UserDao {

    /**
     * 插入用户
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: User): Long

    /**
     * 更新用户
     */
    @Update
    suspend fun update(user: User): Int

    /**
     * 删除用户
     */
    @Delete
    suspend fun delete(user: User): Int

    /**
     * 根据用户名查询用户
     */
    @Query("SELECT * FROM user WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    /**
     * 根据用户名查询用户（Flow）
     */
    @Query("SELECT * FROM user WHERE username = :username LIMIT 1")
    fun getUserByUsernameFlow(username: String): Flow<User?>

    /**
     * 根据 UID 查询用户
     */
    @Query("SELECT * FROM user WHERE uid = :uid LIMIT 1")
    suspend fun getUserByUid(uid: Long): User?

    /**
     * 检查用户名是否存在
     */
    @Query("SELECT COUNT(*) FROM user WHERE username = :username")
    suspend fun isUsernameExists(username: String): Int

    /**
     * 获取所有用户
     */
    @Query("SELECT * FROM user ORDER BY create_time DESC")
    fun getAllUsers(): Flow<List<User>>

    /**
     * 获取用户总数
     */
    @Query("SELECT COUNT(*) FROM user")
    suspend fun getUserCount(): Int

    /**
     * 删除所有用户
     */
    @Query("DELETE FROM user")
    suspend fun deleteAll(): Int
}


