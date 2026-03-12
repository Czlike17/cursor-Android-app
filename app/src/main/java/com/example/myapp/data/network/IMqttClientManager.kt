package com.example.myapp.data.network

import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttMessage

/**
 * MQTT 客户端管理器接口
 * 抽象层 - 用于隔离真实实现和Mock实现
 */
interface IMqttClientManager {
    
    /**
     * 连接到MQTT服务器
     */
    fun connect(
        serverUri: String,
        clientId: String,
        username: String?,
        password: String?,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit
    )
    
    /**
     * 断开连接
     */
    fun disconnect()
    
    /**
     * 订阅主题
     */
    fun subscribe(
        topic: String,
        qos: Int = 1,
        onMessageArrived: (topic: String, message: MqttMessage) -> Unit
    )
    
    /**
     * 取消订阅
     */
    fun unsubscribe(topic: String)
    
    /**
     * 发布消息
     */
    fun publish(
        topic: String,
        payload: String,
        qos: Int = 1,
        retained: Boolean = false,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((Throwable) -> Unit)? = null
    )
    
    /**
     * 检查连接状态
     */
    fun isConnected(): Boolean
    
    /**
     * 设置连接丢失回调
     */
    fun setConnectionLostCallback(callback: (Throwable?) -> Unit)
    
    /**
     * 释放资源
     */
    fun release()
}

















