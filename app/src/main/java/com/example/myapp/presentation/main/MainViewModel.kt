package com.example.myapp.presentation.main

import com.example.myapp.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    // 在这里注入 Repository
) : BaseViewModel() {

    // 示例：加载数据
    fun loadData() {
        executeWithLoading(
            block = {
                // 执行数据加载操作
                // val result = repository.getData()
                Result.success(Unit)
            },
            onSuccess = { data ->
                // 处理成功结果
            },
            onError = { error ->
                // 处理错误
            }
        )
    }
}

