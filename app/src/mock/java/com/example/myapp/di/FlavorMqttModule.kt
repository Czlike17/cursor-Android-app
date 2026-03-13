package com.example.myapp.di

import android.content.Context
import com.example.myapp.data.mqtt.MqttManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import timber.log.Timber

/**
 * 模拟环境特有的注入：彻底阻断真实网络请求
 */
@Module
@InstallIn(SingletonComponent::class)
object FlavorMqttModule {

    @Provides
    @Singleton
    fun provideMqttManager(@ApplicationContext context: Context): MqttManager {
        Timber.w("=== MOCK FLAVOR ACTIVATED: 注入短路 MqttManager ===")
        // Mock 模式下提供一个绝对无法连接外部网络的假地址。
        // Repository 层调用的依然是真正的 MqttManager 代码，但它的指令发不出去。
        // 这为我们下一阶段引入本地 Room 虚拟引擎留出了完美的空间。
        return MqttManager(
            context = context,
            serverUri = "tcp://mock.local.internal:1883",
            clientId = "mock_client_isolated"
        )
    }
}