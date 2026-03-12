package com.example.myapp.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * DataStore 工具类
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

class PreferencesManager(private val context: Context) {

    companion object {
        private val USER_TOKEN = stringPreferencesKey("user_token")
        private val USER_ID = stringPreferencesKey("user_id")
        private val USERNAME = stringPreferencesKey("username")
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val AUTO_LOGIN = booleanPreferencesKey("auto_login")
        private val AT_HOME_MODE = booleanPreferencesKey("at_home_mode")
    }

    /**
     * 保存用户 Token
     */
    suspend fun saveUserToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_TOKEN] = token
        }
    }

    /**
     * 获取用户 Token
     */
    fun getUserToken(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[USER_TOKEN]
        }
    }

    /**
     * 保存用户 ID
     */
    suspend fun saveUserId(userId: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID] = userId
        }
    }

    /**
     * 获取用户 ID
     */
    fun getUserId(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[USER_ID]
        }
    }

    /**
     * 保存用户名
     */
    suspend fun saveUsername(username: String) {
        context.dataStore.edit { preferences ->
            preferences[USERNAME] = username
        }
    }

    /**
     * 获取用户名
     */
    fun getUsername(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[USERNAME]
        }
    }
    
    /**
     * 获取用户名（同步方法）
     */
    suspend fun getUsernameSync(): String? {
        return try {
            context.dataStore.data.map { preferences ->
                preferences[USERNAME]
            }.first()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 保存登录状态
     */
    suspend fun saveLoginState(isLoggedIn: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = isLoggedIn
        }
    }

    /**
     * 获取登录状态
     */
    fun getLoginState(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[IS_LOGGED_IN] ?: false
        }
    }

    /**
     * 保存自动登录设置
     */
    suspend fun saveAutoLogin(autoLogin: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_LOGIN] = autoLogin
        }
    }

    /**
     * 获取自动登录设置
     */
    fun getAutoLogin(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[AUTO_LOGIN] ?: false
        }
    }

    /**
     * 保存登录信息（一次性保存所有）
     */
    suspend fun saveLoginInfo(username: String, autoLogin: Boolean = true) {
        context.dataStore.edit { preferences ->
            preferences[USERNAME] = username
            preferences[IS_LOGGED_IN] = true
            preferences[AUTO_LOGIN] = autoLogin
        }
    }

    /**
     * 清除登录信息
     */
    suspend fun clearLoginInfo() {
        context.dataStore.edit { preferences ->
            preferences.remove(USERNAME)
            preferences.remove(IS_LOGGED_IN)
            preferences.remove(USER_TOKEN)
            preferences.remove(USER_ID)
            // 保留自动登录设置
        }
    }

    /**
     * 清除所有数据
     */
    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * 保存在家模式
     */
    suspend fun saveAtHomeMode(isAtHome: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AT_HOME_MODE] = isAtHome
        }
    }

    /**
     * 获取在家模式
     */
    fun getAtHomeMode(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[AT_HOME_MODE] ?: true
        }
    }
}
