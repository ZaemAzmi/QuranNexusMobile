package com.example.qurannexus.features.quiz.models

import com.google.gson.annotations.SerializedName

data class AnswerResponse(
    val message: String,
    @SerializedName("is_correct")
    val isCorrect: Boolean,
    @SerializedName("current_question_id")
    val currentQuestionId: Int
)
