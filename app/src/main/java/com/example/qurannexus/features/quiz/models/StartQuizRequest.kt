package com.example.qurannexus.features.quiz.models

import com.google.gson.annotations.SerializedName

data class StartQuizRequest(
    @SerializedName("surah_id")
    val surahId: String
)
data class SubmitAnswerRequest(
    val surah_id: String,
    val ayah_key: String,
    val question_id: Int,
    val selected_answer: String,
    val correct_answer: String
)