package com.example.myapp.mock

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.myapp.BuildConfig
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mock 引擎生命周期管理器
 * 
 * 负责管理所有 Mock 引擎的生命周期：
 * 1. 监听 App 前后台切换
 * 2. 自动启动/停止环境数据生成器
 * 3. 管理资源释放
 * 
 * 注意：仅在 IS_MOCK_MODE = true 时初始化
 */
@Singleton
class MockEngineLifecycleManager @Inject constructor(
    private val application: Application,
    private val unifiedMockSystem: UnifiedMockSystem
) : DefaultLifecycleObserver {
    
    private var isInitialized = false
    
    /**
     * 初始化生命周期监听
     */
    fun initialize() {
        if (!BuildConfig.IS_MOCK_MODE) {
            Timber.w("[MOCK] Mock mode is disabled, skipping initialization")
            return
        }
        
        if (isInitialized) {
            Timber.w("[MOCK] MockEngineLifecycleManager already initialized")
            return
        }
        
        Timber.d("[MOCK] Initializing MockEngineLifecycleManager")
        
        // 注册生命周期监听
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        
        // 初始化统一 Mock 系统
        unifiedMockSystem.initialize()
        
        isInitialized = true
        Timber.i("[MOCK] MockEngineLifecycleManager initialized successfully")
    }
    
    /**
     * App 进入前台
     */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Timber.d("[MOCK] App entered foreground")
        // 环境数据生成器已在 UnifiedMockSystem.initialize() 中启动
    }
    
    /**
     * App 进入后台
     */
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Timber.d("[MOCK] App entered background")
        // 可以选择在后台停止环境数据生成以节省资源
        // 但为了保持数据连续性，这里选择继续运行
    }
    
    /**
     * App 销毁
     */
    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        Timber.d("[MOCK] App destroyed, releasing resources")
        unifiedMockSystem.release()
    }
}
