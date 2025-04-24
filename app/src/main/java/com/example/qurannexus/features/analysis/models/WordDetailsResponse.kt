package com.example.qurannexus.features.analysis.models

data class WordDetailsResponse(
    val status: String,
    val data: WordDetailsData
)

data class WordDetailsData(
    val word_text: String,
    val translation: String,
    val transliteration: String?,
    val total_occurrences: Int,
    val first_occurrence: FirstOccurrenceDetails,
    val juz_distribution: Map<String, Int>
)

data class FirstOccurrenceDetails(
    val chapter_id: String,
    val verse_number: String,
    val surah_name: String?,
    val surah_name_english: String?,
    val page_id: String?,
    val juz_id: String,
    val verse_text: String?,
    val ayah_key: String,
    val audio_url: String?
)