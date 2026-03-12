package com.example.myapp.mock

import com.example.myapp.data.network.IMqttClientManager
import kotlinx.coroutines.*
import org.eclipse.paho.client.mqttv3.MqttMessage
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject
import kotlin.random.Random

/**
 * Mock MQTT 客户端管理器
 * 模拟设备连接、指令响应、状态回传、随机掉线
 * 
 * 注意：此类仅在 IS_MOCK_MODE = true 时通过 Hilt 注入
 * 未来移除时，只需删除整个 mock 包即可
 * 
 * 核心功能：
 * 1. 模拟 MQTT 连接（500ms 延迟）
 * 2. 拦截设备指令，延迟 200ms 后回传状态
 * 3. 随机掉线引擎（3-5分钟触发一次）
 * 4. 设备状态缓存
 */
@Singleton
class MockMqttClientManager @Inject constructor(
    private val unifiedMockSystem: UnifiedMockSystem
) : IMqttClientManager {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 模拟连接状态
    private var connected = false
    
    // 订阅的主题及其回调
    private val subscriptions = ConcurrentHashMap<String, (String, MqttMessage) -> Unit>()
    
    // 连接丢失回调
    private var connectionLostCallback: ((Throwable?) -> Unit)? = null
    
    // 随机掉线任务
    private var randomDisconnectJob: Job? = null
    
    // 设备状态缓存（用于模拟设备当前状态）
    private val deviceStates = ConcurrentHashMap<String, JSONObject>()

    override fun connect(
        serverUri: String,
        clientId: String,
        username: String?,
        password: String?,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        Timber.d("[MOCK] Connecting to MQTT: $serverUri, clientId: $clientId")
        
        // 模拟连接延迟
        scope.launch {
            delay(500)
            connected = true
            Timber.d("[MOCK] MQTT connected successfully")
            withContext(Dispatchers.Main) {
                onSuccess()
            }
            
            // 启动随机掉线引擎
            startRandomDisconnectEngine()
        }
    }

    override fun disconnect() {
        Timber.d("[MOCK] Disconnecting MQTT")
        connected = false
        randomDisconnectJob?.cancel()
        subscriptions.clear()
    }

    override fun subscribe(
        topic: String,
        qos: Int,
        onMessageArrived: (topic: String, message: MqttMessage) -> Unit
    ) {
        Timber.d("[MOCK] Subscribing to topic: $topic")
        subscriptions[topic] = onMessageArrived
    }

    override fun unsubscribe(topic: String) {
        Timber.d("[MOCK] Unsubscribing from topic: $topic")
        subscriptions.remove(topic)
    }

    override fun publish(
        topic: String,
        payload: String,
        qos: Int,
        retained: Boolean,
        onSuccess: (() -> Unit)?,
        onFailure: ((Throwable) -> Unit)?
    ) {
        Timber.d("[MOCK] Publishing to topic: $topic, payload: $payload")
        
        if (!connected) {
            onFailure?.invoke(Exception("Not connected"))
            return
        }
        
        scope.launch {
            try {
                // 解析指令
                val command = JSONObject(payload)
                
                // 模拟指令处理延迟（200ms，符合需求）
                delay(200)
                
                // 构造设备状态回传
                val deviceId = extractDeviceIdFromTopic(topic)
                val statusResponse = buildStatusResponse(deviceId, command)
                
                // 缓存设备状态
                deviceStates[deviceId] = statusResponse
                
                // 通过订阅的状态主题回传
                val statusTopic = topic.replace("/command", "/status")
                val callback = subscriptions[statusTopic]
                
                if (callback != null) {
                    val message = MqttMessage(statusResponse.toString().toByteArray())
                    withContext(Dispatchers.Main) {
                        callback(statusTopic, message)
                    }
                    Timber.d("[MOCK] Status response sent: $statusResponse")
                }
                
                withContext(Dispatchers.Main) {
                    onSuccess?.invoke()
                }
                
            } catch (e: Exception) {
                Timber.e(e, "[MOCK] Failed to process publish")
                withContext(Dispatchers.Main) {
                    onFailure?.invoke(e)
                }
            }
        }
    }

    override fun isConnected(): Boolean = connected

    override fun setConnectionLostCallback(callback: (Throwable?) -> Unit) {
        this.connectionLostCallback = callback
    }

    override fun release() {
        Timber.d("[MOCK] Releasing MQTT resources")
        disconnect()
        scope.cancel()
    }

    /**
     * 从主题中提取设备ID
     */
    private fun extractDeviceIdFromTopic(topic: String): String {
        // 假设主题格式: /device/{deviceId}/command
        val parts = topic.split("/")
        return if (parts.size >= 3) parts[2] else "unknown"
    }

    /**
     * 构造设备状态响应
     */
    private fun buildStatusResponse(deviceId: String, command: JSONObject): JSONObject {
        val response = JSONObject()
        
        try {
            response.put("deviceId", deviceId)
            response.put("timestamp", System.currentTimeMillis())
            response.put("online", true)
            
            // 根据指令类型构造不同的状态
            when {
                command.has("power") -> {
                    response.put("power", command.getString("power"))
                }
                command.has("brightness") -> {
                    response.put("brightness", command.getInt("brightness"))
                }
                command.has("color") -> {
                    response.put("color", command.getString("color"))
                }
                command.has("temperature") -> {
                    response.put("temperature", command.getInt("temperature"))
                }
                command.has("mode") -> {
                    response.put("mode", command.getString("mode"))
                }
                command.has("action") -> {
                    // 处理自定义动作
                    val action = command.getString("action")
                    when (action) {
                        "turn_on" -> response.put("power", "on")
                        "turn_off" -> response.put("power", "off")
                        else -> response.put("action", action)
                    }
                }
            }
            
            // 添加模拟的设备属性
            response.put("battery", Random.nextInt(80, 100))
            response.put("signal", Random.nextInt(70, 100))
            
        } catch (e: Exception) {
            Timber.e(e, "[MOCK] Failed to build status response")
        }
        
        return response
    }

    /**
     * 启动随机掉线引擎
     * 每隔3-5分钟随机向某个设备发送离线遗嘱消息
     */
    private fun startRandomDisconnectEngine() {
        randomDisconnectJob?.cancel()
        
        randomDisconnectJob = scope.launch {
            while (isActive && connected) {
                // 随机等待 3-5 分钟
                val delayMinutes = Random.nextInt(3, 6)
                delay(delayMinutes * 60 * 1000L)
                
                if (!connected || subscriptions.isEmpty()) continue
                
                // 随机选择一个订阅的设备主题
                val topics = subscriptions.keys.filter { it.contains("/status") }
                if (topics.isEmpty()) continue
                
                val randomTopic = topics.random()
                val deviceId = extractDeviceIdFromTopic(randomTopic)
                
                // 构造离线遗嘱消息
                val offlineMessage = JSONObject().apply {
                    put("deviceId", deviceId)
                    put("online", false)
                    put("timestamp", System.currentTimeMillis())
                    put("reason", "connection_lost")
                }
                
                val callback = subscriptions[randomTopic]
                if (callback != null) {
                    val message = MqttMessage(offlineMessage.toString().toByteArray())
                    withContext(Dispatchers.Main) {
                        callback(randomTopic, message)
                    }
                    Timber.w("[MOCK] Random disconnect triggered for device: $deviceId")
                }
                
                // 30秒后恢复在线
                delay(30 * 1000L)
                
                val onlineMessage = JSONObject().apply {
                    put("deviceId", deviceId)
                    put("online", true)
                    put("timestamp", System.currentTimeMillis())
                }
                
                if (callback != null) {
                    val message = MqttMessage(onlineMessage.toString().toByteArray())
                    withContext(Dispatchers.Main) {
                        callback(randomTopic, message)
                    }
                    Timber.i("[MOCK] Device reconnected: $deviceId")
                }
            }
        }
    }
}




