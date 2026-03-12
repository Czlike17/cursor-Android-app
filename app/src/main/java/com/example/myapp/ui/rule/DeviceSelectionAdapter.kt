package com.example.myapp.ui.rule

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.data.local.entity.Device
import com.example.myapp.databinding.ItemDeviceSelectionBinding

/**
 * 设备选择适配器
 */
class DeviceSelectionAdapter(
    private val onDeviceSelected: (Device) -> Unit
) : ListAdapter<Device, DeviceSelectionAdapter.DeviceViewHolder>(DeviceDiffCallback()) {

    private var selectedDevice: Device? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemDeviceSelectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun setSelectedDevice(device: Device?) {
        val oldDevice = selectedDevice
        selectedDevice = device
        
        // 刷新旧选中项和新选中项
        oldDevice?.let { old ->
            val oldPosition = currentList.indexOfFirst { it.deviceId == old.deviceId }
            if (oldPosition != -1) notifyItemChanged(oldPosition)
        }
        
        device?.let { new ->
            val newPosition = currentList.indexOfFirst { it.deviceId == new.deviceId }
            if (newPosition != -1) notifyItemChanged(newPosition)
        }
    }

    inner class DeviceViewHolder(
        private val binding: ItemDeviceSelectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(device: Device) {
            binding.apply {
                tvDeviceName.text = device.deviceName
                tvDeviceType.text = "${device.deviceType} · ${if (device.isOnline) "在线" else "离线"}"
                
                radioButton.isChecked = device.deviceId == selectedDevice?.deviceId
                
                root.setOnClickListener {
                    onDeviceSelected(device)
                }
                
                radioButton.setOnClickListener {
                    onDeviceSelected(device)
                }
            }
        }
    }

    private class DeviceDiffCallback : DiffUtil.ItemCallback<Device>() {
        override fun areItemsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem.deviceId == newItem.deviceId
        }

        override fun areContentsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem == newItem
        }
    }
}

















