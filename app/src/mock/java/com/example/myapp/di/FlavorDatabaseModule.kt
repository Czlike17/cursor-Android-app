package com.example.myapp.di

import androidx.room.RoomDatabase
import com.example.myapp.data.local.MockDatabaseCallback
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 模拟环境特有的数据库配置：注入预置假数据回调
 */
@Module
@InstallIn(SingletonComponent::class)
object FlavorDatabaseModule {

    @Provides
    @Singleton
    fun provideDatabaseFlavorConfig(): IDatabaseFlavorConfig {
        return object : IDatabaseFlavorConfig {
            override fun getDatabaseCallback(): RoomDatabase.Callback {
                // 直接返回实例，无需任何 Provider 依赖，彻底消除死锁风险
                return MockDatabaseCallback()
            }
        }
    }
}