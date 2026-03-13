package com.example.myapp.ui.rule

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.myapp.databinding.ActivityRuleEditorBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RuleEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRuleEditorBinding
    private val viewModel: RuleEditorViewModel by viewModels()

    private val steps = listOf(
        StepDeviceFragment(),
        StepActionFragment(),
        StepTimeFragment(),
        StepEnvironmentFragment(),
        StepSummaryFragment()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRuleEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 【新增】：探测编辑模式，加载回显数据并改变标题
        val habitId = intent.getLongExtra("habit_id", -1L)
        if (habitId != -1L) {
            binding.toolbar.title = "编辑规则"
            viewModel.loadHabitData(habitId)
        } else {
            binding.toolbar.title = "新建规则"
        }

        setupBackPressHandler()
        setupToolbar()
        setupViewPager()
        setupButtons()
        observeViewModel()
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isFinishing) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    return
                }

                MaterialAlertDialogBuilder(this@RuleEditorActivity)
                    .setTitle("确认退出")
                    .setMessage("确定要放弃当前编辑吗?")
                    .setPositiveButton("确定") { _, _ ->
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        })
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupViewPager() {
        binding.viewPager.apply {
            adapter = StepPagerAdapter()
            isUserInputEnabled = false // 禁止滑动,只能通过按钮切换

            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    updateButtons(position)
                    updateStepIndicator(position)
                    viewModel.setCurrentStep(position)
                }
            })
        }

        lifecycleScope.launch {
            viewModel.currentStep.collect { step ->
                if (binding.viewPager.currentItem != step) {
                    binding.viewPager.setCurrentItem(step, false)
                }
            }
        }
    }

    private fun updateStepIndicator(position: Int) {
        val stepsIndicators = listOf(
            binding.tvStep1,
            binding.tvStep2,
            binding.tvStep3,
            binding.tvStep4,
            binding.tvStep5
        )

        stepsIndicators.forEachIndexed { index, textView ->
            if (index == position) {
                textView.setTextColor(getColor(com.google.android.material.R.color.material_dynamic_primary50))
                textView.textSize = 14f
                textView.setTypeface(null, android.graphics.Typeface.BOLD)
            } else if (index < position) {
                textView.setTextColor(getColor(com.google.android.material.R.color.material_dynamic_primary70))
                textView.textSize = 12f
                textView.setTypeface(null, android.graphics.Typeface.NORMAL)
            } else {
                textView.setTextColor(getColor(com.google.android.material.R.color.material_dynamic_neutral_variant50))
                textView.textSize = 12f
                textView.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
        }
    }

    private fun setupButtons() {
        binding.btnPrevious.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            if (currentItem > 0) {
                binding.viewPager.currentItem = currentItem - 1
            }
        }

        binding.btnNext.setOnClickListener {
            val currentItem = binding.viewPager.currentItem

            if (!validateCurrentStep(currentItem)) return@setOnClickListener

            if (currentItem < steps.size - 1) {
                binding.viewPager.currentItem = currentItem + 1
            } else {
                binding.btnNext.isEnabled = false
                viewModel.saveRule()
            }
        }

        lifecycleScope.launch {
            viewModel.isSaving.collect { isSaving ->
                binding.btnNext.isEnabled = !isSaving
            }
        }
    }

    private fun updateButtons(position: Int) {
        binding.btnPrevious.visibility = if (position > 0) View.VISIBLE else View.GONE
        binding.btnNext.text = if (position == steps.size - 1) "保存" else "下一步"
    }

    private fun validateCurrentStep(position: Int): Boolean {
        val isValid = when (position) {
            0 -> viewModel.selectedDevice.value != null
            1 -> viewModel.selectedAction.value != null
            2 -> viewModel.timeWindow.value != null
            else -> true
        }

        if (!isValid) {
            MaterialAlertDialogBuilder(this)
                .setTitle("提示")
                .setMessage("请完成当前步骤的设置")
                .setPositiveButton("确定", null)
                .show()
        }

        return isValid
    }

    private fun observeViewModel() {
        viewModel.saveResult.observe(this) { result ->
            result?.let {
                it.onSuccess {
                    MaterialAlertDialogBuilder(this)
                        .setTitle("成功")
                        .setMessage("习惯规则已保存")
                        .setPositiveButton("确定") { _, _ ->
                            finish()
                        }
                        .setCancelable(false)
                        .show()
                }.onFailure { error ->
                    MaterialAlertDialogBuilder(this)
                        .setTitle("错误")
                        .setMessage("保存失败: ${error.message}")
                        .setPositiveButton("确定", null)
                        .show()
                }
            }
        }
    }

    private inner class StepPagerAdapter : FragmentStateAdapter(this) {
        override fun getItemCount(): Int = steps.size
        override fun createFragment(position: Int): Fragment = steps[position]
    }
}