package com.example.myapp.ui.rule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.myapp.databinding.FragmentStepEnvironmentBinding
import com.google.android.material.slider.RangeSlider

/**
 * 步骤4: 设置环境条件
 */
class StepEnvironmentFragment : Fragment() {

    private var _binding: FragmentStepEnvironmentBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: RuleEditorViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStepEnvironmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTemperatureSlider()
        setupHumiditySlider()
        setupEnableSwitch()
    }

    private fun setupTemperatureSlider() {
        binding.sliderTemperature.addOnChangeListener { slider, _, _ ->
            val values = slider.values
            binding.tvTempMin.text = values[0].toInt().toString()
            binding.tvTempMax.text = values[1].toInt().toString()
            viewModel.setTemperatureRange(values[0], values[1])
        }
        
        // 设置初始值
        val initialValues = binding.sliderTemperature.values
        binding.tvTempMin.text = initialValues[0].toInt().toString()
        binding.tvTempMax.text = initialValues[1].toInt().toString()
        viewModel.setTemperatureRange(initialValues[0], initialValues[1])
    }

    private fun setupHumiditySlider() {
        binding.sliderHumidity.addOnChangeListener { slider, _, _ ->
            val values = slider.values
            binding.tvHumidityMin.text = values[0].toInt().toString()
            binding.tvHumidityMax.text = values[1].toInt().toString()
            viewModel.setHumidityRange(values[0], values[1])
        }
        
        // 设置初始值
        val initialValues = binding.sliderHumidity.values
        binding.tvHumidityMin.text = initialValues[0].toInt().toString()
        binding.tvHumidityMax.text = initialValues[1].toInt().toString()
        viewModel.setHumidityRange(initialValues[0], initialValues[1])
    }

    private fun setupEnableSwitch() {
        binding.switchEnableEnvironment.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setEnableEnvironment(isChecked)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

















