package com.example.qurannexus.features.words.di
import android.content.Context
import com.example.qurannexus.core.interfaces.QuranApi
import com.example.qurannexus.core.network.ApiService
import com.example.qurannexus.features.words.services.WordJsonService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WordModule {

    @Provides
    fun provideWordJsonService(@ApplicationContext context: Context): WordJsonService {
        return WordJsonService(context)
    }

    @Provides
    fun provideQuranApi(): QuranApi {
        return ApiService.getQuranClient().create(QuranApi::class.java)
    }
}