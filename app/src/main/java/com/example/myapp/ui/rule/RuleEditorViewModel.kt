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

@HiltViewModel
class RuleEditorViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val habitRepository: UserHabitRepository,
    private val preferencesManager: PreferencesManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val gson = Gson()

    companion object {
        private const val KEY_SELECTED_DEVICE_ID = "selected_device_id"
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
        private const val KEY_EDITING_HABIT_ID = "editing_habit_id"
    }

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    private val _selectedDevice = MutableStateFlow<Device?>(null)
    val selectedDevice: StateFlow<Device?> = _selectedDevice.asStateFlow()

    private val _selectedAction = MutableStateFlow<String?>(savedStateHandle.get<String>(KEY_SELECTED_ACTION))
    val selectedAction: StateFlow<String?> = _selectedAction.asStateFlow()

    private val _timeWindow = MutableStateFlow<Pair<String, String>?>(restoreTimeWindow())
    val timeWindow: StateFlow<Pair<String, String>?> = _timeWindow.asStateFlow()

    private val _selectedWeekdays = MutableStateFlow<Set<Int>>(
        savedStateHandle.get<IntArray>(KEY_SELECTED_WEEKDAYS)?.toSet() ?: emptySet()
    )
    val selectedWeekdays: StateFlow<Set<Int>> = _selectedWeekdays.asStateFlow()

    private val _temperatureRange = MutableStateFlow<Pair<Float, Float>?>(restoreTemperatureRange())
    val temperatureRange: StateFlow<Pair<Float, Float>?> = _temperatureRange.asStateFlow()

    private val _humidityRange = MutableStateFlow<Pair<Float, Float>?>(restoreHumidityRange())
    val humidityRange: StateFlow<Pair<Float, Float>?> = _humidityRange.asStateFlow()

    private val _enableEnvironment = MutableStateFlow(savedStateHandle.get<Boolean>(KEY_ENABLE_ENVIRONMENT) ?: false)
    val enableEnvironment: StateFlow<Boolean> = _enableEnvironment.asStateFlow()

    private val _habitName = MutableStateFlow(savedStateHandle.get<String>(KEY_HABIT_NAME) ?: "")
    val habitName: StateFlow<String> = _habitName.asStateFlow()

    private val _isEnabled = MutableStateFlow(savedStateHandle.get<Boolean>(KEY_IS_ENABLED) ?: true)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _currentStep = MutableStateFlow(savedStateHandle.get<Int>(KEY_CURRENT_STEP) ?: 0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _saveResult = MutableLiveData<Result<Long>>()
    val saveResult: LiveData<Result<Long>> = _saveResult

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var lastSaveTime = 0L
    private val DEBOUNCE_DELAY_MS = 500L
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        loadDevices()
        restoreSelectedDevice()
    }

    // ----------- 数据恢复与回显模块 -----------

    fun loadHabitData(habitId: Long) {
        if (savedStateHandle.get<Long>(KEY_EDITING_HABIT_ID) == habitId) return
        savedStateHandle[KEY_EDITING_HABIT_ID] = habitId

        viewModelScope.launch {
            try {
                val username = preferencesManager.getUsername().first()
                if (username.isNullOrEmpty()) return@launch

                // 复用 UserHabitRepository 中已有的 getAllHabits
                val habitsList = habitRepository.getAllHabits(username).first()
                val targetHabit = habitsList.find { it.id == habitId } ?: return@launch

                // 1. 回显设备（【已修复】：严格使用 DeviceRepository 中存在的 getDevicesByUsernameOnce，并解析 Result）
                val deviceListResult = deviceRepository.getDevicesByUsernameOnce(username)
                val deviceList = deviceListResult.getOrNull() ?: emptyList()
                deviceList.find { it.deviceId == targetHabit.deviceId }?.let { setSelectedDevice(it) }

                // 2. 回显动作
                try {
                    val actionMap = gson.fromJson(targetHabit.actionCommand, Map::class.java) as? Map<String, Any>
                    val power = actionMap?.get("power")?.toString() ?: actionMap?.get("action")?.toString()
                    if (power == "on") setSelectedAction("打开")
                    else if (power == "off") setSelectedAction("关闭")
                } catch (e: Exception) {
                    Timber.e(e, "解析动作回显失败")
                }

                // 3. 回显时间窗
                val times = targetHabit.timeWindow.split("-")
                if (times.size == 2) setTimeWindow(times[0], times[1])

                // 4. 回显星期（Bitmask 解码）
                val mask = targetHabit.weekType
                if (mask == 0) {
                    setSelectedWeekdays(emptySet())
                } else {
                    val parsedDays = mutableSetOf<Int>()
                    for (i in 1..7) {
                        if ((mask and (1 shl i)) != 0) parsedDays.add(i)
                    }
                    setSelectedWeekdays(parsedDays)
                }

                // 5. 回显环境阈值
                if (!targetHabit.environmentThreshold.isNullOrEmpty()) {
                    setEnableEnvironment(true)
                    try {
                        val envMap = gson.fromJson(targetHabit.environmentThreshold, Map::class.java) as? Map<String, Any>
                        (envMap?.get("temperature") as? Map<String, Double>)?.let { tMap ->
                            setTemperatureRange(tMap["min"]?.toFloat() ?: 0f, tMap["max"]?.toFloat() ?: 100f)
                        }
                        (envMap?.get("humidity") as? Map<String, Double>)?.let { hMap ->
                            setHumidityRange(hMap["min"]?.toFloat() ?: 0f, hMap["max"]?.toFloat() ?: 100f)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "解析环境数据回显失败")
                    }
                }

                // 6. 回显基础设置
                setHabitName(targetHabit.habitName)
                setIsEnabled(targetHabit.isEnabled)

            } catch (e: Exception) {
                Timber.e(e, "加载习惯数据失败: ${e.message}")
            }
        }
    }

    private fun restoreTimeWindow(): Pair<String, String>? {
        val start = savedStateHandle.get<String>(KEY_TIME_START)
        val end = savedStateHandle.get<String>(KEY_TIME_END)
        return if (start != null && end != null) Pair(start, end) else null
    }

    private fun restoreTemperatureRange(): Pair<Float, Float>? {
        val min = savedStateHandle.get<Float>(KEY_TEMP_MIN)
        val max = savedStateHandle.get<Float>(KEY_TEMP_MAX)
        return if (min != null && max != null) Pair(min, max) else null
    }

    private fun restoreHumidityRange(): Pair<Float, Float>? {
        val min = savedStateHandle.get<Float>(KEY_HUMIDITY_MIN)
        val max = savedStateHandle.get<Float>(KEY_HUMIDITY_MAX)
        return if (min != null && max != null) Pair(min, max) else null
    }

    private fun restoreSelectedDevice() {
        val savedDeviceId = savedStateHandle.get<Long>(KEY_SELECTED_DEVICE_ID)
        if (savedDeviceId != null) {
            viewModelScope.launch {
                _devices.collect { deviceList ->
                    val device = deviceList.find { it.deviceId == savedDeviceId }
                    if (device != null && _selectedDevice.value == null) {
                        _selectedDevice.value = device
                    }
                }
            }
        }
    }

    private fun loadDevices() {
        viewModelScope.launch {
            try {
                val username = preferencesManager.getUsername().first()
                if (username != null) {
                    // 【严格对应】：DeviceRepository 中的 getAllDevices(username) 返回 Flow<List<Device>>
                    deviceRepository.getAllDevices(username).collect { deviceList ->
                        _devices.value = deviceList
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load devices")
            }
        }
    }

    // ----------- 状态设置器 -----------

    fun setSelectedDevice(device: Device) {
        _selectedDevice.value = device
        savedStateHandle[KEY_SELECTED_DEVICE_ID] = device.deviceId
    }

    fun setSelectedAction(action: String) {
        _selectedAction.value = action
        savedStateHandle[KEY_SELECTED_ACTION] = action
    }

    fun setTimeWindow(startTime: String, endTime: String) {
        _timeWindow.value = Pair(startTime, endTime)
        savedStateHandle[KEY_TIME_START] = startTime
        savedStateHandle[KEY_TIME_END] = endTime
    }

    fun setSelectedWeekdays(weekdays: Set<Int>) {
        _selectedWeekdays.value = weekdays
        savedStateHandle[KEY_SELECTED_WEEKDAYS] = weekdays.toIntArray()
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
    }

    // ----------- 保存规则逻辑 -----------

    fun saveRule() {
        viewModelScope.launch {
            try {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastSaveTime < DEBOUNCE_DELAY_MS || _isSaving.value) return@launch

                _isSaving.value = true
                lastSaveTime = currentTime

                val username = preferencesManager.getUsername().first()
                if (username == null) {
                    _saveResult.value = Result.failure(Exception("Username not found"))
                    return@launch
                }

                val device = _selectedDevice.value ?: run { _saveResult.value = Result.failure(Exception("请选择设备")); return@launch }
                val action = _selectedAction.value ?: run { _saveResult.value = Result.failure(Exception("请选择动作")); return@launch }
                val timeWindow = _timeWindow.value ?: run { _saveResult.value = Result.failure(Exception("请设置时间窗口")); return@launch }

                val editingId = savedStateHandle.get<Long>(KEY_EDITING_HABIT_ID) ?: 0L

                val habit = UserHabit(
                    id = editingId,
                    deviceId = device.deviceId,
                    habitName = _habitName.value.ifEmpty { "自定义习惯" },
                    triggerCondition = buildTriggerCondition(),
                    actionCommand = buildActionCommand(action),
                    weekType = calculateWeekType(),
                    timeWindow = "${timeWindow.first}-${timeWindow.second}",
                    environmentThreshold = buildEnvironmentThreshold(),
                    confidence = 1.0,
                    isEnabled = _isEnabled.value,
                    username = username
                )

                // 【严格对应】：UserHabitRepository 中的 saveHabit(habit) 返回 Result<Long>
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
        val command = mutableMapOf<String, Any>()
        when (action.lowercase()) {
            "on", "打开", "开" -> command["power"] = "on"
            "off", "关闭", "关" -> command["power"] = "off"
            else -> {
                Timber.w("Unknown action format: $action, fallback to power")
                command["power"] = action
            }
        }
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
        if (weekdays.isEmpty() || weekdays.size == 7) return 0

        var mask = 0
        for (day in weekdays) {
            mask = mask or (1 shl day)
        }
        return mask
    }
}