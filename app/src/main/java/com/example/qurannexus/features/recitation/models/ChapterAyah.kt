package com.example.qurannexus.features.recitation.models

import com.google.gson.annotations.SerializedName

// For chapter verses
data class ChapterAyah(
    @SerializedName("Id")
    override val id: String,
    @SerializedName("Surah Id")
    override val surahId: String,
    @SerializedName("Ayah Index")
    override val ayahIndex: String,
    @SerializedName("Ayah Key")
    override val ayahKey: String,
    @SerializedName("Page Id")
    override val pageId: String,
    @SerializedName("Juz Id")
    override val juzId: String,
    @SerializedName("Bismillah")
    override val bismillah: String?,
    @SerializedName("Arabic Text")
    override val arabicText: String,
    @SerializedName("Words")
    override val words: List<Word>,
    @SerializedName("Translations")
    override val translations: List<Translation>?,
    override var isBookmarked: Boolean = false
) : AyahInterface

// For page verses
data class PageAyah(
    @SerializedName("Id")
    override val id: String,
    @SerializedName("Surah Id")
    override val surahId: String,
    @SerializedName("Ayah Index")
    override val ayahIndex: String,
    @SerializedName("Ayah Key")
    override val ayahKey: String,
    @SerializedName("Page Id")
    override val pageId: String,
    @SerializedName("Juz Id")
    override val juzId: String,
    @SerializedName("Bismillah")
    override val bismillah: String?,
    @SerializedName("Arabic Text")
    override val arabicText: String,
    @SerializedName("Words")
    override val words: List<Word>,
    @SerializedName("Translations")
    override val translations: List<Translation>? = null,
    override var isBookmarked: Boolean = false
) : AyahInterface