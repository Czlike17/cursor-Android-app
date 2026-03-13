package com.example.myapp.di

import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 生产环境的数据库配置
 */
@Module
@InstallIn(SingletonComponent::class)
object FlavorDatabaseModule {

    @Provides
    @Singleton
    fun provideDatabaseFlavorConfig(): IDatabaseFlavorConfig {
        return object : IDatabaseFlavorConfig {
            // 生产环境绝对纯净，不提供任何预置数据 Callback
            override fun getDatabaseCallback(): RoomDatabase.Callback? = null
        }
    }
}