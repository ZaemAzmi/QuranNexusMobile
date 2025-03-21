package com.example.qurannexus.features.home.models

import com.google.gson.annotations.SerializedName

data class WordDetailsResponse(
    @SerializedName("data") val data: WordDetails
)
data class WordDetails(
    @SerializedName("Id") val id: String = "",
    @SerializedName("Surah Id") val surahId: String = "",
    @SerializedName("Ayah Index") val ayahIndex: String = "",
    @SerializedName("Word Index") val wordIndex: String = "",
    @SerializedName("Ayah Key") val ayahKey: String = "",
    @SerializedName("Word Key") val wordKey: String = "",
    @SerializedName("Audio Url") val audioUrl: String = "",
    @SerializedName("Page Id") val pageId: String = "",
    @SerializedName("Line Number") val lineNumber: Int = 0,
    @SerializedName("Text") val text: String = "",
    @SerializedName("Translation") val translation: String = "",
    @SerializedName("Transliteration") val transliteration: String = ""
)
