package com.example.myapp.mock

import com.example.myapp.util.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mock 初始化器
 * 
 * 负责在用户登录/注册后自动注入虚拟设备
 * 确保用户首次使用时就能看到设备数据
 */
@Singleton
class MockInitializer @Inject constructor(
    private val unifiedMockSystem: UnifiedMockSystem,
    private val preferencesManager: PreferencesManager
) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * 在用户登录成功后调用
     * 自动检查并注入虚拟设备
     */
    fun onUserLoggedIn(username: String) {
        Timber.d("[MOCK] User logged in: $username")
        
        scope.launch {
            try {
                // 注入虚拟设备（如果已存在则跳过）
                unifiedMockSystem.injectVirtualDevices(username)
                
            } catch (e: Exception) {
                Timber.e(e, "[MOCK] Failed to initialize mock data for user: $username")
            }
        }
    }
    
    /**
     * 在用户注册成功后调用
     * 自动注入虚拟设备
     */
    fun onUserRegistered(username: String) {
        Timber.d("[MOCK] User registered: $username")
        
        scope.launch {
            try {
                // 注入虚拟设备
                unifiedMockSystem.injectVirtualDevices(username)
                
            } catch (e: Exception) {
                Timber.e(e, "[MOCK] Failed to initialize mock data for new user: $username")
            }
        }
    }
    
    /**
     * 检查并初始化当前用户的 Mock 数据
     */
    suspend fun checkAndInitialize() {
        try {
            val username = preferencesManager.getUsername().first()
            if (username != null) {
                unifiedMockSystem.injectVirtualDevices(username)
            }
        } catch (e: Exception) {
            Timber.e(e, "[MOCK] Failed to check and initialize mock data")
        }
    }
}














