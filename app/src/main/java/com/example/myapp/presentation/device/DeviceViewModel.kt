package com.example.myapp.presentation.device

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.local.entity.Device
import com.example.myapp.data.repository.DeviceRepository
import com.example.myapp.presentation.base.BaseViewModel
import com.example.myapp.util.PreferencesManager
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 设备列表 ViewModel
 * 修复说明：
 * 1. 彻底解决 DataStore 异步写入导致的 username 竞态条件死锁。
 * 2. 引入响应式 collectLatest 监听用户名，保证账号切换或延迟写入时，设备列表能 100% 自动装载。
 */
@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val preferencesManager: PreferencesManager
) : BaseViewModel() {

    private val gson = Gson()

    // 使用 Volatile 保证多协程可见性
    @Volatile
    private var currentUsername: String = ""

    private var observeJob: Job? = null

    // 设备列表
    private val _devices = MutableLiveData<List<Device>>()
    val devices: LiveData<List<Device>> = _devices

    init {
        // 【核心安全修复】：弃用只读一次的 .first()，改为持续监听用户名流
        viewModelScope.launch {
            preferencesManager.getUsername().collectLatest { username ->
                val newUsername = username ?: ""
                // 只要用户名发生合法改变（比如从空变为 admin），立即触发数据库监听
                if (newUsername.isNotEmpty() && newUsername != currentUsername) {
                    currentUsername = newUsername
                    observeDevices(newUsername)
                }
            }
        }
    }

    /**
     * 监听设备列表变化（自动刷新）
     */
    private fun observeDevices(username: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            try {
                deviceRepository.getDevicesByUsername(username).collect { devices ->
                    _devices.postValue(devices)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _devices.postValue(emptyList())
            }
        }
    }

    /**
     * 加载设备列表（带超时保护与容错）
     */
    fun loadDevices() {
        viewModelScope.launch {
            // 双重保险：如果此时内存中的用户名还是空，强制同步阻塞读取一次兜底
            if (currentUsername.isEmpty()) {
                currentUsername = preferencesManager.getUsername().first() ?: ""
            }

            if (currentUsername.isEmpty()) {
                // 如果兜底依然为空，说明真的未登录，静默返回即可
                return@launch
            }

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

                timeoutJob.cancel()
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