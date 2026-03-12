package com.example.myapp.data.model

/**
 * 设备状态数据模型
 * 用于解析设备状态 JSON
 */
data class DeviceStatus(
    val power: String? = null,              // 电源状态: "on", "off"
    val brightness: Int? = null,            // 亮度: 0-100
    val temperature: Float? = null,         // 温度
    val humidity: Float? = null,            // 湿度
    val mode: String? = null,               // 模式
    val speed: Int? = null,                 // 速度
    val color: String? = null,              // 颜色
    val extraData: Map<String, Any>? = null // 其他数据
)

/**
 * 环境条件数据模型
 * 用于解析习惯的环境条件 JSON
 */
data class EnvironmentCondition(
    val temperature: Range? = null,
    val humidity: Range? = null,
    val lightIntensity: Range? = null,
    val pm25: Range? = null
) {
    data class Range(
        val min: Float,
        val max: Float
    )
}

/**
 * 环境数据模型
 * 用于解析操作日志的环境数据 JSON
 */
data class EnvironmentData(
    val temperature: Float? = null,
    val humidity: Float? = null,
    val lightIntensity: Float? = null,
    val pm25: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 设备类型枚举
 */
enum class DeviceType(val typeName: String, val displayName: String) {
    LIGHT("light", "灯光"),
    AIR_CONDITIONER("air_conditioner", "空调"),
    FAN("fan", "风扇"),
    CURTAIN("curtain", "窗帘"),
    SENSOR("sensor", "传感器"),
    SWITCH("switch", "开关"),
    CAMERA("camera", "摄像头"),
    LOCK("lock", "门锁"),
    OTHER("other", "其他");

    companion object {
        fun fromTypeName(typeName: String): DeviceType {
            return values().find { it.typeName == typeName } ?: OTHER
        }
    }
}

/**
 * 设备动作枚举
 */
enum class DeviceAction(val action: String, val displayName: String) {
    TURN_ON("turn_on", "打开"),
    TURN_OFF("turn_off", "关闭"),
    SET_BRIGHTNESS("set_brightness", "设置亮度"),
    SET_TEMPERATURE("set_temperature", "设置温度"),
    SET_MODE("set_mode", "设置模式"),
    INCREASE("increase", "增加"),
    DECREASE("decrease", "减少"),
    OPEN("open", "开启"),
    CLOSE("close", "关闭"),
    LOCK("lock", "锁定"),
    UNLOCK("unlock", "解锁");

    companion object {
        fun fromAction(action: String): DeviceAction? {
            return values().find { it.action == action }
        }
    }
}


