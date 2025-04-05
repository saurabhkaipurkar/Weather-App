package com.example.weatherapp

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.weatherapp.apiservices.ApiServices
import com.example.weatherapp.apiservices.RetrofitInstance
import com.example.weatherapp.databinding.ActivityMainBinding
import com.example.weatherapp.models.ApiResponse
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val APIKEY = "8141f502ab11cace94aa0f8d5ab1c7ca"
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var lat: Double = 0.0
    private var lon: Double = 0.0
    private var cityName: String = ""
    private lateinit var getData: ApiResponse

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,5000).build()
        locationFinder()
        requestPermission()
        setupLoadingBar()

        binding.searchBtn.setOnClickListener {
            search(getData)
        }

    }
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                getUserLocation()
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }

    private fun requestPermission(){
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissionLauncher.launch(
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }else{
            getUserLocation()
        }
    }

    private fun getUserLocation(){
        if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            fusedLocationClient.requestLocationUpdates(locationRequest,locationCallback,mainLooper)
        }
    }

    private fun locationFinder(){
        locationCallback = object : LocationCallback() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                val lastLocation: Location? = p0.lastLocation
                if (lastLocation != null) {
                    lat = lastLocation.latitude
                    lon = lastLocation.longitude

                    lifecycleScope.launch {
                        getData = getWeatherData()
                        updateUI(getData)
                        weatherDetection(getData)
                    }
                    if(true){
                        binding.loading.visibility = GONE
                    }else{
                        binding.loading.visibility = VISIBLE
                    }
                }
            }
        }
    }
    private suspend fun getWeatherData(): ApiResponse {
        return withContext(Dispatchers.IO) {
            RetrofitInstance.getRetrofitInstance.create(ApiServices::class.java)
                .getWeather(lat, lon, APIKEY, cityName)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    private fun updateUI(getData: ApiResponse) {
        binding.mainTemp.text = kelvinToCelsius(getData.main.temp).toString()+"°C"
        binding.maxandminTemp.text = "MAX: ${kelvinToCelsius(getData.main.temp_max)}°C"+" . "+
                                            "LOW: ${kelvinToCelsius(getData.main.temp_min)}°C"
        binding.skydispcrit.text = getData.weather[0].description.toString()
        binding.feelsLike.text = kelvinToCelsius(getData.main.feels_like).toString()+"°"
        binding.cloudsValue.text = getData.clouds.all.toString()
        binding.humidityValue.text = getData.main.humidity.toString()+"%"
        binding.windspeed.text = getData.wind.speed.toString()+" m/s"
        binding.visibilityValue.text = getData.visibility.toString()
        binding.sunriseValue.text = timeStampConvertor(getData.sys.sunrise).toString()
        binding.sunsetValue.text = timeStampConvertor(getData.sys.sunset).toString()
        binding.pressureValue.text = getData.main.pressure.toString()
        binding.timezoneValue.text = convertTimeZone(getData.timezone).toString()
        binding.cityName.text = getData.name.toString()
    }

    private fun kelvinToCelsius(kelvin: Double): Int {
        return (kelvin - 273.15).toInt()
    }
    private fun setupLoadingBar(){
        if(lat == 0.0 && lon == 0.0){
            binding.loading.visibility = VISIBLE
        }else{
            binding.loading.visibility = GONE
        }
    }

    private fun weatherDetection(getData: ApiResponse){
        val weather = when (getData.weather[0].main){
            "Clouds" -> R.drawable.cloudy_weather
            "Sunny" -> R.drawable.sunny_weather
            "Rain" -> R.drawable.heavyrain_weather
            "Snow" -> R.drawable.snowy_weather
            "Thunderstorm" -> R.drawable.thunderstorm_weather
            "LightRain" -> R.drawable.lightrain_weather
            else -> R.drawable.clear_weather
        }
        binding.main.setBackgroundResource(weather)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun timeStampConvertor(timeStamp: Long): String{
        var sourceZone = ZoneId.of("UTC")
        var targetZone = ZoneId.of("Asia/Kolkata")
        var sourceTimeStamp = Instant.ofEpochSecond(timeStamp).atZone(sourceZone)
        var targetTimeStamp = sourceTimeStamp.withZoneSameInstant(targetZone)
        return targetTimeStamp.format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun convertTimeZone(timeStamp: Long): String {
        val sourceZone = ZoneId.of("UTC")
        val targetZone = ZoneId.of("Asia/Kolkata")
        var sourceTimeZone = Instant.ofEpochSecond(timeStamp).atZone(sourceZone)
        var targetTimeZone = sourceTimeZone.withZoneSameInstant(targetZone)

        return targetTimeZone.format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun search(getData: ApiResponse) {
        cityName = binding.searchCity.text.toString()
        binding.searchCity.text.clear()
        binding.mainTemp.text =  kelvinToCelsius(getData.main.temp).toString()+"°C"
        binding.maxandminTemp.text = "MAX: ${kelvinToCelsius(getData.main.temp_max)}°C"+" . "+
                                            "LOW: ${kelvinToCelsius(getData.main.temp_min)}°C"
        binding.skydispcrit.text = getData.weather[0].description.toString()
        binding.feelsLike.text = kelvinToCelsius(getData.main.feels_like).toString()+"°"
        binding.cloudsValue.text = getData.clouds.all.toString()
        binding.humidityValue.text = getData.main.humidity.toString()+"%"
        binding.windspeed.text = getData.wind.speed.toString()+" m/s"
        binding.visibilityValue.text = getData.visibility.toString()
        binding.sunriseValue.text = timeStampConvertor(getData.sys.sunrise).toString()
        binding.sunsetValue.text = timeStampConvertor(getData.sys.sunset).toString()
        binding.pressureValue.text = getData.main.pressure.toString()
        binding.timezoneValue.text = convertTimeZone(getData.timezone).toString()
        binding.cityName.text = getData.name.toString()
    }
}