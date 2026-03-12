package com.example.myapp.data.model

import com.google.gson.annotations.SerializedName

/**
 * 通用 API 响应模型
 */
data class ApiResponse<T>(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: T?
) {
    fun isSuccess(): Boolean = code == 200
}

/**
 * 分页数据模型
 */
data class PageData<T>(
    @SerializedName("list")
    val list: List<T>,
    
    @SerializedName("total")
    val total: Int,
    
    @SerializedName("page")
    val page: Int,
    
    @SerializedName("pageSize")
    val pageSize: Int
)

