package com.example.myapp.presentation.device

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.example.myapp.data.model.DeviceType
import com.example.myapp.databinding.ActivityAddDeviceBinding
import com.example.myapp.presentation.base.BaseActivity
import com.example.myapp.util.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 添加设备页
 */
@AndroidEntryPoint
class AddDeviceActivity : BaseActivity<ActivityAddDeviceBinding, AddDeviceViewModel>() {

    override fun createViewBinding(inflater: LayoutInflater): ActivityAddDeviceBinding {
        return ActivityAddDeviceBinding.inflate(inflater)
    }

    override fun getViewModelClass(): Class<AddDeviceViewModel> {
        return AddDeviceViewModel::class.java
    }

    override fun initView() {
        // 设置 Toolbar
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedWithConfirmation()
        }

        // 设置设备类型 Spinner
        val deviceTypes = DeviceType.values().map { it.displayName }
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            deviceTypes
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDeviceType.adapter = adapter

        // 自动生成 Client ID
        generateClientId()

        // Client ID 刷新按钮
        binding.tilClientId.setEndIconOnClickListener {
            generateClientId()
        }

        // 输入监听
        binding.etDeviceName.addTextChangedListener {
            viewModel.updateDeviceName(it?.toString() ?: "")
        }

        binding.etMqttBroker.addTextChangedListener {
            viewModel.updateMqttBroker(it?.toString() ?: "")
        }

        binding.etMqttPort.addTextChangedListener {
            viewModel.updateMqttPort(it?.toString() ?: "")
        }

        binding.etClientId.addTextChangedListener {
            viewModel.updateClientId(it?.toString() ?: "")
        }

        binding.etSubscribeTopic.addTextChangedListener {
            viewModel.updateSubscribeTopic(it?.toString() ?: "")
        }

        binding.etPublishTopic.addTextChangedListener {
            viewModel.updatePublishTopic(it?.toString() ?: "")
        }

        // 连接测试按钮
        binding.btnTestConnection.setOnClickListener {
            val selectedType = DeviceType.values()[binding.spinnerDeviceType.selectedItemPosition]
            viewModel.testMqttConnection(selectedType)
        }

        // 保存按钮
        binding.btnSave.setOnClickListener {
            val selectedType = DeviceType.values()[binding.spinnerDeviceType.selectedItemPosition]
            viewModel.saveDevice(selectedType)
        }
    }

    override fun initData() {
        // 无需初始化数据
    }

    override fun observeData() {
        lifecycleScope.launch {
            // 观察测试按钮状态
            viewModel.isTestButtonEnabled.collect { enabled ->
                binding.btnTestConnection.isEnabled = enabled
            }
        }

        lifecycleScope.launch {
            // 观察保存按钮状态
            viewModel.isSaveButtonEnabled.collect { enabled ->
                binding.btnSave.isEnabled = enabled
            }
        }

        lifecycleScope.launch {
            // 观察加载状态
            viewModel.isLoading.collect { isLoading ->
                if (isLoading) {
                    showLoading("处理中...")
                } else {
                    hideLoading()
                }
            }
        }

        lifecycleScope.launch {
            // 观察连接测试状态
            viewModel.isTestingConnection.collect { isTesting ->
                if (isTesting) {
                    showLoading("连接测试中...")
                } else {
                    hideLoading()
                }
            }
        }

        lifecycleScope.launch {
            // 观察错误信息
            viewModel.error.collect { error ->
                if (error.isNotEmpty()) {
                    showToast(error)
                }
            }
        }

        lifecycleScope.launch {
            // 观察成功消息
            viewModel.message.collect { message ->
                if (message.isNotEmpty()) {
                    showToast(message)
                }
            }
        }

        lifecycleScope.launch {
            // 观察保存成功
            viewModel.saveSuccess.collect { success ->
                if (success) {
                    finish()
                }
            }
        }
    }

    /**
     * 生成随机 Client ID
     */
    private fun generateClientId() {
        val clientId = "device_${UUID.randomUUID().toString().substring(0, 8)}"
        binding.etClientId.setText(clientId)
    }

    /**
     * 返回时确认
     */
    private fun onBackPressedWithConfirmation() {
        // 检查是否有输入内容
        val hasInput = binding.etDeviceName.text?.isNotEmpty() == true ||
                binding.etMqttBroker.text?.isNotEmpty() == true ||
                binding.etSubscribeTopic.text?.isNotEmpty() == true ||
                binding.etPublishTopic.text?.isNotEmpty() == true
        
        if (hasInput) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("放弃添加")
                .setMessage("确定要放弃添加设备吗？未保存的配置将丢失。")
                .setPositiveButton("确定") { _, _ ->
                    finish()
                }
                .setNegativeButton("取消", null)
                .show()
        } else {
            finish()
        }
    }

    override fun onBackPressed() {
        onBackPressedWithConfirmation()
    }
}

