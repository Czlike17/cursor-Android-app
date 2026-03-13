package com.example.myapp.presentation.device

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.local.entity.Device
import com.example.myapp.data.repository.DeviceRepository
import com.example.myapp.engine.CustomAutoControlEngine
import com.example.myapp.presentation.base.BaseViewModel
import com.example.myapp.util.PreferencesManager
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val preferencesManager: PreferencesManager,
    private val autoControlEngine: CustomAutoControlEngine // 【新增注入】：引入规则引擎
) : BaseViewModel() {

    private val gson = Gson()

    @Volatile
    private var currentUsername: String = ""

    private var observeJob: Job? = null

    private val _devices = MutableLiveData<List<Device>>()
    val devices: LiveData<List<Device>> = _devices

    init {
        viewModelScope.launch {
            preferencesManager.getUsername().collectLatest { username ->
                val newUsername = username ?: ""
                if (newUsername.isNotEmpty()) {
                    currentUsername = newUsername
                    observeDevices(newUsername)
                }
            }
        }
    }

    private fun observeDevices(username: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            try {
                deviceRepository.getDevicesByUsername(username).collect { devices ->
                    _devices.value = devices
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _devices.value = emptyList()
            }
        }
    }

    fun forceRefresh() {
        viewModelScope.launch {
            val user = currentUsername.ifEmpty { preferencesManager.getUsernameSync() ?: "" }
            if (user.isNotEmpty()) {
                currentUsername = user
                deviceRepository.getDevicesByUsernameOnce(user).onSuccess { list ->
                    _devices.value = list
                }
                observeDevices(user)
            }
        }
    }

    fun toggleDevicePower(device: Device, isOn: Boolean) {
        viewModelScope.launch {
            try {
                // 模拟网络延迟
                kotlinx.coroutines.delay(500)

                val statusMap = try {
                    val existingStatus = gson.fromJson(device.status, Map::class.java) as? MutableMap<String, Any> ?: mutableMapOf()
                    existingStatus["power"] = if (isOn) "on" else "off"
                    existingStatus
                } catch (e: Exception) {
                    mutableMapOf<String, Any>("power" to if (isOn) "on" else "off")
                }

                val newStatusJson = gson.toJson(statusMap)
                val updatedDevice = device.copy(status = newStatusJson)

                deviceRepository.updateDevice(updatedDevice).onSuccess {
                    // 【关键闭环 1】：手动操作成功后，通知引擎暂停该设备的自动化习惯，防止抢占
                    autoControlEngine.recordManualOperation(
                        deviceId = device.deviceId,
                        action = newStatusJson
                    )
                    showMessage(if (isOn) "已打开" else "已关闭")
                }.onFailure { exception ->
                    showError(exception.message ?: "操作失败")
                }
            } catch (e: Exception) {
                showError(e.message ?: "操作失败")
            }
        }
    }

    fun deleteDevice(device: Device) {
        executeWithLoading(
            block = { deviceRepository.deleteDevice(device) },
            onSuccess = { viewModelScope.launch { showMessage("设备已删除") } },
            onError = { error -> viewModelScope.launch { showError(error) } }
        )
    }
}