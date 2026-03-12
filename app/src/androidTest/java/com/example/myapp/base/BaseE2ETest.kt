package com.example.myapp.base

import androidx.test.espresso.IdlingRegistry
import com.example.myapp.data.local.AppDatabase
import com.example.myapp.utils.CountingIdlingResource
import com.example.myapp.utils.EspressoTestUtils
import dagger.hilt.android.testing.HiltAndroidRule
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import javax.inject.Inject

/**
 * 测试基类
 * 提供统一的测试环境配置和工具方法
 * 
 * 功能：
 * 1. 自动注入 Hilt 依赖
 * 2. 管理 IdlingResource 注册/注销
 * 3. 提供数据库清理和初始化方法
 * 4. 统一异常处理和日志输出
 */
abstract class BaseE2ETest {

    @get:org.junit.Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var database: AppDatabase

    // IdlingResource 用于等待异步操作
    protected val idlingResource = CountingIdlingResource("BaseE2ETestIdlingResource")

    @Before
    open fun baseSetup() {
        // 注入 Hilt 依赖
        hiltRule.inject()
        
        // 注册 IdlingResource
        IdlingRegistry.getInstance().register(idlingResource)
        
        // 清空数据库
        runBlocking {
            database.clearAllTables()
        }
        
        println("=== 测试环境初始化完成 ===")
    }

    @After
    open fun baseTearDown() {
        // 注销 IdlingResource
        IdlingRegistry.getInstance().unregister(idlingResource)
        
        println("=== 测试环境清理完成 ===")
    }

    /**
     * 安全延迟（使用工具类）
     */
    protected fun safeDelay(delayMs: Long) {
        EspressoTestUtils.safeDelay(delayMs)
    }

    /**
     * 等待刷新完成
     */
    protected fun waitForRefresh(swipeRefreshId: Int, maxWaitMs: Long = 3000) {
        EspressoTestUtils.waitForRefreshComplete(swipeRefreshId, maxWaitMs)
    }

    /**
     * 打印测试日志
     */
    protected fun logTest(message: String) {
        println("[TEST] $message")
    }

    /**
     * 打印警告日志
     */
    protected fun logWarning(message: String) {
        println("[WARNING] $message")
    }

    /**
     * 打印错误日志
     */
    protected fun logError(message: String, throwable: Throwable? = null) {
        println("[ERROR] $message")
        throwable?.printStackTrace()
    }
}









