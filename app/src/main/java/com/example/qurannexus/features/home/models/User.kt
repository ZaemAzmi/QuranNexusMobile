package com.example.qurannexus.features.home.models

import com.example.qurannexus.features.quiz.models.QuizProgress

data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val settings: Map<String, Any?>,
    val recitationTimes: Map<String, Int>? = null,  // Changed from List<String> to Map<String, Int>
    val recitationStreak: Int? = null,
    val lastRecitationDate: String? = null,
    val recitationGoal: String? = null,
    val profilePhotoUrl: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val quiz_progress: List<QuizProgress>? = null  // Add this
)
data class UserResponse(
    val status: String,
    val data: User
)
