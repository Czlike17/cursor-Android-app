package com.example.myapp.presentation.main

import android.view.LayoutInflater
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.myapp.R
import com.example.myapp.databinding.ActivityMainBinding
import com.example.myapp.presentation.base.BaseActivity
import com.example.myapp.presentation.device.HomeFragment
import com.example.myapp.presentation.device.ProfileFragment
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
        // 处理返回键
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 如果不在首页，返回首页
                if (binding.bottomNavigation.selectedItemId != R.id.nav_home) {
                    binding.bottomNavigation.selectedItemId = R.id.nav_home
                } else {
                    // 在首页则最小化应用
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
                    // 【核心修复】：点击规则，直接作为快捷入口打开规则编辑器！
                    val intent = android.content.Intent(this@MainActivity, com.example.myapp.ui.rule.RuleEditorActivity::class.java)
                    startActivity(intent)
                    // 返回 false，底部的光标不移动，依然停留在当前页面，维持原有栈结构
                    false
                }
                R.id.nav_profile -> {
                    switchFragment(ProfileFragment.newInstance())
                    true
                }
                else -> false
            }
        }

        // 默认显示首页
        binding.bottomNavigation.selectedItemId = R.id.nav_home
    }

    override fun initData() {
        // 无需初始化数据
    }

    override fun observeData() {
        // 无需观察数据
    }

    /**
     * 切换 Fragment
     */
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
    
    /**
     * 刷新所有 Fragment（供测试工具调用）
     */
    fun refreshAllFragments() {
        // 刷新首页
        val homeFragment = supportFragmentManager.findFragmentByTag(HomeFragment::class.java.simpleName) as? HomeFragment
        homeFragment?.refreshData()
        
        // 刷新习惯页面
        val habitFragment = supportFragmentManager.findFragmentByTag(com.example.myapp.ui.habit.HabitFragment::class.java.simpleName) as? com.example.myapp.ui.habit.HabitFragment
        habitFragment?.refreshData()
    }
    // 在 MainActivity.kt 底部添加
    // 2. 在 MainActivity.kt 类的最底部，添加这个跳转方法
    /**
     * 【新增支持】：提供给 HabitFragment 等内部跳转详情页使用
     */
    fun navigateToFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            currentFragment?.let { hide(it) }
            val tag = fragment.javaClass.simpleName
            add(R.id.fragmentContainer, fragment, tag)
            addToBackStack(null) // 允许按返回键退回
            currentFragment = fragment
            commit()
        }
    }
}
