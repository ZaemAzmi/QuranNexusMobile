package com.example.qurannexus.features.bookmark.interfaces

import com.example.qurannexus.features.bookmark.models.BookmarksResponse
import retrofit2.Response
import retrofit2.http.GET

interface BookmarkService {
    @GET("mobile/bookmarks")
    suspend fun getBookmarks(): Response<BookmarksResponse>
}
