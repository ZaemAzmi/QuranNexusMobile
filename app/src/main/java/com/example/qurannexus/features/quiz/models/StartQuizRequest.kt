package com.example.qurannexus.features.quiz.models

data class StartQuizRequest(
    val surah_id: String
)

data class SubmitAnswerRequest(
    val surah_id: String,
    val ayah_key: String,
    val question_id: Int,
    val selected_answer: String,
    val correct_answer: String
)