package com.example.myapp.ui.rule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.myapp.databinding.FragmentStepSummaryBinding
import kotlinx.coroutines.launch

/**
 * 步骤5: 确认摘要
 */
class StepSummaryFragment : Fragment() {

    private var _binding: FragmentStepSummaryBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: RuleEditorViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStepSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupHabitName()
        setupEnableSwitch()
        observeViewModel()
    }

    private fun setupHabitName() {
        binding.etHabitName.addTextChangedListener { text ->
            viewModel.setHabitName(text.toString())
        }
    }

    private fun setupEnableSwitch() {
        binding.switchEnableHabit.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setIsEnabled(isChecked)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedDevice.collect { device ->
                binding.tvDeviceSummary.text = "设备: ${device?.deviceName ?: "未选择"}"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedAction.collect { action ->
                val actionText = when (action) {
                    "turn_on" -> "打开"
                    "turn_off" -> "关闭"
                    else -> action ?: "未选择"
                }
                binding.tvActionSummary.text = "动作: $actionText"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.timeWindow.collect { timeWindow ->
                if (timeWindow != null) {
                    binding.tvTimeSummary.text = "时间: ${timeWindow.first} - ${timeWindow.second}"
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedWeekdays.collect { weekdays ->
                val weekText = when {
                    weekdays.isEmpty() -> "每天"
                    weekdays.size == 7 -> "每天"
                    weekdays.size == 5 && weekdays.containsAll(listOf(1, 2, 3, 4, 5)) -> "工作日"
                    weekdays.size == 2 && weekdays.containsAll(listOf(6, 7)) -> "周末"
                    else -> weekdays.sorted().joinToString(", ") { "周$it" }
                }
                binding.tvWeekSummary.text = "周期: $weekText"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.enableEnvironment.collect { enabled ->
                if (enabled) {
                    updateEnvironmentSummary()
                } else {
                    binding.tvEnvironmentSummary.text = "环境: 未设置"
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.temperatureRange.collect { 
                if (viewModel.enableEnvironment.value) {
                    updateEnvironmentSummary()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.humidityRange.collect { 
                if (viewModel.enableEnvironment.value) {
                    updateEnvironmentSummary()
                }
            }
        }
    }

    private fun updateEnvironmentSummary() {
        val tempRange = viewModel.temperatureRange.value
        val humidityRange = viewModel.humidityRange.value
        
        val parts = mutableListOf<String>()
        
        tempRange?.let { (min, max) ->
            parts.add("温度 ${min.toInt()}-${max.toInt()}°C")
        }
        
        humidityRange?.let { (min, max) ->
            parts.add("湿度 ${min.toInt()}-${max.toInt()}%")
        }
        
        binding.tvEnvironmentSummary.text = "环境: ${parts.joinToString(", ")}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

















