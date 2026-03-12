package com.example.myapp.util

import android.text.TextUtils
import java.util.regex.Pattern

/**
 * 验证工具类
 */
object ValidateUtils {

    /**
     * 验证手机号
     */
    fun isMobilePhone(phone: String): Boolean {
        if (TextUtils.isEmpty(phone)) return false
        val pattern = "^1[3-9]\\d{9}$"
        return Pattern.matches(pattern, phone)
    }

    /**
     * 验证邮箱
     */
    fun isEmail(email: String): Boolean {
        if (TextUtils.isEmpty(email)) return false
        val pattern = "^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$"
        return Pattern.matches(pattern, email)
    }

    /**
     * 验证身份证号
     */
    fun isIdCard(idCard: String): Boolean {
        if (TextUtils.isEmpty(idCard)) return false
        val pattern = "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$"
        return Pattern.matches(pattern, idCard)
    }

    /**
     * 验证 URL
     */
    fun isUrl(url: String): Boolean {
        if (TextUtils.isEmpty(url)) return false
        val pattern = "^(http|https)://[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-.,@?^=%&:/~+#]*[\\w\\-@?^=%&/~+#])?$"
        return Pattern.matches(pattern, url)
    }

    /**
     * 验证 IP 地址
     */
    fun isIpAddress(ip: String): Boolean {
        if (TextUtils.isEmpty(ip)) return false
        val pattern = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$"
        return Pattern.matches(pattern, ip)
    }

    /**
     * 验证密码强度（至少包含数字和字母，长度 6-20）
     */
    fun isStrongPassword(password: String): Boolean {
        if (TextUtils.isEmpty(password)) return false
        val pattern = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,20}$"
        return Pattern.matches(pattern, password)
    }

    /**
     * 验证是否为纯数字
     */
    fun isNumeric(str: String): Boolean {
        if (TextUtils.isEmpty(str)) return false
        val pattern = "^[0-9]+$"
        return Pattern.matches(pattern, str)
    }

    /**
     * 验证是否为纯字母
     */
    fun isAlpha(str: String): Boolean {
        if (TextUtils.isEmpty(str)) return false
        val pattern = "^[A-Za-z]+$"
        return Pattern.matches(pattern, str)
    }

    /**
     * 验证是否为字母数字组合
     */
    fun isAlphanumeric(str: String): Boolean {
        if (TextUtils.isEmpty(str)) return false
        val pattern = "^[A-Za-z0-9]+$"
        return Pattern.matches(pattern, str)
    }
}

