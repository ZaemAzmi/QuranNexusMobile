package com.example.qurannexus.features.recitation.audio.models

import com.google.gson.annotations.SerializedName

data class AudioRecitationResponse(
    @SerializedName("data")
    val data: List<AudioRecitation>
)

data class AudioRecitation(
    @SerializedName("Id")
    val id: String,
    @SerializedName("Audio Info Id")
    val audioInfoId: String,
    @SerializedName("Surah Id")
    val surahId: String,
    @SerializedName("Ayah Index")
    val ayahIndex: String,
    @SerializedName("Ayah Key")
    val ayahKey: String,
    @SerializedName("Audio Url")
    val audioUrl: String
)