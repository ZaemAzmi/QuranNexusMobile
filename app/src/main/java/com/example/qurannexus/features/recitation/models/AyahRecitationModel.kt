package com.example.qurannexus.features.recitation.models

import com.google.gson.annotations.SerializedName

data class AyahRecitationModel(
    @SerializedName("data")
    val data: List<ChapterAyah>
)

data class PageVerseResponse(
    @SerializedName("data")
    val data: PageData? = null
) {
    data class PageData(
        @SerializedName("Id")
        val id: String,
        @SerializedName("Surah Id")
        val surahId: String,
        @SerializedName("Ayah Index")
        val ayahIndex: String,
        @SerializedName("Ayah Key")
        val ayahKey: String,
        @SerializedName("Ayahs")
        val ayahs: List<PageAyah>
    )
}

interface AyahInterface {
    val id: String
    val surahId: String
    val ayahIndex: String
    val ayahKey: String
    val pageId: String
    val juzId: String
    val bismillah: String?
    val arabicText: String
    val words: List<Word>
    val translations: List<Translation>?
    var isBookmarked: Boolean
}