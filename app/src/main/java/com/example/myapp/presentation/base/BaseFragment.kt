package com.example.myapp.presentation.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding

/**
 * Fragment 基类
 * 提供 ViewBinding 和 ViewModel 的通用处理
 */
abstract class BaseFragment<VB : ViewBinding, VM : BaseViewModel> : Fragment() {

    private var _binding: VB? = null
    protected val binding get() = _binding!!
    
    protected lateinit var viewModel: VM

    /**
     * 创建 ViewBinding 实例
     */
    abstract fun createViewBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    /**
     * 获取 ViewModel 的 Class
     */
    abstract fun getViewModelClass(): Class<VM>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = createViewBinding(inflater, container)
        viewModel = ViewModelProvider(this)[getViewModelClass()]
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
