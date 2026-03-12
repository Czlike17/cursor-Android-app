package com.example.myapp.data.mqtt

import android.content.Context
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import timber.log.Timber

/**
 * MQTT 客户端管理类
 * * 架构更新说明：
 * 1. [资源泄露修复] connect 时清理旧实例，防止多次重连导致底层 Socket 连接和资源双重泄漏
 * 2. [OOM 修复] release 时强制清空高阶函数回调引用，彻底阻断生命周期泄漏
 */
class MqttManager(
    private val context: Context,
    private val serverUri: String,
    private val clientId: String
) {

    private var mqttClient: MqttAndroidClient? = null
    private var connectionCallback: ((Boolean, String?) -> Unit)? = null
    private var messageCallback: ((String, String) -> Unit)? = null

    /**
     * 连接 MQTT 服务器
     */
    fun connect(
        username: String? = null,
        password: String? = null,
        callback: (Boolean, String?) -> Unit
    ) {
        this.connectionCallback = callback

        // 【核心安全修复 1】：建立新实例前，彻底释放旧实例的句柄和资源
        cleanupOldClient()

        mqttClient = MqttAndroidClient(context, serverUri, clientId)

        val options = MqttConnectOptions().apply {
            isCleanSession = true
            connectionTimeout = 30
            keepAliveInterval = 60
            isAutomaticReconnect = true

            if (username != null) {
                userName = username
            }
            if (password != null) {
                setPassword(password.toCharArray())
            }
        }

        mqttClient?.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                Timber.e(cause, "MQTT connection lost")
                connectionCallback?.invoke(false, cause?.message)
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                if (topic != null && message != null) {
                    val payload = String(message.payload)
                    Timber.d("Message arrived - Topic: $topic, Message: $payload")
                    messageCallback?.invoke(topic, payload)
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Timber.d("Message delivery complete")
            }
        })

        try {
            mqttClient?.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Timber.d("MQTT connected successfully")
                    connectionCallback?.invoke(true, null)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Timber.e(exception, "MQTT connection failed")
                    connectionCallback?.invoke(false, exception?.message)
                }
            })
        } catch (e: MqttException) {
            Timber.e(e, "MQTT connection error")
            connectionCallback?.invoke(false, e.message)
        }
    }

    /**
     * 内部安全辅助方法：清理旧的 MQTT 客户端实例
     */
    private fun cleanupOldClient() {
        mqttClient?.let { client ->
            try {
                client.setCallback(null) // 剥离旧实例的内部回调，防止僵尸回调触发
                if (client.isConnected) {
                    client.disconnect()
                }
                client.close() // 彻底释放底层的 Android Service 和 Socket 资源
            } catch (e: Exception) {
                Timber.e(e, "Error cleaning up old MQTT client")
            } finally {
                mqttClient = null
            }
        }
    }

    /**
     * 订阅主题
     */
    fun subscribe(topic: String, qos: Int = 1, callback: (Boolean, String?) -> Unit) {
        try {
            mqttClient?.subscribe(topic, qos, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Timber.d("Subscribed to topic: $topic")
                    callback(true, null)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Timber.e(exception, "Failed to subscribe to topic: $topic")
                    callback(false, exception?.message)
                }
            })
        } catch (e: MqttException) {
            Timber.e(e, "Subscribe error")
            callback(false, e.message)
        }
    }

    /**
     * 取消订阅
     */
    fun unsubscribe(topic: String, callback: (Boolean, String?) -> Unit) {
        try {
            mqttClient?.unsubscribe(topic, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Timber.d("Unsubscribed from topic: $topic")
                    callback(true, null)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Timber.e(exception, "Failed to unsubscribe from topic: $topic")
                    callback(false, exception?.message)
                }
            })
        } catch (e: MqttException) {
            Timber.e(e, "Unsubscribe error")
            callback(false, e.message)
        }
    }

    /**
     * 发布消息
     */
    fun publish(topic: String, message: String, qos: Int = 1, retained: Boolean = false) {
        try {
            val mqttMessage = MqttMessage().apply {
                payload = message.toByteArray()
                this.qos = qos
                this.isRetained = retained
            }

            mqttClient?.publish(topic, mqttMessage, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Timber.d("Message published to topic: $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Timber.e(exception, "Failed to publish message to topic: $topic")
                }
            })
        } catch (e: MqttException) {
            Timber.e(e, "Publish error")
        }
    }

    /**
     * 设置消息回调
     */
    fun setMessageCallback(callback: (String, String) -> Unit) {
        messageCallback = callback
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        try {
            mqttClient?.disconnect(null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Timber.d("MQTT disconnected")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Timber.e(exception, "Failed to disconnect MQTT")
                }
            })
        } catch (e: MqttException) {
            Timber.e(e, "Disconnect error")
        }
    }

    /**
     * 检查是否已连接
     */
    fun isConnected(): Boolean {
        return mqttClient?.isConnected == true
    }

    /**
     * 释放资源
     */
    fun release() {
        disconnect()
        try {
            mqttClient?.setCallback(null) // 清除内部回调
            mqttClient?.close()
        } catch (e: Exception) {
            Timber.e(e, "Error closing MQTT client in release")
        } finally {
            mqttClient = null

            // 【核心安全修复 2】：强制清空外部高阶函数引用，安全切断与 UI/ViewModel 的关联
            connectionCallback = null
            messageCallback = null
        }
    }
}