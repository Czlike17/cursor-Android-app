package com.example.myapp.presentation.device

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapp.presentation.base.BaseViewModel
import com.example.myapp.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 个人中心 ViewModel
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : BaseViewModel() {

    // 用户名
    private val _username = MutableLiveData<String>()
    val username: LiveData<String> = _username

    // 在家模式
    private val _isAtHome = MutableLiveData<Boolean>()
    val isAtHome: LiveData<Boolean> = _isAtHome

    // 退出登录成功
    private val _logoutSuccess = MutableLiveData<Boolean>()
    val logoutSuccess: LiveData<Boolean> = _logoutSuccess

    /**
     * 加载用户信息
     */
    fun loadUserInfo() {
        viewModelScope.launch {
            try {
                // 获取用户名（只取第一个值，避免持续监听）
                val username = preferencesManager.getUsername().first()
                _username.value = username ?: "未知用户"

                // 获取在家模式状态（只取第一个值，避免持续监听）
                val isAtHome = preferencesManager.getAtHomeMode().first()
                _isAtHome.value = isAtHome
            } catch (e: Exception) {
                showError("加载用户信息失败")
            }
        }
    }

    /**
     * 设置在家模式
     */
    fun setAtHomeMode(isAtHome: Boolean) {
        viewModelScope.launch {
            try {
                preferencesManager.saveAtHomeMode(isAtHome)
                _isAtHome.value = isAtHome
            } catch (e: Exception) {
                showError("保存设置失败")
            }
        }
    }

    /**
     * 退出登录
     */
    fun logout() {
        viewModelScope.launch {
            try {
                setLoading(true)
                
                // 清除登录信息
                preferencesManager.clearLoginInfo()
                
                _logoutSuccess.value = true
                showMessage("已退出登录")
            } catch (e: Exception) {
                showError("退出登录失败")
            } finally {
                setLoading(false)
            }
        }
    }
}

