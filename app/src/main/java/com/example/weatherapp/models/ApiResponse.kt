package com.example.weatherapp.models

data class ApiResponse(
    val coord: Coord,
    val weather: List<Weather>,
    val main: Main,
    val wind: Wind,
    val name: String,
    val sys: sys,
    val clouds: Clouds,
    val visibility: Int,
    val timezone: Long
)

data class Coord (
     val lon: Double,
     val lat: Double
)

data class Weather(
    val id: Int,
    val description: String,
    val main: String,
    val icon: String
)

data class Main(
    val temp: Double,
    val feels_like: Double,
    val temp_min: Double,
    val temp_max: Double,
    val humidity: Double,
    val pressure: Double
)

data class Wind(
    val speed: Double,
    val deg: Double,
    val gust: Double?
)

data class sys(
    val country: String,
    val sunrise: Long,
    val sunset: Long
)

data class Clouds(
    val all: Int
)