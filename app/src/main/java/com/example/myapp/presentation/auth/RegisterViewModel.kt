package com.example.myapp.presentation.auth

import androidx.lifecycle.viewModelScope
import com.example.myapp.BuildConfig
import com.example.myapp.data.repository.UserRepository
import com.example.myapp.mock.MockInitializer
import com.example.myapp.presentation.base.BaseViewModel
import com.example.myapp.util.PasswordUtils
import com.example.myapp.util.PreferencesManager
import com.example.myapp.util.ValidateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 注册页 ViewModel
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val preferencesManager: PreferencesManager,
    private val mockInitializer: MockInitializer
) : BaseViewModel() {

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _usernameError = MutableStateFlow<String?>(null)
    val usernameError: StateFlow<String?> = _usernameError.asStateFlow()

    private val _isUsernameChecking = MutableStateFlow(false)
    val isUsernameChecking: StateFlow<Boolean> = _isUsernameChecking.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _passwordStrength = MutableStateFlow(0)
    val passwordStrength: StateFlow<Int> = _passwordStrength.asStateFlow()

    private val _passwordStrengthText = MutableStateFlow("")
    val passwordStrengthText: StateFlow<String> = _passwordStrengthText.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    private val _confirmPasswordError = MutableStateFlow<String?>(null)
    val confirmPasswordError: StateFlow<String?> = _confirmPasswordError.asStateFlow()

    private val _isPasswordVisible = MutableStateFlow(false)
    val isPasswordVisible: StateFlow<Boolean> = _isPasswordVisible.asStateFlow()

    private val _isConfirmPasswordVisible = MutableStateFlow(false)
    val isConfirmPasswordVisible: StateFlow<Boolean> = _isConfirmPasswordVisible.asStateFlow()

    private val _isRegisterButtonEnabled = MutableStateFlow(false)
    val isRegisterButtonEnabled: StateFlow<Boolean> = _isRegisterButtonEnabled.asStateFlow()

    private val _registerSuccess = MutableStateFlow(false)
    val registerSuccess: StateFlow<Boolean> = _registerSuccess.asStateFlow()

    private var usernameCheckJob: Job? = null

    /**
     * 更新用户名
     */
    fun updateUsername(value: String) {
        _username.value = value
        _usernameError.value = null
        
        // 防抖检查用户名
        usernameCheckJob?.cancel()
        if (value.isNotBlank() && value.length >= 3) {
            usernameCheckJob = viewModelScope.launch {
                delay(500) // 延迟 500ms
                checkUsernameAvailability(value)
            }
        }
        
        checkRegisterButtonState()
    }

    /**
     * 更新密码
     */
    fun updatePassword(value: String) {
        _password.value = value
        
        // 检查密码强度
        val strength = PasswordUtils.checkPasswordStrength(value)
        _passwordStrength.value = strength
        _passwordStrengthText.value = if (value.isNotEmpty()) {
            "密码强度: ${PasswordUtils.getPasswordStrengthText(strength)}"
        } else {
            ""
        }
        
        // 如果已经输入了确认密码，重新验证
        if (_confirmPassword.value.isNotEmpty()) {
            checkPasswordMatch()
        }
        
        checkRegisterButtonState()
    }

    /**
     * 更新确认密码
     */
    fun updateConfirmPassword(value: String) {
        _confirmPassword.value = value
        checkPasswordMatch()
        checkRegisterButtonState()
    }

    /**
     * 切换密码可见性
     */
    fun togglePasswordVisibility() {
        _isPasswordVisible.value = !_isPasswordVisible.value
    }

    /**
     * 切换确认密码可见性
     */
    fun toggleConfirmPasswordVisibility() {
        _isConfirmPasswordVisible.value = !_isConfirmPasswordVisible.value
    }

    /**
     * 检查用户名可用性（实时校验）
     */
    private fun checkUsernameAvailability(username: String) {
        viewModelScope.launch {
            _isUsernameChecking.value = true
            
            try {
                val result = userRepository.isUsernameExists(username)
                result.onSuccess { exists ->
                    _usernameError.value = if (exists) {
                        "用户名已存在"
                    } else {
                        null
                    }
                }.onFailure {
                    _usernameError.value = null
                }
            } finally {
                _isUsernameChecking.value = false
            }
        }
    }

    /**
     * 检查密码一致性
     */
    private fun checkPasswordMatch() {
        _confirmPasswordError.value = if (_confirmPassword.value.isNotEmpty() && 
                                         _password.value != _confirmPassword.value) {
            "两次密码输入不一致"
        } else {
            null
        }
    }

    /**
     * 检查注册按钮状态
     */
    private fun checkRegisterButtonState() {
        _isRegisterButtonEnabled.value = _username.value.isNotBlank() &&
                                         _username.value.length >= 3 &&
                                         _usernameError.value == null &&
                                         _password.value.length >= 6 &&
                                         _confirmPassword.value.isNotEmpty() &&
                                         _password.value == _confirmPassword.value
    }

    /**
     * 注册
     */
    fun register() {
        val usernameValue = _username.value.trim()
        val passwordValue = _password.value
        val confirmPasswordValue = _confirmPassword.value

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

        if (passwordValue != confirmPasswordValue) {
            launchOnUI { showError("两次密码输入不一致") }
            return
        }

        // 检查密码强度
        val strength = PasswordUtils.checkPasswordStrength(passwordValue)
        if (strength == 0) {
            launchOnUI { showError("密码强度太弱，建议包含数字、字母和特殊字符") }
            return
        }

        // 执行注册
        executeWithLoading(
            block = {
                userRepository.register(usernameValue, passwordValue)
            },
            onSuccess = { user ->
                // 保存登录信息到 DataStore
                viewModelScope.launch {
                    preferencesManager.saveLoginInfo(
                        username = user.username,
                        autoLogin = true
                    )
                    
                    // Mock 模式：自动注入虚拟设备
                    if (BuildConfig.IS_MOCK_MODE) {
                        mockInitializer.onUserRegistered(user.username)
                    }
                    
                    _registerSuccess.value = true
                    showMessage("注册成功")
                }
            },
            onError = { error ->
                launchOnUI { showError(error) }
            }
        )
    }
}

