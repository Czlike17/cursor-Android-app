package com.example.myapp.presentation.auth

import android.content.Intent
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.myapp.databinding.ActivityRegisterBinding
import com.example.myapp.presentation.base.BaseActivity
import com.example.myapp.presentation.main.MainActivity
import com.example.myapp.util.PasswordUtils
import com.example.myapp.util.gone
import com.example.myapp.util.showToast
import com.example.myapp.util.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 注册页
 * 支持账号唯一性实时校验、密码强度提示、两次密码一致性校验
 */
@AndroidEntryPoint
class RegisterActivity : BaseActivity<ActivityRegisterBinding, RegisterViewModel>() {

    override fun createViewBinding(inflater: LayoutInflater): ActivityRegisterBinding {
        return ActivityRegisterBinding.inflate(inflater)
    }

    override fun getViewModelClass(): Class<RegisterViewModel> {
        return RegisterViewModel::class.java
    }

    override fun initView() {
        // 返回按钮
        binding.ivBack.setOnClickListener {
            finish()
        }

        // 用户名输入监听
        binding.etUsername.addTextChangedListener { text ->
            viewModel.updateUsername(text?.toString() ?: "")
        }

        // 密码输入监听
        binding.etPassword.addTextChangedListener { text ->
            viewModel.updatePassword(text?.toString() ?: "")
        }

        // 确认密码输入监听
        binding.etConfirmPassword.addTextChangedListener { text ->
            viewModel.updateConfirmPassword(text?.toString() ?: "")
        }

        // 注册按钮
        binding.btnRegister.setOnClickListener {
            viewModel.register()
        }

        // 登录按钮
        binding.tvLogin.setOnClickListener {
            finish()
        }
    }

    override fun initData() {
        // 无需初始化数据
    }

    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 观察用户名错误
                launch {
                    viewModel.usernameError.collect { error ->
                        binding.tilUsername.error = error
                    }
                }

                // 观察用户名检查状态
                launch {
                    viewModel.isUsernameChecking.collect { checking ->
                        if (checking) {
                            binding.pbUsernameCheck.visible()
                        } else {
                            binding.pbUsernameCheck.gone()
                        }
                    }
                }

                // 观察密码强度
                launch {
                    viewModel.passwordStrength.collect { strength ->
                        if (binding.etPassword.text?.isNotEmpty() == true) {
                            binding.tvPasswordStrength.visible()
                            binding.tvPasswordStrength.text = viewModel.passwordStrengthText.value
                            binding.tvPasswordStrength.setTextColor(
                                ContextCompat.getColor(
                                    this@RegisterActivity,
                                    PasswordUtils.getPasswordStrengthColor(strength)
                                )
                            )
                        } else {
                            binding.tvPasswordStrength.gone()
                        }
                    }
                }

                // 观察确认密码错误
                launch {
                    viewModel.confirmPasswordError.collect { error ->
                        binding.tilConfirmPassword.error = error
                    }
                }

                // 观察注册按钮状态
                launch {
                    viewModel.isRegisterButtonEnabled.collect { enabled ->
                        binding.btnRegister.isEnabled = enabled
                    }
                }

                // 观察加载状态
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        if (isLoading) {
                            showLoading("注册中...")
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

                // 观察注册成功
                launch {
                    viewModel.registerSuccess.collect { success ->
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
        val username = viewModel.username.value
        val intent = Intent(this@RegisterActivity, MainActivity::class.java).apply {
            putExtra("username", username)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}

