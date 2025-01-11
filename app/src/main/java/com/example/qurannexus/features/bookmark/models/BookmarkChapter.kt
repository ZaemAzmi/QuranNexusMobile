package com.example.qurannexus.features.bookmark.models

data class BookmarkChapter(
    val chapterNumber: Int,
    val chapterTitle: String,      // englishName
    val chapterInfo: String,       // revelationPlace
    val arabicTitle: String,       // arabicName
    val verseCount: Int,           // numberOfVerses
    val translationName: String    // translationName
)