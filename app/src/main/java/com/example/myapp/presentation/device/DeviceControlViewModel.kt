package com.example.myapp.presentation.device

import android.app.Application
import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.local.AppDatabase
import com.example.myapp.data.local.entity.EnvironmentCache
import com.example.myapp.data.local.entity.OfflineCommand
import com.example.myapp.data.local.entity.UserHabitLog
import com.example.myapp.data.mqtt.MqttClientManager
import com.example.myapp.util.PreferencesManager
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * 设备控制 ViewModel
 * 负责设备控制逻辑、操作日志采集、离线缓存
 */
@HiltViewModel
class DeviceControlViewModel @Inject constructor(
    application: Application,
    private val mqttClientManager: MqttClientManager,
    private val preferencesManager: PreferencesManager
) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val deviceDao = database.deviceDao()
    private val userHabitLogDao = database.userHabitLogDao()
    private val environmentCacheDao = database.environmentCacheDao()
    private val offlineCommandDao = database.offlineCommandDao()
    
    private val gson = Gson()
    
    // 设备信息
    private val _device = MutableStateFlow<com.example.myapp.data.local.entity.Device?>(null)
    val device: StateFlow<com.example.myapp.data.local.entity.Device?> = _device.asStateFlow()
    
    // 控制结果
    private val _controlResult = MutableStateFlow<Result<Unit>?>(null)
    val controlResult: StateFlow<Result<Unit>?> = _controlResult.asStateFlow()
    
    /**
     * 加载设备信息
     */
    fun loadDevice(deviceId: Long) {
        viewModelScope.launch {
            val username = preferencesManager.getUsernameSync() ?: return@launch
            deviceDao.getDeviceByIdFlow(deviceId, username).collect { device ->
                _device.value = device
            }
        }
    }
    
    /**
     * 控制灯光设备
     * @param deviceId 设备ID
     * @param power 电源状态 ("on" / "off")
     * @param brightness 亮度 (0-100)
     * @param color RGB颜色值
     */
    fun controlLight(deviceId: Long, power: String, brightness: Int, color: Int) {
        viewModelScope.launch {
            try {
                val device = _device.value ?: return@launch
                val username = preferencesManager.getUsernameSync() ?: return@launch
                
                // 构建控制指令
                val command = mapOf(
                    "action" to "control",
                    "power" to power,
                    "brightness" to brightness,
                    "color" to color
                )
                val payload = gson.toJson(command)
                
                // 检查网络状态
                if (isNetworkAvailable()) {
                    // 在线：直接发送MQTT指令
                    mqttClientManager.publish(
                        topic = device.publishTopic,
                        payload = payload,
                        qos = 1 // 控制指令使用 QoS 1
                    )
                    _controlResult.value = Result.success(Unit)
                } else {
                    // 离线：缓存指令
                    val offlineCommand = OfflineCommand(
                        deviceId = deviceId,
                        topic = device.publishTopic,
                        payload = payload,
                        qos = 1,
                        username = username
                    )
                    offlineCommandDao.insert(offlineCommand)
                    _controlResult.value = Result.failure(Exception("网络不可用，指令已缓存"))
                }
                
                // 记录操作日志（操作五要素）
                logUserHabit(
                    deviceId = deviceId,
                    action = "set_light_${power}_${brightness}_${color}",
                    username = username
                )
                
            } catch (e: Exception) {
                e.printStackTrace()
                _controlResult.value = Result.failure(e)
            }
        }
    }
    
    /**
     * 控制空调设备
     * @param deviceId 设备ID
     * @param power 电源状态 ("on" / "off")
     * @param mode 模式 ("cool" / "heat" / "dehumidify")
     * @param temperature 温度 (16-30)
     */
    fun controlAc(deviceId: Long, power: String, mode: String, temperature: Int) {
        viewModelScope.launch {
            try {
                val device = _device.value ?: return@launch
                val username = preferencesManager.getUsernameSync() ?: return@launch
                
                // 构建控制指令
                val command = mapOf(
                    "action" to "control",
                    "power" to power,
                    "mode" to mode,
                    "temperature" to temperature
                )
                val payload = gson.toJson(command)
                
                // 检查网络状态
                if (isNetworkAvailable()) {
                    // 在线：直接发送MQTT指令
                    mqttClientManager.publish(
                        topic = device.publishTopic,
                        payload = payload,
                        qos = 1 // 控制指令使用 QoS 1
                    )
                    _controlResult.value = Result.success(Unit)
                } else {
                    // 离线：缓存指令
                    val offlineCommand = OfflineCommand(
                        deviceId = deviceId,
                        topic = device.publishTopic,
                        payload = payload,
                        qos = 1,
                        username = username
                    )
                    offlineCommandDao.insert(offlineCommand)
                    _controlResult.value = Result.failure(Exception("网络不可用，指令已缓存"))
                }
                
                // 记录操作日志（操作五要素）
                logUserHabit(
                    deviceId = deviceId,
                    action = "set_ac_${power}_${mode}_${temperature}",
                    username = username
                )
                
            } catch (e: Exception) {
                e.printStackTrace()
                _controlResult.value = Result.failure(e)
            }
        }
    }
    
    /**
     * 记录用户操作日志（操作五要素）
     * 1. 设备ID
     * 2. 操作行为
     * 3. 时间戳
     * 4. 周期类型（星期几）
     * 5. 当前环境数据
     */
    private suspend fun logUserHabit(deviceId: Long, action: String, username: String) {
        try {
            val currentTime = System.currentTimeMillis()
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = currentTime
            val weekType = calendar.get(Calendar.DAY_OF_WEEK) // 1-7 (周日-周六)
            
            // 获取当前环境数据
            val environmentData = getCurrentEnvironmentData(username)
            
            // 插入操作日志
            val habitLog = UserHabitLog(
                deviceId = deviceId,
                action = action,
                operateTime = currentTime,
                weekType = weekType,
                environmentData = environmentData,
                username = username
            )
            userHabitLogDao.insert(habitLog)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 获取当前环境数据（JSON格式）
     */
    private suspend fun getCurrentEnvironmentData(username: String): String? {
        return try {
            // 获取最新的环境数据
            val latestEnvironment = environmentCacheDao.getLatestByUsername(username)
            if (latestEnvironment != null) {
                val envMap = mapOf(
                    "temperature" to latestEnvironment.temperature,
                    "humidity" to latestEnvironment.humidity,
                    "light_intensity" to latestEnvironment.lightIntensity,
                    "pm25" to latestEnvironment.pm25
                )
                gson.toJson(envMap)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 检查网络是否可用
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    /**
     * 重试发送离线指令
     */
    fun retryOfflineCommands() {
        viewModelScope.launch {
            try {
                val username = preferencesManager.getUsernameSync() ?: return@launch
                
                if (!isNetworkAvailable()) {
                    return@launch
                }
                
                // 获取所有离线指令
                val offlineCommands = offlineCommandDao.getAllByUsername(username)
                
                for (command in offlineCommands) {
                    try {
                        // 发送MQTT指令
                        mqttClientManager.publish(
                            topic = command.topic,
                            payload = command.payload,
                            qos = command.qos
                        )
                        
                        // 发送成功，删除缓存
                        offlineCommandDao.deleteById(command.id)
                        
                    } catch (e: Exception) {
                        e.printStackTrace()
                        
                        // 更新重试次数
                        val newRetryCount = command.retryCount + 1
                        if (newRetryCount >= 3) {
                            // 重试3次后仍失败，删除指令
                            offlineCommandDao.deleteById(command.id)
                        } else {
                            offlineCommandDao.updateRetryCount(command.id, newRetryCount)
                        }
                    }
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

