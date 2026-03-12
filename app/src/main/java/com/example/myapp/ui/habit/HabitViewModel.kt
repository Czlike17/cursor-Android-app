package com.example.myapp.ui.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.local.entity.UserHabit
import com.example.myapp.data.repository.UserHabitRepository
import com.example.myapp.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 习惯列表 ViewModel
 */
@HiltViewModel
class HabitViewModel @Inject constructor(
    private val habitRepository: UserHabitRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _habits = MutableStateFlow<List<UserHabit>>(emptyList())
    val habits: StateFlow<List<UserHabit>> = _habits.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadHabits()
    }

    /**
     * 加载习惯列表
     */
    fun loadHabits() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // 获取用户名（只取第一个值）
                val username = preferencesManager.getUsername().first()
                
                if (username.isNullOrEmpty()) {
                    Timber.w("Username is null or empty, cannot load habits")
                    _habits.value = emptyList()
                    _isLoading.value = false
                    return@launch
                }
                
                // 监听习惯列表变化
                habitRepository.getAllHabits(username).collect { habitList ->
                    _habits.value = habitList
                    _isLoading.value = false
                    Timber.d("Loaded ${habitList.size} habits for user: $username")
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to load habits")
                _habits.value = emptyList()
                _isLoading.value = false
            }
        }
    }

    /**
     * 更新习惯启用状态
     */
    fun updateHabitEnabled(habit: UserHabit, isEnabled: Boolean) {
        viewModelScope.launch {
            try {
                val username = preferencesManager.getUsername().first()
                if (username.isNullOrEmpty()) return@launch
                
                habitRepository.updateHabitEnabled(habit.id, isEnabled, username)
                Timber.d("Updated habit ${habit.habitName} enabled: $isEnabled")
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to update habit enabled state")
            }
        }
    }

    /**
     * 删除低置信度习惯
     */
    fun deleteLowConfidenceHabits(minConfidence: Double = 0.5) {
        viewModelScope.launch {
            try {
                val username = preferencesManager.getUsername().first()
                if (username.isNullOrEmpty()) return@launch
                
                val result = habitRepository.deleteLowConfidenceHabits(minConfidence, username)
                result.onSuccess { count ->
                    Timber.d("Deleted $count low confidence habits")
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete low confidence habits")
            }
        }
    }
}

