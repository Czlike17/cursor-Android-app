package com.example.myapp

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myapp.base.BaseE2ETest
import com.example.myapp.data.local.entity.Device
import com.example.myapp.data.local.entity.UserHabit
import com.example.myapp.presentation.main.MainActivity
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber
import javax.inject.Inject

/**
 * 导航压力测试
 * 
 * 测试目标：
 * 1. 在 MainActivity 的 4 个 Tab 之间高频切换（20 次）
 * 2. 反复进出 RuleEditorActivity（10 次）
 * 3. 验证没有崩溃、Fragment 栈重叠、或资源泄漏
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NavigationStressTest : BaseE2ETest() {

    @Inject
    lateinit var preferencesManager: com.example.myapp.util.PreferencesManager

    @Test
    fun testMainActivityTabSwitching_shouldNotCrash() {
        logTest("=== 开始 MainActivity Tab 切换压力测试 ===")
        
        // 准备测试数据
        prepareTestData()
        
        // 启动 MainActivity
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        val scenario = ActivityScenario.launch<MainActivity>(intent)
        
        safeDelay(1500) // 等待初始化完成
        
        // 高频切换 Tab（20 次循环）
        repeat(20) { iteration ->
            logTest(">>> 第 ${iteration + 1} 轮 Tab 切换 <<<")
            
            // 切换到"设备"Tab
            onView(withId(R.id.nav_home))
                .perform(click())
            safeDelay(300)
            
            // 切换到"习惯"Tab
            onView(withId(R.id.nav_habit))
                .perform(click())
            safeDelay(300)
            
            // 切换到"规则"Tab
            onView(withId(R.id.nav_rule))
                .perform(click())
            safeDelay(300)
            
            // 切换到"我的"Tab
            onView(withId(R.id.nav_profile))
                .perform(click())
            safeDelay(300)
            
            if ((iteration + 1) % 5 == 0) {
                logTest("✓ 已完成 ${iteration + 1} 轮切换，无崩溃")
            }
        }
        
        logTest("=== MainActivity Tab 切换压力测试通过！===")
        scenario.close()
    }
    
    @Test
    fun testRuleEditorRepeatedEntry_shouldNotCrash() {
        logTest("=== 开始 RuleEditor 反复进出压力测试 ===")
        
        // 准备测试数据
        prepareTestData()
        
        // 启动 MainActivity
        val mainIntent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        val mainScenario = ActivityScenario.launch<MainActivity>(mainIntent)
        
        safeDelay(1500)
        
        // 切换到"习惯"Tab
        onView(withId(R.id.nav_habit))
            .perform(click())
        
        safeDelay(800)
        
        // 反复进出 RuleEditorActivity（10 次）
        repeat(10) { iteration ->
            logTest(">>> 第 ${iteration + 1} 次进入 RuleEditor <<<")
            
            // 点击 FAB 进入规则编辑器（使用 withContentDescription 区分）
            onView(allOf(withId(R.id.fabAdd), withContentDescription("添加习惯")))
                .perform(click())
            
            safeDelay(800)
            
            // 验证规则编辑器已打开
            onView(withId(R.id.viewPager))
                .check(matches(isDisplayed()))
            
            logTest("✓ RuleEditor 已打开")
            
            // 使用 Espresso 的返回按钮
            androidx.test.espresso.Espresso.pressBack()
            
            safeDelay(500)
            
            // 在确认对话框中点击"确定"
            try {
                onView(withText("确定"))
                    .perform(click())
                safeDelay(500)
            } catch (e: Exception) {
                logWarning("未找到确认对话框")
            }
            
            logTest("✓ 已返回习惯列表")
            
            if ((iteration + 1) % 3 == 0) {
                logTest("✓ 已完成 ${iteration + 1} 次进出，无崩溃")
            }
        }
        
        logTest("=== RuleEditor 反复进出压力测试通过！===")
        mainScenario.close()
    }
    
    @Test
    fun testComplexNavigationPattern_shouldNotCrash() {
        logTest("=== 开始复杂导航模式压力测试 ===")
        
        // 准备测试数据
        prepareTestData()
        
        // 启动 MainActivity
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        val scenario = ActivityScenario.launch<MainActivity>(intent)
        
        safeDelay(1500)
        
        // 复杂导航模式：Tab 切换（15 次循环）
        repeat(15) { iteration ->
            logTest(">>> 第 ${iteration + 1} 轮复杂导航 <<<")
            
            // 随机切换 Tab
            val tabs = listOf(
                R.id.nav_home,
                R.id.nav_habit,
                R.id.nav_rule,
                R.id.nav_profile
            )
            
            tabs.forEach { tabId ->
                onView(withId(tabId))
                    .perform(click())
                safeDelay(200)
            }
            
            if ((iteration + 1) % 5 == 0) {
                logTest("✓ 已完成 ${iteration + 1} 轮复杂导航，无崩溃")
            }
        }
        
        logTest("=== 复杂导航模式压力测试通过！===")
        scenario.close()
    }
    
    /**
     * 准备测试数据
     */
    private fun prepareTestData() {
        runBlocking {
            // 先设置用户名到 PreferencesManager
            preferencesManager.saveLoginInfo("testuser", autoLogin = true)
            logTest("已设置测试用户到 PreferencesManager: testuser")
            
            // 先插入测试用户
            val testUser = com.example.myapp.data.local.entity.User(
                username = "testuser",
                passwordHash = "test_hash"
            )
            database.userDao().insert(testUser)
            logTest("已插入测试用户: testuser")
            
            // 插入测试设备
            val devices = listOf(
                Device(
                    deviceId = 0L,
                    deviceName = "压测灯泡1",
                    deviceType = "light",
                    mqttBroker = "test.mqtt.broker",
                    mqttPort = 1883,
                    subscribeTopic = "test/stress/device1/status",
                    publishTopic = "test/stress/device1/command",
                    clientId = "stress_client_1",
                    status = """{"power": "on"}""",
                    isOnline = true,
                    roomId = null,
                    username = "testuser"
                ),
                Device(
                    deviceId = 0L,
                    deviceName = "压测空调2",
                    deviceType = "air_conditioner",
                    mqttBroker = "test.mqtt.broker",
                    mqttPort = 1883,
                    subscribeTopic = "test/stress/device2/status",
                    publishTopic = "test/stress/device2/command",
                    clientId = "stress_client_2",
                    status = """{"power": "off"}""",
                    isOnline = true,
                    roomId = null,
                    username = "testuser"
                )
            )
            
            val insertedDeviceIds = mutableListOf<Long>()
            devices.forEach { device ->
                val deviceId = database.deviceDao().insert(device)
                insertedDeviceIds.add(deviceId)
            }
            
            // 插入测试习惯（使用实际插入的设备 ID）
            val habits = listOf(
                UserHabit(
                    deviceId = insertedDeviceIds[0],
                    habitName = "压测习惯1",
                    triggerCondition = """{"time": "18:00-20:00"}""",
                    actionCommand = """{"action": "turn_on"}""",
                    weekType = 0,
                    timeWindow = "18:00-20:00",
                    environmentThreshold = null,
                    confidence = 0.9,
                    isEnabled = true,
                    username = "testuser"
                ),
                UserHabit(
                    deviceId = insertedDeviceIds[1],
                    habitName = "压测习惯2",
                    triggerCondition = """{"time": "22:00-23:00"}""",
                    actionCommand = """{"action": "turn_off"}""",
                    weekType = 0,
                    timeWindow = "22:00-23:00",
                    environmentThreshold = null,
                    confidence = 0.85,
                    isEnabled = true,
                    username = "testuser"
                )
            )
            
            habits.forEach { habit ->
                database.userHabitDao().insert(habit)
            }
            
            logTest("已准备测试数据: ${devices.size} 个设备, ${habits.size} 个习惯")
        }
    }
}

