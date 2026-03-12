package com.example.myapp.data.repository

import com.example.myapp.data.local.dao.UserDao
import com.example.myapp.data.local.entity.User
import com.example.myapp.domain.base.BaseRepository
import com.example.myapp.util.PasswordUtils
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户 Repository
 * 处理用户账号相关的业务逻辑
 */
@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao
) : BaseRepository() {

    /**
     * 用户注册
     */
    suspend fun register(username: String, password: String): Result<User> = safeDatabaseCall {
        // 检查用户名是否已存在
        val exists = userDao.isUsernameExists(username) > 0
        if (exists) {
            throw Exception("用户名已存在")
        }

        // 密码加密（MD5 + 盐值）
        val passwordHash = PasswordUtils.encryptPassword(password)

        // 创建用户
        val user = User(
            username = username,
            passwordHash = passwordHash
        )

        // 插入数据库
        val uid = userDao.insert(user)
        user.copy(uid = uid)
    }

    /**
     * 用户登录
     */
    suspend fun login(username: String, password: String): Result<User> = safeDatabaseCall {
        // 查询用户
        val user = userDao.getUserByUsername(username)
            ?: throw Exception("用户名或密码错误")

        // 验证密码（MD5 + 盐值）
        if (!PasswordUtils.verifyPassword(password, user.passwordHash)) {
            throw Exception("用户名或密码错误")
        }

        user
    }

    /**
     * 获取用户信息
     */
    suspend fun getUserByUsername(username: String): Result<User?> = safeDatabaseCall {
        userDao.getUserByUsername(username)
    }

    /**
     * 获取用户信息（Flow）
     */
    fun getUserByUsernameFlow(username: String): Flow<User?> {
        return userDao.getUserByUsernameFlow(username)
    }

    /**
     * 修改密码
     */
    suspend fun changePassword(username: String, oldPassword: String, newPassword: String): Result<Unit> = safeDatabaseCall {
        // 验证旧密码
        val user = userDao.getUserByUsername(username)
            ?: throw Exception("用户不存在")

        if (!PasswordUtils.verifyPassword(oldPassword, user.passwordHash)) {
            throw Exception("原密码错误")
        }

        // 更新密码
        val newPasswordHash = PasswordUtils.encryptPassword(newPassword)
        val updatedUser = user.copy(passwordHash = newPasswordHash)
        userDao.update(updatedUser)
    }

    /**
     * 删除用户
     */
    suspend fun deleteUser(username: String): Result<Unit> = safeDatabaseCall {
        val user = userDao.getUserByUsername(username)
            ?: throw Exception("用户不存在")
        userDao.delete(user)
    }

    /**
     * 检查用户名是否存在
     */
    suspend fun isUsernameExists(username: String): Result<Boolean> = safeDatabaseCall {
        userDao.isUsernameExists(username) > 0
    }
}


