package com.example.qurannexus.features.prayerTimes

import com.example.qurannexus.features.prayerTimes.di.PrayerTimesApi
import com.example.qurannexus.features.prayerTimes.models.PrayerTimesResponse
import retrofit2.Call

class PrayerTimesRepository(private val apiService: PrayerTimesApi) {

    fun getPrayerTimes(date: String, location: String, countryCode: String): Call<PrayerTimesResponse> {
        return apiService.getPrayerTimes(date, location, countryCode)
    }
}
