package com.example.myapp.di

import androidx.room.RoomDatabase

/**
 * 数据库环境配置抽象接口
 * 物理隔离：供 main 层调用，具体实现由 mock 和 prod 各自的源码集提供
 */
interface IDatabaseFlavorConfig {
    fun getDatabaseCallback(): RoomDatabase.Callback?
}