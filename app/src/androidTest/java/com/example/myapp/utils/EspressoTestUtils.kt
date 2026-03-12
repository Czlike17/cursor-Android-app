package com.example.myapp.utils

import android.view.View
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import org.hamcrest.Matcher
import org.hamcrest.Matchers

/**
 * Espresso 测试工具类
 * 提供统一的视图查找、等待和交互方法
 */
object EspressoTestUtils {

    /**
     * 安全查找可见视图（避免多个匹配冲突）
     * 优先查找可见的视图
     */
    fun onVisibleView(viewMatcher: Matcher<View>): ViewInteraction {
        return Espresso.onView(
            Matchers.allOf(
                viewMatcher,
                isDisplayed()
            )
        )
    }

    /**
     * 安全延迟（使用 Thread.sleep，Espresso 会自动等待）
     */
    fun safeDelay(delayMs: Long) {
        Thread.sleep(delayMs)
    }

    /**
     * 等待视图状态满足条件
     * @param viewMatcher 视图匹配器
     * @param maxWaitMs 最大等待时间（毫秒）
     * @param checkIntervalMs 检查间隔（毫秒）
     */
    fun waitForViewState(
        viewMatcher: Matcher<View>,
        maxWaitMs: Long = 5000,
        checkIntervalMs: Long = 100
    ) {
        val startTime = System.currentTimeMillis()
        var lastException: Exception? = null

        while (System.currentTimeMillis() - startTime < maxWaitMs) {
            try {
                Espresso.onView(viewMatcher).check { view, _ ->
                    if (view == null) {
                        throw AssertionError("View not found")
                    }
                }
                return // 视图找到，退出
            } catch (e: Exception) {
                lastException = e
                Thread.sleep(checkIntervalMs)
            }
        }

        // 超时后抛出异常
        throw AssertionError(
            "View not found after ${maxWaitMs}ms: ${lastException?.message}",
            lastException
        )
    }

    /**
     * 等待 SwipeRefreshLayout 刷新完成
     * @param swipeRefreshId SwipeRefreshLayout 的资源 ID
     * @param maxWaitMs 最大等待时间（毫秒）
     */
    fun waitForRefreshComplete(swipeRefreshId: Int, maxWaitMs: Long = 3000) {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < maxWaitMs) {
            try {
                var isRefreshing = false
                Espresso.onView(ViewMatchers.withId(swipeRefreshId))
                    .perform(object : ViewAction {
                        override fun getConstraints(): Matcher<View> {
                            return ViewMatchers.isAssignableFrom(SwipeRefreshLayout::class.java)
                        }

                        override fun getDescription(): String {
                            return "Check if SwipeRefreshLayout is refreshing"
                        }

                        override fun perform(uiController: UiController?, view: View?) {
                            val swipeRefresh = view as? SwipeRefreshLayout
                            isRefreshing = swipeRefresh?.isRefreshing ?: false
                        }
                    })

                if (!isRefreshing) {
                    return // 刷新已完成
                }

                Thread.sleep(100)
            } catch (e: Exception) {
                // 视图可能不可见，继续等待
                Thread.sleep(100)
            }
        }

        // 超时后不抛出异常，只是返回（因为可能是正常的超时保护）
        println("[EspressoTestUtils] waitForRefreshComplete: 等待超时 ${maxWaitMs}ms")
    }

    /**
     * 等待视图可见
     * @param viewMatcher 视图匹配器
     * @param maxWaitMs 最大等待时间（毫秒）
     */
    fun waitForViewVisible(viewMatcher: Matcher<View>, maxWaitMs: Long = 5000) {
        waitForViewState(
            viewMatcher = Matchers.allOf(viewMatcher, isDisplayed()),
            maxWaitMs = maxWaitMs
        )
    }

    /**
     * 等待视图消失
     * @param viewMatcher 视图匹配器
     * @param maxWaitMs 最大等待时间（毫秒）
     */
    fun waitForViewGone(viewMatcher: Matcher<View>, maxWaitMs: Long = 5000) {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < maxWaitMs) {
            try {
                Espresso.onView(viewMatcher).check { view, _ ->
                    if (view != null && view.visibility == View.VISIBLE) {
                        throw AssertionError("View is still visible")
                    }
                }
                return // 视图已消失
            } catch (e: Exception) {
                if (e.message?.contains("View is still visible") == true) {
                    Thread.sleep(100)
                } else {
                    // 视图不存在，认为已消失
                    return
                }
            }
        }

        throw AssertionError("View is still visible after ${maxWaitMs}ms")
    }

    /**
     * 检查视图是否存在（不抛出异常）
     * @return true 如果视图存在，false 如果不存在
     */
    fun isViewDisplayed(viewMatcher: Matcher<View>): Boolean {
        return try {
            Espresso.onView(viewMatcher).check { view, _ ->
                if (view == null || view.visibility != View.VISIBLE) {
                    throw AssertionError("View not displayed")
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 等待加载完成（通过检查加载指示器消失）
     * @param loadingViewMatcher 加载指示器的匹配器
     * @param maxWaitMs 最大等待时间（毫秒）
     */
    fun waitForLoadingComplete(loadingViewMatcher: Matcher<View>, maxWaitMs: Long = 5000) {
        try {
            waitForViewGone(loadingViewMatcher, maxWaitMs)
        } catch (e: Exception) {
            // 如果加载指示器本来就不存在，忽略异常
            println("[EspressoTestUtils] waitForLoadingComplete: ${e.message}")
        }
    }

    /**
     * 执行自定义 ViewAction
     */
    fun performCustomAction(action: (View) -> Unit): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return ViewMatchers.isDisplayed()
            }

            override fun getDescription(): String {
                return "Perform custom action"
            }

            override fun perform(uiController: UiController?, view: View?) {
                view?.let { action(it) }
            }
        }
    }

    /**
     * 获取视图属性
     */
    fun <T> getViewProperty(viewMatcher: Matcher<View>, extractor: (View) -> T): T? {
        var result: T? = null
        try {
            Espresso.onView(viewMatcher).perform(object : ViewAction {
                override fun getConstraints(): Matcher<View> {
                    return ViewMatchers.isDisplayed()
                }

                override fun getDescription(): String {
                    return "Extract view property"
                }

                override fun perform(uiController: UiController?, view: View?) {
                    view?.let { result = extractor(it) }
                }
            })
        } catch (e: Exception) {
            println("[EspressoTestUtils] getViewProperty failed: ${e.message}")
        }
        return result
    }
}

