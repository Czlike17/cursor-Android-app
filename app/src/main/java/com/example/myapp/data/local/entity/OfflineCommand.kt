package com.example.myapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 离线指令缓存表
 * 当网络不可用时，缓存控制指令，待网络恢复后自动补发
 */
@Entity(
    tableName = "offline_command",
    foreignKeys = [
        ForeignKey(
            entity = Device::class,
            parentColumns = ["device_id"],
            childColumns = ["device_id"],
            onDelete = ForeignKey.CASCADE
        ),
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
        Index(value = ["create_time", "username"])
    ]
)
data class OfflineCommand(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "device_id")
    val deviceId: Long,
    
    /**
     * MQTT 发布主题
     */
    @ColumnInfo(name = "topic")
    val topic: String,
    
    /**
     * 指令内容（JSON 格式）
     */
    @ColumnInfo(name = "payload")
    val payload: String,
    
    /**
     * QoS 等级
     */
    @ColumnInfo(name = "qos")
    val qos: Int = 1,
    
    /**
     * 创建时间戳
     */
    @ColumnInfo(name = "create_time")
    val createTime: Long = System.currentTimeMillis(),
    
    /**
     * 重试次数
     */
    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,
    
    /**
     * 关联的用户账号
     */
    @ColumnInfo(name = "username")
    val username: String
)

















