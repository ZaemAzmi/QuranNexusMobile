package com.example.qurannexus.features.recitation.models

import com.google.gson.annotations.SerializedName

class PageVerseResponse {
    @SerializedName("data")
    val data: Data? = null

    inner class Data {
        @SerializedName("Id")
        var id: String? = null
        @SerializedName("Surah Id")
        var surahId: String? = null
        @SerializedName("Ayah Index")
        var ayahIndex: String? = null
        @SerializedName("Ayah Key")
        var ayahKey: String? = null
        @SerializedName("Ayahs")
        var ayahs: List<Ayah>? = null
    }
}
