package com.example.qurannexus.interfaces

import com.example.qurannexus.models.SurahModel
import retrofit2.Call
import retrofit2.http.GET



interface QuranApi {
    @GET("/api/surahs")
    fun getAllSurahs(): Call<List<SurahModel?>?>?
}