package com.example.myapp.di

import com.example.myapp.BuildConfig
import com.example.myapp.data.network.IMqttClientManager
import com.example.myapp.mock.MockMqttClientManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 通信层依赖注入模块
 * 根据 BuildConfig.IS_MOCK_MODE 动态注入真实或Mock实现
 * 
 * 架构隔离关键点：
 * 1. 所有业务层只依赖 IMqttClientManager 接口
 * 2. 通过 Hilt 在编译时决定注入哪个实现
 * 3. 未来移除 Mock 时，只需删除 mock 包和此模块中的 Mock 绑定
 * 
 * 当前状态：Mock 模式（IS_MOCK_MODE = true）
 * 
 * 移除 Mock 步骤：
 * 1. 创建 RealMqttClientManager 实现 IMqttClientManager
 * 2. 修改下面的 @Binds 绑定为 RealMqttClientManager
 * 3. 设置 BuildConfig.IS_MOCK_MODE = false
 * 4. 删除整个 mock 包
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CommunicationModule {
    
    /**
     * 提供 MQTT 客户端管理器
     * 
     * 当前：注入 MockMqttClientManager（模拟实现）
     * 未来：注入 RealMqttClientManager（真实实现）
     */
    @Binds
    @Singleton
    abstract fun bindMqttClientManager(
        impl: MockMqttClientManager
    ): IMqttClientManager
    
    // TODO: 当硬件就绪后，取消注释并替换上面的绑定
    // @Binds
    // @Singleton
    // abstract fun bindMqttClientManager(
    //     impl: RealMqttClientManager
    // ): IMqttClientManager
}
