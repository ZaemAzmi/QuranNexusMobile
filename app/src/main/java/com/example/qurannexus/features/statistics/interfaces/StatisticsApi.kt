package com.example.qurannexus.features.statistics.interfaces

import com.example.qurannexus.features.bookmark.models.SimpleResponse
import com.example.qurannexus.features.statistics.models.RecitationStreakResponse
import com.example.qurannexus.features.statistics.models.RecitationTimesResponse
import com.example.qurannexus.features.statistics.models.UpdateRecitationTimesRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface StatisticsApi {

    @POST("recitation-times")
    fun updateRecitationTimes(
        @Header("Authorization") token: String,
        @Body request: UpdateRecitationTimesRequest
    ): Call<SimpleResponse>

    @GET("recitation-times")
    fun getRecitationTimes(
        @Header("Authorization") token: String
    ): Call<RecitationTimesResponse>

    // endpoints for streak
    @GET("recitation-streak")
    fun getRecitationStreak(
        @Header("Authorization") token: String
    ): Call<RecitationStreakResponse>

    @POST("recitation-streak")
    fun updateRecitationStreak(
        @Header("Authorization") token: String,
        @Body request: UpdateRecitationTimesRequest
    ): Call<RecitationStreakResponse>
}