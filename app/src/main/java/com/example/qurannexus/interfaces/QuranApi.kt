package com.example.qurannexus.interfaces

import com.example.qurannexus.models.AyahRecitationModel
import com.example.qurannexus.models.PageVerseResponse
import com.example.qurannexus.models.SurahListResponse
import com.example.qurannexus.models.WordDetailsResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query


interface QuranApi {
    @GET("/api/v1/surahs")
    fun getAllSurahs(): Call<SurahListResponse?>?
    @GET("api/v1/chapters/{surahId}/verses")
    fun getVersesBySurah(@Path("surahId") surahId: Int?): Call<AyahRecitationModel?>?

    @GET("api/v1/pages/{page_id}")
    fun getPageVerses(
        @Path("page_id") pageId: Int,
        @Query("page_ayahs") pageAyahs: Boolean
    ): Call<PageVerseResponse?>?
    @GET("api/v1/words/{word_key}")
    fun getWordDetails(@Path("word_key") wordKey: String?): Call<WordDetailsResponse?>?
}