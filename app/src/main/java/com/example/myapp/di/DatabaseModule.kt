package com.example.myapp.di

import android.content.Context
import androidx.room.Room
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
 * 架构升级：完全由 Hilt 接管数据库实例化，并动态支持 Flavor 级回调注入
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        flavorConfig: IDatabaseFlavorConfig // 【核心】：动态获取当前 Flavor 的配置
    ): AppDatabase {
        val builder = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "smart_home_database"
        ).fallbackToDestructiveMigration()

        // 如果当前是 Mock 环境，这里会自动挂载填充假数据的 Callback
        // 如果是 Prod 环境，返回 null，什么都不做
        flavorConfig.getDatabaseCallback()?.let {
            builder.addCallback(it)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()

    @Provides
    @Singleton
    fun provideDeviceDao(database: AppDatabase): DeviceDao = database.deviceDao()

    @Provides
    @Singleton
    fun provideUserHabitLogDao(database: AppDatabase): UserHabitLogDao = database.userHabitLogDao()

    @Provides
    @Singleton
    fun provideUserHabitDao(database: AppDatabase): UserHabitDao = database.userHabitDao()

    @Provides
    @Singleton
    fun provideEnvironmentCacheDao(database: AppDatabase): EnvironmentCacheDao = database.environmentCacheDao()

    @Provides
    @Singleton
    fun provideAutoControlLogDao(database: AppDatabase): AutoControlLogDao = database.autoControlLogDao()

    @Provides
    @Singleton
    fun provideOfflineCommandDao(database: AppDatabase): OfflineCommandDao = database.offlineCommandDao()
}