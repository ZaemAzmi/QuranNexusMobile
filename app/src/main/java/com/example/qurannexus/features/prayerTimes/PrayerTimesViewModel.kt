package com.example.qurannexus.features.prayerTimes

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qurannexus.features.prayerTimes.models.PrayerTime
import com.example.qurannexus.features.prayerTimes.models.PrayerTimesResponse
import dagger.hilt.android.lifecycle.HiltViewModel
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

    val prayerTimesLiveData = MutableLiveData<List<PrayerTime>>()
    val nextPrayerLiveData = MutableLiveData<PrayerTime?>()
    val dateLiveData = MutableLiveData<String>()
    val weekdayLiveData = MutableLiveData<String>()
    val timerLiveData = MutableLiveData<String>()
    val errorLiveData = MutableLiveData<String>()

    fun fetchPrayerTimes(date: String, location: String, countryCode: String) {
        viewModelScope.launch {
            try {
                // Use enqueue for asynchronous call
                repository.getPrayerTimes(date, location, countryCode).enqueue(object : Callback<PrayerTimesResponse> {
                    override fun onResponse(call: Call<PrayerTimesResponse>, response: Response<PrayerTimesResponse>) {
                        if (response.isSuccessful) {
                            val body = response.body()
                            val timings = body?.data?.timings
                            val dateInfo = body?.data?.date

                            if (timings != null) {
                                val prayerTimesList = listOf(
                                    PrayerTime("Fajr", timings.Fajr ?: ""),
                                    PrayerTime("Sunrise", timings.Sunrise ?: ""),
                                    PrayerTime("Dhuhr", timings.Dhuhr ?: ""),
                                    PrayerTime("Asr", timings.Asr ?: ""),
                                    PrayerTime("Maghrib", timings.Maghrib ?: ""),
                                    PrayerTime("Isha", timings.Isha ?: ""),
                                    PrayerTime("Imsak", timings.Imsak ?: "")
                                )
                                prayerTimesLiveData.postValue(prayerTimesList)

                                dateLiveData.postValue(dateInfo?.readable)
                                weekdayLiveData.postValue(dateInfo?.gregorian?.weekday?.en)
                                calculateNextPrayer(prayerTimesList)
                            }
                        } else {
                            errorLiveData.postValue("Error: ${response.message()}")
                        }
                    }

                    override fun onFailure(call: Call<PrayerTimesResponse>, t: Throwable) {
                        errorLiveData.postValue("Failed to fetch prayer times: ${t.message}")
                    }
                })
            } catch (e: Exception) {
                errorLiveData.postValue("Failed to fetch prayer times: ${e.message}")
            }
        }
    }

    private fun calculateNextPrayer(prayerTimes: List<PrayerTime>) {
        val currentTime = getCurrentTimeIn24H()
        val nextPrayer = prayerTimes.firstOrNull {
            convertTimeToMinutes(it.time) > currentTime
        }
        nextPrayerLiveData.postValue(nextPrayer)
    }

    private fun convertTimeToMinutes(time: String): Int {
        return time.split(":").let {
            val hours = it[0].toInt()
            val minutes = it[1].toInt()
            hours * 60 + minutes
        }
    }

    private fun getCurrentTimeIn24H(): Int {
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        return currentTime.split(":").let {
            val hours = it[0].toInt()
            val minutes = it[1].toInt()
            hours * 60 + minutes
        }
    }

    fun updateCountdown(nextPrayerTime: String) {
        val prayerTimeInMinutes = convertTimeToMinutes(nextPrayerTime)
        val currentTimeInMinutes = getCurrentTimeIn24H()

        val timeLeft = if (prayerTimeInMinutes > currentTimeInMinutes) {
            prayerTimeInMinutes - currentTimeInMinutes
        } else {
            prayerTimeInMinutes + (24 * 60) - currentTimeInMinutes
        }

        val hours = timeLeft / 60
        val minutes = timeLeft % 60
        timerLiveData.postValue(String.format("%02d:%02d", hours, minutes))
    }
}
