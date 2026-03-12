package com.example.myapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 自动控制日志表
 * 记录系统自动控制设备的操作记录
 */
@Entity(
    tableName = "auto_control_log",
    foreignKeys = [
        ForeignKey(
            entity = Device::class,
            parentColumns = ["device_id"],
            childColumns = ["device_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserHabit::class,
            parentColumns = ["habit_id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.SET_NULL
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
        Index(value = ["habit_id", "username"]),
        Index(value = ["execute_time", "username"]),
        Index(value = ["is_success", "username"])
    ]
)
data class AutoControlLog(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "device_id")
    val deviceId: Long,
    
    /**
     * 触发的习惯 ID，可能为空（如果是其他规则触发）
     */
    @ColumnInfo(name = "habit_id")
    val habitId: Long?,
    
    /**
     * 控制动作
     */
    @ColumnInfo(name = "action")
    val action: String,
    
    /**
     * 触发原因
     */
    @ColumnInfo(name = "trigger_reason")
    val triggerReason: String,
    
    /**
     * 执行时间
     */
    @ColumnInfo(name = "execute_time")
    val executeTime: Long,
    
    /**
     * 是否执行成功
     */
    @ColumnInfo(name = "is_success")
    val isSuccess: Boolean,
    
    /**
     * 执行结果描述
     */
    @ColumnInfo(name = "result_message")
    val resultMessage: String?,
    
    /**
     * 执行时的环境数据，JSON 格式
     */
    @ColumnInfo(name = "environment_data")
    val environmentData: String?,
    
    /**
     * 关联的用户账号
     */
    @ColumnInfo(name = "username")
    val username: String
)


