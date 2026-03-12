package com.example.myapp.ui.rule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapp.data.local.entity.Device
import com.example.myapp.databinding.FragmentStepDeviceBinding
import kotlinx.coroutines.launch

/**
 * 步骤1: 选择设备
 */
class StepDeviceFragment : Fragment() {

    private var _binding: FragmentStepDeviceBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: RuleEditorViewModel by activityViewModels()
    private lateinit var deviceAdapter: DeviceSelectionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStepDeviceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        deviceAdapter = DeviceSelectionAdapter { device ->
            viewModel.setSelectedDevice(device)
        }

        binding.recyclerViewDevices.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = deviceAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.devices.collect { devices ->
                deviceAdapter.submitList(devices)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedDevice.collect { device ->
                deviceAdapter.setSelectedDevice(device)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

















