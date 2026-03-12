package com.example.myapp.presentation.auth

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.myapp.databinding.ActivityLoginBinding
import com.example.myapp.presentation.base.BaseActivity
import com.example.myapp.presentation.main.MainActivity
import com.example.myapp.util.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 登录页
 * 支持账号密码登录，密码明文/密文切换
 * 输入不完整时按钮置灰
 */
@AndroidEntryPoint
class LoginActivity : BaseActivity<ActivityLoginBinding, LoginViewModel>() {

    override fun createViewBinding(inflater: LayoutInflater): ActivityLoginBinding {
        return ActivityLoginBinding.inflate(inflater)
    }

    override fun getViewModelClass(): Class<LoginViewModel> {
        return LoginViewModel::class.java
    }

    override fun initView() {
        // 处理返回键
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 禁用返回键，最小化应用
                moveTaskToBack(true)
            }
        })
        
        // 用户名输入监听
        binding.etUsername.addTextChangedListener { text ->
            viewModel.updateUsername(text?.toString() ?: "")
        }

        // 密码输入监听
        binding.etPassword.addTextChangedListener { text ->
            viewModel.updatePassword(text?.toString() ?: "")
        }

        // 记住密码复选框（默认已选中）
        binding.cbRememberPassword.isChecked = true

        // 登录按钮
        binding.btnLogin.setOnClickListener {
            viewModel.login()
        }

        // 注册按钮
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    override fun initData() {
        // 无需初始化数据
    }

    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 观察登录按钮状态
                launch {
                    viewModel.isLoginButtonEnabled.collect { enabled ->
                        binding.btnLogin.isEnabled = enabled
                    }
                }

                // 观察加载状态
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        if (isLoading) {
                            showLoading("登录中...")
                        } else {
                            hideLoading()
                        }
                    }
                }

                // 观察错误信息
                launch {
                    viewModel.error.collect { error ->
                        if (error.isNotEmpty()) {
                            showToast(error)
                        }
                    }
                }

                // 观察成功消息
                launch {
                    viewModel.message.collect { message ->
                        if (message.isNotEmpty()) {
                            showToast(message)
                        }
                    }
                }

                // 观察登录成功
                launch {
                    viewModel.loginSuccess.collect { success ->
                        if (success) {
                            navigateToMain()
                        }
                    }
                }
            }
        }
    }

    /**
     * 跳转到主页
     */
    private fun navigateToMain() {
        lifecycleScope.launch {
            val username = viewModel.username.value
            val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
                putExtra("username", username)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}

