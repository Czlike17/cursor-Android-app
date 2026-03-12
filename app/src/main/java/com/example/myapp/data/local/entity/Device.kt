package com.example.myapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 设备表
 * 存储智能家居设备信息，支持多账号数据隔离
 */
@Entity(
    tableName = "device",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["username"],
            childColumns = ["username"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["username"]),
        Index(value = ["device_id", "username"]),
        Index(value = ["room_id", "username"])
    ]
)
data class Device(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "device_id")
    val deviceId: Long = 0,
    
    @ColumnInfo(name = "device_name")
    val deviceName: String,
    
    @ColumnInfo(name = "device_type")
    val deviceType: String,
    
    @ColumnInfo(name = "mqtt_broker")
    val mqttBroker: String,
    
    @ColumnInfo(name = "mqtt_port")
    val mqttPort: Int,
    
    @ColumnInfo(name = "subscribe_topic")
    val subscribeTopic: String,
    
    @ColumnInfo(name = "publish_topic")
    val publishTopic: String,
    
    @ColumnInfo(name = "client_id")
    val clientId: String,
    
    /**
     * 设备状态，JSON 格式存储
     * 例如: {"power": "on", "brightness": 80, "temperature": 25}
     */
    @ColumnInfo(name = "status")
    val status: String,
    
    @ColumnInfo(name = "is_online")
    val isOnline: Boolean = false,
    
    @ColumnInfo(name = "room_id")
    val roomId: Long? = null,
    
    @ColumnInfo(name = "create_time")
    val createTime: Long = System.currentTimeMillis(),
    
    /**
     * 关联的用户账号，用于数据隔离
     */
    @ColumnInfo(name = "username")
    val username: String
)


