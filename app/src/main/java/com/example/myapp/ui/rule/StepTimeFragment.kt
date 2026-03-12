package com.example.myapp.ui.rule

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.myapp.databinding.FragmentStepTimeBinding
import java.util.*

/**
 * 步骤3: 设置时间
 */
class StepTimeFragment : Fragment() {

    private var _binding: FragmentStepTimeBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: RuleEditorViewModel by activityViewModels()
    
    private var startTime = "18:00"
    private var endTime = "20:00"
    private val selectedWeekdays = mutableSetOf<Int>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStepTimeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTimePickers()
        setupWeekdayChips()
        
        // 设置默认值
        viewModel.setTimeWindow(startTime, endTime)
    }

    private fun setupTimePickers() {
        binding.tvStartTime.setOnClickListener {
            showTimePicker(startTime) { time ->
                startTime = time
                binding.tvStartTime.text = time
                viewModel.setTimeWindow(startTime, endTime)
            }
        }

        binding.tvEndTime.setOnClickListener {
            showTimePicker(endTime) { time ->
                endTime = time
                binding.tvEndTime.text = time
                viewModel.setTimeWindow(startTime, endTime)
            }
        }
    }

    private fun showTimePicker(currentTime: String, onTimeSet: (String) -> Unit) {
        val parts = currentTime.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()

        TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                val time = String.format("%02d:%02d", selectedHour, selectedMinute)
                onTimeSet(time)
            },
            hour,
            minute,
            true
        ).show()
    }

    private fun setupWeekdayChips() {
        val chips = listOf(
            binding.chipMonday to 1,
            binding.chipTuesday to 2,
            binding.chipWednesday to 3,
            binding.chipThursday to 4,
            binding.chipFriday to 5,
            binding.chipSaturday to 6,
            binding.chipSunday to 7
        )

        chips.forEach { (chip, weekday) ->
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedWeekdays.add(weekday)
                    binding.chipEveryday.isChecked = false
                } else {
                    selectedWeekdays.remove(weekday)
                }
                viewModel.setSelectedWeekdays(selectedWeekdays)
            }
        }

        binding.chipEveryday.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // 取消所有单独的星期选择
                chips.forEach { (chip, _) ->
                    chip.isChecked = false
                }
                selectedWeekdays.clear()
                viewModel.setSelectedWeekdays(emptySet())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

















