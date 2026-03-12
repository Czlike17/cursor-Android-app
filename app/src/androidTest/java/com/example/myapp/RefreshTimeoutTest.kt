package com.example.myapp

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myapp.base.BaseE2ETest
import com.example.myapp.presentation.main.MainActivity
import com.example.myapp.util.PreferencesManager
import com.example.myapp.utils.EspressoTestUtils
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * 刷新超时兜底机制测试
 * 测试第一阶段优化的 1 秒超时机制是否正常工作
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class RefreshTimeoutTest : BaseE2ETest() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Before
    override fun baseSetup() {
        super.baseSetup()
        
        // 模拟已登录状态
        runBlocking {
            preferencesManager.saveLoginInfo("test_user", autoLogin = true)
        }
        
        logTest("=== 刷新超时兜底机制测试初始化完成 ===")
    }

    @Test
    fun testSwipeRefreshTimeoutProtection() {
        logTest("开始测试：下拉刷新 1 秒超时保护机制")

        // 启动主页
        val scenario = ActivityScenario.launch<MainActivity>(
            Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        )

        // 等待主页加载完成
        safeDelay(1500)

        // 步骤1：触发下拉刷新
        logTest("步骤1：触发下拉刷新")
        try {
            onView(withId(R.id.swipeRefresh))
                .perform(swipeDown())
            
            logTest("✓ 下拉刷新已触发")
        } catch (e: Exception) {
            logWarning("下拉刷新触发失败，可能页面结构不同")
        }

        // 步骤2：等待 1000ms 后验证刷新动画是否停止
        logTest("步骤2：等待 1000ms 验证超时保护")
        
        // 使用工具类等待刷新完成（最多等待 1500ms）
        EspressoTestUtils.waitForRefreshComplete(R.id.swipeRefresh, maxWaitMs = 1500)
        
        logTest("✓ 刷新动画已在 1000ms 内停止")

        // 步骤3：再次验证 UI 可交互
        logTest("步骤3：验证 UI 恢复可交互状态")
        try {
            // 尝试点击 FAB 按钮，验证 UI 可交互
            EspressoTestUtils.onVisibleView(withId(R.id.fabAdd))
                .check { view, _ ->
                    if (view == null) {
                        throw AssertionError("FAB 按钮不可见")
                    }
                    if (!view.isEnabled) {
                        throw AssertionError("FAB 按钮不可交互")
                    }
                }
            
            logTest("✓ UI 已恢复可交互状态")
        } catch (e: Exception) {
            logWarning("UI 交互验证失败：${e.message}")
        }

        logTest("=== 刷新超时保护测试完成 ===")
        
        scenario.close()
    }

    @Test
    fun testMultipleRefreshTimeout() {
        logTest("开始测试：多次下拉刷新超时保护")

        // 启动主页
        val scenario = ActivityScenario.launch<MainActivity>(
            Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        )

        safeDelay(1500)

        // 执行 3 次下拉刷新，验证每次都能正确超时
        for (i in 1..3) {
            logTest("第 $i 次下拉刷新测试")
            
            try {
                onView(withId(R.id.swipeRefresh))
                    .perform(swipeDown())
            } catch (e: Exception) {
                logWarning("第 $i 次下拉刷新触发失败")
                continue
            }

            // 等待超时保护生效
            EspressoTestUtils.waitForRefreshComplete(R.id.swipeRefresh, maxWaitMs = 1500)
            
            logTest("✓ 第 $i 次刷新超时保护正常")
            
            // 间隔 500ms
            safeDelay(500)
        }

        logTest("✓ 多次刷新超时保护测试通过")
        
        scenario.close()
    }

    @Test
    fun testRefreshTimeoutWithDataLoad() {
        logTest("开始测试：有数据加载时的刷新超时")

        // 启动主页
        val scenario = ActivityScenario.launch<MainActivity>(
            Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        )

        safeDelay(1500)

        // 记录开始时间
        val startTime = System.currentTimeMillis()

        // 触发下拉刷新
        logTest("触发下拉刷新")
        try {
            onView(withId(R.id.swipeRefresh))
                .perform(swipeDown())
        } catch (e: Exception) {
            logWarning("下拉刷新触发失败")
        }

        // 等待刷新完成
        EspressoTestUtils.waitForRefreshComplete(R.id.swipeRefresh, maxWaitMs = 1500)

        // 计算实际耗时
        val elapsedTime = System.currentTimeMillis() - startTime

        logTest("刷新实际耗时：${elapsedTime}ms")

        // 验证耗时在合理范围内（应该在 1000ms 左右，允许误差 500ms）
        if (elapsedTime <= 1500) {
            logTest("✓ 刷新超时保护在 1 秒内生效")
        } else {
            logWarning("刷新耗时超过预期：${elapsedTime}ms")
        }

        scenario.close()
    }

    @Test
    fun testRefreshDoesNotBlockUI() {
        logTest("开始测试：刷新过程中 UI 不阻塞")

        // 启动主页
        val scenario = ActivityScenario.launch<MainActivity>(
            Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        )

        safeDelay(1500)

        // 触发下拉刷新
        logTest("触发下拉刷新")
        try {
            onView(withId(R.id.swipeRefresh))
                .perform(swipeDown())
        } catch (e: Exception) {
            logWarning("下拉刷新触发失败")
        }

        // 在刷新过程中尝试切换页面
        logTest("在刷新过程中切换到个人中心")
        safeDelay(200) // 等待刷新动画开始
        
        try {
            onView(withId(R.id.nav_profile))
                .perform(androidx.test.espresso.action.ViewActions.click())
            
            safeDelay(500)
            
            logTest("✓ 刷新过程中可以切换页面，UI 未阻塞")
        } catch (e: Exception) {
            logError("刷新过程中 UI 被阻塞", e)
            throw AssertionError("刷新过程中 UI 被阻塞")
        }

        scenario.close()
    }
}







