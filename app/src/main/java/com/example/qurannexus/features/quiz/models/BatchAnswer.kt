package com.example.qurannexus.features.quiz.models

import com.google.gson.annotations.SerializedName

data class BatchAnswer(
    val questionId: Int,
    val ayahKey: String,
    val selectedAnswer: String,
    val correctAnswer: String,
    val isCorrect: Boolean
)

data class BatchData(
    val batchNumber: Int,
    val currentQuestionNumber: Int,
    val questions: List<QuestionData>,
    val currentQuestion: QuestionData?
)

data class SubmitBatchRequest(
    val answers: List<SubmitAnswerRequest>,
    @SerializedName("batch_number")
    val batchNumber: Int
)

data class BatchAnswerResponse(
    val message: String,
    val correct_answers: Int,
    val total_questions: Int
)