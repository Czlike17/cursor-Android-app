package com.example.myapp.util

/**
 * MQTT Topic 工具类
 * 严格按照 /user/{uid}/device/{deviceId}/xxx 格式生成 Topic
 * 防止越权控制
 */
object MqttTopicUtils {

    /**
     * 生成设备控制 Topic
     * 格式: /user/{uid}/device/{deviceId}/control
     */
    fun getControlTopic(uid: String, deviceId: Long): String {
        return "/user/$uid/device/$deviceId/control"
    }

    /**
     * 生成设备状态 Topic
     * 格式: /user/{uid}/device/{deviceId}/status
     */
    fun getStatusTopic(uid: String, deviceId: Long): String {
        return "/user/$uid/device/$deviceId/status"
    }

    /**
     * 生成设备环境数据 Topic
     * 格式: /user/{uid}/device/{deviceId}/environment
     */
    fun getEnvironmentTopic(uid: String, deviceId: Long): String {
        return "/user/$uid/device/$deviceId/environment"
    }

    /**
     * 生成设备在线状态 Topic (遗嘱消息)
     * 格式: /user/{uid}/device/{deviceId}/online
     */
    fun getOnlineTopic(uid: String, deviceId: Long): String {
        return "/user/$uid/device/$deviceId/online"
    }

    /**
     * 生成用户所有设备的通配符 Topic
     * 格式: /user/{uid}/device/+/status
     */
    fun getAllDevicesStatusTopic(uid: String): String {
        return "/user/$uid/device/+/status"
    }

    /**
     * 生成用户所有设备的在线状态通配符 Topic
     * 格式: /user/{uid}/device/+/online
     */
    fun getAllDevicesOnlineTopic(uid: String): String {
        return "/user/$uid/device/+/online"
    }

    /**
     * 从 Topic 中提取设备 ID
     * 例如: /user/123/device/456/status -> 456
     */
    fun extractDeviceId(topic: String): Long? {
        return try {
            val regex = Regex("/user/\\d+/device/(\\d+)/.*")
            val matchResult = regex.find(topic)
            matchResult?.groupValues?.get(1)?.toLongOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 验证 Topic 格式是否合法
     */
    fun isValidTopic(topic: String): Boolean {
        val pattern = Regex("^/user/\\d+/device/(\\d+|\\+)/(control|status|environment|online)$")
        return pattern.matches(topic)
    }

    /**
     * 检查 Topic 是否属于指定用户
     */
    fun isTopicOwnedByUser(topic: String, uid: String): Boolean {
        return topic.startsWith("/user/$uid/device/")
    }
}

















