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
        @SerializedName("ayah_index")
        val ayahIndex: String,
        @SerializedName("surah_id")
        val surahId: String
    )
}