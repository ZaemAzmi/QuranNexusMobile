package com.example.qurannexus.features.quiz.models

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
    val answers: List<QuizAnswer>,
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