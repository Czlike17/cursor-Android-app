package com.example.myapp.data.mqtt

import android.content.Context
import com.example.myapp.data.local.entity.Device
import com.example.myapp.data.model.*
import com.example.myapp.data.repository.DeviceRepository
import com.example.myapp.util.MqttTopicUtils
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MQTT 客户端管理器（全局单例）
 * 基于 Eclipse Paho Android Service
 * 
 * 核心功能：
 * 1. 连接管理：自动重连（最多5次，间隔5秒）
 * 2. Topic 规范：严格按照 /user/{uid}/device/{deviceId}/xxx 格式
 * 3. 消息收发：控制指令 QoS 1，状态数据 QoS 0
 * 4. 上下线监测：处理遗嘱消息，更新数据库
 */
@Singleton
class MqttClientManager @Inject constructor(
    private val context: Context,
    private val deviceRepository: DeviceRepository
) {
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // MQTT 客户端
    private var mqttClient: MqttAndroidClient? = null
    
    // 当前用户 ID
    private var currentUserId: String = ""
    
    // 连接状态
    private val _connectionState = MutableStateFlow(MqttConnectionState.DISCONNECTED)
    val connectionState: StateFlow<MqttConnectionState> = _connectionState.asStateFlow()
    
    // 重连计数
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private val reconnectDelayMs = 5000L
    
    // 重连任务
    private var reconnectJob: Job? = null
    
    // 连接参数
    private var brokerUrl: String = ""
    private var clientId: String = ""
    
    companion object {
        private const val QOS_CONTROL = 1  // 控制指令 QoS
        private const val QOS_STATUS = 0   // 状态数据 QoS
        private const val KEEP_ALIVE = 60  // Keep Alive 60秒
    }

    /**
     * 初始化并连接 MQTT
     * @param userId 当前用户 ID
     * @param broker MQTT Broker 地址
     * @param port MQTT 端口
     * @param clientId 客户端 ID
     */
    fun connect(userId: String, broker: String, port: Int, clientId: String) {
        this.currentUserId = userId
        this.brokerUrl = "tcp://$broker:$port"
        this.clientId = clientId
        
        Timber.d("MQTT connecting to $brokerUrl with clientId: $clientId")
        
        // 创建 MQTT 客户端
        mqttClient = MqttAndroidClient(context, brokerUrl, clientId)
        
        // 设置回调
        mqttClient?.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                Timber.w(cause, "MQTT connection lost")
                _connectionState.value = MqttConnectionState.DISCONNECTED
                
                // 自动重连
                scheduleReconnect()
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                topic?.let { t ->
                    message?.let { m ->
                        handleMessage(t, String(m.payload))
                    }
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Timber.d("MQTT message delivered")
            }
        })
        
        // 执行连接
        doConnect()
    }

    /**
     * 执行连接
     */
    private fun doConnect() {
        _connectionState.value = MqttConnectionState.CONNECTING
        
        val options = MqttConnectOptions().apply {
            isCleanSession = false  // 保持会话
            keepAliveInterval = KEEP_ALIVE
            isAutomaticReconnect = false  // 手动控制重连
            connectionTimeout = 30
            
            // 设置遗嘱消息（客户端异常断开时发送）
            setWill(
                MqttTopicUtils.getOnlineTopic(currentUserId, 0),
                gson.toJson(DeviceOnlineMessage(0, false)).toByteArray(),
                QOS_STATUS,
                false
            )
        }
        
        try {
            mqttClient?.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Timber.i("MQTT connected successfully")
                    _connectionState.value = MqttConnectionState.CONNECTED
                    reconnectAttempts = 0
                    
                    // 订阅所有设备的状态和在线 Topic
                    subscribeToAllDevices()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Timber.e(exception, "MQTT connection failed")
                    _connectionState.value = MqttConnectionState.FAILED
                    
                    // 自动重连
                    scheduleReconnect()
                }
            })
        } catch (e: Exception) {
            Timber.e(e, "MQTT connect exception")
            _connectionState.value = MqttConnectionState.FAILED
            scheduleReconnect()
        }
    }

    /**
     * 安排重连
     */
    private fun scheduleReconnect() {
        if (reconnectAttempts >= maxReconnectAttempts) {
            Timber.w("MQTT max reconnect attempts reached")
            _connectionState.value = MqttConnectionState.FAILED
            return
        }
        
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            _connectionState.value = MqttConnectionState.RECONNECTING
            reconnectAttempts++
            
            Timber.d("MQTT reconnecting in ${reconnectDelayMs}ms (attempt $reconnectAttempts/$maxReconnectAttempts)")
            delay(reconnectDelayMs)
            
            doConnect()
        }
    }

    /**
     * 订阅所有设备的 Topic
     */
    private fun subscribeToAllDevices() {
        scope.launch {
            try {
                // 订阅所有设备的状态 Topic
                val statusTopic = MqttTopicUtils.getAllDevicesStatusTopic(currentUserId)
                subscribe(statusTopic, QOS_STATUS)
                
                // 订阅所有设备的在线状态 Topic
                val onlineTopic = MqttTopicUtils.getAllDevicesOnlineTopic(currentUserId)
                subscribe(onlineTopic, QOS_STATUS)
                
                Timber.d("MQTT subscribed to all devices topics")
            } catch (e: Exception) {
                Timber.e(e, "MQTT subscribe failed")
            }
        }
    }

    /**
     * 订阅 Topic
     */
    private fun subscribe(topic: String, qos: Int) {
        try {
            mqttClient?.subscribe(topic, qos, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Timber.d("MQTT subscribed to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Timber.e(exception, "MQTT subscribe failed: $topic")
                }
            })
        } catch (e: Exception) {
            Timber.e(e, "MQTT subscribe exception: $topic")
        }
    }

    /**
     * 发布消息（公共方法）
     * @param topic MQTT主题
     * @param payload 消息内容
     * @param qos QoS等级
     */
    fun publish(topic: String, payload: String, qos: Int = QOS_CONTROL) {
        if (_connectionState.value != MqttConnectionState.CONNECTED) {
            Timber.w("MQTT not connected, cannot publish")
            throw IllegalStateException("MQTT not connected")
        }
        
        publishInternal(topic, payload, qos)
    }
    
    /**
     * 发布控制指令
     * @param deviceId 设备 ID
     * @param action 动作
     * @param value 参数值
     */
    fun publishControl(deviceId: Long, action: String, value: Any? = null) {
        if (_connectionState.value != MqttConnectionState.CONNECTED) {
            Timber.w("MQTT not connected, cannot publish")
            return
        }
        
        val topic = MqttTopicUtils.getControlTopic(currentUserId, deviceId)
        val message = DeviceControlMessage(action, value)
        val payload = gson.toJson(message)
        
        publishInternal(topic, payload, QOS_CONTROL)
    }

    /**
     * 发布消息（内部方法）
     */
    private fun publishInternal(topic: String, payload: String, qos: Int) {
        try {
            val message = MqttMessage(payload.toByteArray()).apply {
                this.qos = qos
                isRetained = false
            }
            
            mqttClient?.publish(topic, message, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Timber.d("MQTT published to $topic: $payload")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Timber.e(exception, "MQTT publish failed: $topic")
                }
            })
        } catch (e: Exception) {
            Timber.e(e, "MQTT publish exception: $topic")
        }
    }

    /**
     * 处理接收到的消息
     */
    private fun handleMessage(topic: String, payload: String) {
        Timber.d("MQTT message received: $topic -> $payload")
        
        // 验证 Topic 格式
        if (!MqttTopicUtils.isValidTopic(topic)) {
            Timber.w("MQTT invalid topic format: $topic")
            return
        }
        
        // 验证 Topic 所有权
        if (!MqttTopicUtils.isTopicOwnedByUser(topic, currentUserId)) {
            Timber.w("MQTT topic not owned by current user: $topic")
            return
        }
        
        // 提取设备 ID
        val deviceId = MqttTopicUtils.extractDeviceId(topic)
        if (deviceId == null) {
            Timber.w("MQTT cannot extract device ID from topic: $topic")
            return
        }
        
        // 根据 Topic 类型处理消息
        when {
            topic.endsWith("/status") -> handleStatusMessage(deviceId, payload)
            topic.endsWith("/environment") -> handleEnvironmentMessage(deviceId, payload)
            topic.endsWith("/online") -> handleOnlineMessage(deviceId, payload)
            else -> Timber.w("MQTT unknown topic type: $topic")
        }
    }

    /**
     * 处理设备状态消息
     */
    private fun handleStatusMessage(deviceId: Long, payload: String) {
        try {
            val status = gson.fromJson(payload, DeviceStatusMessage::class.java)
            
            // 更新数据库
            scope.launch {
                val statusJson = gson.toJson(status)
                deviceRepository.updateDeviceStatus(deviceId, statusJson, currentUserId)
                    .onSuccess {
                        Timber.d("Device $deviceId status updated")
                    }
                    .onFailure {
                        Timber.e(it, "Failed to update device $deviceId status")
                    }
            }
        } catch (e: JsonSyntaxException) {
            Timber.e(e, "MQTT invalid status JSON: $payload")
            // 格式错误直接丢弃，防止崩溃
        }
    }

    /**
     * 处理环境数据消息
     */
    private fun handleEnvironmentMessage(deviceId: Long, payload: String) {
        try {
            val environment = gson.fromJson(payload, EnvironmentMessage::class.java)
            
            // 更新数据库（可以存储到环境数据表）
            Timber.d("Environment data received for device $deviceId: $environment")
            
            // TODO: 存储到 EnvironmentCache 表
        } catch (e: JsonSyntaxException) {
            Timber.e(e, "MQTT invalid environment JSON: $payload")
            // 格式错误直接丢弃，防止崩溃
        }
    }

    /**
     * 处理设备在线状态消息（遗嘱消息）
     */
    private fun handleOnlineMessage(deviceId: Long, payload: String) {
        try {
            val onlineMsg = gson.fromJson(payload, DeviceOnlineMessage::class.java)
            
            // 更新数据库中的在线状态
            scope.launch {
                deviceRepository.updateOnlineStatus(deviceId, onlineMsg.online, currentUserId)
                    .onSuccess {
                        Timber.d("Device $deviceId online status updated: ${onlineMsg.online}")
                    }
                    .onFailure {
                        Timber.e(it, "Failed to update device $deviceId online status")
                    }
            }
        } catch (e: JsonSyntaxException) {
            Timber.e(e, "MQTT invalid online JSON: $payload")
            // 格式错误直接丢弃，防止崩溃
        }
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        reconnectJob?.cancel()
        
        try {
            mqttClient?.disconnect(null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Timber.i("MQTT disconnected")
                    _connectionState.value = MqttConnectionState.DISCONNECTED
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Timber.e(exception, "MQTT disconnect failed")
                }
            })
        } catch (e: Exception) {
            Timber.e(e, "MQTT disconnect exception")
        }
        
        mqttClient?.close()
        mqttClient = null
    }

    /**
     * 检查是否已连接
     */
    fun isConnected(): Boolean {
        return mqttClient?.isConnected == true
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        disconnect()
        scope.cancel()
    }
}

