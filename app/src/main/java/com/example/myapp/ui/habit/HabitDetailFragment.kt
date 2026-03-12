package com.example.myapp.ui.habit

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.myapp.databinding.FragmentHabitDetailBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 习惯详情 Fragment
 */
@AndroidEntryPoint
class HabitDetailFragment : Fragment() {

    private var _binding: FragmentHabitDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HabitDetailViewModel by viewModels()
    private var habitId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        habitId = arguments?.getLong(ARG_HABIT_ID) ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHabitDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupCharts()
        observeViewModel()
        viewModel.loadHabitDetail(habitId)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun setupCharts() {
        setupLineChart()
        setupBarChart()
    }

    private fun setupLineChart() {
        binding.lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDrawGridBackground(false)
            setPinchZoom(true)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
            }
            
            axisRight.isEnabled = false
            legend.isEnabled = true
        }
    }

    private fun setupBarChart() {
        binding.barChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDrawGridBackground(false)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
            }
            
            axisRight.isEnabled = false
            legend.isEnabled = true
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.habit.collect { habit ->
                habit?.let {
                    binding.tvHabitName.text = it.habitName
                    binding.tvDeviceInfo.text = "设备ID: ${it.deviceId}"
                    binding.tvTimeWindow.text = "时间窗口: ${it.timeWindow}"
                    binding.tvConfidence.text = "置信度: ${(it.confidence * 100).toInt()}%"
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.timeDistribution.collect { data ->
                updateLineChart(data)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.environmentCorrelation.collect { data ->
                updateBarChart(data)
            }
        }
    }

    private fun updateLineChart(data: List<Pair<Long, Int>>) {
        if (data.isEmpty()) return

        val entries = data.mapIndexed { index, pair ->
            Entry(index.toFloat(), pair.second.toFloat())
        }

        val dataSet = LineDataSet(entries, "操作次数").apply {
            color = Color.parseColor("#6200EE")
            setCircleColor(Color.parseColor("#6200EE"))
            lineWidth = 2f
            circleRadius = 3f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        binding.lineChart.apply {
            this.data = LineData(dataSet)
            xAxis.valueFormatter = object : ValueFormatter() {
                private val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    return if (index in data.indices) {
                        dateFormat.format(Date(data[index].first))
                    } else ""
                }
            }
            invalidate()
        }
    }

    private fun updateBarChart(data: Map<String, Float>) {
        if (data.isEmpty()) return

        val entries = data.entries.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.value)
        }

        val dataSet = BarDataSet(entries, "环境因素").apply {
            color = Color.parseColor("#03DAC5")
            valueTextSize = 10f
        }

        binding.barChart.apply {
            this.data = BarData(dataSet)
            xAxis.valueFormatter = object : ValueFormatter() {
                private val labels = data.keys.toList()
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    return if (index in labels.indices) labels[index] else ""
                }
            }
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_HABIT_ID = "habit_id"

        fun newInstance(habitId: Long) = HabitDetailFragment().apply {
            arguments = Bundle().apply {
                putLong(ARG_HABIT_ID, habitId)
            }
        }
    }
}

















