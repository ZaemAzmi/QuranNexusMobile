package com.example.qurannexus.core.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ReadingTracker.kt
object ReadingTracker {
    private const val MINIMUM_READING_DURATION = 60 // 1 minute in seconds

    fun isValidReadingDuration(startTime: Long): Boolean {
        val duration = (System.currentTimeMillis() - startTime) / 1000
        return duration >= MINIMUM_READING_DURATION
    }

    fun formatStartTime(startTime: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date(startTime))
    }
}