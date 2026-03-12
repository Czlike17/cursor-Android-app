package com.example.myapp.di

import android.content.Context
import com.example.myapp.data.mqtt.MqttManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * MQTT 模块
 * 提供 MqttManager 实例
 */
@Module
@InstallIn(SingletonComponent::class)
object MqttModule {
    
    @Provides
    @Singleton
    fun provideMqttManager(
        @ApplicationContext context: Context
    ): MqttManager {
        // Mock 模式下的配置可以是任意值,因为会被 MockMqttClientManager 拦截
        val serverUri = "tcp://mock.mqtt.server:1883"
        val clientId = "mock_client_${System.currentTimeMillis()}"
        
        return MqttManager(
            context = context,
            serverUri = serverUri,
            clientId = clientId
        )
    }
}

















