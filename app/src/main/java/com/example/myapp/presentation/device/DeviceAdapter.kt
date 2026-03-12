package com.example.myapp.presentation.device

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.data.local.entity.Device
import com.example.myapp.data.model.DeviceType
import com.example.myapp.databinding.ItemDeviceBinding

/**
 * 设备列表适配器
 */
class DeviceAdapter(
    private val onDeviceClick: (Device) -> Unit,
    private val onPowerToggle: (Device, Boolean) -> Unit
) : ListAdapter<Device, DeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemDeviceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeviceViewHolder(binding, onDeviceClick, onPowerToggle)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DeviceViewHolder(
        private val binding: ItemDeviceBinding,
        private val onDeviceClick: (Device) -> Unit,
        private val onPowerToggle: (Device, Boolean) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(device: Device) {
            binding.apply {
                // 设备名称
                tvDeviceName.text = device.deviceName

                // 设备类型 - 使用模糊匹配
                val deviceType = when {
                    device.deviceType.contains("light", ignoreCase = true) || 
                    device.deviceType.contains("灯", ignoreCase = true) -> DeviceType.LIGHT
                    device.deviceType.contains("ac", ignoreCase = true) || 
                    device.deviceType.contains("空调", ignoreCase = true) -> DeviceType.AIR_CONDITIONER
                    device.deviceType.contains("sensor", ignoreCase = true) || 
                    device.deviceType.contains("传感器", ignoreCase = true) -> DeviceType.SENSOR
                    device.deviceType.contains("fan", ignoreCase = true) || 
                    device.deviceType.contains("风扇", ignoreCase = true) -> DeviceType.FAN
                    device.deviceType.contains("curtain", ignoreCase = true) || 
                    device.deviceType.contains("窗帘", ignoreCase = true) -> DeviceType.CURTAIN
                    device.deviceType.contains("camera", ignoreCase = true) || 
                    device.deviceType.contains("摄像头", ignoreCase = true) -> DeviceType.CAMERA
                    device.deviceType.contains("lock", ignoreCase = true) || 
                    device.deviceType.contains("锁", ignoreCase = true) -> DeviceType.LOCK
                    else -> DeviceType.OTHER
                }
                tvDeviceType.text = deviceType.displayName

                // 在线状态
                val isOnline = device.isOnline
                tvOnlineStatus.text = if (isOnline) "在线" else "离线"
                
                // 在线状态图标颜色
                val statusColor = if (isOnline) {
                    android.R.color.holo_green_dark
                } else {
                    android.R.color.darker_gray
                }
                tvOnlineStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    android.R.drawable.presence_online, 0, 0, 0
                )
                tvOnlineStatus.compoundDrawablesRelative[0]?.setTint(
                    tvOnlineStatus.context.getColor(statusColor)
                )

                // Broker 地址和端口
                tvBroker.text = "${device.mqttBroker}:${device.mqttPort}"

                // Client ID
                tvClientId.text = "Client: ${device.clientId}"

                // 电源开关状态（从 status JSON 解析）
                val isPowerOn = try {
                    device.status.contains("\"power\":\"on\"", ignoreCase = true)
                } catch (e: Exception) {
                    false
                }
                switchPower.isChecked = isPowerOn

                // 开关监听（防止递归触发）
                switchPower.setOnCheckedChangeListener(null)
                switchPower.setOnCheckedChangeListener { _, isChecked ->
                    onPowerToggle(device, isChecked)
                }

                // 卡片点击
                root.setOnClickListener {
                    onDeviceClick(device)
                }

                // 根据设备类型设置图标
                ivDeviceIcon.setImageResource(getDeviceIcon(deviceType))
                
                // 离线设备置灰效果
                if (isOnline) {
                    root.alpha = 1.0f
                    switchPower.isEnabled = true
                } else {
                    root.alpha = 0.5f
                    switchPower.isEnabled = false
                }
            }
        }

        private fun getDeviceIcon(deviceType: DeviceType): Int {
            return when (deviceType) {
                DeviceType.LIGHT -> android.R.drawable.ic_menu_day
                DeviceType.AIR_CONDITIONER -> android.R.drawable.ic_menu_compass
                DeviceType.FAN -> android.R.drawable.ic_menu_rotate
                DeviceType.CURTAIN -> android.R.drawable.ic_menu_view
                DeviceType.SENSOR -> android.R.drawable.ic_menu_info_details
                DeviceType.SWITCH -> android.R.drawable.ic_menu_preferences
                DeviceType.CAMERA -> android.R.drawable.ic_menu_camera
                DeviceType.LOCK -> android.R.drawable.ic_lock_lock
                else -> android.R.drawable.ic_menu_manage
            }
        }
    }

    class DeviceDiffCallback : DiffUtil.ItemCallback<Device>() {
        override fun areItemsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem.deviceId == newItem.deviceId
        }

        override fun areContentsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem == newItem
        }
    }
}

