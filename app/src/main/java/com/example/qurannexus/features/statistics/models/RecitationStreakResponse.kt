package com.example.qurannexus.features.statistics.models

import com.example.qurannexus.core.interfaces.ApiResponse
import com.google.gson.annotations.SerializedName

data class RecitationStreakResponse(
    override val status: String,
    override val message: String? = null,
    @SerializedName("recitation_streak")
    val recitationStreak: Int,
    @SerializedName("last_recitation_date")
    val lastRecitationDate: String?,
    @SerializedName("streak_data")
    val streakData: RecitationStreakData
) : ApiResponse

data class RecitationStreakData(
    @SerializedName("current_streak")
    val currentStreak: Int,
    @SerializedName("longest_streak")
    val longestStreak: Int,
    @SerializedName("last_recitation_date")
    val lastRecitationDate: String?,
    @SerializedName("streak_start_date")
    val streakStartDate: String?,
    @SerializedName("streaks_history")
    val streaksHistory: List<StreakHistoryEntry>,
    @SerializedName("total_days_recited")
    val totalDaysRecited: Int,
    @SerializedName("consistency_score")
    val consistencyScore: Int,
    @SerializedName("recitation_times")
    val recitationTimes: Map<String, Int>?  // Make nullable since it might be null from API
)

data class StreakHistoryEntry(
    @SerializedName("start_date")
    val startDate: String,
    @SerializedName("end_date")
    val endDate: String,
    val length: Int
)