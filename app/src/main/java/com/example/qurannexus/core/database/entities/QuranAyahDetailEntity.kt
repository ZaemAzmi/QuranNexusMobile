// In com.example.qurannexus.core.database.entities.QuranAyahDetailEntity.kt

package com.example.qurannexus.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "quran_ayah_details",
    primaryKeys = ["surah_id", "ayah_index"], // This matches your DB's PRIMARY KEY (surah_id, ayah_index)
    indices = [Index(value = ["page_id"], name = "idx_quran_ayah_details_page_id")] // 2. Define the index
)
data class QuranAyahDetailEntity(
    // 3. Make fields nullable where the DB allows it, by adding '?'

    @ColumnInfo(name = "surah_id")
    val surahId: Int,

    @ColumnInfo(name = "ayah_index")
    val ayahIndex: Int,

    @ColumnInfo(name = "page_id")
    val pageId: Int?, // <-- MODIFIED: Nullable to match DB

    @ColumnInfo(name = "juz_id")
    val juzId: Int?, // <-- MODIFIED: Nullable to match DB

    @ColumnInfo(name = "ayah_key")
    val ayahKey: String,

    @ColumnInfo(name = "ayah_text_uthmani")
    val ayahTextUthmani: String?, // This was already correct

    @ColumnInfo(name = "words_data_json")
    val wordsDataJson: String?, // <-- MODIFIED: Nullable to match DB

    @ColumnInfo(name = "ayah_translations_json")
    val ayahTranslationsJson: String?, // <-- MODIFIED: Nullable to match DB

    @ColumnInfo(name = "ayah_audio_urls_json")
    val ayahAudioUrlsJson: String? // <-- MODIFIED: Nullable to match DB

    // 4. REMOVE the @PrimaryKey `id` field. Room will use the composite PK defined above.
    // @PrimaryKey(autoGenerate = true) val id: Int = 0,
)

data class WordData(
    val word_index: Int,
    val word_key: String, // "S:A:W"
    val text: String,
    val transliteration: String,
    val translation_en: String,
    val audio_url: String,
    val line_number : Int
)

data class AyahTranslations(
    val en: String?,
    val my: String?
)