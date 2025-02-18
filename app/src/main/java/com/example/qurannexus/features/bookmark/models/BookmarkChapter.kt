package com.example.qurannexus.features.bookmark.models

import com.google.gson.annotations.SerializedName

data class BookmarkChapter(
    @SerializedName("item_properties")
    val itemProperties: ChapterProperties,
    val notes: String = "",
    @SerializedName("created_at")
    val createdAt: String
) {
    data class ChapterProperties(
        @SerializedName("chapter_id")
        val chapterId: String,
        @SerializedName("chapter_number")
        val chapterNumber: Int,
        @SerializedName("chapter_title")
        val chapterTitle: String,
        @SerializedName("chapter_info")
        val chapterInfo: String,
        @SerializedName("arabic_title")
        val arabicTitle: String,
        @SerializedName("verse_count")
        val verseCount: Int,
        @SerializedName("translation_name")
        val translationName: String
    )
}