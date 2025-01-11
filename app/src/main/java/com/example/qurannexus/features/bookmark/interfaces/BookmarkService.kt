package com.example.qurannexus.features.bookmark.interfaces

import com.example.qurannexus.features.bookmark.models.BookmarksResponse
import retrofit2.Response
import retrofit2.http.GET

interface BookmarkService {
//    @POST("mobile/bookmarks")
//    suspend fun addBookmark(@Body request: BookmarkRequest): Response<BookmarkResponse>
//
//    @DELETE("mobile/bookmarks/{bookmarkId}")
//    suspend fun removeBookmark(@Path("bookmarkId") bookmarkId: String): Response<RemoveBookmarkResponse>
//
      @GET("api/v1/mobile/bookmarks")
     suspend fun getBookmarks(): Response<BookmarksResponse>
}
