package com.example.myapp.data.remote

import com.example.myapp.data.model.ApiResponse
import retrofit2.http.*

/**
 * API 服务接口示例
 * 根据实际需求修改
 */
interface ApiService {

    // 示例：获取用户信息
    // @GET("user/info")
    // suspend fun getUserInfo(@Query("userId") userId: String): ApiResponse<UserInfo>

    // 示例：登录
    // @POST("auth/login")
    // suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>

    // 示例：获取列表数据
    // @GET("data/list")
    // suspend fun getDataList(
    //     @Query("page") page: Int,
    //     @Query("pageSize") pageSize: Int
    // ): ApiResponse<PageData<DataItem>>
}

