package com.example.myapp.presentation.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel 基类
 * 提供通用的状态管理和错误处理
 */
abstract class BaseViewModel : ViewModel() {

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 错误信息
    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    // 成功消息
    private val _message = MutableSharedFlow<String>()
    val message: SharedFlow<String> = _message.asSharedFlow()

    /**
     * 协程异常处理器
     */
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "Coroutine exception")
        launchOnUI {
            _isLoading.value = false
            _error.emit(throwable.message ?: "Unknown error occurred")
        }
    }

    /**
     * 在主线程启动协程
     */
    protected fun launchOnUI(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(exceptionHandler) {
            block()
        }
    }

    /**
     * 执行带加载状态的操作
     */
    protected fun <T> executeWithLoading(
        block: suspend () -> Result<T>,
        onSuccess: (T) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        launchOnUI {
            _isLoading.value = true
            try {
                val result = block()
                result.onSuccess { data ->
                    onSuccess(data)
                }.onFailure { exception ->
                    val errorMsg = exception.message ?: "Operation failed"
                    _error.emit(errorMsg)
                    onError(errorMsg)
                    Timber.e(exception, "Operation failed")
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 显示成功消息
     */
    protected suspend fun showMessage(msg: String) {
        _message.emit(msg)
    }

    /**
     * 显示错误消息
     */
    protected suspend fun showError(msg: String) {
        _error.emit(msg)
    }

    /**
     * 设置加载状态
     */
    protected fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }
}

