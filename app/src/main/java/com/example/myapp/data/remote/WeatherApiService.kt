package com.example.myapp.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 气象 API 服务接口
 * 使用和风天气 API (https://dev.qweather.com/)
 */
interface WeatherApiService {
    
    /**
     * 获取实时天气
     * @param location 位置（经纬度或城市ID）
     * @param key API密钥
     */
    @GET("v7/weather/now")
    suspend fun getCurrentWeather(
        @Query("location") location: String,
        @Query("key") key: String
    ): Response<WeatherResponse>
    
    /**
     * 获取空气质量
     * @param location 位置（经纬度或城市ID）
     * @param key API密钥
     */
    @GET("v7/air/now")
    suspend fun getAirQuality(
        @Query("location") location: String,
        @Query("key") key: String
    ): Response<AirQualityResponse>
}

/**
 * 天气响应数据
 */
data class WeatherResponse(
    val code: String,
    val updateTime: String,
    val fxLink: String,
    val now: WeatherNow,
    val refer: Refer?
)

data class WeatherNow(
    val obsTime: String,
    val temp: String,
    val feelsLike: String,
    val icon: String,
    val text: String,
    val wind360: String,
    val windDir: String,
    val windScale: String,
    val windSpeed: String,
    val humidity: String,
    val precip: String,
    val pressure: String,
    val vis: String,
    val cloud: String?,
    val dew: String?
)

/**
 * 空气质量响应数据
 */
data class AirQualityResponse(
    val code: String,
    val updateTime: String,
    val fxLink: String,
    val now: AirQualityNow,
    val refer: Refer?
)

data class AirQualityNow(
    val pubTime: String,
    val aqi: String,
    val level: String,
    val category: String,
    val primary: String,
    val pm10: String,
    val pm2p5: String,
    val no2: String,
    val so2: String,
    val co: String,
    val o3: String
)

data class Refer(
    val sources: List<String>?,
    val license: List<String>?
)

















