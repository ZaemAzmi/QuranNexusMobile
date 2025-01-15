package com.example.qurannexus.features.bookmark.models

data class BookmarkResponse(
    val status: String,
    val message: String
)
data class BookmarkRequest(
    val type: String,         // "chapter", "verse", "word", or "quote"
    val item_id: String,      // ID of the item to bookmark
    // Optional fields based on type
    val chapter_id: String? = null,    // For verse bookmarks
    val notes: String? = null,         // For verse bookmarks
    // Word-specific fields
    val word_text: String? = null,
    val translation: String? = null,
    val transliteration: String? = null,
    val surah_name: String? = null,
    val ayah_key: String? = null,
    // Quote-specific fields
    val title: String? = null,
    val description: String? = null,
    val source: String? = null
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
    val verses: List<BookmarkVerse>,
    val words: List<BookmarkWord>,
    val quotes: List<BookmarkQuote>
)

