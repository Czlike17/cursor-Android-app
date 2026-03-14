package com.example.myapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application(), Configuration.Provider {

    // 【核心注入】：引入 Hilt 专用的 Worker 工厂
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // 初始化日志工具
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    // 【核心修复】：接管全局 WorkManager 的配置，强制使用 Hilt 的工厂来实例化 Worker
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    // 注意：如果你的 androidx.work 版本较低，可能需要重写 getWorkManagerConfiguration() 方法，如下所示：
    // override fun getWorkManagerConfiguration(): Configuration {
    //     return Configuration.Builder()
    //         .setWorkerFactory(workerFactory)
    //         .build()
    // }
}