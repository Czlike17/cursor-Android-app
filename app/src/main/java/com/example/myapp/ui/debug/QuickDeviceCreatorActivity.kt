package com.example.myapp.ui.debug

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapp.data.local.entity.Device
import com.example.myapp.data.repository.DeviceRepository
import com.example.myapp.databinding.ActivityQuickDeviceCreatorBinding
import com.example.myapp.util.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 快速设备创建工具
 * 用于在Mock模式下快速创建测试设备
 */
@AndroidEntryPoint
class QuickDeviceCreatorActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityQuickDeviceCreatorBinding
    
    @Inject
    lateinit var deviceRepository: DeviceRepository
    
    @Inject
    lateinit var preferencesManager: PreferencesManager
    
    @Inject
    lateinit var userRepository: com.example.myapp.data.repository.UserRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuickDeviceCreatorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
    }
    
    private fun setupUI() {
        binding.btnCreateSingle.setOnClickListener {
            createSingleDevice()
        }
        
        binding.btnCreateBatch.setOnClickListener {
            createBatchDevices()
        }
        
        binding.btnViewDevices.setOnClickListener {
            viewDevices()
        }
        
        binding.btnClearAll.setOnClickListener {
            clearAllDevices()
        }
    }
    
    private fun createSingleDevice() {
        val name = binding.etDeviceName.text.toString()
        val type = binding.etDeviceType.text.toString()
        val clientIdInput = binding.etDeviceId.text.toString()
        
        if (name.isEmpty() || type.isEmpty() || clientIdInput.isEmpty()) {
            Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            val username = preferencesManager.getUsername().first() ?: "test_user"
            
            // 确保用户存在
            val userCheckResult = ensureUserExists(username)
            if (userCheckResult.isFailure) {
                binding.tvStatus.text = "❌ 用户验证失败: ${userCheckResult.exceptionOrNull()?.message}"
                Toast.makeText(this@QuickDeviceCreatorActivity, "用户验证失败", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            val device = Device(
                deviceName = name,
                deviceType = type,
                mqttBroker = "mock.broker.local",
                mqttPort = 1883,
                subscribeTopic = "device/$clientIdInput/command",
                publishTopic = "device/$clientIdInput/status",
                clientId = clientIdInput,
                isOnline = true,
                status = """{"power": "off"}""",
                username = username
            )
            
            val result = deviceRepository.addDevice(device)
            
            result.onSuccess { id ->
                binding.tvStatus.text = "✅ 设备创建成功!\nID: $id\n设备名: $name"
                Toast.makeText(this@QuickDeviceCreatorActivity, "创建成功!", Toast.LENGTH_SHORT).show()
                
                binding.etDeviceName.text?.clear()
                binding.etDeviceType.text?.clear()
                binding.etDeviceId.text?.clear()
                
            }.onFailure { error ->
                binding.tvStatus.text = "❌ 创建失败: ${error.message}"
                Toast.makeText(this@QuickDeviceCreatorActivity, "创建失败!", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun createBatchDevices() {
        binding.tvStatus.text = "正在批量创建设备..."
        
        lifecycleScope.launch {
            val username = preferencesManager.getUsername().first() ?: "test_user"
            Timber.d("Creating batch devices for user: $username")
            
            // 确保用户存在
            val userCheckResult = ensureUserExists(username)
            if (userCheckResult.isFailure) {
                val errorMsg = "❌ 用户验证失败: ${userCheckResult.exceptionOrNull()?.message}"
                binding.tvStatus.text = errorMsg
                Toast.makeText(this@QuickDeviceCreatorActivity, "用户验证失败", Toast.LENGTH_SHORT).show()
                Timber.e("User check failed: ${userCheckResult.exceptionOrNull()?.message}")
                return@launch
            }
            
            val testDevices = listOf(
                Device(
                    deviceName = "客厅灯",
                    deviceType = "智能灯",
                    mqttBroker = "mock.broker.local",
                    mqttPort = 1883,
                    subscribeTopic = "device/light_001/command",
                    publishTopic = "device/light_001/status",
                    clientId = "light_001",
                    isOnline = true,
                    status = """{"power": "off", "brightness": 0}""",
                    username = username
                ),
                Device(
                    deviceName = "卧室空调",
                    deviceType = "空调",
                    mqttBroker = "mock.broker.local",
                    mqttPort = 1883,
                    subscribeTopic = "device/ac_001/command",
                    publishTopic = "device/ac_001/status",
                    clientId = "ac_001",
                    isOnline = true,
                    status = """{"power": "off", "temperature": 26, "mode": "cool"}""",
                    username = username
                ),
                Device(
                    deviceName = "阳台窗帘",
                    deviceType = "窗帘",
                    mqttBroker = "mock.broker.local",
                    mqttPort = 1883,
                    subscribeTopic = "device/curtain_001/command",
                    publishTopic = "device/curtain_001/status",
                    clientId = "curtain_001",
                    isOnline = true,
                    status = """{"position": "closed", "level": 0}""",
                    username = username
                ),
                Device(
                    deviceName = "厨房插座",
                    deviceType = "智能插座",
                    mqttBroker = "mock.broker.local",
                    mqttPort = 1883,
                    subscribeTopic = "device/socket_001/command",
                    publishTopic = "device/socket_001/status",
                    clientId = "socket_001",
                    isOnline = true,
                    status = """{"power": "off"}""",
                    username = username
                ),
                Device(
                    deviceName = "书房台灯",
                    deviceType = "智能灯",
                    mqttBroker = "mock.broker.local",
                    mqttPort = 1883,
                    subscribeTopic = "device/light_002/command",
                    publishTopic = "device/light_002/status",
                    clientId = "light_002",
                    isOnline = true,
                    status = """{"power": "off", "brightness": 0}""",
                    username = username
                )
            )
            
            var successCount = 0
            var failCount = 0
            val errorMessages = mutableListOf<String>()
            
            testDevices.forEachIndexed { index, device ->
                Timber.d("Creating device ${index + 1}/${testDevices.size}: ${device.deviceName}")
                val result = deviceRepository.addDevice(device)
                if (result.isSuccess) {
                    successCount++
                    val deviceId = result.getOrNull()
                    Timber.d("✅ Created device: ${device.deviceName} with ID: $deviceId")
                } else {
                    failCount++
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    errorMessages.add("${device.deviceName}: $error")
                    Timber.e("❌ Failed to create device: ${device.deviceName}, error: $error")
                }
            }
            
            Timber.d("Batch creation completed: $successCount success, $failCount failed")
            
            val statusText = buildString {
                appendLine("✅ 批量创建完成!")
                appendLine()
                appendLine("成功: $successCount 个")
                appendLine("失败: $failCount 个")
                appendLine()
                if (successCount > 0) {
                    appendLine("已创建设备:")
                    appendLine("1. 客厅灯 (light_001)")
                    appendLine("2. 卧室空调 (ac_001)")
                    appendLine("3. 阳台窗帘 (curtain_001)")
                    appendLine("4. 厨房插座 (socket_001)")
                    appendLine("5. 书房台灯 (light_002)")
                }
                if (errorMessages.isNotEmpty()) {
                    appendLine()
                    appendLine("错误信息:")
                    errorMessages.forEach { appendLine("- $it") }
                }
            }
            
            binding.tvStatus.text = statusText
            
            Toast.makeText(
                this@QuickDeviceCreatorActivity,
                "创建完成: $successCount 成功, $failCount 失败",
                Toast.LENGTH_LONG
            ).show()
            
            // 自动刷新设备列表
            if (successCount > 0) {
                viewDevices()
            }
        }
    }
    
    private fun viewDevices() {
        lifecycleScope.launch {
            val username = preferencesManager.getUsername().first() ?: "test_user"
            Timber.d("Viewing devices for user: $username")
            
            val result = deviceRepository.getDevicesByUsernameOnce(username)
            
            result.onSuccess { devices ->
                Timber.d("Found ${devices.size} devices")
                if (devices.isEmpty()) {
                    binding.tvStatus.text = "暂无设备\n请先创建设备"
                } else {
                    val deviceList = devices.mapIndexed { index, device ->
                        "${index + 1}. ${device.deviceName} (${device.clientId})\n" +
                        "   类型: ${device.deviceType}\n" +
                        "   ID: ${device.deviceId}\n" +
                        "   状态: ${if (device.isOnline) "在线" else "离线"}\n" +
                        "   数据: ${device.status}"
                    }.joinToString("\n\n")
                    
                    binding.tvStatus.text = "已创建设备 (${devices.size}个):\n\n$deviceList"
                }
            }.onFailure { error ->
                val errorMsg = "❌ 查询失败: ${error.message}"
                binding.tvStatus.text = errorMsg
                Timber.e("Failed to view devices: ${error.message}")
            }
        }
    }
    
    private fun clearAllDevices() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("确认清空")
            .setMessage("确定要删除所有设备吗?")
            .setPositiveButton("确定") { _, _ ->
                lifecycleScope.launch {
                    val username = preferencesManager.getUsername().first() ?: "test_user"
                    Timber.d("Clearing all devices for user: $username")
                    
                    val result = deviceRepository.getDevicesByUsernameOnce(username)
                    
                    result.onSuccess { devices ->
                        Timber.d("Found ${devices.size} devices to delete")
                        var deleteCount = 0
                        devices.forEach { device ->
                            val deleteResult = deviceRepository.deleteDevice(device)
                            if (deleteResult.isSuccess) {
                                deleteCount++
                                Timber.d("Deleted device: ${device.deviceName}")
                            } else {
                                Timber.e("Failed to delete device: ${device.deviceName}")
                            }
                        }
                        
                        binding.tvStatus.text = "✅ 已删除 $deleteCount 个设备"
                        Toast.makeText(
                            this@QuickDeviceCreatorActivity,
                            "已删除 $deleteCount 个设备",
                            Toast.LENGTH_SHORT
                        ).show()
                        Timber.d("Cleared $deleteCount devices")
                    }.onFailure { error ->
                        binding.tvStatus.text = "❌ 清空失败: ${error.message}"
                        Timber.e("Failed to clear devices: ${error.message}")
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 确保用户存在于数据库中
     * 如果用户不存在,则创建一个临时用户
     */
    private suspend fun ensureUserExists(username: String): Result<Unit> {
        return try {
            // 检查用户是否存在
            val userExists = userRepository.isUsernameExists(username)
            
            if (userExists.isSuccess && userExists.getOrNull() == true) {
                Timber.d("User $username already exists")
                Result.success(Unit)
            } else {
                // 用户不存在,创建临时用户
                Timber.d("User $username does not exist, creating temporary user")
                val registerResult = userRepository.register(username, "temp_password_123")
                
                if (registerResult.isSuccess) {
                    Timber.d("✅ Temporary user created: $username")
                    Result.success(Unit)
                } else {
                    val error = registerResult.exceptionOrNull()?.message ?: "Unknown error"
                    Timber.e("❌ Failed to create user: $error")
                    Result.failure(Exception("创建用户失败: $error"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking/creating user")
            Result.failure(e)
        }
    }
}


