package com.example.myapp.presentation.device

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.local.entity.Device
import com.example.myapp.data.local.entity.UserHabitLog
import com.example.myapp.data.repository.DeviceRepository
import com.example.myapp.data.repository.EnvironmentRepository
import com.example.myapp.data.repository.UserHabitRepository
import com.example.myapp.engine.CustomAutoControlEngine
import com.example.myapp.presentation.base.BaseViewModel
import com.example.myapp.util.PreferencesManager
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val preferencesManager: PreferencesManager,
    private val autoControlEngine: CustomAutoControlEngine,
    private val userHabitRepository: UserHabitRepository,     // 【新增注入】：用于记录操作日志
    private val environmentRepository: EnvironmentRepository  // 【新增注入】：用于获取环境快照
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
                    // 1. 拦截抢占
                    autoControlEngine.recordManualOperation(
                        deviceId = device.deviceId,
                        action = newStatusJson
                    )

                    showMessage(if (isOn) "已打开" else "已关闭")

                    // 2. 【阶段一核心】：静默采集 AI 矿脉数据
                    recordHabitLog(device.deviceId, newStatusJson)

                }.onFailure { exception ->
                    showError(exception.message ?: "操作失败")
                }
            } catch (e: Exception) {
                showError(e.message ?: "操作失败")
            }
        }
    }

    /**
     * 【企业级新增】：异步记录用户操作与环境快照，为 AI 提取算法提供数据源
     */
    private fun recordHabitLog(deviceId: Long, actionJson: String) {
        // 使用 IO 线程防止阻塞主线程与数据库死锁
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val username = currentUsername.ifEmpty { preferencesManager.getUsernameSync() ?: return@launch }
                val currentTime = System.currentTimeMillis()

                // 1. 解析标准星期类型 (1=周一, 7=周日)
                val calendar = Calendar.getInstance().apply { timeInMillis = currentTime }
                val weekType = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> 1
                    Calendar.TUESDAY -> 2
                    Calendar.WEDNESDAY -> 3
                    Calendar.THURSDAY -> 4
                    Calendar.FRIDAY -> 5
                    Calendar.SATURDAY -> 6
                    Calendar.SUNDAY -> 7
                    else -> 1
                }

                // 2. 抓取全局最新的环境数据作为快照
                val recentEnvList = environmentRepository.getRecentEnvironmentData(1, username).firstOrNull()
                val envSnapshot = recentEnvList?.firstOrNull()

                val envJson = envSnapshot?.let {
                    val map = mutableMapOf<String, Any>()
                    it.temperature?.let { t -> map["temperature"] = t }
                    it.humidity?.let { h -> map["humidity"] = h }
                    it.lightIntensity?.let { l -> map["lightIntensity"] = l }
                    if (map.isNotEmpty()) gson.toJson(map) else null
                }

                // 3. 构建并插入日志
                val log = UserHabitLog(
                    deviceId = deviceId,
                    action = actionJson,
                    operateTime = currentTime,
                    weekType = weekType,
                    environmentData = envJson,
                    isManualCancel = false,
                    username = username
                )

                userHabitRepository.logUserOperation(log)
                Timber.d("AI Sourcing: 成功埋点设备操作记录 -> %s, Env: %s", actionJson, envJson)

            } catch (e: Exception) {
                Timber.e(e, "AI Sourcing: 埋点记录失败")
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