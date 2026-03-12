package com.example.myapp.ui.habit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapp.databinding.FragmentHabitBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 习惯列表 Fragment
 */
@AndroidEntryPoint
class HabitFragment : Fragment() {

    private var _binding: FragmentHabitBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HabitViewModel by viewModels()
    private lateinit var habitAdapter: HabitAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHabitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeRefresh()
        setupFab()
        observeViewModel()
    }
    
    override fun onResume() {
        super.onResume()
        // 每次返回时刷新数据
        viewModel.loadHabits()
    }

    private fun setupRecyclerView() {
        habitAdapter = HabitAdapter(
            onItemClick = { habit ->
                // 跳转到习惯详情页
                navigateToHabitDetail(habit.id)
            },
            onSwitchChanged = { habit, isEnabled ->
                viewModel.updateHabitEnabled(habit, isEnabled)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = habitAdapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadHabits()
        }
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            // 跳转到规则编辑器
            navigateToRuleEditor()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.habits.collect { habits ->
                habitAdapter.submitList(habits)
                updateEmptyView(habits.isEmpty())
            }
        }

        // 观察加载状态（强制 1 秒超时）
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                if (isLoading) {
                    binding.swipeRefresh.isRefreshing = true
                    
                    // 启动超时保护：1 秒后强制停止刷新
                    launch {
                        kotlinx.coroutines.delay(1000)
                        if (binding.swipeRefresh.isRefreshing) {
                            binding.swipeRefresh.isRefreshing = false
                            Timber.w("[HabitFragment] 刷新超时，强制停止")
                        }
                    }
                } else {
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        binding.emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun navigateToHabitDetail(habitId: Long) {
        // TODO: 实现导航到详情页
    }

    private fun navigateToRuleEditor() {
        // TODO: 实现导航到规则编辑器
    }
    
    /**
     * 刷新数据（公开方法，供 MainActivity 调用）
     */
    fun refreshData() {
        viewModel.loadHabits()
        Timber.d("[HabitFragment] Data refreshed")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}




