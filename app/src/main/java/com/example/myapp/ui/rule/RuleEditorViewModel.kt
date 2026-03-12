package com.example.myapp.ui.rule

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.local.entity.Device
import com.example.myapp.data.local.entity.UserHabit
import com.example.myapp.data.repository.DeviceRepository
import com.example.myapp.data.repository.UserHabitRepository
import com.example.myapp.util.PreferencesManager
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 规则编辑器 ViewModel（支持 SavedStateHandle 状态恢复）
 */
@HiltViewModel
class RuleEditorViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val habitRepository: UserHabitRepository,
    private val preferencesManager: PreferencesManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val gson = Gson()
    
    // SavedStateHandle 键名常量
    companion object {
        private const val KEY_SELECTED_DEVICE_ID = "selected_device_id"  // Long 类型
        private const val KEY_SELECTED_ACTION = "selected_action"
        private const val KEY_TIME_START = "time_start"
        private const val KEY_TIME_END = "time_end"
        private const val KEY_SELECTED_WEEKDAYS = "selected_weekdays"
        private const val KEY_TEMP_MIN = "temp_min"
        private const val KEY_TEMP_MAX = "temp_max"
        private const val KEY_HUMIDITY_MIN = "humidity_min"
        private const val KEY_HUMIDITY_MAX = "humidity_max"
        private const val KEY_ENABLE_ENVIRONMENT = "enable_environment"
        private const val KEY_HABIT_NAME = "habit_name"
        private const val KEY_IS_ENABLED = "is_enabled"
        private const val KEY_CURRENT_STEP = "current_step"
    }

    // 设备列表
    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    // 选中的设备（从 SavedStateHandle 恢复）
    private val _selectedDevice = MutableStateFlow<Device?>(null)
    val selectedDevice: StateFlow<Device?> = _selectedDevice.asStateFlow()

    // 选中的动作（与 SavedStateHandle 绑定）
    private val _selectedAction = MutableStateFlow<String?>(
        savedStateHandle.get<String>(KEY_SELECTED_ACTION)
    )
    val selectedAction: StateFlow<String?> = _selectedAction.asStateFlow()

    // 时间窗口（从 SavedStateHandle 恢复）
    private val _timeWindow = MutableStateFlow<Pair<String, String>?>(
        restoreTimeWindow()
    )
    val timeWindow: StateFlow<Pair<String, String>?> = _timeWindow.asStateFlow()

    // 选中的星期（从 SavedStateHandle 恢复）
    private val _selectedWeekdays = MutableStateFlow<Set<Int>>(
        savedStateHandle.get<IntArray>(KEY_SELECTED_WEEKDAYS)?.toSet() ?: emptySet()
    )
    val selectedWeekdays: StateFlow<Set<Int>> = _selectedWeekdays.asStateFlow()

    // 环境条件（从 SavedStateHandle 恢复）
    private val _temperatureRange = MutableStateFlow<Pair<Float, Float>?>(
        restoreTemperatureRange()
    )
    val temperatureRange: StateFlow<Pair<Float, Float>?> = _temperatureRange.asStateFlow()

    private val _humidityRange = MutableStateFlow<Pair<Float, Float>?>(
        restoreHumidityRange()
    )
    val humidityRange: StateFlow<Pair<Float, Float>?> = _humidityRange.asStateFlow()

    private val _enableEnvironment = MutableStateFlow(
        savedStateHandle.get<Boolean>(KEY_ENABLE_ENVIRONMENT) ?: false
    )
    val enableEnvironment: StateFlow<Boolean> = _enableEnvironment.asStateFlow()

    // 习惯名称（从 SavedStateHandle 恢复）
    private val _habitName = MutableStateFlow(
        savedStateHandle.get<String>(KEY_HABIT_NAME) ?: ""
    )
    val habitName: StateFlow<String> = _habitName.asStateFlow()

    // 是否启用（从 SavedStateHandle 恢复）
    private val _isEnabled = MutableStateFlow(
        savedStateHandle.get<Boolean>(KEY_IS_ENABLED) ?: true
    )
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    // 当前步骤（从 SavedStateHandle 恢复）
    private val _currentStep = MutableStateFlow(
        savedStateHandle.get<Int>(KEY_CURRENT_STEP) ?: 0
    )
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    // 保存结果
    private val _saveResult = MutableLiveData<Result<Long>>()
    val saveResult: LiveData<Result<Long>> = _saveResult
    
    // 错误消息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadDevices()
        restoreSelectedDevice()
    }
    
    /**
     * 从 SavedStateHandle 恢复时间窗口
     */
    private fun restoreTimeWindow(): Pair<String, String>? {
        val start = savedStateHandle.get<String>(KEY_TIME_START)
        val end = savedStateHandle.get<String>(KEY_TIME_END)
        return if (start != null && end != null) Pair(start, end) else null
    }
    
    /**
     * 从 SavedStateHandle 恢复温度范围
     */
    private fun restoreTemperatureRange(): Pair<Float, Float>? {
        val min = savedStateHandle.get<Float>(KEY_TEMP_MIN)
        val max = savedStateHandle.get<Float>(KEY_TEMP_MAX)
        return if (min != null && max != null) Pair(min, max) else null
    }
    
    /**
     * 从 SavedStateHandle 恢复湿度范围
     */
    private fun restoreHumidityRange(): Pair<Float, Float>? {
        val min = savedStateHandle.get<Float>(KEY_HUMIDITY_MIN)
        val max = savedStateHandle.get<Float>(KEY_HUMIDITY_MAX)
        return if (min != null && max != null) Pair(min, max) else null
    }
    
    /**
     * 恢复选中的设备（异步加载设备列表后匹配）
     */
    private fun restoreSelectedDevice() {
        val savedDeviceId = savedStateHandle.get<Long>(KEY_SELECTED_DEVICE_ID)
        if (savedDeviceId != null) {
            viewModelScope.launch {
                _devices.collect { deviceList ->
                    val device = deviceList.find { it.deviceId == savedDeviceId }
                    if (device != null && _selectedDevice.value == null) {
                        _selectedDevice.value = device
                        Timber.d("恢复选中设备: ${device.deviceName}")
                    }
                }
            }
        }
    }

    /**
     * 加载设备列表（修复协程阻塞问题）
     */
    private fun loadDevices() {
        viewModelScope.launch {
            try {
                // 使用 first() 获取用户名，避免 collect 阻塞
                val username = preferencesManager.getUsername().first()
                if (username != null) {
                    // 启动持续监听设备列表变化
                    deviceRepository.getAllDevices(username).collect { deviceList ->
                        _devices.value = deviceList
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load devices")
            }
        }
    }

    fun setSelectedDevice(device: Device) {
        _selectedDevice.value = device
        savedStateHandle[KEY_SELECTED_DEVICE_ID] = device.deviceId  // Long 类型
        Timber.d("保存选中设备: ${device.deviceName}")
    }

    fun setSelectedAction(action: String) {
        _selectedAction.value = action
        savedStateHandle[KEY_SELECTED_ACTION] = action
        Timber.d("保存选中动作: $action")
    }

    fun setTimeWindow(startTime: String, endTime: String) {
        _timeWindow.value = Pair(startTime, endTime)
        savedStateHandle[KEY_TIME_START] = startTime
        savedStateHandle[KEY_TIME_END] = endTime
        Timber.d("保存时间窗口: $startTime - $endTime")
    }

    fun setSelectedWeekdays(weekdays: Set<Int>) {
        _selectedWeekdays.value = weekdays
        savedStateHandle[KEY_SELECTED_WEEKDAYS] = weekdays.toIntArray()
        Timber.d("保存选中星期: $weekdays")
    }

    fun setTemperatureRange(min: Float, max: Float) {
        _temperatureRange.value = Pair(min, max)
        savedStateHandle[KEY_TEMP_MIN] = min
        savedStateHandle[KEY_TEMP_MAX] = max
    }

    fun setHumidityRange(min: Float, max: Float) {
        _humidityRange.value = Pair(min, max)
        savedStateHandle[KEY_HUMIDITY_MIN] = min
        savedStateHandle[KEY_HUMIDITY_MAX] = max
    }

    fun setEnableEnvironment(enabled: Boolean) {
        _enableEnvironment.value = enabled
        savedStateHandle[KEY_ENABLE_ENVIRONMENT] = enabled
    }

    fun setHabitName(name: String) {
        _habitName.value = name
        savedStateHandle[KEY_HABIT_NAME] = name
    }

    fun setIsEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        savedStateHandle[KEY_IS_ENABLED] = enabled
    }
    
    fun setCurrentStep(step: Int) {
        _currentStep.value = step
        savedStateHandle[KEY_CURRENT_STEP] = step
        Timber.d("保存当前步骤: $step")
    }

    // 防抖控制：记录上次保存时间
    private var lastSaveTime = 0L
    private val DEBOUNCE_DELAY_MS = 500L
    
    // 保存状态标志
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    /**
     * 保存规则（带防抖逻辑）
     */
    fun saveRule() {
        viewModelScope.launch {
            try {
                // 防抖检查：如果距离上次保存不足 500ms，直接返回
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastSaveTime < DEBOUNCE_DELAY_MS) {
                    Timber.d("Debounce: Ignoring duplicate save request")
                    return@launch
                }
                
                // 防止重复保存：如果正在保存中，直接返回
                if (_isSaving.value) {
                    Timber.d("Already saving, ignoring duplicate request")
                    return@launch
                }
                
                _isSaving.value = true
                lastSaveTime = currentTime
                
                // 使用 first() 获取用户名，避免 collect 阻塞
                val username = preferencesManager.getUsername().first()
                if (username == null) {
                    _saveResult.value = Result.failure(Exception("Username not found"))
                    _isSaving.value = false
                    return@launch
                }
                
                val device = _selectedDevice.value
                if (device == null) {
                    _saveResult.value = Result.failure(Exception("请选择设备"))
                    _isSaving.value = false
                    return@launch
                }
                
                val action = _selectedAction.value
                if (action == null) {
                    _saveResult.value = Result.failure(Exception("请选择动作"))
                    _isSaving.value = false
                    return@launch
                }
                
                val timeWindow = _timeWindow.value
                if (timeWindow == null) {
                    _saveResult.value = Result.failure(Exception("请设置时间窗口"))
                    _isSaving.value = false
                    return@launch
                }

                // 构建触发条件JSON
                val triggerCondition = buildTriggerCondition()

                // 构建动作指令JSON
                val actionCommand = buildActionCommand(action)

                // 构建环境阈值JSON
                val environmentThreshold = buildEnvironmentThreshold()

                // 计算星期类型
                val weekType = calculateWeekType()

                val habit = UserHabit(
                    deviceId = device.deviceId,
                    habitName = _habitName.value.ifEmpty { "自定义习惯" },
                    triggerCondition = triggerCondition,
                    actionCommand = actionCommand,
                    weekType = weekType,
                    timeWindow = "${timeWindow.first}-${timeWindow.second}",
                    environmentThreshold = environmentThreshold,
                    confidence = 1.0, // 手动创建的规则置信度为1.0
                    isEnabled = _isEnabled.value,
                    username = username
                )

                val result = habitRepository.saveHabit(habit)
                result.onSuccess {
                    _saveResult.value = Result.success(it)
                }.onFailure { error ->
                    _errorMessage.value = "保存失败: ${error.message}"
                    _saveResult.value = Result.failure(error)
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to save rule")
                _errorMessage.value = "保存失败: ${e.message}"
                _saveResult.value = Result.failure(e)
            } finally {
                _isSaving.value = false
            }
        }
    }
    
    /**
     * 清除错误消息
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    private fun buildTriggerCondition(): String {
        val timeWindow = _timeWindow.value ?: return "{}"
        val condition = mutableMapOf<String, Any>()
        condition["time"] = "${timeWindow.first}-${timeWindow.second}"
        condition["weekType"] = calculateWeekType()
        return gson.toJson(condition)
    }

    private fun buildActionCommand(action: String): String {
        val command = mapOf("action" to action)
        return gson.toJson(command)
    }

    private fun buildEnvironmentThreshold(): String? {
        if (!_enableEnvironment.value) return null

        val threshold = mutableMapOf<String, Any>()
        
        _temperatureRange.value?.let { (min, max) ->
            threshold["temperature"] = mapOf("min" to min.toInt(), "max" to max.toInt())
        }
        
        _humidityRange.value?.let { (min, max) ->
            threshold["humidity"] = mapOf("min" to min.toInt(), "max" to max.toInt())
        }

        return if (threshold.isEmpty()) null else gson.toJson(threshold)
    }

    private fun calculateWeekType(): Int {
        val weekdays = _selectedWeekdays.value
        return when {
            weekdays.isEmpty() -> 0 // 每天
            weekdays.size == 7 -> 0 // 每天
            weekdays.size == 1 -> weekdays.first() // 单个星期
            else -> 0 // 多个星期暂时返回0(每天)
        }
    }
}

