package com.example.myapp.presentation.auth

import androidx.lifecycle.viewModelScope
import com.example.myapp.data.repository.UserRepository
import com.example.myapp.presentation.base.BaseViewModel
import com.example.myapp.util.PreferencesManager
import com.example.myapp.util.ValidateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 启动页 ViewModel
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : BaseViewModel() {

    private val _navigationEvent = MutableStateFlow<NavigationEvent>(NavigationEvent.None)
    val navigationEvent: StateFlow<NavigationEvent> = _navigationEvent.asStateFlow()

    /**
     * 检查登录状态
     */
    fun checkLoginState() {
        viewModelScope.launch {
            try {
                // 获取登录状态和自动登录设置
                val isLoggedIn = preferencesManager.getLoginState().first()
                val autoLogin = preferencesManager.getAutoLogin().first()
                val username = preferencesManager.getUsername().first()

                // 根据状态决定跳转
                _navigationEvent.value = when {
                    isLoggedIn && autoLogin && !username.isNullOrEmpty() -> {
                        NavigationEvent.ToMain(username)
                    }
                    else -> NavigationEvent.ToLogin
                }
            } catch (e: Exception) {
                _navigationEvent.value = NavigationEvent.ToLogin
            }
        }
    }

    /**
     * 导航事件
     */
    sealed class NavigationEvent {
        object None : NavigationEvent()
        object ToLogin : NavigationEvent()
        data class ToMain(val username: String) : NavigationEvent()
    }
}

