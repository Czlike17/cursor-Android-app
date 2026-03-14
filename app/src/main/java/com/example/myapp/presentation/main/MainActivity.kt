package com.example.myapp.presentation.main

import android.content.Intent
import android.view.LayoutInflater
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.myapp.R
import com.example.myapp.databinding.ActivityMainBinding
import com.example.myapp.presentation.base.BaseActivity
import com.example.myapp.presentation.device.HomeFragment
import com.example.myapp.presentation.device.ProfileFragment
import com.example.myapp.ui.rule.RuleEditorActivity
import com.example.myapp.util.WorkManagerScheduler
import com.example.myapp.util.showToast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding, MainViewModel>() {

    private var currentFragment: Fragment? = null

    override fun createViewBinding(inflater: LayoutInflater): ActivityMainBinding {
        return ActivityMainBinding.inflate(inflater)
    }

    override fun getViewModelClass(): Class<MainViewModel> {
        return MainViewModel::class.java
    }

    override fun initView() {
        // 【企业级挂载】：App 启动时，静默挂载 AI 习惯提取的每日凌晨定时任务
        WorkManagerScheduler.scheduleHabitExtraction(this)

        // 处理返回键
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.bottomNavigation.selectedItemId != R.id.nav_home) {
                    binding.bottomNavigation.selectedItemId = R.id.nav_home
                } else {
                    moveTaskToBack(true)
                }
            }
        })

        // 设置底部导航栏
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    switchFragment(HomeFragment.newInstance())
                    true
                }
                R.id.nav_habit -> {
                    switchFragment(com.example.myapp.ui.habit.HabitFragment())
                    true
                }
                R.id.nav_rule -> {
                    // 【已修复】：解锁规则页入口，直接启动规则编辑器
                    val intent = Intent(this@MainActivity, RuleEditorActivity::class.java)
                    startActivity(intent)
                    false // 保持底部 Icon 焦点不变
                }
                R.id.nav_profile -> {
                    switchFragment(ProfileFragment.newInstance())
                    true
                }
                else -> false
            }
        }

        binding.bottomNavigation.selectedItemId = R.id.nav_home
    }

    override fun initData() { }

    override fun observeData() { }

    private fun switchFragment(fragment: Fragment) {
        if (currentFragment?.javaClass == fragment.javaClass) {
            return
        }

        supportFragmentManager.beginTransaction().apply {
            currentFragment?.let { hide(it) }

            val tag = fragment.javaClass.simpleName
            val existingFragment = supportFragmentManager.findFragmentByTag(tag)

            if (existingFragment != null) {
                show(existingFragment)
                currentFragment = existingFragment
            } else {
                add(R.id.fragmentContainer, fragment, tag)
                currentFragment = fragment
            }

            commit()
        }
    }

    fun refreshAllFragments() {
        val homeFragment = supportFragmentManager.findFragmentByTag(HomeFragment::class.java.simpleName) as? HomeFragment
        homeFragment?.refreshData()

        val habitFragment = supportFragmentManager.findFragmentByTag(com.example.myapp.ui.habit.HabitFragment::class.java.simpleName) as? com.example.myapp.ui.habit.HabitFragment
        habitFragment?.refreshData()
    }

    /**
     * 【补齐架构方法】：允许从内部 Fragment 跳转到详情页 Fragment
     */
    fun navigateToFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            currentFragment?.let { hide(it) }
            val tag = fragment.javaClass.simpleName
            add(R.id.fragmentContainer, fragment, tag)
            addToBackStack(null)
            currentFragment = fragment
            commit()
        }
    }
}