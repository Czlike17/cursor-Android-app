package com.example.myapp.mock

import com.example.myapp.BuildConfig
import com.example.myapp.data.local.entity.Device
import com.example.myapp.data.repository.DeviceRepository
import com.example.myapp.util.PreferencesManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mock 设备控制助手
 * 
 * 负责拦截设备控制指令，模拟设备响应
 * 在 DeviceControlActivity 中使用
 */
@Singleton
class MockDeviceControlHelper @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val preferencesManager: PreferencesManager,
    private val unifiedMockSystem: UnifiedMockSystem
) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * 模拟设备控制
     * 
     * @param deviceId 设备 ID
     * @param command 控制指令（Map 格式）
     * @param onSuccess 成功回调
     * @param onFailure 失败回调
     */
    suspend fun simulateDeviceControl(
        deviceId: Long,
        command: Map<String, Any>,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (!BuildConfig.IS_MOCK_MODE) {
            onFailure("Not in mock mode")
            return
        }
        
        try {
            val username = preferencesManager.getUsername().first() ?: run {
                onFailure("User not logged in")
                return
            }
            
            // 获取设备
            val deviceResult = deviceRepository.getDeviceById(deviceId, username)
            val device = deviceResult.getOrNull() ?: run {
                onFailure("Device not found")
                return
            }
            
            // 检查全局离线模式
            if (unifiedMockSystem.globalOfflineMode.value) {
                onFailure("Device is offline (global offline mode)")
                return
            }
            
            // 检查设备在线状态
            if (!device.isOnline) {
                onFailure("Device is offline")
                return
            }
            
            Timber.d("[MOCK] Simulating device control: deviceId=$deviceId, command=$command")
            
            // 模拟网络延迟（200ms）
            delay(200)
            
            // 使用统一 Mock 系统模拟设备响应
            val newStatus = unifiedMockSystem.simulateDeviceControl(
                deviceType = device.deviceType,
                currentStatus = device.status,
                command = command
            )
            
            // 更新数据库中的设备状态
            deviceRepository.updateDeviceStatus(deviceId, newStatus, username)
            
            Timber.d("[MOCK] Device control success: newStatus=$newStatus")
            onSuccess(newStatus)
            
        } catch (e: Exception) {
            Timber.e(e, "[MOCK] Device control failed")
            onFailure(e.message ?: "Unknown error")
        }
    }
    
    /**
     * 获取设备状态流（实时更新）
     */
    fun getDeviceStatusFlow(deviceId: Long, username: String): Flow<String?> {
        return deviceRepository.getDeviceByIdFlow(deviceId, username)
            .map { device -> device?.status }
            .distinctUntilChanged()
    }
    
    /**
     * 释放资源
     */
    fun release() {
        scope.cancel()
    }
}














