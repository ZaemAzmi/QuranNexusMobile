package com.example.qurannexus.core.interfaces

import com.example.qurannexus.features.bookmark.models.BookmarkRequest
import com.example.qurannexus.features.bookmark.models.BookmarksResponse
import com.example.qurannexus.features.home.models.WordDetailsResponse
import com.example.qurannexus.features.recitation.models.AyahRecitationModel
import com.example.qurannexus.features.recitation.models.PageVerseResponse
import com.example.qurannexus.features.recitation.models.SurahListResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
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

    @GET("/api/v1/bookmark/{id}")
    fun getBookmarks(@Path("id") bookmarkId: Int): Call<BookmarksResponse?>?
    @POST("/api/v1/bookmark")
    fun addBookmark(@Body bookmarkRequest: BookmarkRequest?): Call<BookmarksResponse?>?

    @DELETE("/api/v1/bookmark/{id}")
    fun removeBookmark(@Path("id") bookmarkId: Int): Call<BookmarksResponse?>?
}