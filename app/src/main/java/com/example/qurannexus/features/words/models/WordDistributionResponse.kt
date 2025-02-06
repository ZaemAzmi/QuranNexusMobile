package com.example.qurannexus.features.words.models

data class WordDistributionResponse(
    val status: String,
    val data: WordDistributionData
)

data class WordDistributionData(
    val word_text: String,
    val total_occurrences: Int,
    val juz_distribution: Map<String, Int>
)

data class WordsChaptersDistributionResponse(
    val status: String,
    val data: WordsChaptersDistributionData
)

data class WordsChaptersDistributionData(
    val chapters: Map<String, Int> // Just chapter_id -> total_occurrences
)