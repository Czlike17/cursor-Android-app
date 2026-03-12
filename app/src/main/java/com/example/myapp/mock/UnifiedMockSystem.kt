package com.example.myapp.mock

import android.content.Context
import com.example.myapp.data.local.entity.Device
import com.example.myapp.data.repository.DeviceRepository
import com.example.myapp.util.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * 统一 Mock 系统
 * 
 * 这是整个项目中唯一的 Mock 数据管理类，负责：
 * 1. 虚拟设备注入
 * 2. 环境数据生成
 * 3. 设备状态模拟
 * 4. 离线状态模拟
 * 
 * 设计原则：
 * - 所有 Mock 逻辑集中在此类
 * - 通过 Hilt 注入，业务层无感知
 * - 可通过 BuildConfig.IS_MOCK_MODE 完全禁用
 */
@Singleton
class UnifiedMockSystem @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceRepository: DeviceRepository,
    private val preferencesManager: PreferencesManager
) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 环境数据流
    private val _environmentDataFlow = MutableStateFlow(EnvironmentData())
    val environmentDataFlow: StateFlow<EnvironmentData> = _environmentDataFlow.asStateFlow()
    
    // 全局离线模拟开关
    private val _globalOfflineMode = MutableStateFlow(false)
    val globalOfflineMode: StateFlow<Boolean> = _globalOfflineMode.asStateFlow()
    
    // 环境数据生成任务
    private var environmentJob: Job? = null
    
    // 当前环境数据（用于平滑过渡）
    private var currentTemp = 22.5f
    private var currentHumidity = 55f
    private var currentLight = 400f
    
    /**
     * 初始化 Mock 系统
     */
    fun initialize() {
        Timber.d("[MOCK] UnifiedMockSystem initialized")
        startEnvironmentDataGeneration()
    }
    
    /**
     * 注入虚拟设备（在用户首次登录/注册后调用）
     */
    suspend fun injectVirtualDevices(username: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Timber.d("[MOCK] Injecting virtual devices for user: $username")
            
            // 检查是否已经注入过设备
            val existingDevices = deviceRepository.getDevicesByUsernameOnce(username)
            if (existingDevices.isSuccess && existingDevices.getOrNull()?.isNotEmpty() == true) {
                Timber.d("[MOCK] Devices already exist, skipping injection")
                return@withContext Result.success(Unit)
            }
            
            // 创建 3 个虚拟设备
            val devices = listOf(
                createVirtualLight(username),
                createVirtualAirConditioner(username),
                createVirtualTempHumiditySensor(username)
            )
            
            // 批量插入
            devices.forEach { device ->
                val result = deviceRepository.insertDevice(device)
                if (result.isSuccess) {
                    Timber.d("[MOCK] Device created: ${device.deviceName}")
                } else {
                    Timber.e("[MOCK] Failed to create device: ${device.deviceName}")
                }
            }
            
            Timber.i("[MOCK] Successfully injected ${devices.size} virtual devices")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Timber.e(e, "[MOCK] Failed to inject virtual devices")
            Result.failure(e)
        }
    }
    
    /**
     * 创建虚拟灯光设备
     */
    private fun createVirtualLight(username: String): Device {
        return Device(
            deviceName = "客厅智能灯",
            deviceType = "智能灯",
            mqttBroker = "mock.broker.local",
            mqttPort = 1883,
            subscribeTopic = "/device/light_001/status",
            publishTopic = "/device/light_001/command",
            clientId = "light_001",
            status = """{"power":"off","brightness":0,"color":"#FFFFFF"}""",
            isOnline = true,
            username = username
        )
    }
    
    /**
     * 创建虚拟空调设备
     */
    private fun createVirtualAirConditioner(username: String): Device {
        return Device(
            deviceName = "卧室空调",
            deviceType = "空调",
            mqttBroker = "mock.broker.local",
            mqttPort = 1883,
            subscribeTopic = "/device/ac_001/status",
            publishTopic = "/device/ac_001/command",
            clientId = "ac_001",
            status = """{"power":"off","mode":"cool","temperature":26}""",
            isOnline = true,
            username = username
        )
    }
    
    /**
     * 创建虚拟温湿度传感器
     */
    private fun createVirtualTempHumiditySensor(username: String): Device {
        return Device(
            deviceName = "温湿度传感器",
            deviceType = "传感器",
            mqttBroker = "mock.broker.local",
            mqttPort = 1883,
            subscribeTopic = "/device/sensor_001/status",
            publishTopic = "/device/sensor_001/command",
            clientId = "sensor_001",
            status = """{"temperature":22.5,"humidity":55}""",
            isOnline = true,
            username = username
        )
    }
    
    /**
     * 启动环境数据生成（每 3 秒更新一次）
     */
    private fun startEnvironmentDataGeneration() {
        environmentJob?.cancel()
        
        environmentJob = scope.launch {
            while (isActive) {
                try {
                    // 生成平滑过渡的环境数据
                    currentTemp = smoothTransition(currentTemp, 20f, 25f, 0.5f)
                    currentHumidity = smoothTransition(currentHumidity, 45f, 60f, 2f)
                    currentLight = smoothTransition(currentLight, 300f, 500f, 50f)
                    
                    val data = EnvironmentData(
                        temperature = currentTemp,
                        humidity = currentHumidity,
                        light = currentLight,
                        timestamp = System.currentTimeMillis()
                    )
                    
                    _environmentDataFlow.emit(data)
                    Timber.d("[MOCK] Environment data updated: temp=${data.temperature}°C, humidity=${data.humidity}%, light=${data.light}lux")
                    
                } catch (e: Exception) {
                    Timber.e(e, "[MOCK] Failed to generate environment data")
                }
                
                delay(3000) // 每 3 秒更新一次
            }
        }
    }
    
    /**
     * 平滑过渡算法
     */
    private fun smoothTransition(current: Float, min: Float, max: Float, step: Float): Float {
        val target = Random.nextFloat() * (max - min) + min
        return when {
            target > current -> (current + step).coerceAtMost(max)
            target < current -> (current - step).coerceAtLeast(min)
            else -> current
        }
    }
    
    /**
     * 模拟设备控制响应
     * 返回更新后的设备状态 JSON
     * 
     * @param deviceType 设备类型（预留参数，未来可用于不同设备的特殊处理）
     * @param currentStatus 当前设备状态 JSON
     * @param command 控制指令
     */
    fun simulateDeviceControl(
        @Suppress("UNUSED_PARAMETER") deviceType: String,
        currentStatus: String,
        command: Map<String, Any>
    ): String {
        return try {
            val statusJson = org.json.JSONObject(currentStatus)
            
            // 根据指令更新状态
            command.forEach { (key, value) ->
                when (value) {
                    is String -> statusJson.put(key, value)
                    is Int -> statusJson.put(key, value)
                    is Boolean -> statusJson.put(key, value)
                    is Double -> statusJson.put(key, value)
                    is Float -> statusJson.put(key, value)
                }
            }
            
            statusJson.toString()
            
        } catch (e: Exception) {
            Timber.e(e, "[MOCK] Failed to simulate device control")
            currentStatus
        }
    }
    
    /**
     * 切换全局离线模式
     */
    fun toggleGlobalOfflineMode() {
        scope.launch {
            val newMode = !_globalOfflineMode.value
            _globalOfflineMode.emit(newMode)
            Timber.i("[MOCK] Global offline mode: $newMode")
            
            // 更新所有设备的在线状态
            val username = preferencesManager.getUsername().first()
            if (username != null) {
                updateAllDevicesOnlineStatus(username, !newMode)
            }
        }
    }
    
    /**
     * 更新所有设备的在线状态
     */
    private suspend fun updateAllDevicesOnlineStatus(username: String, isOnline: Boolean) {
        try {
            val devices = deviceRepository.getDevicesByUsernameOnce(username).getOrNull() ?: return
            
            devices.forEach { device ->
                deviceRepository.updateOnlineStatus(device.deviceId, isOnline, username)
            }
            
            Timber.d("[MOCK] Updated ${devices.size} devices online status to: $isOnline")
            
        } catch (e: Exception) {
            Timber.e(e, "[MOCK] Failed to update devices online status")
        }
    }
    
    /**
     * 生成图表假数据（用于习惯详情页）
     * 
     * @param days 天数（预留参数，未来可用于自定义天数）
     */
    fun generateChartMockData(@Suppress("UNUSED_PARAMETER") days: Int = 30): ChartMockData {
        val timeDistribution = mutableMapOf<Int, Int>() // 小时 -> 次数
        val environmentCorrelation = mutableListOf<Pair<Float, Int>>() // 温度 -> 次数
        
        // 生成时间分布数据（模拟早上7点和晚上10点的高峰）
        for (hour in 0..23) {
            val count = when (hour) {
                in 7..8 -> Random.nextInt(15, 25) // 早高峰
                in 22..23 -> Random.nextInt(20, 30) // 晚高峰
                in 18..21 -> Random.nextInt(10, 20) // 晚间
                else -> Random.nextInt(0, 5) // 其他时间
            }
            timeDistribution[hour] = count
        }
        
        // 生成环境关联数据
        for (temp in 18..30) {
            val count = when {
                temp > 28 -> Random.nextInt(15, 25) // 高温时开空调频繁
                temp < 20 -> Random.nextInt(10, 20) // 低温时开灯频繁
                else -> Random.nextInt(5, 15)
            }
            environmentCorrelation.add(temp.toFloat() to count)
        }
        
        return ChartMockData(
            timeDistribution = timeDistribution,
            environmentCorrelation = environmentCorrelation
        )
    }
    
    /**
     * 释放资源
     */
    fun release() {
        Timber.d("[MOCK] Releasing UnifiedMockSystem resources")
        environmentJob?.cancel()
        scope.cancel()
    }
}

/**
 * 环境数据模型
 */
data class EnvironmentData(
    val temperature: Float = 22.5f,
    val humidity: Float = 55f,
    val light: Float = 400f,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 图表假数据模型
 */
data class ChartMockData(
    val timeDistribution: Map<Int, Int>,
    val environmentCorrelation: List<Pair<Float, Int>>
)

