package com.example.myapp.ui.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.local.entity.UserHabit
import com.example.myapp.data.repository.UserHabitRepository
import com.example.myapp.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import javax.inject.Inject

/**
 * 习惯详情 ViewModel
 */
@HiltViewModel
class HabitDetailViewModel @Inject constructor(
    private val habitRepository: UserHabitRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _habit = MutableStateFlow<UserHabit?>(null)
    val habit: StateFlow<UserHabit?> = _habit.asStateFlow()

    private val _timeDistribution = MutableStateFlow<List<Pair<Long, Int>>>(emptyList())
    val timeDistribution: StateFlow<List<Pair<Long, Int>>> = _timeDistribution.asStateFlow()

    private val _environmentCorrelation = MutableStateFlow<Map<String, Float>>(emptyMap())
    val environmentCorrelation: StateFlow<Map<String, Float>> = _environmentCorrelation.asStateFlow()

    /**
     * 加载习惯详情
     */
    fun loadHabitDetail(habitId: Long) {
        viewModelScope.launch {
            try {
                preferencesManager.getUsername().collect { username ->
                    if (username == null) return@collect
                    
                    // 加载习惯信息
                    habitRepository.getAllHabits(username).collect { habits ->
                        _habit.value = habits.find { it.id == habitId }
                    }
                    
                    // 加载30天时间分布数据
                    loadTimeDistribution(habitId, username)
                    
                    // 加载环境关联数据
                    loadEnvironmentCorrelation(habitId, username)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load habit detail")
            }
        }
    }

    /**
     * 加载30天时间分布
     */
    private suspend fun loadTimeDistribution(habitId: Long, username: String) {
        try {
            val calendar = Calendar.getInstance()
            val endTime = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, -30)
            val startTime = calendar.timeInMillis

            val result = habitRepository.getLogsForLearning(
                _habit.value?.deviceId ?: 0,
                startTime,
                endTime,
                username
            )

            result.onSuccess { logs ->
                // 按天分组统计
                val dailyCount = mutableMapOf<Long, Int>()
                logs.forEach { log ->
                    val dayStart = getDayStart(log.operateTime)
                    dailyCount[dayStart] = (dailyCount[dayStart] ?: 0) + 1
                }

                // 填充30天数据（包括没有操作的日期）
                val distribution = mutableListOf<Pair<Long, Int>>()
                val cal = Calendar.getInstance()
                cal.timeInMillis = startTime
                
                for (i in 0 until 30) {
                    val dayStart = getDayStart(cal.timeInMillis)
                    distribution.add(Pair(dayStart, dailyCount[dayStart] ?: 0))
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }

                _timeDistribution.value = distribution
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load time distribution")
        }
    }

    /**
     * 加载环境关联数据
     */
    private suspend fun loadEnvironmentCorrelation(habitId: Long, username: String) {
        try {
            // 模拟环境关联数据（实际应从日志中分析）
            val correlation = mapOf(
                "温度" to 0.75f,
                "湿度" to 0.60f,
                "光照" to 0.85f,
                "时间" to 0.90f
            )
            _environmentCorrelation.value = correlation
        } catch (e: Exception) {
            Timber.e(e, "Failed to load environment correlation")
        }
    }

    /**
     * 获取某天的开始时间戳
     */
    private fun getDayStart(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}

