package com.example.myapp.mock

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import com.example.myapp.BuildConfig
import com.example.myapp.data.repository.DeviceRepository
import com.example.myapp.util.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mock 测试助手
 * 
 * 简洁、专业的测试工具
 * 提供快速生成测试数据的功能
 */
@Singleton
class MockTestHelper @Inject constructor(
    private val unifiedMockSystem: UnifiedMockSystem,
    private val mockTestDataGenerator: MockTestDataGenerator,
    private val preferencesManager: PreferencesManager,
    private val deviceRepository: DeviceRepository
) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * 显示测试菜单
     */
    fun showTestMenu(context: Context, onDataGenerated: () -> Unit) {
        if (!BuildConfig.IS_MOCK_MODE) return
        
        val options = arrayOf(
            "生成测试数据（10条日志）",
            "清除所有测试数据",
            "切换离线模式",
            "查看环境数据"
        )
        
        AlertDialog.Builder(context)
            .setTitle("测试工具")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> generateTestData(context, onDataGenerated)
                    1 -> clearTestData(context, onDataGenerated)
                    2 -> toggleOfflineMode(context)
                    3 -> showEnvironmentData(context)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 生成测试数据
     */
    private fun generateTestData(context: Context, onDataGenerated: () -> Unit) {
        scope.launch {
            var progressDialog: ProgressDialog? = null
            
            try {
                // 显示进度
                withContext(Dispatchers.Main) {
                    progressDialog = ProgressDialog(context).apply {
                        setMessage("正在生成测试数据...")
                        setCancelable(false)
                        show()
                    }
                }
                
                // 获取用户名
                val username = preferencesManager.getUsername().first()
                if (username.isNullOrEmpty()) {
                    showError(context, "用户未登录")
                    return@launch
                }
                
                // 获取设备列表
                val devicesResult = deviceRepository.getDevicesByUsernameOnce(username)
                val devices = devicesResult.getOrNull()
                
                if (devices.isNullOrEmpty()) {
                    showError(context, "没有找到设备\n请重新登录以创建虚拟设备")
                    return@launch
                }
                
                // 生成数据
                val device = devices.first()
                val count = mockTestDataGenerator.generateTestHabitLogs(username, device.deviceId)
                
                // 显示成功
                withContext(Dispatchers.Main) {
                    progressDialog?.dismiss()
                    
                    AlertDialog.Builder(context)
                        .setTitle("✅ 生成成功")
                        .setMessage("已生成 $count 条测试数据\n设备：${device.deviceName}")
                        .setPositiveButton("确定") { _, _ ->
                            onDataGenerated()
                        }
                        .setCancelable(false)
                        .show()
                }
                
                Timber.i("[MOCK] Test data generated successfully")
                
            } catch (e: Exception) {
                Timber.e(e, "[MOCK] Failed to generate test data")
                showError(context, "生成失败：${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    progressDialog?.dismiss()
                }
            }
        }
    }
    
    /**
     * 清除测试数据
     */
    private fun clearTestData(context: Context, onDataGenerated: () -> Unit) {
        scope.launch {
            var progressDialog: ProgressDialog? = null
            
            try {
                withContext(Dispatchers.Main) {
                    progressDialog = ProgressDialog(context).apply {
                        setMessage("正在清除测试数据...")
                        setCancelable(false)
                        show()
                    }
                }
                
                val username = preferencesManager.getUsername().first()
                if (username.isNullOrEmpty()) {
                    showError(context, "用户未登录")
                    return@launch
                }
                
                val count = mockTestDataGenerator.clearAllTestData(username)
                
                withContext(Dispatchers.Main) {
                    progressDialog?.dismiss()
                    
                    AlertDialog.Builder(context)
                        .setTitle("✅ 清除成功")
                        .setMessage("已清除 $count 条测试数据")
                        .setPositiveButton("确定") { _, _ ->
                            onDataGenerated()
                        }
                        .setCancelable(false)
                        .show()
                }
                
            } catch (e: Exception) {
                Timber.e(e, "[MOCK] Failed to clear test data")
                showError(context, "清除失败：${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    progressDialog?.dismiss()
                }
            }
        }
    }
    
    /**
     * 切换离线模式
     */
    private fun toggleOfflineMode(context: Context) {
        unifiedMockSystem.toggleGlobalOfflineMode()
        
        val isOffline = unifiedMockSystem.globalOfflineMode.value
        val message = if (isOffline) "所有设备已离线" else "所有设备已恢复在线"
        
        AlertDialog.Builder(context)
            .setTitle("离线模式")
            .setMessage(message)
            .setPositiveButton("确定", null)
            .show()
    }
    
    /**
     * 显示环境数据
     */
    private fun showEnvironmentData(context: Context) {
        val envData = unifiedMockSystem.environmentDataFlow.value
        
        val message = """
            温度：${String.format("%.1f", envData.temperature)}°C
            湿度：${String.format("%.0f", envData.humidity)}%
            光照：${String.format("%.0f", envData.light)} lux
        """.trimIndent()
        
        AlertDialog.Builder(context)
            .setTitle("环境数据")
            .setMessage(message)
            .setPositiveButton("确定", null)
            .show()
    }
    
    /**
     * 显示错误
     */
    private suspend fun showError(context: Context, message: String) {
        withContext(Dispatchers.Main) {
            AlertDialog.Builder(context)
                .setTitle("❌ 错误")
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show()
        }
    }
}














