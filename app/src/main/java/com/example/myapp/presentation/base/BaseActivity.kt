package com.example.myapp.presentation.base

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.example.myapp.presentation.widget.LoadingDialog
import timber.log.Timber

/**
 * Activity 基类
 * 提供 ViewBinding 和 ViewModel 的通用处理
 */
abstract class BaseActivity<VB : ViewBinding, VM : BaseViewModel> : AppCompatActivity() {

    protected lateinit var binding: VB
    protected lateinit var viewModel: VM
    
    private var loadingDialog: LoadingDialog? = null

    /**
     * 创建 ViewBinding 实例
     */
    abstract fun createViewBinding(inflater: LayoutInflater): VB

    /**
     * 获取 ViewModel 的 Class
     */
    abstract fun getViewModelClass(): Class<VM>

    /**
     * 初始化视图
     */
    abstract fun initView()

    /**
     * 初始化数据
     */
    abstract fun initData()

    /**
     * 观察数据变化
     */
    abstract fun observeData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化 ViewBinding
        binding = createViewBinding(layoutInflater)
        setContentView(binding.root)
        
        // 初始化 ViewModel
        viewModel = ViewModelProvider(this)[getViewModelClass()]
        
        Timber.d("${this::class.simpleName} created")
        
        // 初始化流程
        initView()
        observeData()
        initData()
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissLoading()
        Timber.d("${this::class.simpleName} destroyed")
    }

    /**
     * 显示加载对话框
     */
    protected open fun showLoading(message: String = "加载中...") {
        Timber.d("showLoading: $message")
        if (loadingDialog == null) {
            loadingDialog = LoadingDialog(this)
        }
        loadingDialog?.setMessage(message)
        loadingDialog?.show()
    }

    /**
     * 隐藏加载对话框
     */
    protected open fun hideLoading() {
        Timber.d("hideLoading")
        dismissLoading()
    }

    /**
     * 关闭加载对话框
     */
    private fun dismissLoading() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    /**
     * 显示错误信息
     */
    protected open fun showError(message: String) {
        Timber.e("Error: $message")
    }
}

