package com.example.myapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.myapp.engine.CustomAutoControlEngine
import com.example.myapp.mock.MockEngineLifecycleManager
import com.example.myapp.util.WorkManagerScheduler
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var autoControlEngine: CustomAutoControlEngine
    @Inject
    lateinit var mockEngineLifecycleManager: MockEngineLifecycleManager

    override fun onCreate() {
        super.onCreate()
        
        // 初始化 Timber
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        // 启动环境数据采集任务
        WorkManagerScheduler.scheduleEnvironmentDataCollection(this)
        
        // 启动习惯提取任务（每日凌晨2点）
        WorkManagerScheduler.scheduleHabitExtraction(this)
        
        // 启动自动控制引擎
        autoControlEngine.start()
        
        Timber.d("Application initialized")
        if (BuildConfig.IS_MOCK_MODE) {
            mockEngineLifecycleManager.initialize()
        }
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}

