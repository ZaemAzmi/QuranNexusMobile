package com.example.qurannexus.features.words.models

data class WordOccurrenceResponse(
    val status: String,
    val data: WordOccurrenceData
)
data class WordOccurrenceData(
    val words: List<WordOccurrence>,
    val pagination: PaginationInfo
)


data class WordOccurrence(
    val word_id: String,
    val word_text: String,
    val translation: String,
    val transliteration: String?,
    val chapter_id: String,
    val verse_number: String,
    val verse_text: String?,
    val ayah_key: String,
    val juz_number: String,
    val position: Int,
    val audio_url: String?
)

data class WordDetails(
    val word_id: String,
    val word_text: String,
    val translation: String,
    val transliteration: String,
    val total_occurrences: Int,
    val first_occurrence: FirstWordOccurrence,
    val juz_distribution: Map<String, Int>
)

data class FirstWordOccurrence(
    val surah_name: String,
    val ayah_key: String,
    val chapter_id: String,
    val verse_number: String
)