package com.example.myapp.data.model

/**
 * MQTT 消息数据模型
 */

/**
 * 设备控制指令
 */
data class DeviceControlMessage(
    val action: String,              // 动作: turn_on, turn_off, set_brightness, etc.
    val value: Any? = null,          // 参数值
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 设备状态消息
 */
data class DeviceStatusMessage(
    val deviceId: Long,
    val power: String? = null,       // on/off
    val brightness: Int? = null,     // 0-100
    val temperature: Float? = null,
    val humidity: Float? = null,
    val mode: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 环境数据消息
 */
data class EnvironmentMessage(
    val deviceId: Long,
    val temperature: Float? = null,
    val humidity: Float? = null,
    val lightIntensity: Float? = null,
    val pm25: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 设备在线状态消息
 */
data class DeviceOnlineMessage(
    val deviceId: Long,
    val online: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * MQTT 连接状态
 */
enum class MqttConnectionState {
    DISCONNECTED,    // 断开连接
    CONNECTING,      // 连接中
    CONNECTED,       // 已连接
    RECONNECTING,    // 重连中
    FAILED           // 连接失败
}

















