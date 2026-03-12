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
                    // 规则页面 - 暂时显示提示
                    showToast("规则功能开发中，请在习惯页面查看习惯数据")
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
}
