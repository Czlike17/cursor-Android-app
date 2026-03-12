package com.example.myapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 环境数据缓存表
 * 缓存传感器采集的环境数据
 */
@Entity(
    tableName = "environment_cache",
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
        Index(value = ["timestamp", "username"])
    ]
)
data class EnvironmentCache(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    /**
     * 设备 ID（传感器设备）
     */
    @ColumnInfo(name = "device_id")
    val deviceId: Long,
    
    /**
     * 温度（摄氏度）
     */
    @ColumnInfo(name = "temperature")
    val temperature: Float?,
    
    /**
     * 湿度（百分比）
     */
    @ColumnInfo(name = "humidity")
    val humidity: Float?,
    
    /**
     * 光照强度（lux）
     */
    @ColumnInfo(name = "light_intensity")
    val lightIntensity: Float?,
    
    /**
     * PM2.5 浓度
     */
    @ColumnInfo(name = "pm25")
    val pm25: Float?,
    
    /**
     * 其他环境数据，JSON 格式
     */
    @ColumnInfo(name = "extra_data")
    val extraData: String?,
    
    /**
     * 数据采集时间戳
     */
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    /**
     * 关联的用户账号
     */
    @ColumnInfo(name = "username")
    val username: String
)


