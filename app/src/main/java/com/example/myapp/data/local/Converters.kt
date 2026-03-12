package com.example.myapp.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import org.json.JSONObject

/**
 * Room 类型转换器
 * 用于处理复杂数据类型的存储和读取
 */
class Converters {

    private val gson = Gson()

    /**
     * JSON 字符串转 Map
     */
    @TypeConverter
    fun fromStringToMap(value: String?): Map<String, Any>? {
        if (value == null) return null
        val mapType = object : TypeToken<Map<String, Any>>() {}.type
        return try {
            gson.fromJson(value, mapType)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Map 转 JSON 字符串
     */
    @TypeConverter
    fun fromMapToString(map: Map<String, Any>?): String? {
        return map?.let { gson.toJson(it) }
    }

    /**
     * JSON 字符串转 List<String>
     */
    @TypeConverter
    fun fromStringToList(value: String?): List<String>? {
        if (value == null) return null
        val listType = object : TypeToken<List<String>>() {}.type
        return try {
            gson.fromJson(value, listType)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * List<String> 转 JSON 字符串
     */
    @TypeConverter
    fun fromListToString(list: List<String>?): String? {
        return list?.let { gson.toJson(it) }
    }

    /**
     * 时间戳转 Date
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): java.util.Date? {
        return value?.let { java.util.Date(it) }
    }

    /**
     * Date 转时间戳
     */
    @TypeConverter
    fun dateToTimestamp(date: java.util.Date?): Long? {
        return date?.time
    }

    /**
     * JSON 字符串验证和格式化
     */
    fun isValidJson(jsonString: String?): Boolean {
        if (jsonString.isNullOrBlank()) return false
        return try {
            JSONObject(jsonString)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 安全解析 JSON 字符串
     */
    fun safeParseJson(jsonString: String?): JsonObject? {
        if (jsonString.isNullOrBlank()) return null
        return try {
            gson.fromJson(jsonString, JsonObject::class.java)
        } catch (e: Exception) {
            null
        }
    }
}

