package com.example.weatherapp

import retrofit2.http.GET
import retrofit2.http.Query

interface ApiServices {

    @GET("weather")
    suspend fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") appid: String,
    ): ApiResponse
}