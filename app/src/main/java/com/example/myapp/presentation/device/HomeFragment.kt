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
        // 已删除 viewModel.loadDevices()
    }

    private fun initView() {
        deviceAdapter = DeviceAdapter(
            onDeviceClick = { device ->
                val intent = Intent(requireContext(), DeviceControlActivity::class.java).apply {
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

        // 数据是实时响应的，下拉刷新只需重置状态即可，提供视觉反馈
        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
        }

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(requireContext(), AddDeviceActivity::class.java))
        }
    }

    private fun observeData() {
        viewModel.devices.observe(viewLifecycleOwner) { devices ->
            if (devices == null) return@observe

            // 【关键修复】：深度复制生成全新内存地址，彻底打断 DiffUtil 错觉
            val newList = ArrayList(devices)

            // 优先处理空视图逻辑，保证布局可见性正确
            if (newList.isEmpty()) {
                binding.layoutEmpty.visible()
                binding.rvDevices.gone()
            } else {
                binding.layoutEmpty.gone()
                binding.rvDevices.visible()
            }

            // 【关键修复】：利用 submitList 回调，在数据计算完毕后强制重绘 View
            deviceAdapter.submitList(newList) {
                if (isAdded && view != null) {
                    binding.rvDevices.requestLayout()
                }
            }

            updateEnvironmentData(newList)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                if (error.isNotEmpty()) requireContext().showToast(error)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.message.collect { message ->
                if (message.isNotEmpty()) requireContext().showToast(message)
            }
        }

        if (BuildConfig.IS_MOCK_MODE) {
            viewLifecycleOwner.lifecycleScope.launch {
                unifiedMockSystem.environmentDataFlow.collect { envData ->
                    updateMockEnvironmentData(envData)
                }
            }
        }
    }

    private fun updateEnvironmentData(devices: List<com.example.myapp.data.local.entity.Device>) {
        if (!isAdded || view == null) return

        val sensorDevice = devices.find { it.deviceType.contains("sensor", ignoreCase = true) && it.isOnline }

        if (sensorDevice != null) {
            try {
                val statusJson = org.json.JSONObject(sensorDevice.status)
                val temperature = statusJson.optDouble("temperature", 0.0)
                val humidity = statusJson.optDouble("humidity", 0.0)
                val lightIntensity = statusJson.optDouble("lightIntensity", 0.0)

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

    private fun showNoEnvironmentData() {
        if (!isAdded || view == null) return
        binding.layoutEnvironmentData.gone()
        binding.tvNoEnvironmentData.visible()
    }

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
        // 页面显示时，激活双重硬核兜底
        viewModel.forceRefresh()
    }

    fun refreshData() {
        Timber.d("[HomeFragment] Manual refresh ignored, using Flow + forceRefresh")
    }

    companion object {
        fun newInstance() = HomeFragment()
    }
}
