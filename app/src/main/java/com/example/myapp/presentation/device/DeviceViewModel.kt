package com.example.myapp.presentation.device

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.local.entity.Device
import com.example.myapp.data.repository.DeviceRepository
import com.example.myapp.presentation.base.BaseViewModel
import com.example.myapp.util.PreferencesManager
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 设备列表 ViewModel
 */
@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val preferencesManager: PreferencesManager
) : BaseViewModel() {

    private val gson = Gson()
    
    // 当前用户名
    private var currentUsername: String = ""

    // 设备列表（LiveData 自动监听数据库变化）
    private val _devices = MutableLiveData<List<Device>>()
    val devices: LiveData<List<Device>> = _devices

    init {
        // 获取当前用户名并启动设备列表监听
        viewModelScope.launch {
            currentUsername = preferencesManager.getUsername().first() ?: ""
            if (currentUsername.isNotEmpty()) {
                // 统一使用 Flow 监听设备列表变化，移除竞态冲突
                observeDevices()
            }
        }
    }

    /**
     * 监听设备列表变化（自动刷新）
     */
    private fun observeDevices() {
        viewModelScope.launch {
            try {
                deviceRepository.getDevicesByUsername(currentUsername).collect { devices ->
                    _devices.postValue(devices)
                    // Flow 监听会自动触发，无需手动调用 loadDevices()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _devices.postValue(emptyList())
            }
        }
    }

    /**
     * 加载设备列表（带超时保护）
     */
    fun loadDevices() {
        if (currentUsername.isEmpty()) {
            viewModelScope.launch {
                showError("用户未登录")
            }
            return
        }

        viewModelScope.launch {
            try {
                setLoading(true)
                
                // 启动超时保护：1 秒后强制停止加载
                val timeoutJob = launch {
                    kotlinx.coroutines.delay(1000)
                    if (isLoading.value) {
                        setLoading(false)
                    }
                }
                
                val result = deviceRepository.getDevicesByUsernameOnce(currentUsername)
                result.onSuccess { devices ->
                    _devices.postValue(devices)
                }.onFailure { exception ->
                    showError(exception.message ?: "加载失败")
                }
                
                timeoutJob.cancel() // 取消超时任务
                setLoading(false)
            } catch (e: Exception) {
                setLoading(false)
                showError(e.message ?: "加载失败")
            }
        }
    }

    /**
     * 切换设备电源
     */
    fun toggleDevicePower(device: Device, isOn: Boolean) {
        viewModelScope.launch {
            try {
                // 更新设备状态
                val statusMap = try {
                    val existingStatus = gson.fromJson(device.status, Map::class.java) as? MutableMap<String, Any> ?: mutableMapOf()
                    existingStatus["power"] = if (isOn) "on" else "off"
                    existingStatus
                } catch (e: Exception) {
                    mutableMapOf<String, Any>("power" to if (isOn) "on" else "off")
                }
                
                val newStatus = gson.toJson(statusMap)
                val updatedDevice = device.copy(status = newStatus)
                
                deviceRepository.updateDevice(updatedDevice).onSuccess {
                    showMessage(if (isOn) "已打开" else "已关闭")
                }.onFailure { exception ->
                    showError(exception.message ?: "操作失败")
                }
            } catch (e: Exception) {
                showError(e.message ?: "操作失败")
            }
        }
    }

    /**
     * 删除设备
     */
    fun deleteDevice(device: Device) {
        executeWithLoading(
            block = {
                deviceRepository.deleteDevice(device)
            },
            onSuccess = {
                viewModelScope.launch {
                    showMessage("设备已删除")
                }
            },
            onError = { error ->
                viewModelScope.launch {
                    showError(error)
                }
            }
        )
    }
}

