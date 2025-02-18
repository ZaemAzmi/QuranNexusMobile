package com.example.qurannexus.features.bookmark.models

import com.google.gson.annotations.SerializedName

data class BookmarkResponse(
    val status: String,
    val message: String
)
data class BookmarkRequest(
    val type: String,         // "chapter", "verse", "word", "quote", or "page"
    @SerializedName("item_properties")
    val itemProperties: Map<String, Any>,
    val notes: String? = null
)

data class RemoveBookmarkResponse(
    val status: String,
    val message: String
)

// BookmarksResponse.kt - Main response structure
data class BookmarksResponse(
    val status: String,
    @SerializedName("user_id")
    val userId: String,
    val bookmarks: BookmarkList
)

// BookmarkList.kt - Main grouped structure
data class BookmarkList(
    val chapters: List<BookmarkChapter>,
    val verses: List<BookmarkVerse>,
    val words: List<BookmarkWord>,
    val quotes: List<BookmarkQuote>,
    val pages: List<BookmarkPage>
)


