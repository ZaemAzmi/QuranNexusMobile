package com.example.qurannexus.features.quiz.models

import com.google.gson.annotations.SerializedName

// Create a new file for this data class
data class QuizProgressResponse(
    val quiz: QuizProgressData?
)

data class QuizProgressData(
    @SerializedName("surah_id")
    val surahId: String,

    @SerializedName("batch_scores")
    val batchScores: Map<String, BatchScoreDto>?, // e.g., "1": { "correct": 8, "total": 10 }

    // Add other fields you might need from the progress object
    val status: String
)

data class BatchScoreDto(
    val correct: Int,
    val total: Int
)