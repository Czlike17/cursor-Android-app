package com.example.myapp.presentation.auth

import androidx.lifecycle.viewModelScope
import com.example.myapp.BuildConfig
import com.example.myapp.data.repository.UserRepository
import com.example.myapp.mock.MockInitializer
import com.example.myapp.presentation.base.BaseViewModel
import com.example.myapp.util.PreferencesManager
import com.example.myapp.util.ValidateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 登录页 ViewModel
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val preferencesManager: PreferencesManager,
    private val mockInitializer: MockInitializer
) : BaseViewModel() {

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _isPasswordVisible = MutableStateFlow(false)
    val isPasswordVisible: StateFlow<Boolean> = _isPasswordVisible.asStateFlow()

    private val _rememberPassword = MutableStateFlow(true)
    val rememberPassword: StateFlow<Boolean> = _rememberPassword.asStateFlow()

    private val _isLoginButtonEnabled = MutableStateFlow(false)
    val isLoginButtonEnabled: StateFlow<Boolean> = _isLoginButtonEnabled.asStateFlow()

    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess: StateFlow<Boolean> = _loginSuccess.asStateFlow()

    /**
     * 更新用户名
     */
    fun updateUsername(value: String) {
        _username.value = value
        checkLoginButtonState()
    }

    /**
     * 更新密码
     */
    fun updatePassword(value: String) {
        _password.value = value
        checkLoginButtonState()
    }

    /**
     * 切换密码可见性
     */
    fun togglePasswordVisibility() {
        _isPasswordVisible.value = !_isPasswordVisible.value
    }

    /**
     * 设置记住密码
     */
    fun setRememberPassword(remember: Boolean) {
        _rememberPassword.value = remember
    }

    /**
     * 检查登录按钮状态
     */
    private fun checkLoginButtonState() {
        _isLoginButtonEnabled.value = _username.value.isNotBlank() && 
                                      _password.value.isNotBlank() &&
                                      _username.value.length >= 3 &&
                                      _password.value.length >= 6
    }

    /**
     * 登录
     */
    fun login() {
        val usernameValue = _username.value.trim()
        val passwordValue = _password.value

        // 输入验证
        if (usernameValue.isBlank()) {
            launchOnUI { showError("请输入用户名") }
            return
        }

        if (usernameValue.length < 3 || usernameValue.length > 20) {
            launchOnUI { showError("用户名长度应为 3-20 个字符") }
            return
        }

        if (!ValidateUtils.isAlphanumeric(usernameValue)) {
            launchOnUI { showError("用户名只能包含字母和数字") }
            return
        }

        if (passwordValue.isBlank()) {
            launchOnUI { showError("请输入密码") }
            return
        }

        if (passwordValue.length < 6) {
            launchOnUI { showError("密码长度不能少于 6 位") }
            return
        }

        // 执行登录
        executeWithLoading(
            block = {
                userRepository.login(usernameValue, passwordValue)
            },
            onSuccess = { user ->
                // 保存登录信息到 DataStore
                viewModelScope.launch {
                    preferencesManager.saveLoginInfo(
                        username = user.username,
                        autoLogin = _rememberPassword.value
                    )
                    
                    // Mock 模式：自动注入虚拟设备
                    if (BuildConfig.IS_MOCK_MODE) {
                        mockInitializer.onUserLoggedIn(user.username)
                    }
                    
                    _loginSuccess.value = true
                    showMessage("登录成功")
                }
            },
            onError = { error ->
                launchOnUI { showError(error) }
            }
        )
    }
}

