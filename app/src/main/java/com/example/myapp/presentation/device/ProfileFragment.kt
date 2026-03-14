package com.example.myapp.presentation.device

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.myapp.BuildConfig
import com.example.myapp.databinding.FragmentProfileBinding
import com.example.myapp.mock.MockTestHelper
import com.example.myapp.presentation.auth.LoginActivity
import com.example.myapp.presentation.base.BaseFragment
import com.example.myapp.presentation.main.MainActivity
import com.example.myapp.util.WorkManagerScheduler
import com.example.myapp.util.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : BaseFragment<FragmentProfileBinding, ProfileViewModel>() {

    @Inject
    lateinit var mockTestHelper: MockTestHelper

    override fun createViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentProfileBinding {
        return FragmentProfileBinding.inflate(inflater, container, false)
    }

    override fun getViewModelClass(): Class<ProfileViewModel> {
        return ProfileViewModel::class.java
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        observeData()

        if (BuildConfig.IS_MOCK_MODE) {
            setupTestEntry()
        }
        viewModel.loadUserInfo()
    }

    private fun setupTestEntry() {
        var clickCount = 0
        binding.tvVersion.setOnClickListener {
            clickCount++
            if (clickCount >= 5) {
                clickCount = 0
                mockTestHelper.showTestMenu(requireContext()) {
                    (activity as? MainActivity)?.refreshAllFragments()
                }
            }
        }
    }

    private fun initView() {
        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            binding.tvVersion.text = "版本 v${packageInfo.versionName}"
        } catch (e: Exception) {
            binding.tvVersion.text = "版本 v1.0.0"
        }

        binding.switchAtHome.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAtHomeMode(isChecked)
            val mode = if (isChecked) "在家模式" else "外出模式"
            requireContext().showToast("已切换到$mode")
        }

        binding.btnLogout.setOnClickListener { showLogoutDialog() }

        // 【新增极客暗门】：在开发模式下，长按用户名即可强制触发一次 AI 习惯提取
        binding.tvUsername.setOnLongClickListener {
            if (BuildConfig.DEBUG) {
                WorkManagerScheduler.runHabitExtractionNow(requireContext())
                requireContext().showToast("【测试】已触发后台 AI 习惯算法聚类")
            }
            true
        }
    }

    private fun observeData() {
        viewModel.username.observe(viewLifecycleOwner) { username ->
            binding.tvUsername.text = username
        }

        viewModel.isAtHome.observe(viewLifecycleOwner) { isAtHome ->
            binding.switchAtHome.isChecked = isAtHome
        }

        viewModel.logoutSuccess.observe(viewLifecycleOwner) { success ->
            if (success) navigateToLogin()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                if (error.isNotEmpty()) requireContext().showToast(error)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.message.collect { message ->
                if (message.isNotEmpty()) requireContext().showToast(message)
            }
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("退出登录")
            .setMessage("确定要退出登录吗？")
            .setPositiveButton("确定") { _, _ -> viewModel.logout() }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun navigateToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    companion object {
        fun newInstance() = ProfileFragment()
    }
}