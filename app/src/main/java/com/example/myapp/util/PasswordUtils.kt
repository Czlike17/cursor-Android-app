package com.example.myapp.util

import java.security.MessageDigest

/**
 * 密码加密工具类
 * 使用 MD5 + 盐值进行加密
 */
object PasswordUtils {

    // 固定盐值（生产环境建议使用配置文件或环境变量）
    private const val SALT = "SmartHome@2024#Security"

    /**
     * 加密密码（MD5 + 盐值）
     */
    fun encryptPassword(password: String): String {
        val saltedPassword = password + SALT
        return md5(saltedPassword)
    }

    /**
     * 验证密码
     */
    fun verifyPassword(inputPassword: String, storedHash: String): Boolean {
        val encryptedInput = encryptPassword(inputPassword)
        return encryptedInput == storedHash
    }

    /**
     * MD5 加密
     */
    private fun md5(input: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(input.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 检查密码强度
     * @return 0: 弱, 1: 中, 2: 强
     */
    fun checkPasswordStrength(password: String): Int {
        if (password.length < 6) return 0

        var strength = 0
        
        // 包含数字
        if (password.any { it.isDigit() }) strength++
        
        // 包含小写字母
        if (password.any { it.isLowerCase() }) strength++
        
        // 包含大写字母
        if (password.any { it.isUpperCase() }) strength++
        
        // 包含特殊字符
        if (password.any { !it.isLetterOrDigit() }) strength++

        return when {
            strength >= 3 && password.length >= 8 -> 2 // 强
            strength >= 2 && password.length >= 6 -> 1 // 中
            else -> 0 // 弱
        }
    }

    /**
     * 获取密码强度描述
     */
    fun getPasswordStrengthText(strength: Int): String {
        return when (strength) {
            0 -> "弱"
            1 -> "中"
            2 -> "强"
            else -> "未知"
        }
    }

    /**
     * 获取密码强度颜色资源 ID
     */
    fun getPasswordStrengthColor(strength: Int): Int {
        return when (strength) {
            0 -> android.R.color.holo_red_dark
            1 -> android.R.color.holo_orange_dark
            2 -> android.R.color.holo_green_dark
            else -> android.R.color.darker_gray
        }
    }
}

