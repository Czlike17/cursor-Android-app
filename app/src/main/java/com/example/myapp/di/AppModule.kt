package com.example.myapp.di

import android.content.Context
import com.example.myapp.data.mqtt.MqttClientManager
import com.example.myapp.data.repository.DeviceRepository
import com.example.myapp.util.PreferencesManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 应用级别依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideMqttClientManager(
        @ApplicationContext context: Context,
        deviceRepository: DeviceRepository
    ): MqttClientManager {
        return MqttClientManager(context, deviceRepository)
    }
}

