package com.example.myapp.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * 日期时间工具类
 */
object DateUtils {

    const val FORMAT_FULL = "yyyy-MM-dd HH:mm:ss"
    const val FORMAT_DATE = "yyyy-MM-dd"
    const val FORMAT_TIME = "HH:mm:ss"
    const val FORMAT_DATE_TIME = "yyyy-MM-dd HH:mm"

    /**
     * 格式化时间戳
     */
    fun formatTimestamp(timestamp: Long, pattern: String = FORMAT_FULL): String {
        return try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 格式化日期
     */
    fun formatDate(date: Date, pattern: String = FORMAT_FULL): String {
        return try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            sdf.format(date)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 解析日期字符串
     */
    fun parseDate(dateString: String, pattern: String = FORMAT_FULL): Date? {
        return try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            sdf.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取当前时间戳
     */
    fun getCurrentTimestamp(): Long {
        return System.currentTimeMillis()
    }

    /**
     * 获取当前日期字符串
     */
    fun getCurrentDateString(pattern: String = FORMAT_FULL): String {
        return formatTimestamp(getCurrentTimestamp(), pattern)
    }
}

