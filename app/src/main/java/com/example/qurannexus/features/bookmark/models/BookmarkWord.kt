package com.example.qurannexus.features.bookmark.models

data class BookmarkWord(
    val word_text: String,
    val translation: String,
    val transliteration: String,
    val total_occurrences: Int,
    val first_occurrence: FirstOccurrence,
    val bookmark_date: String
)