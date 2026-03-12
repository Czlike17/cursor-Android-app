package com.example.myapp.ui.habit

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.data.local.entity.UserHabit
import com.example.myapp.databinding.ItemHabitBinding

/**
 * 习惯列表适配器
 */
class HabitAdapter(
    private val onItemClick: (UserHabit) -> Unit,
    private val onSwitchChanged: (UserHabit, Boolean) -> Unit
) : ListAdapter<UserHabit, HabitAdapter.HabitViewHolder>(HabitDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val binding = ItemHabitBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HabitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HabitViewHolder(
        private val binding: ItemHabitBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(habit: UserHabit) {
            binding.apply {
                tvHabitName.text = habit.habitName
                tvDeviceName.text = "设备ID: ${habit.deviceId}"
                tvTimeWindow.text = "时间: ${habit.timeWindow}"
                tvWeekType.text = "周期: ${getWeekTypeText(habit.weekType)}"
                
                val confidencePercent = (habit.confidence * 100).toInt()
                progressConfidence.progress = confidencePercent
                tvConfidence.text = "$confidencePercent%"
                
                switchEnabled.isChecked = habit.isEnabled
                switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                    onSwitchChanged(habit, isChecked)
                }
                
                root.setOnClickListener {
                    onItemClick(habit)
                }
            }
        }

        private fun getWeekTypeText(weekType: Int): String {
            return when (weekType) {
                0 -> "每天"
                1 -> "周一"
                2 -> "周二"
                3 -> "周三"
                4 -> "周四"
                5 -> "周五"
                6 -> "周六"
                7 -> "周日"
                else -> "未知"
            }
        }
    }

    private class HabitDiffCallback : DiffUtil.ItemCallback<UserHabit>() {
        override fun areItemsTheSame(oldItem: UserHabit, newItem: UserHabit): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: UserHabit, newItem: UserHabit): Boolean {
            return oldItem == newItem
        }
    }
}

















