package com.example.qurannexus.features.quiz.models

import com.google.gson.annotations.SerializedName

data class QuizResponse(
    val message: String,
    val quiz: QuizProgress
)

data class QuizProgress(
    val surah_id: String?,
    val current_ayah_index: String,
    val current_question_id: Int,
    val correct_answers: Int,
    val wrong_answers: Int,
    // It's a Map where the key is the batch number (as a String)
    @SerializedName("answers") val answers: Map<String, List<QuizAnswer>>?,
    // Also model the batch_scores
    @SerializedName("batch_scores") val batchScores: Map<String, BatchScoreDto>?,
    val start_time: String?,
    val end_time: String?,
    val status: String
)

data class QuizAnswer(
    val ayah_key: String,
    val question_id: Int,
    val selected_answer: String,
    val is_correct: Boolean
)