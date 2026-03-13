package com.example.myapp.ui.habit

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.data.local.entity.UserHabit
import com.example.myapp.databinding.FragmentHabitBinding
import com.example.myapp.presentation.main.MainActivity
import com.example.myapp.ui.rule.RuleEditorActivity
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

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
        setupSwipeToDelete()
        setupSwipeRefresh()
        setupFab()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        habitAdapter = HabitAdapter(
            onItemClick = { habit ->
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

    private fun setupSwipeToDelete() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val habitToTrash = habitAdapter.currentList[position]

                // 执行删除逻辑
                viewModel.deleteHabit(habitToTrash)

                // 弹出原生底部提示框
                Snackbar.make(
                    binding.root,
                    "习惯/规则已删除",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.recyclerView)
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false // Flow 是实时的，下拉只重置 UI 状态
        }
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
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
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        binding.emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun navigateToHabitDetail(habitId: Long) {
        // 【已修复：引用错误】不猜测是否存在 newInstance 方法，使用原生的 Bundle 传递参数最安全
        val detailFragment = HabitDetailFragment().apply {
            arguments = Bundle().apply {
                putLong("habit_id", habitId)
            }
        }

        // 【已修复：引用错误】使用明确的 import 进行类型转换
        (requireActivity() as? MainActivity)?.navigateToFragment(detailFragment)
    }

    private fun navigateToRuleEditor() {
        // 【已修复：引用错误】使用已 import 的目标 Activity，防止路径缺失报错
        val intent = Intent(requireContext(), RuleEditorActivity::class.java)
        startActivity(intent)
    }

    fun refreshData() {
        Timber.d("[HabitFragment] Data refresh handled by StateFlow")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}