package com.example.qurannexus.core.interfaces

import com.example.qurannexus.features.bookmark.models.BookmarkRequest
import com.example.qurannexus.features.bookmark.models.BookmarkResponse
import com.example.qurannexus.features.bookmark.models.BookmarksResponse
import com.example.qurannexus.features.bookmark.models.RemoveBookmarkResponse
import com.example.qurannexus.features.home.models.WordDetailsResponse
import com.example.qurannexus.features.recitation.models.AyahRecitationModel
import com.example.qurannexus.features.recitation.models.PageVerseResponse
import com.example.qurannexus.features.recitation.models.SurahListResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
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
        @Query("ayahs") ayahs: Boolean = true,
        @Query("words") words: Boolean = true
    ): Call<PageVerseResponse?>?
    @GET("api/v1/words/{word_key}")
    fun getWordDetails(@Path("word_key") wordKey: String?): Call<WordDetailsResponse?>?

// bookmarks
    @POST("api/v1/mobile/bookmarks")
    fun addBookmark(
    @Header("Authorization") token: String,
    @Body request: BookmarkRequest
    ): Call<BookmarkResponse>

    @DELETE("api/v1/mobile/bookmarks/{type}/{itemId}")
    fun removeBookmark(
        @Header("Authorization") token: String,
        @Path("type") type: String,
        @Path("itemId") itemId: String
    ): Call<RemoveBookmarkResponse>

    @GET("api/v1/mobile/bookmarks")
    fun getBookmarks(
        @Header("Authorization") token: String
    ): Call<BookmarksResponse>
}