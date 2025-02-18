package com.example.qurannexus.features.bookmark.models

import com.google.gson.annotations.SerializedName

data class BookmarkVerse(
    @SerializedName("item_properties")
    val itemProperties: VerseProperties,
    val notes: String = "",
    @SerializedName("created_at")
    val createdAt: String
) {
    data class VerseProperties(
        @SerializedName("verse_id")
        val verseId: String,
        @SerializedName("chapter_id")
        val chapterId: String
    )
}