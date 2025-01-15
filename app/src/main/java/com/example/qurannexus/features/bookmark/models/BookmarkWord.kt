package com.example.qurannexus.features.bookmark.models

data class BookmarkWord(
    val word_id: String,
    val word_text: String,
    val translation: String,
    val transliteration: String,
    val surah_name: String,
    val ayah_key: String
)