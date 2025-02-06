package com.example.qurannexus.features.words.models

data class WordSearchResponse(
    val status: String,
    val data: WordSearchData
)

data class WordSearchData(
    val wordSearches: List<WordSearch>,
    val pagination: PaginationInfo
)

data class PaginationInfo(
    val current_page: Int,
    val per_page: Int,
    val total: Int,
    val total_pages: Int
)

data class WordSearch(
    val id: String,
    val text: String,
    val translation: String,
    val transliteration: String?,
    val surah_id: String?,
    val verse_id: String?,
    val position: Int?
)