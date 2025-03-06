package com.example.qurannexus.features.statistics.interfaces

import com.example.qurannexus.features.statistics.models.RecitationStreakData

interface RecitationDataReceiver {
    fun onRecitationDataReceived(data: RecitationStreakData)
    fun onEmptyState()
}