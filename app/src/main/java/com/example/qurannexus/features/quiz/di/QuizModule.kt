package com.example.qurannexus.features.quiz.di

import com.example.qurannexus.core.interfaces.QuizApi
import com.example.qurannexus.core.network.ApiService
import com.example.qurannexus.features.quiz.repository.QuizRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object QuizModule {

    @Provides
    fun provideQuizApi(): QuizApi {
        val client = ApiService.getQuranClient()
        return client.create(QuizApi::class.java)
    }

    @Provides
    fun provideQuizRepository(api: QuizApi): QuizRepository {
        return QuizRepository(api)
    }
}