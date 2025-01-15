package com.example.qurannexus.features.words.models

data class WordOccurrencesResponse(
    val status: String,
    val occurrences: List<WordOccurrence>
)

data class WordOccurrence(
    val surah_name: String,
    val ayah_key: String,
    val chapter_id: String,
    val verse_number: String,
    val position: Int,
    val juz_number: Int
)

data class JuzDistribution(
    val juz_number: Int,
    val count: Int
)

data class WordDetails(
    val word_id: String,
    val word_text: String,
    val translation: String,
    val transliteration: String,
    val total_occurrences: Int,
    val first_occurrence: FirstOccurrence,
    val juz_distribution: Map<String, Int>
)

data class FirstOccurrence(
    val surah_name: String,
    val ayah_key: String,
    val chapter_id: String,
    val verse_number: String
)