package com.example.qurannexus.features.bookmark.models

data class BookmarkResponse(
    val status: String,
    val message: String
)
data class BookmarkRequest(
    val type: String,         // "chapter" or "verse"
    val item_id: String,      // chapter_id or ayah_id
    val chapter_id: String?,  // Optional, for verse bookmarks
    val notes: String? = ""
)
data class RemoveBookmarkResponse(
    val status: String,
    val message: String
)

// BookmarksResponse.kt
data class BookmarksResponse(
    val status: String,
    val user_id: String,
    val bookmarks: BookmarkList
)

data class BookmarkList(
    val chapters: List<String>,
    val verses: List<BookmarkVerse>
)


