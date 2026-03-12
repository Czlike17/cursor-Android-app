package com.example.myapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 用户操作日志表
 * 记录用户对设备的操作行为，用于习惯学习
 */
@Entity(
    tableName = "user_habit_log",
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
        Index(value = ["device_id", "operate_time", "username"]),
        Index(value = ["week_type", "username"])
    ]
)
data class UserHabitLog(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "device_id")
    val deviceId: Long,
    
    /**
     * 操作动作，如 "turn_on", "turn_off", "set_brightness"
     */
    @ColumnInfo(name = "action")
    val action: String,
    
    /**
     * 操作时间戳
     */
    @ColumnInfo(name = "operate_time")
    val operateTime: Long,
    
    /**
     * 星期类型：1-7 表示周一到周日
     */
    @ColumnInfo(name = "week_type")
    val weekType: Int,
    
    /**
     * 环境数据，JSON 格式
     * 例如: {"temperature": 25, "humidity": 60, "light": 300}
     */
    @ColumnInfo(name = "environment_data")
    val environmentData: String?,
    
    /**
     * 是否手动取消了自动控制
     */
    @ColumnInfo(name = "is_manual_cancel")
    val isManualCancel: Boolean = false,
    
    /**
     * 关联的用户账号
     */
    @ColumnInfo(name = "username")
    val username: String
)


