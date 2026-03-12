package com.example.myapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 用户习惯模型表
 * 存储从操作日志中学习到的用户习惯模式
 */
@Entity(
    tableName = "user_habit",
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
        Index(value = ["device_id", "week_type", "username"]),
        Index(value = ["is_enabled", "username"])
    ]
)
data class UserHabit(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "habit_id")
    val id: Long = 0,
    
    @ColumnInfo(name = "device_id")
    val deviceId: Long,
    
    /**
     * 习惯名称
     */
    @ColumnInfo(name = "habit_name")
    val habitName: String,
    
    /**
     * 触发条件，JSON格式
     * 例如: {"time": "18:00-20:00", "weekType": 5, "environment": {...}}
     */
    @ColumnInfo(name = "trigger_condition")
    val triggerCondition: String,
    
    /**
     * 动作指令，JSON格式
     */
    @ColumnInfo(name = "action_command")
    val actionCommand: String,
    
    /**
     * 星期类型：1-7 表示周一到周日，0 表示每天
     */
    @ColumnInfo(name = "week_type")
    val weekType: Int,
    
    /**
     * 时间窗口，格式: "HH:mm-HH:mm"
     */
    @ColumnInfo(name = "time_window")
    val timeWindow: String,
    
    /**
     * 环境阈值，JSON 格式
     * 例如: {"temperature": {"min": 20, "max": 28}, "humidity": {"min": 40, "max": 70}}
     */
    @ColumnInfo(name = "environment_threshold")
    val environmentThreshold: String?,
    
    /**
     * 置信度（0.0-1.0），表示该习惯的可靠程度
     */
    @ColumnInfo(name = "confidence")
    val confidence: Double,
    
    /**
     * 是否启用该习惯
     */
    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,
    
    /**
     * 关联的用户账号
     */
    @ColumnInfo(name = "username")
    val username: String,
    
    /**
     * 创建时间
     */
    @ColumnInfo(name = "create_time")
    val createTime: Long = System.currentTimeMillis(),
    
    /**
     * 最后更新时间
     */
    @ColumnInfo(name = "update_time")
    val updateTime: Long = System.currentTimeMillis()
)


