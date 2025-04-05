package com.example.weatherapp.apiservices

import com.example.weatherapp.models.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiServices {

    @GET("weather")
    suspend fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") appId: String,
        @Query("q") cityName: String
    ): ApiResponse
}