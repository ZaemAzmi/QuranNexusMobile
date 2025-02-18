package com.example.qurannexus.features.bookmark.interfaces

import com.example.qurannexus.features.bookmark.models.AddRecentlyReadRequest
import com.example.qurannexus.features.bookmark.models.BookmarksResponse
import com.example.qurannexus.features.bookmark.models.ChapterWordCountsResponse
import com.example.qurannexus.features.bookmark.models.RecentlyReadResponse
import com.example.qurannexus.features.bookmark.models.SimpleResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface BookmarkApi {
    @GET("recently-read")
    fun getRecentlyRead(
        @Header("Authorization") token: String,
        @Query("type") type: String? = null
    ): Call<RecentlyReadResponse>

    @POST("recently-read")
    fun addRecentlyRead(
        @Header("Authorization") token: String,
        @Body request: AddRecentlyReadRequest
    ): Call<SimpleResponse>

    @DELETE("recently-read/{type}/{itemId}")
    fun removeRecentlyRead(
        @Header("Authorization") token: String,
        @Path("type") type: String,
        @Path("itemId") itemId: String
    ): Call<SimpleResponse>

    @GET("chapters/word-counts")
    fun getChapterWordCounts(): Call<ChapterWordCountsResponse>
}
