package com.example.myapp.ui.rule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.myapp.databinding.FragmentStepActionBinding

/**
 * 步骤2: 选择动作
 */
class StepActionFragment : Fragment() {

    private var _binding: FragmentStepActionBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: RuleEditorViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStepActionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRadioButtons()
        setupCustomAction()
    }

    private fun setupRadioButtons() {
        binding.radioGroupActions.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                binding.radioTurnOn.id -> {
                    viewModel.setSelectedAction("turn_on")
                    binding.tilCustomAction.visibility = View.GONE
                }
                binding.radioTurnOff.id -> {
                    viewModel.setSelectedAction("turn_off")
                    binding.tilCustomAction.visibility = View.GONE
                }
                binding.radioCustom.id -> {
                    binding.tilCustomAction.visibility = View.VISIBLE
                    val customAction = binding.etCustomAction.text.toString()
                    if (customAction.isNotEmpty()) {
                        viewModel.setSelectedAction(customAction)
                    }
                }
            }
        }
    }

    private fun setupCustomAction() {
        binding.etCustomAction.addTextChangedListener { text ->
            if (binding.radioCustom.isChecked) {
                viewModel.setSelectedAction(text.toString())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

















