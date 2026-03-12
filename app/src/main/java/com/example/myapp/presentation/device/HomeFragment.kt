package com.example.myapp.presentation.device

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapp.BuildConfig
import com.example.myapp.databinding.FragmentHomeBinding
import com.example.myapp.mock.UnifiedMockSystem
import com.example.myapp.presentation.base.BaseFragment
import com.example.myapp.util.gone
import com.example.myapp.util.showToast
import com.example.myapp.util.visible
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import timber.log.Timber

/** 首页 Fragment - 设备列表 */
@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding, DeviceViewModel>() {

    @Inject lateinit var unifiedMockSystem: UnifiedMockSystem

    private lateinit var deviceAdapter: DeviceAdapter

    override fun createViewBinding(
            inflater: LayoutInflater,
            container: ViewGroup?
    ): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    override fun getViewModelClass(): Class<DeviceViewModel> {
        return DeviceViewModel::class.java
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        observeData()

        // 加载设备列表
        viewModel.loadDevices()
    }

    private fun initView() {
        // 设置 RecyclerView
        deviceAdapter =
                DeviceAdapter(
                        onDeviceClick = { device ->
                            // 跳转到设备控制详情页
                            val intent =
                                    Intent(requireContext(), DeviceControlActivity::class.java)
                                            .apply {
                                                putExtra("device_id", device.deviceId)
                                                putExtra("device_type", device.deviceType)
                                            }
                            startActivity(intent)
                        },
                        onPowerToggle = { device, isOn ->
                            viewModel.toggleDevicePower(device, isOn)
                        }
                )

        binding.rvDevices.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = deviceAdapter
        }

        // 下拉刷新
        binding.swipeRefresh.setOnRefreshListener { viewModel.loadDevices() }

        // 添加设备按钮
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(requireContext(), AddDeviceActivity::class.java))
        }
    }

    private fun observeData() {
        // 观察设备列表
        viewModel.devices.observe(viewLifecycleOwner) { devices ->
            if (devices == null) return@observe

            deviceAdapter.submitList(devices)

            // 显示/隐藏空状态
            if (devices.isEmpty()) {
                binding.layoutEmpty.visible()
                binding.rvDevices.gone()
            } else {
                binding.layoutEmpty.gone()
                binding.rvDevices.visible()
            }

            // 更新环境数据
            updateEnvironmentData(devices)
        }

        // 观察加载状态（强制 1 秒超时）
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                if (isLoading) {
                    binding.swipeRefresh.isRefreshing = true

                    // 启动超时保护：1 秒后强制停止刷新
                    launch {
                        kotlinx.coroutines.delay(1000)
                        if (binding.swipeRefresh.isRefreshing) {
                            binding.swipeRefresh.isRefreshing = false
                            Timber.w("[HomeFragment] 刷新超时，强制停止")
                        }
                    }
                } else {
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }

        // 观察错误信息
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                if (error.isNotEmpty()) {
                    requireContext().showToast(error)
                }
            }
        }

        // 观察成功消息
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.message.collect { message ->
                if (message.isNotEmpty()) {
                    requireContext().showToast(message)
                }
            }
        }

        // Mock 模式：观察环境数据流
        if (BuildConfig.IS_MOCK_MODE) {
            viewLifecycleOwner.lifecycleScope.launch {
                unifiedMockSystem.environmentDataFlow.collect { envData ->
                    updateMockEnvironmentData(envData)
                }
            }
        }
    }

    /** 更新环境数据显示 */
    private fun updateEnvironmentData(devices: List<com.example.myapp.data.local.entity.Device>) {
        if (!isAdded || view == null) return

        // 查找温湿度传感器设备
        val sensorDevice =
                devices.find { it.deviceType.contains("sensor", ignoreCase = true) && it.isOnline }

        if (sensorDevice != null) {
            try {
                // 解析设备状态 JSON
                val statusJson = org.json.JSONObject(sensorDevice.status)
                val temperature = statusJson.optDouble("temperature", 0.0)
                val humidity = statusJson.optDouble("humidity", 0.0)
                val lightIntensity = statusJson.optDouble("lightIntensity", 0.0)

                // 显示环境数据
                binding.layoutEnvironmentData.visible()
                binding.tvNoEnvironmentData.gone()

                binding.tvTemperature.text = String.format("%.1f°C", temperature)
                binding.tvHumidity.text = String.format("%.0f%%", humidity)
                binding.tvLightIntensity.text = String.format("%.0f lx", lightIntensity)
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse sensor data")
                showNoEnvironmentData()
            }
        } else {
            showNoEnvironmentData()
        }
    }

    /** 显示无环境数据状态 */
    private fun showNoEnvironmentData() {
        if (!isAdded || view == null) return

        binding.layoutEnvironmentData.gone()
        binding.tvNoEnvironmentData.visible()
    }

    /** 更新 Mock 环境数据（每 3 秒自动更新） */
    private fun updateMockEnvironmentData(envData: com.example.myapp.mock.EnvironmentData) {
        try {
            if (!isAdded || view == null) return

            binding.layoutEnvironmentData.visible()
            binding.tvNoEnvironmentData.gone()

            binding.tvTemperature.text = String.format("%.1f°C", envData.temperature)
            binding.tvHumidity.text = String.format("%.0f%%", envData.humidity)
            binding.tvLightIntensity.text = String.format("%.0f lx", envData.light)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update mock environment data")
        }
    }

    override fun onResume() {
        super.onResume()
        // 页面显示时刷新数据
        refreshData()
    }

    /** 刷新数据（公开方法，供 MainActivity 调用） */
    fun refreshData() {
        viewModel.loadDevices()
        Timber.d("[HomeFragment] Data refreshed")
    }

    companion object {
        fun newInstance() = HomeFragment()
    }
}
