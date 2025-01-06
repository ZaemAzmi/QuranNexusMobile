package com.example.qurannexus.features.bookmark.models

data class BookmarksResponse(
    val status: String,
    val user_id: String,
    val bookmarks: List<Bookmark>
)

data class Bookmark(
    val _id: String,
    val bookmarkType: String,
    val name: String? = null, // For chapters
    val tname: String? = null, // Transliteration
    val ename: String? = null, // English name
    val type: String? = null, // Meccan or Medinan
    val ayas: Int? = null, // Number of ayahs (chapters only)
    val page_id: String? = null, // For verses
    val juz_id: String? = null,
    val surah_id: String? = null,
    val ayah_index: String? = null,
    val ayah_key: String? = null,
    val bismillah: String? = null,
    val text: String? = null,
    val isVerified: Boolean? = null,
    val title: String? = null, // For daily quotes
    val description: String? = null,
    val source: String? = null
)
