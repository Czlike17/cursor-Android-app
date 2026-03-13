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
 * 生产环境特有的注入：连接真实的物理硬件/服务器
 */
@Module
@InstallIn(SingletonComponent::class)
object FlavorMqttModule {

    @Provides
    @Singleton
    fun provideMqttManager(@ApplicationContext context: Context): MqttManager {
        // 这里填写真实的生产环境服务器地址
        val serverUri = "tcp://real.production.server:1883"
        val clientId = "prod_client_${System.currentTimeMillis()}"

        return MqttManager(
            context = context,
            serverUri = serverUri,
            clientId = clientId
        )
    }
}