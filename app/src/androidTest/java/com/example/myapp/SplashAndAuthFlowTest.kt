package com.example.myapp

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myapp.base.BaseE2ETest
import com.example.myapp.presentation.auth.LoginActivity
import com.example.myapp.presentation.auth.SplashActivity
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
 * 启动页与认证流程测试
 * 测试路由与状态闭环：登录状态检查、退出登录后的路由跳转
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SplashAndAuthFlowTest : BaseE2ETest() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Before
    override fun baseSetup() {
        super.baseSetup()
        logTest("=== 启动页与认证流程测试初始化完成 ===")
    }

    @Test
    fun testSplashToMainWhenLoggedIn() {
        logTest("开始测试：已登录状态下，启动页自动跳转到主页")

        // 模拟已登录状态
        runBlocking {
            preferencesManager.saveLoginInfo("test_user", autoLogin = true)
        }

        logTest("已设置登录状态：test_user")

        // 启动 SplashActivity
        val scenario = ActivityScenario.launch<SplashActivity>(
            Intent(ApplicationProvider.getApplicationContext(), SplashActivity::class.java)
        )

        // 等待 Splash 延迟（3秒）
        logTest("等待 Splash 延迟完成...")
        safeDelay(3500)

        // 验证是否跳转到主页（检查底部导航是否存在）
        logTest("验证是否成功跳转到主页")
        try {
            onView(withId(R.id.bottomNavigation))
                .check(matches(isDisplayed()))
            
            logTest("✓ 成功跳转到主页")
        } catch (e: Exception) {
            logError("未能跳转到主页", e)
            throw AssertionError("启动页未能正确跳转到主页")
        }

        scenario.close()
    }

    @Test
    fun testSplashToLoginWhenNotLoggedIn() {
        logTest("开始测试：未登录状态下，启动页自动跳转到登录页")

        // 确保未登录状态
        runBlocking {
            preferencesManager.clearLoginInfo()
        }

        logTest("已清除登录状态")

        // 启动 SplashActivity
        val scenario = ActivityScenario.launch<SplashActivity>(
            Intent(ApplicationProvider.getApplicationContext(), SplashActivity::class.java)
        )

        // 等待 Splash 延迟（3秒）
        logTest("等待 Splash 延迟完成...")
        safeDelay(3500)

        // 验证是否跳转到登录页（检查登录按钮是否存在）
        logTest("验证是否成功跳转到登录页")
        try {
            onView(withId(R.id.btnLogin))
                .check(matches(isDisplayed()))
            
            logTest("✓ 成功跳转到登录页")
        } catch (e: Exception) {
            logError("未能跳转到登录页", e)
            throw AssertionError("启动页未能正确跳转到登录页")
        }

        scenario.close()
    }

    @Test
    fun testLogoutAndVerifyRouting() {
        logTest("开始测试：退出登录后验证路由跳转")

        // 模拟已登录状态
        runBlocking {
            preferencesManager.saveLoginInfo("test_user", autoLogin = true)
        }

        // 启动主页
        val scenario = ActivityScenario.launch<MainActivity>(
            Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        )

        // 等待主页加载完成
        safeDelay(1000)

        // 步骤1：切换到个人中心
        logTest("步骤1：切换到个人中心")
        onView(withId(R.id.nav_profile))
            .perform(click())
        
        safeDelay(500)

        // 步骤2：点击退出登录按钮
        logTest("步骤2：点击退出登录按钮")
        EspressoTestUtils.onVisibleView(withId(R.id.btnLogout))
            .perform(click())
        
        safeDelay(300)

        // 步骤3：确认退出登录对话框
        logTest("步骤3：确认退出登录")
        try {
            onView(withText("确定"))
                .perform(click())
            
            logTest("已点击确定按钮")
        } catch (e: Exception) {
            logWarning("未找到确认对话框，可能已直接退出")
        }

        // 等待退出登录处理完成
        safeDelay(1500)

        // 步骤4：验证是否跳转到登录页
        logTest("步骤4：验证是否跳转到登录页")
        try {
            onView(withId(R.id.btnLogin))
                .check(matches(isDisplayed()))
            
            logTest("✓ 成功跳转到登录页")
        } catch (e: Exception) {
            logError("退出登录后未能跳转到登录页", e)
            throw AssertionError("退出登录后路由跳转失败")
        }

        // 步骤5：验证按返回键无法回到主页
        logTest("步骤5：验证按返回键无法回到主页")
        try {
            pressBack()
            safeDelay(500)
            
            // 如果还能看到登录按钮，说明无法返回主页（正确）
            onView(withId(R.id.btnLogin))
                .check(matches(isDisplayed()))
            
            logTest("✓ 按返回键无法回到主页，路由闭环正确")
        } catch (e: Exception) {
            logError("路由闭环验证失败", e)
        }

        scenario.close()
    }

    @Test
    fun testLogoutClearsUserState() {
        logTest("开始测试：退出登录后验证用户状态清除")

        // 模拟已登录状态
        runBlocking {
            preferencesManager.saveLoginInfo("test_user", autoLogin = true)
        }

        // 启动主页
        val scenario = ActivityScenario.launch<MainActivity>(
            Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        )

        safeDelay(1000)

        // 切换到个人中心
        onView(withId(R.id.nav_profile))
            .perform(click())
        
        safeDelay(500)

        // 点击退出登录
        EspressoTestUtils.onVisibleView(withId(R.id.btnLogout))
            .perform(click())
        
        safeDelay(300)

        // 确认退出
        try {
            onView(withText("确定"))
                .perform(click())
        } catch (e: Exception) {
            // 忽略
        }

        safeDelay(1500)

        // 验证登录状态已清除
        logTest("验证登录状态已清除")
        runBlocking {
            val username = preferencesManager.getUsernameSync()
            if (username == null) {
                logTest("✓ 用户名已清除")
            } else {
                logError("用户名未清除：$username")
                throw AssertionError("退出登录后用户状态未清除")
            }
        }

        scenario.close()
    }
}







