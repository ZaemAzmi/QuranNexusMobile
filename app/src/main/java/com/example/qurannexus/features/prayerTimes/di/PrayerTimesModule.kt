package com.example.qurannexus.features.prayerTimes.di

import com.example.qurannexus.core.network.ApiService
import com.example.qurannexus.features.prayerTimes.PrayerTimesRepository
import com.example.qurannexus.features.prayerTimes.PrayerTimesViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object PrayerTimesModule {

    @Provides
    fun providePrayerTimesApi(): PrayerTimesApi {
        return ApiService.getPrayerTimesClient().create(PrayerTimesApi::class.java)
    }

    @Provides
    fun providePrayerTimesRepository(apiService: PrayerTimesApi): PrayerTimesRepository {
        return PrayerTimesRepository(apiService)
    }
}
