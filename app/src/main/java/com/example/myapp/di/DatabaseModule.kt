package com.example.myapp.di

import android.content.Context
import com.example.myapp.data.local.AppDatabase
import com.example.myapp.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据库依赖注入模块
 * 提供数据库实例和所有 DAO
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * 提供数据库实例
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    /**
     * 提供用户 DAO
     */
    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    /**
     * 提供设备 DAO
     */
    @Provides
    @Singleton
    fun provideDeviceDao(database: AppDatabase): DeviceDao {
        return database.deviceDao()
    }

    /**
     * 提供用户操作日志 DAO
     */
    @Provides
    @Singleton
    fun provideUserHabitLogDao(database: AppDatabase): UserHabitLogDao {
        return database.userHabitLogDao()
    }

    /**
     * 提供用户习惯模型 DAO
     */
    @Provides
    @Singleton
    fun provideUserHabitDao(database: AppDatabase): UserHabitDao {
        return database.userHabitDao()
    }

    /**
     * 提供环境数据缓存 DAO
     */
    @Provides
    @Singleton
    fun provideEnvironmentCacheDao(database: AppDatabase): EnvironmentCacheDao {
        return database.environmentCacheDao()
    }

    /**
     * 提供自动控制日志 DAO
     */
    @Provides
    @Singleton
    fun provideAutoControlLogDao(database: AppDatabase): AutoControlLogDao {
        return database.autoControlLogDao()
    }
}

