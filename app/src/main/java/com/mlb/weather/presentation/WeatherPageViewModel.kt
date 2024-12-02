package com.mlb.weather.presentation

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mlb.weather.data.Cache
import com.mlb.weather.data.OpenWeatherApi
import com.mlb.weather.models.City
import com.mlb.weather.models.WeatherItem
import com.mlb.weather.models.WeatherReport
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherPageViewModel(cache: Cache) : ViewModel() {
    private val cache = cache
    private val baseUrl = "https://api.openweathermap.org/data/2.5/"
    private val apiKey = "ea8f48805c0549b80e1f74e9c15f465e"
    private val _weatherData = mutableStateOf<List<WeatherItem>?>(null)
    private val weatherData: State<List<WeatherItem>?> = _weatherData
    private val _city = MutableLiveData<City?>()
    val city: LiveData<City?> = _city
    private val _zipCode = mutableStateOf<String?>(null)
    val zipCode: State<String?> = _zipCode

    fun getWeatherData(): List<WeatherItem>? {
        return weatherData.value
    }

    fun setZipCode(zipCode: String) {
        _zipCode.value = zipCode
    }

    fun getZipCode(): String? {
        return zipCode.value
    }

    fun callWeatherApi(zipCode: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(OpenWeatherApi::class.java)
        val call = api.getForecastByZip(zip = "$zipCode,us", apiKey = apiKey)
        // Clear old weather data for now. For future growth, will have this under a conditional that checks for network access
        cache.clearCache()
        if (cache.getWeather(zipCode) != null) {
            _weatherData.value = listOf(cache.getWeather(zipCode)!!)
            _city.value = cache.getCity(zipCode)
        } else {
            call.enqueue(object : Callback<WeatherReport> {
                override fun onResponse(
                    call: Call<WeatherReport>,
                    response: Response<WeatherReport>
                ) {
                    if (response.isSuccessful) {
                        val weatherResponse = response.body()
                        _weatherData.value = weatherResponse?.list
                        _city.value = weatherResponse?.city
                        cache.saveWeather(city.value?.name, weatherResponse?.list)
                        cache.saveCity(zipCode, weatherResponse?.city)
                        println("Weather data fetched successfully")
                    } else {
                        println("Error: ${response.code()} - ${response.message()}")
                    }
                }

                override fun onFailure(call: Call<WeatherReport>, t: Throwable) {
                    println("Failed to fetch weather data: ${t.message}")
                }
            })
        }
    }
}