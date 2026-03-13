package com.example.myapp.data.local

import androidx.room.RoomDatabase

/**
 * Mock 环境专用的数据库初始化引擎 (V3 终极版)
 * 已彻底废弃底层危险的 SQL 硬编码注入！
 * 所有的测试设备注入已全部交由 UnifiedMockSystem 在 App 启动时响应式完成。
 */
class MockDatabaseCallback : RoomDatabase.Callback() {
    // 留空即可，不再干预真实的数据库生命周期
}