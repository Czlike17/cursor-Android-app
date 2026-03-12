package com.example.myapp.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * SharedPreferences 工具类
 * 支持加密存储
 */
class SPUtils private constructor(context: Context, name: String = "app_prefs", encrypted: Boolean = false) {

    private val sp: SharedPreferences = if (encrypted) {
        // 使用加密的 SharedPreferences
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        EncryptedSharedPreferences.create(
            context,
            name,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } else {
        context.getSharedPreferences(name, Context.MODE_PRIVATE)
    }

    companion object {
        @Volatile
        private var instance: SPUtils? = null

        fun getInstance(context: Context, name: String = "app_prefs", encrypted: Boolean = false): SPUtils {
            return instance ?: synchronized(this) {
                instance ?: SPUtils(context.applicationContext, name, encrypted).also { instance = it }
            }
        }
    }

    fun putString(key: String, value: String) {
        sp.edit().putString(key, value).apply()
    }

    fun getString(key: String, defaultValue: String = ""): String {
        return sp.getString(key, defaultValue) ?: defaultValue
    }

    fun putInt(key: String, value: Int) {
        sp.edit().putInt(key, value).apply()
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        return sp.getInt(key, defaultValue)
    }

    fun putLong(key: String, value: Long) {
        sp.edit().putLong(key, value).apply()
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return sp.getLong(key, defaultValue)
    }

    fun putBoolean(key: String, value: Boolean) {
        sp.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return sp.getBoolean(key, defaultValue)
    }

    fun putFloat(key: String, value: Float) {
        sp.edit().putFloat(key, value).apply()
    }

    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return sp.getFloat(key, defaultValue)
    }

    fun contains(key: String): Boolean {
        return sp.contains(key)
    }

    fun remove(key: String) {
        sp.edit().remove(key).apply()
    }

    fun clear() {
        sp.edit().clear().apply()
    }

    fun getAll(): Map<String, *> {
        return sp.all
    }
}

