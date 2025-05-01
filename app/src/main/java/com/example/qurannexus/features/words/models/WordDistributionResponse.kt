package com.example.qurannexus.features.words.models

import com.example.qurannexus.features.bookmark.models.FirstOccurrence
import com.google.gson.annotations.SerializedName

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

// Request class for word first occurrence
data class WordFirstOccurrenceRequest(
    @SerializedName("word_text")
    val wordText: String
)

// Response class for word first occurrence
data class WordFirstOccurrenceResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("data")
    val data: FirstOccurrence
)