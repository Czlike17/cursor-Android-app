package com.example.myapp.ui.debug

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapp.BuildConfig
import com.example.myapp.databinding.ActivityMockTestBinding
import com.example.myapp.mock.MockMqttClientManager
import com.example.myapp.mock.UnifiedMockSystem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Mock 测试 Activity
 * 用于快速验证Mock层是否正常工作
 * 
 * 使用方式:
 * 1. 在任意页面添加按钮跳转到此Activity
 * 2. 或在AndroidManifest.xml中设置为启动Activity
 */
@AndroidEntryPoint
class MockTestActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMockTestBinding
    
    @Inject
    lateinit var mockMqtt: MockMqttClientManager
    
    @Inject
    lateinit var unifiedMockSystem: UnifiedMockSystem
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMockTestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        if (!BuildConfig.IS_MOCK_MODE) {
            binding.tvStatus.text = "❌ Mock模式未开启\n请在build.gradle.kts中设置IS_MOCK_MODE=true"
            return
        }
        
        setupUI()
    }
    
    private fun setupUI() {
        binding.tvStatus.text = "✅ Mock模式已开启\n准备测试..."
        
        // 测试1: MQTT连接
        binding.btnTestConnect.setOnClickListener {
            testMqttConnection()
        }
        
        // 测试2: 发送指令
        binding.btnTestPublish.setOnClickListener {
            testMqttPublish()
        }
        
        // 测试3: 订阅消息
        binding.btnTestSubscribe.setOnClickListener {
            testMqttSubscribe()
        }
        
        // 测试4: 完整流程
        binding.btnTestAll.setOnClickListener {
            testCompleteFlow()
        }
    }
    
    /**
     * 测试1: MQTT连接
     */
    private fun testMqttConnection() {
        binding.tvStatus.text = "测试1: 正在连接MQTT..."
        
        lifecycleScope.launch {
            mockMqtt.connect(
                serverUri = "tcp://mock.server:1883",
                clientId = "test_client",
                username = "test_user",
                password = null,
                onSuccess = {
                    binding.tvStatus.text = "✅ 测试1通过: MQTT连接成功!\n" +
                            "连接延迟: 500ms (模拟)"
                    Toast.makeText(this@MockTestActivity, "连接成功!", Toast.LENGTH_SHORT).show()
                    Timber.d("[TEST] MQTT connection success")
                },
                onFailure = { error ->
                    binding.tvStatus.text = "❌ 测试1失败: ${error.message}"
                    Toast.makeText(this@MockTestActivity, "连接失败!", Toast.LENGTH_SHORT).show()
                    Timber.e(error, "[TEST] MQTT connection failed")
                }
            )
        }
    }
    
    /**
     * 测试2: 发送指令
     */
    private fun testMqttPublish() {
        if (!mockMqtt.isConnected()) {
            binding.tvStatus.text = "❌ 请先测试连接"
            return
        }
        
        binding.tvStatus.text = "测试2: 正在发送开灯指令..."
        
        val command = """{"action": "turn_on", "brightness": 80}"""
        
        mockMqtt.publish(
            topic = "/device/light_001/command",
            payload = command,
            qos = 1,
            retained = false,
            onSuccess = {
                binding.tvStatus.text = "✅ 测试2通过: 指令发送成功!\n" +
                        "Topic: /device/light_001/command\n" +
                        "Payload: $command\n" +
                        "预计300ms后收到状态回传"
                Timber.d("[TEST] Publish success")
            },
            onFailure = { error ->
                binding.tvStatus.text = "❌ 测试2失败: ${error.message}"
                Timber.e(error, "[TEST] Publish failed")
            }
        )
    }
    
    /**
     * 测试3: 订阅消息
     */
    private fun testMqttSubscribe() {
        if (!mockMqtt.isConnected()) {
            binding.tvStatus.text = "❌ 请先测试连接"
            return
        }
        
        binding.tvStatus.text = "测试3: 正在订阅设备状态..."
        
        mockMqtt.subscribe(
            topic = "/device/light_001/status",
            qos = 1,
            onMessageArrived = { topic, message ->
                val payload = String(message.payload)
                binding.tvStatus.text = "✅ 测试3通过: 收到状态消息!\n" +
                        "Topic: $topic\n" +
                        "Message: $payload"
                Timber.d("[TEST] Message received: $payload")
            }
        )
        
        binding.tvStatus.text = "✅ 订阅成功!\n" +
                "Topic: /device/light_001/status\n" +
                "等待消息..."
    }
    
    /**
     * 测试4: 完整流程
     */
    private fun testCompleteFlow() {
        binding.tvStatus.text = "测试4: 开始完整流程测试...\n"
        
        lifecycleScope.launch {
            // 步骤1: 连接
            appendStatus("步骤1: 连接MQTT...")
            mockMqtt.connect(
                serverUri = "tcp://mock.server:1883",
                clientId = "test_client",
                username = "test_user",
                password = null,
                onSuccess = {
                    appendStatus("✅ 连接成功 (500ms)")
                    
                    lifecycleScope.launch {
                        delay(500)
                        
                        // 步骤2: 订阅
                        appendStatus("\n步骤2: 订阅状态主题...")
                        mockMqtt.subscribe(
                            topic = "/device/light_001/status",
                            qos = 1,
                            onMessageArrived = { topic, message ->
                                val payload = String(message.payload)
                                appendStatus("\n✅ 收到状态回传:\n$payload")
                            }
                        )
                        appendStatus("✅ 订阅成功")
                        
                        delay(500)
                        
                        // 步骤3: 发送指令
                        appendStatus("\n步骤3: 发送开灯指令...")
                        mockMqtt.publish(
                            topic = "/device/light_001/command",
                            payload = """{"action": "turn_on"}""",
                            qos = 1,
                            retained = false,
                            onSuccess = {
                                appendStatus("✅ 指令发送成功")
                                appendStatus("\n等待300ms后收到状态...")
                            },
                            onFailure = { error ->
                                appendStatus("❌ 指令发送失败: ${error.message}")
                            }
                        )
                    }
                },
                onFailure = { error ->
                    appendStatus("❌ 连接失败: ${error.message}")
                }
            )
        }
    }
    
    private fun appendStatus(text: String) {
        binding.tvStatus.append(text)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        mockMqtt.release()
    }
}




