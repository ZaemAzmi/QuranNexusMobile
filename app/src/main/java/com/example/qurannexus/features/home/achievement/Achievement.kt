package com.example.qurannexus.features.home.achievement


data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon_name: String,
    val is_unlocked: Boolean,
    val unlock_date: String?,
    val progress: Map<String, Any>? = null
)
data class AchievementResponse(
    val status: String,
    val achievements: List<Achievement>
)
data class AchievementStatusResponse(
    val status: String,
    val achievement_status: Map<String, AchievementStatus>
)
data class UnlockAchievementRequest(
    val achievement_id: String
)

data class BaseResponse(
    val status: String,
    val message: String
)

data class StreakResponse(
    val status: String,
    val is_eligible: Boolean,
    val current_streak: Int
)
data class AchievementStatus(
    val id: String,
    val status: String,
    val unlock_date: String?,
    val progress: StreakProgress? = null
)

data class StreakProgress(
    val current_streak: Int,
    val target_streak: Int
)
data class ChapterReadRequest(
    val chapter_id: String
)

data class ChapterReadResponse(
    val status: String,
    val message: String,
    val achievements: List<Achievement>?
)
