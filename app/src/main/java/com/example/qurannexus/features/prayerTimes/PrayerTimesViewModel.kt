package com.example.qurannexus.features.prayerTimes

import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qurannexus.features.prayerTimes.models.PrayerTime
import com.example.qurannexus.features.prayerTimes.models.PrayerTimesResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response
import retrofit2.Callback
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class PrayerTimesViewModel @Inject constructor(
    private val repository: PrayerTimesRepository
) : ViewModel() {

    private val _weekdayLiveData = MutableLiveData<String>()
    val weekdayLiveData: LiveData<String> = _weekdayLiveData

    private val _dateLiveData = MutableLiveData<String>()
    val dateLiveData: LiveData<String> = _dateLiveData

    private val _nextPrayerLiveData = MutableLiveData<PrayerTime>()
    val nextPrayerLiveData: LiveData<PrayerTime> = _nextPrayerLiveData

    private val _timerLiveData = MutableLiveData<String>()
    val timerLiveData: LiveData<String> = _timerLiveData

    private val _prayerTimesLiveData = MutableLiveData<List<PrayerTime>>()
    val prayerTimesLiveData: LiveData<List<PrayerTime>> = _prayerTimesLiveData

    private val _errorLiveData = MutableLiveData<String>()
    val errorLiveData: LiveData<String> = _errorLiveData

    private var countDownTimer: CountDownTimer? = null
    fun fetchPrayerTimes(date: String, city: String, country: String) {
        viewModelScope.launch {
            try {
                Log.d("PrayerTimesViewModel", "Fetching prayer times for $city, $country on $date")
                repository.getPrayerTimes(date, city, country).enqueue(object : Callback<PrayerTimesResponse> {
                    override fun onResponse(call: Call<PrayerTimesResponse>, response: Response<PrayerTimesResponse>) {
                        if (response.isSuccessful) {
                            Log.d("PrayerTimesViewModel", "API response successful")
                            response.body()?.data?.let { data ->
                                processApiResponse(data)
                            }
                        } else {
                            Log.e("PrayerTimesViewModel", "API error: ${response.message()}")
                            _errorLiveData.postValue("Error: ${response.message()}")
                        }
                    }

                    override fun onFailure(call: Call<PrayerTimesResponse>, t: Throwable) {
                        Log.e("PrayerTimesViewModel", "API failure", t)
                        _errorLiveData.postValue("Network error: ${t.message}")
                    }
                })
            } catch (e: Exception) {
                Log.e("PrayerTimesViewModel", "Exception in fetchPrayerTimes", e)
                _errorLiveData.postValue("Error: ${e.message}")
            }
        }
    }

    private fun processApiResponse(data: PrayerTimesResponse.Data) {
        try {
            _dateLiveData.postValue(data.date?.readable)
            _weekdayLiveData.postValue(data.date?.gregorian?.weekday?.en)
            val prayerTimesList = listOf(
                PrayerTime("Fajr", data.timings?.Fajr ?: "-"),
                PrayerTime("Sunrise", data.timings?.Sunrise ?: "-"),
                PrayerTime("Dhuhr", data.timings?.Dhuhr ?: "-"),
                PrayerTime("Asr", data.timings?.Asr ?: "-"),
                PrayerTime("Maghrib", data.timings?.Maghrib ?: "-"),
                PrayerTime("Isha", data.timings?.Isha ?: "-")
            )
            _prayerTimesLiveData.postValue(prayerTimesList)

            // Start countdown immediately after getting data
            calculateNextPrayer()
        } catch (e: Exception) {
            Log.e("PrayerTimesViewModel", "Error processing API response", e)
            _errorLiveData.postValue("Error processing data: ${e.message}")
        }
    }
    fun calculateNextPrayer() {
        val prayerTimes = prayerTimesLiveData.value
        if (prayerTimes != null) {
            val currentTime = getCurrentTimeIn24H()

            val nextPrayer = prayerTimes.firstOrNull {
                convertTimeToMinutes(it.time) > currentTime
            } ?: prayerTimes.first()

            _nextPrayerLiveData.postValue(nextPrayer)
            updateCountdown(nextPrayer.time)
        }
    }

    private fun convertTimeToMinutes(time: String): Int {
        return try {
            val parts = time.split(":")
            val hours = parts[0].toInt()
            val minutes = parts[1].toInt()
            hours * 60 + minutes
        } catch (e: Exception) {
            0
        }
    }
    private fun getCurrentTimeIn24H(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }
    fun updateCountdown(nextPrayerTime: String) {
        countDownTimer?.cancel()

        val prayerTimeInMinutes = convertTimeToMinutes(nextPrayerTime)
        val currentTimeInMinutes = getCurrentTimeIn24H()

        val timeLeftMinutes = if (prayerTimeInMinutes > currentTimeInMinutes) {
            prayerTimeInMinutes - currentTimeInMinutes
        } else {
            (24 * 60 - currentTimeInMinutes) + prayerTimeInMinutes
        }

        // Convert to milliseconds and add current seconds
        val timeLeftMillis = timeLeftMinutes * 60 * 1000L -
                (Calendar.getInstance().get(Calendar.SECOND) * 1000)

        countDownTimer = object : CountDownTimer(timeLeftMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = millisUntilFinished / (1000 * 60 * 60)
                val minutes = (millisUntilFinished % (1000 * 60 * 60)) / (1000 * 60)
                val seconds = (millisUntilFinished % (1000 * 60)) / 1000

                _timerLiveData.postValue(String.format("%02d:%02d:%02d", hours, minutes, seconds))
            }

            override fun onFinish() {
                calculateNextPrayer()
            }
        }.start()
    }

    override fun onCleared() {
        super.onCleared()
        countDownTimer?.cancel()
    }
}
