package com.example.myapp.data.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 天气API接口
 * 抽象层 - 用于隔离真实实现和Mock实现
 */
interface IWeatherApi {
    
    /**
     * 获取实时天气
     */
    @GET("weather/now")
    suspend fun getCurrentWeather(
        @Query("location") location: String,
        @Query("key") apiKey: String
    ): Response<WeatherResponse>
    
    /**
     * 获取天气预报
     */
    @GET("weather/7d")
    suspend fun getWeatherForecast(
        @Query("location") location: String,
        @Query("key") apiKey: String
    ): Response<WeatherForecastResponse>
}

/**
 * 天气响应数据类
 */
data class WeatherResponse(
    val code: String,
    val now: WeatherNow?
)

data class WeatherNow(
    val temp: String,          // 温度
    val humidity: String,      // 湿度
    val precip: String,        // 降水量
    val windSpeed: String,     // 风速
    val text: String,          // 天气状况
    val obsTime: String        // 观测时间
)

data class WeatherForecastResponse(
    val code: String,
    val daily: List<WeatherDaily>?
)

data class WeatherDaily(
    val fxDate: String,        // 日期
    val tempMax: String,       // 最高温度
    val tempMin: String,       // 最低温度
    val textDay: String,       // 白天天气
    val textNight: String,     // 夜间天气
    val precip: String,        // 降水量
    val humidity: String       // 湿度
)

















