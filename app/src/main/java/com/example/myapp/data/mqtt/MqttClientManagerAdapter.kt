package com.example.myapp.data.mqtt

import com.example.myapp.data.network.IMqttClientManager
import org.eclipse.paho.client.mqttv3.MqttMessage
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MQTT 客户端管理器适配器
 * 将旧的 MqttManager 适配到 IMqttClientManager 接口
 * 
 * 注意: 这是真实实现的适配器,当 IS_MOCK_MODE = false 时使用
 */
@Singleton
class MqttClientManagerAdapter @Inject constructor(
    private val mqttManager: MqttManager
) : IMqttClientManager {
    
    private val messageCallbacks = mutableMapOf<String, (String, MqttMessage) -> Unit>()
    
    override fun connect(
        serverUri: String,
        clientId: String,
        username: String?,
        password: String?,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        Timber.d("Connecting to MQTT: $serverUri")
        
        mqttManager.connect(username, password) { success, error ->
            if (success) {
                onSuccess()
            } else {
                onFailure(Exception(error ?: "Connection failed"))
            }
        }
    }
    
    override fun disconnect() {
        mqttManager.disconnect()
    }
    
    override fun subscribe(
        topic: String,
        qos: Int,
        onMessageArrived: (topic: String, message: MqttMessage) -> Unit
    ) {
        messageCallbacks[topic] = onMessageArrived
        
        mqttManager.subscribe(topic, qos) { success, error ->
            if (!success) {
                Timber.e("Failed to subscribe to $topic: $error")
            }
        }
        
        // 设置消息回调
        mqttManager.setMessageCallback { receivedTopic, payload ->
            messageCallbacks[receivedTopic]?.let { callback ->
                val message = MqttMessage(payload.toByteArray())
                callback(receivedTopic, message)
            }
        }
    }
    
    override fun unsubscribe(topic: String) {
        messageCallbacks.remove(topic)
        mqttManager.unsubscribe(topic) { _, _ -> }
    }
    
    override fun publish(
        topic: String,
        payload: String,
        qos: Int,
        retained: Boolean,
        onSuccess: (() -> Unit)?,
        onFailure: ((Throwable) -> Unit)?
    ) {
        try {
            mqttManager.publish(topic, payload, qos, retained)
            onSuccess?.invoke()
        } catch (e: Exception) {
            onFailure?.invoke(e)
        }
    }
    
    override fun isConnected(): Boolean {
        return mqttManager.isConnected()
    }
    
    override fun setConnectionLostCallback(callback: (Throwable?) -> Unit) {
        // MqttManager 已经在内部处理连接丢失
    }
    
    override fun release() {
        mqttManager.release()
    }
}

















