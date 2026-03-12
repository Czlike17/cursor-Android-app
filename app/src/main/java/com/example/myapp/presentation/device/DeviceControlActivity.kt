package com.example.myapp.presentation.device

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapp.R
import com.example.myapp.databinding.ActivityDeviceControlBinding
import com.larswerkman.holocolorpicker.ColorPicker
import com.larswerkman.holocolorpicker.OpacityBar
import com.larswerkman.holocolorpicker.SVBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 设备控制详情页
 * 支持灯光和空调设备的控制
 */
@AndroidEntryPoint
class DeviceControlActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeviceControlBinding
    private val viewModel: DeviceControlViewModel by viewModels()
    
    private var deviceId: Long = -1
    private var deviceType: String = ""
    
    // 灯光控制状态
    private var currentColor: Int = Color.WHITE
    private var currentBrightness: Int = 50
    
    // 空调控制状态
    private var currentTemperature: Int = 26

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceControlBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 获取设备信息
        deviceId = intent.getLongExtra("device_id", -1)
        deviceType = intent.getStringExtra("device_type") ?: ""
        
        if (deviceId == -1L) {
            finish()
            return
        }
        
        setupToolbar()
        setupUI()
        observeViewModel()
        
        // 加载设备信息
        viewModel.loadDevice(deviceId)
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupUI() {
        // 根据设备类型显示对应的控制界面
        when {
            deviceType.contains("light", ignoreCase = true) || deviceType.contains("灯", ignoreCase = true) -> {
                binding.cardLightControl.visibility = View.VISIBLE
                binding.cardAcControl.visibility = View.GONE
                binding.cardSensorControl.visibility = View.GONE
                setupLightControls()
            }
            deviceType.contains("ac", ignoreCase = true) || deviceType.contains("空调", ignoreCase = true) -> {
                binding.cardLightControl.visibility = View.GONE
                binding.cardAcControl.visibility = View.VISIBLE
                binding.cardSensorControl.visibility = View.GONE
                setupAcControls()
            }
            deviceType.contains("sensor", ignoreCase = true) || deviceType.contains("传感器", ignoreCase = true) -> {
                binding.cardLightControl.visibility = View.GONE
                binding.cardAcControl.visibility = View.GONE
                binding.cardSensorControl.visibility = View.VISIBLE
                setupSensorDisplay()
            }
            else -> {
                // 默认显示灯光控制
                binding.cardLightControl.visibility = View.VISIBLE
                binding.cardAcControl.visibility = View.GONE
                binding.cardSensorControl.visibility = View.GONE
                setupLightControls()
            }
        }
    }
    
    /**
     * 设置灯光控制
     */
    private fun setupLightControls() {
        // 开关控制
        binding.toggleLightPower.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val power = when (checkedId) {
                    R.id.btnLightOn -> "on"
                    R.id.btnLightOff -> "off"
                    else -> return@addOnButtonCheckedListener
                }
                viewModel.controlLight(deviceId, power, currentBrightness, currentColor)
            }
        }
        
        // 亮度调节
        binding.seekBarBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentBrightness = progress
                binding.tvBrightnessValue.text = "$progress%"
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val power = if (binding.btnLightOn.isChecked) "on" else "off"
                viewModel.controlLight(deviceId, power, currentBrightness, currentColor)
            }
        })
        
        // 颜色选择
        binding.btnColorPicker.setOnClickListener {
            showColorPickerDialog()
        }
    }
    
    /**
     * 设置空调控制
     */
    private fun setupAcControls() {
        // 初始化NumberPicker
        binding.numberPickerTemperature.minValue = 16
        binding.numberPickerTemperature.maxValue = 30
        binding.numberPickerTemperature.value = currentTemperature
        binding.numberPickerTemperature.wrapSelectorWheel = false
        
        // 开关控制
        binding.switchAcPower.setOnCheckedChangeListener { _, isChecked ->
            val power = if (isChecked) "on" else "off"
            val mode = getSelectedAcMode()
            viewModel.controlAc(deviceId, power, mode, currentTemperature)
        }
        
        // 模式选择
        binding.radioGroupAcMode.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.radioAcCool -> "cool"
                R.id.radioAcHeat -> "heat"
                R.id.radioAcDehumidify -> "dehumidify"
                else -> "cool"
            }
            val power = if (binding.switchAcPower.isChecked) "on" else "off"
            viewModel.controlAc(deviceId, power, mode, currentTemperature)
        }
        
        // NumberPicker温度调节
        binding.numberPickerTemperature.setOnValueChangedListener { _, _, newVal ->
            currentTemperature = newVal
            val power = if (binding.switchAcPower.isChecked) "on" else "off"
            val mode = getSelectedAcMode()
            viewModel.controlAc(deviceId, power, mode, currentTemperature)
        }
    }
    
    /**
     * 获取选中的空调模式
     */
    private fun getSelectedAcMode(): String {
        return when (binding.radioGroupAcMode.checkedRadioButtonId) {
            R.id.radioAcCool -> "cool"
            R.id.radioAcHeat -> "heat"
            R.id.radioAcDehumidify -> "dehumidify"
            else -> "cool"
        }
    }
    
    /**
     * 设置温湿度传感器显示
     */
    private fun setupSensorDisplay() {
        // 传感器只读，无需设置控制逻辑
    }
    
    /**
     * 显示颜色选择对话框（使用HoloColorPicker）
     */
    private fun showColorPickerDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_color_picker, null)
        val colorPicker = dialogView.findViewById<ColorPicker>(R.id.colorPicker)
        val svBar = dialogView.findViewById<SVBar>(R.id.svBar)
        
        // 设置当前颜色
        colorPicker.color = currentColor
        colorPicker.oldCenterColor = currentColor
        
        // 连接SVBar
        if (svBar != null) {
            colorPicker.addSVBar(svBar)
        }
        
        AlertDialog.Builder(this)
            .setTitle("选择颜色")
            .setView(dialogView)
            .setPositiveButton("确定") { _, _ ->
                currentColor = colorPicker.color
                val power = if (binding.btnLightOn.isChecked) "on" else "off"
                viewModel.controlLight(deviceId, power, currentBrightness, currentColor)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 观察 ViewModel 数据
     */
    private fun observeViewModel() {
        // 观察设备信息
        lifecycleScope.launch {
            viewModel.device.collect { device ->
                device?.let {
                    binding.tvDeviceName.text = it.deviceName
                    updateDeviceStatus(it.isOnline)
                    
                    // 解析设备状态并更新UI
                    parseDeviceStatus(it.status)
                }
            }
        }
        
        // 观察控制结果
        lifecycleScope.launch {
            viewModel.controlResult.collect { result ->
                result?.let {
                    if (it.isSuccess) {
                        // 控制成功
                    } else {
                        // 控制失败，显示错误信息
                        AlertDialog.Builder(this@DeviceControlActivity)
                            .setTitle("控制失败")
                            .setMessage(it.exceptionOrNull()?.message ?: "未知错误")
                            .setPositiveButton("确定", null)
                            .show()
                    }
                }
            }
        }
    }
    
    /**
     * 更新设备在线状态
     */
    private fun updateDeviceStatus(isOnline: Boolean) {
        if (isOnline) {
            binding.tvDeviceStatus.text = "在线"
            binding.tvDeviceStatus.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_online, 0, 0, 0
            )
            binding.cardOfflineHint.visibility = View.GONE
            enableControls(true)
        } else {
            binding.tvDeviceStatus.text = "离线"
            binding.tvDeviceStatus.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_offline, 0, 0, 0
            )
            binding.cardOfflineHint.visibility = View.VISIBLE
            enableControls(false)
        }
    }
    
    /**
     * 启用/禁用控制控件
     */
    private fun enableControls(enabled: Boolean) {
        val alpha = if (enabled) 1.0f else 0.5f
        
        // 灯光控制
        binding.cardLightControl.alpha = alpha
        binding.toggleLightPower.isEnabled = enabled
        binding.seekBarBrightness.isEnabled = enabled
        binding.btnColorPicker.isEnabled = enabled
        
        // 空调控制
        binding.cardAcControl.alpha = alpha
        binding.switchAcPower.isEnabled = enabled
        binding.radioGroupAcMode.isEnabled = enabled
        binding.radioAcCool.isEnabled = enabled
        binding.radioAcHeat.isEnabled = enabled
        binding.radioAcDehumidify.isEnabled = enabled
        binding.numberPickerTemperature.isEnabled = enabled
        
        // 传感器显示（只读，不需要禁用）
        binding.cardSensorControl.alpha = if (enabled) 1.0f else 0.7f
    }
    
    /**
     * 解析设备状态JSON并更新UI
     */
    private fun parseDeviceStatus(statusJson: String) {
        if (statusJson.isBlank()) return
        
        try {
            val status = com.google.gson.Gson().fromJson(
                statusJson,
                Map::class.java
            ) as? Map<String, Any> ?: return
            
            when (deviceType.lowercase()) {
                "light", "智能灯" -> {
                    // 解析灯光状态
                    val power = status["power"] as? String
                    val brightness = (status["brightness"] as? Double)?.toInt() ?: 50
                    val color = (status["color"] as? Double)?.toInt() ?: Color.WHITE
                    
                    // 更新UI
                    if (power == "on") {
                        binding.btnLightOn.isChecked = true
                    } else {
                        binding.btnLightOff.isChecked = true
                    }
                    
                    currentBrightness = brightness
                    binding.seekBarBrightness.progress = brightness
                    binding.tvBrightnessValue.text = "$brightness%"
                    
                    currentColor = color
                }
                "ac", "air_conditioner", "空调" -> {
                    // 解析空调状态
                    val power = status["power"] as? String
                    val mode = status["mode"] as? String
                    val temperature = (status["temperature"] as? Double)?.toInt() ?: 26
                    
                    // 更新UI
                    binding.switchAcPower.isChecked = power == "on"
                    
                    when (mode) {
                        "cool" -> binding.radioAcCool.isChecked = true
                        "heat" -> binding.radioAcHeat.isChecked = true
                        "dehumidify" -> binding.radioAcDehumidify.isChecked = true
                    }
                    
                    currentTemperature = temperature
                    binding.numberPickerTemperature.value = temperature
                }
                "sensor", "传感器" -> {
                    // 解析传感器数据
                    val temperature = (status["temperature"] as? Double)?.toFloat()
                    val humidity = (status["humidity"] as? Double)?.toFloat()
                    val timestamp = (status["timestamp"] as? Double)?.toLong() ?: System.currentTimeMillis()
                    
                    // 更新温度显示
                    if (temperature != null) {
                        binding.tvSensorTemperature.text = String.format("%.1f°C", temperature)
                        binding.progressTemperature.progress = temperature.toInt().coerceIn(0, 50)
                    }
                    
                    // 更新湿度显示
                    if (humidity != null) {
                        binding.tvSensorHumidity.text = String.format("%.0f%%", humidity)
                        binding.progressHumidity.progress = humidity.toInt().coerceIn(0, 100)
                    }
                    
                    // 更新时间
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    binding.tvSensorUpdateTime.text = "更新时间：${dateFormat.format(Date(timestamp))}"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

