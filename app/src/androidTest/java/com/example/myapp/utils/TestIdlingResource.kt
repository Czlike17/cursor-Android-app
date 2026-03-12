package com.example.myapp.utils

import androidx.test.espresso.IdlingResource
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 自定义 IdlingResource
 * 用于等待异步操作完成（如网络请求、数据库查询、协程任务）
 */
class SimpleIdlingResource(private val resourceName: String = "SimpleIdlingResource") : IdlingResource {

    @Volatile
    private var callback: IdlingResource.ResourceCallback? = null

    private val isIdle = AtomicBoolean(true)

    override fun getName(): String = resourceName

    override fun isIdleNow(): Boolean = isIdle.get()

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        this.callback = callback
    }

    /**
     * 设置为忙碌状态
     */
    fun setIdleState(isIdleNow: Boolean) {
        isIdle.set(isIdleNow)
        if (isIdleNow && callback != null) {
            callback?.onTransitionToIdle()
        }
    }

    /**
     * 开始异步操作
     */
    fun beginAsyncOperation() {
        setIdleState(false)
    }

    /**
     * 完成异步操作
     */
    fun endAsyncOperation() {
        setIdleState(true)
    }
}

/**
 * 计数型 IdlingResource
 * 用于跟踪多个并发异步操作
 */
class CountingIdlingResource(private val resourceName: String = "CountingIdlingResource") : IdlingResource {

    @Volatile
    private var callback: IdlingResource.ResourceCallback? = null

    private val counter = AtomicBoolean(false)
    private var count = 0

    override fun getName(): String = resourceName

    override fun isIdleNow(): Boolean = !counter.get()

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        this.callback = callback
    }

    /**
     * 增加计数（开始一个异步操作）
     */
    @Synchronized
    fun increment() {
        count++
        counter.set(true)
    }

    /**
     * 减少计数（完成一个异步操作）
     */
    @Synchronized
    fun decrement() {
        if (count == 0) {
            throw IllegalStateException("Counter is already 0")
        }
        count--
        if (count == 0) {
            counter.set(false)
            callback?.onTransitionToIdle()
        }
    }

    /**
     * 重置计数
     */
    @Synchronized
    fun reset() {
        count = 0
        counter.set(false)
    }
}









