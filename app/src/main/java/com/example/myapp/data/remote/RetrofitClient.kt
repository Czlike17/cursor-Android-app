package com.example.myapp.data.remote

import com.example.myapp.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit 网络服务工厂 (主线业务 API)
 * * 架构更新说明：
 * 1. [安全修复] 实现了 BuildConfig.DEBUG 严格隔离，阻断生产环境 (Release) 日志泄露
 * 2. [性能优化] 引入 by lazy 懒加载，避免 App 冷启动时的无效内存占用
 */
object RetrofitClient {

    private const val BASE_URL = "https://api.example.com/"  // 主线业务 API 地址
    private const val TIMEOUT = 30L

    // 使用 lazy 延迟初始化，只有在第一次发生网络请求时才会构建 Client
    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.SECONDS)

        // 【核心安全修复】：强制判定编译环境
        // 只有在 Debug 模式下才挂载日志拦截器，绝不将明文 Token 或设备控制报文带入 Release 包的 Logcat
        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }

        builder.build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * 创建对应的 API Service
     * 维持原有 API 签名不变，保证项目中其他调用方 0 成本迁移
     */
    fun <T> createService(serviceClass: Class<T>): T {
        return retrofit.create(serviceClass)
    }
}