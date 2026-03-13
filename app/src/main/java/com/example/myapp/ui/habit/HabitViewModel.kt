package com.example.myapp.ui.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.local.entity.UserHabit
import com.example.myapp.data.repository.UserHabitRepository
import com.example.myapp.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HabitViewModel @Inject constructor(
    private val habitRepository: UserHabitRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 【企业级修复 1】：彻底拆除协程炸弹，使用 flatMapLatest 将数据库查询转为响应式单向数据流
    @OptIn(ExperimentalCoroutinesApi::class)
    val habits: StateFlow<List<UserHabit>> = preferencesManager.getUsername()
        .filterNotNull()
        .filter { it.isNotEmpty() }
        .onEach { _isLoading.value = true }
        .flatMapLatest { username ->
            habitRepository.getAllHabits(username)
        }
        .onEach { _isLoading.value = false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // 防止后台应用被杀时的数据丢失
            initialValue = emptyList()
        )

    init {
        // 数据现由 Flow 自动驱动，无需手动 loadHabits()
    }

    fun loadHabits() {
        // 已废弃。保留空方法以防外部调用导致崩溃
        Timber.d("loadHabits() called but ignored. Using Flow auto-updates.")
    }

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

    // 【企业级修复 2】：新增删除功能
    fun deleteHabit(habit: UserHabit) {
        viewModelScope.launch {
            try {
                // 假设 Repository 中封装了删除方法，直接调用
                habitRepository.deleteHabit(habit)
                Timber.d("Deleted habit: ${habit.habitName}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete habit")
            }
        }
    }
}