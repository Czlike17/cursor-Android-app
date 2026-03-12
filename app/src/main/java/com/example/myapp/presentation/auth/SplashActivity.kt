package com.example.myapp.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.myapp.databinding.ActivitySplashBinding
import com.example.myapp.presentation.base.BaseActivity
import com.example.myapp.presentation.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 启动页
 * 使用 SplashScreen API，展示 Logo 和版本号
 * 检查登录状态，自动跳转到相应页面
 */
@AndroidEntryPoint
class SplashActivity : BaseActivity<ActivitySplashBinding, SplashViewModel>() {

    private var isNavigated = false

    override fun createViewBinding(inflater: LayoutInflater): ActivitySplashBinding {
        return ActivitySplashBinding.inflate(inflater)
    }

    override fun getViewModelClass(): Class<SplashViewModel> {
        return SplashViewModel::class.java
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 安装 SplashScreen
        installSplashScreen()
        super.onCreate(savedInstanceState)
    }

    override fun initView() {
        // 设置版本号
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            binding.tvVersion.text = "v${packageInfo.versionName}"
        } catch (e: Exception) {
            binding.tvVersion.text = "v1.0.0"
        }
    }

    override fun initData() {
        // 延迟 3 秒后检查登录状态
        lifecycleScope.launch {
            delay(3000)
            if (!isNavigated) {
                viewModel.checkLoginState()
            }
        }
    }

    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationEvent.collect { event ->
                    if (!isNavigated) {
                        when (event) {
                            is SplashViewModel.NavigationEvent.ToMain -> {
                                navigateToMain(event.username)
                            }
                            is SplashViewModel.NavigationEvent.ToLogin -> {
                                navigateToLogin()
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    /**
     * 跳转到主页
     */
    private fun navigateToMain(username: String) {
        isNavigated = true
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("username", username)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    /**
     * 跳转到登录页
     */
    private fun navigateToLogin() {
        isNavigated = true
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}

