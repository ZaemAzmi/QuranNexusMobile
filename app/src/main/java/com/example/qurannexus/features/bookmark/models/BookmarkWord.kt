package com.example.qurannexus.features.bookmark.models

import com.google.gson.annotations.SerializedName

data class BookmarkWord(
    @SerializedName("item_properties")
    val itemProperties: WordProperties,
    val notes: String = "",
    @SerializedName("created_at")
    val createdAt: String
) {
    data class WordProperties(
        @SerializedName("word_statistics_id") // New field
        val wordStatisticsId: String?, // Can be nullable if not found in backend statistics

        @SerializedName("word_text")
        val wordText: String,

        val translation: String?, // Make nullable to handle cases where it might not be sent/available
        val transliteration: String?, // Make nullable

        @SerializedName("total_occurrences")
        val totalOccurrences: Int? // Make nullable
        // Removed: val firstOccurrence: FirstOccurrence
    )
}
//data class FirstOccurrence(
//    @SerializedName("word_key")
//    val wordKey: String,
//    @SerializedName("chapter_id")
//    val chapterId: String,
//    @SerializedName("verse_number")
//    val verseNumber: String,
//    @SerializedName("surah_name")
//    val surahName: String,
//    @SerializedName("page_id")
//    val pageId: String,
//    @SerializedName("juz_id")
//    val juzId: String,
//    @SerializedName("verse_text")
//    val verseText: String,
//    @SerializedName("audio_url")
//    val audioUrl: String?
//)