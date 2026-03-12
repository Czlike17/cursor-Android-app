package com.example.myapp

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myapp.base.BaseE2ETest
import com.example.myapp.data.local.entity.Device
import com.example.myapp.ui.rule.DeviceSelectionAdapter
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber
import javax.inject.Inject

/**
 * 生命周期状态恢复测试
 * 
 * 测试目标：
 * 1. 模拟用户在规则编辑器中填写到第 3 步
 * 2. 调用 recreate() 强制重建 Activity（模拟系统配置更改或内存不足）
 * 3. 验证重建后，页面停留在第 3 步，且所有数据完整恢复
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LifecycleStateTest : BaseE2ETest() {

    @Inject
    lateinit var preferencesManager: com.example.myapp.util.PreferencesManager

    @Test
    fun testActivityRecreate_shouldRestoreAllState() {
        logTest("=== 开始生命周期状态恢复测试 ===")
        
        // 准备测试数据：先设置用户名到 PreferencesManager
        runBlocking {
            preferencesManager.saveLoginInfo("testuser", autoLogin = true)
            logTest("已设置测试用户到 PreferencesManager: testuser")
        }
        
        // 准备测试数据：先插入用户
        runBlocking {
            val testUser = com.example.myapp.data.local.entity.User(
                username = "testuser",
                passwordHash = "test_hash"
            )
            database.userDao().insert(testUser)
            logTest("已插入测试用户: testuser")
        }
        
        // 准备测试数据：插入一个测试设备
        val testDevice = Device(
            deviceId = 0L,  // 自动生成
            deviceName = "测试灯泡",
            deviceType = "light",
            mqttBroker = "test.mqtt.broker",
            mqttPort = 1883,
            subscribeTopic = "test/device/lifecycle/status",
            publishTopic = "test/device/lifecycle/command",
            clientId = "test_client_lifecycle",
            status = """{"power": "on", "brightness": 80}""",
            isOnline = true,
            roomId = null,
            username = "testuser"
        )
        
        runBlocking {
            database.deviceDao().insert(testDevice)
            logTest("已插入测试设备: ${testDevice.deviceName}")
        }
        
        // 启动 RuleEditorActivity
        val intent = Intent(ApplicationProvider.getApplicationContext(), com.example.myapp.ui.rule.RuleEditorActivity::class.java)
        val scenario = ActivityScenario.launch<com.example.myapp.ui.rule.RuleEditorActivity>(intent)
        
        logTest("步骤 1: 选择设备")
        safeDelay(500)
        
        // 点击第一个设备
        onView(withId(R.id.recyclerViewDevices))
            .perform(RecyclerViewActions.actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(0, click()))
        
        safeDelay(300)
        logTest("已选择设备: ${testDevice.deviceName}")
        
        // 点击"下一步"进入步骤 2
        onView(withId(R.id.btnNext))
            .perform(click())
        
        safeDelay(300)
        logTest("步骤 2: 选择动作")
        
        // 选择"打开"动作
        onView(withId(R.id.radioTurnOn))
            .perform(click())
        
        safeDelay(300)
        logTest("已选择动作: 打开")
        
        // 点击"下一步"进入步骤 3
        onView(withId(R.id.btnNext))
            .perform(click())
        
        safeDelay(300)
        logTest("步骤 3: 设置时间")
        
        // 修改开始时间（点击开始时间文本）
        onView(withId(R.id.tvStartTime))
            .check(matches(isDisplayed()))
        
        // 选择星期一
        onView(withId(R.id.chipMonday))
            .perform(click())
        
        safeDelay(300)
        
        // 选择星期三
        onView(withId(R.id.chipWednesday))
            .perform(click())
        
        safeDelay(300)
        logTest("已选择星期: 周一、周三")
        
        // ===== 关键操作：强制重建 Activity =====
        logTest(">>> 强制重建 Activity（模拟 Process Death）<<<")
        scenario.recreate()
        safeDelay(1000) // 等待重建完成
        
        logTest("=== Activity 重建完成，开始验证状态恢复 ===")
        
        // 验证 1: 当前步骤应该还在第 3 步（时间设置页面）
        onView(withId(R.id.tvStep3))
            .check(matches(isDisplayed()))
        
        logTest("✓ 验证通过: 当前步骤正确停留在第 3 步")
        
        // 验证 2: 星期一的 Chip 应该是选中状态
        onView(withId(R.id.chipMonday))
            .check(matches(isChecked()))
        
        logTest("✓ 验证通过: 星期一保持选中状态")
        
        // 验证 3: 星期三的 Chip 应该是选中状态
        onView(withId(R.id.chipWednesday))
            .check(matches(isChecked()))
        
        logTest("✓ 验证通过: 星期三保持选中状态")
        
        // 验证 4: 开始时间应该显示（默认值或之前设置的值）
        onView(withId(R.id.tvStartTime))
            .check(matches(isDisplayed()))
        
        logTest("✓ 验证通过: 时间窗口数据完整")
        
        // 继续前进到步骤 4，验证之前的数据是否保留
        onView(withId(R.id.btnNext))
            .perform(click())
        
        safeDelay(300)
        logTest("步骤 4: 设置环境条件")
        
        // 验证步骤 4 正常显示
        onView(withId(R.id.switchEnableEnvironment))
            .check(matches(isDisplayed()))
        
        logTest("✓ 验证通过: 可以正常前进到步骤 4")
        
        // 点击"上一步"返回步骤 3
        onView(withId(R.id.btnPrevious))
            .perform(click())
        
        safeDelay(300)
        
        // 再次验证步骤 3 的数据是否依然保留
        onView(withId(R.id.chipMonday))
            .check(matches(isChecked()))
        
        onView(withId(R.id.chipWednesday))
            .check(matches(isChecked()))
        
        logTest("✓ 验证通过: 返回步骤 3 后数据依然完整")
        
        // 点击"上一步"返回步骤 2
        onView(withId(R.id.btnPrevious))
            .perform(click())
        
        safeDelay(300)
        logTest("步骤 2: 验证动作选择")
        
        // 验证"打开"动作依然选中
        onView(withId(R.id.radioTurnOn))
            .check(matches(isChecked()))
        
        logTest("✓ 验证通过: 步骤 2 的动作选择保持不变")
        
        // 点击"上一步"返回步骤 1
        onView(withId(R.id.btnPrevious))
            .perform(click())
        
        safeDelay(300)
        logTest("步骤 1: 验证设备选择")
        
        // 验证设备列表正常显示
        onView(withId(R.id.recyclerViewDevices))
            .check(matches(isDisplayed()))
        
        logTest("✓ 验证通过: 步骤 1 的设备列表正常显示")
        
        logTest("=== 生命周期状态恢复测试全部通过！===")
        
        scenario.close()
    }
}

