package com.example.qurannexus.features.statistics.models

import java.util.Date

data class WeekDetails(
    val startDate: Date,
    val endDate: Date,
    val totalMinutes: Int,
    val averageMinutes: Double,
    val daysRecited: Int,
    val dailyRecitations: List<DailyRecitation>
)
