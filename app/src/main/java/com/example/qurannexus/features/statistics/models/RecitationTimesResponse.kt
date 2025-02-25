package com.example.qurannexus.features.statistics.models

import com.example.qurannexus.core.interfaces.ApiResponse
import com.google.gson.annotations.SerializedName

data class RecitationTimesResponse(
    override val status: String,
    override val message: String? = null,
    @SerializedName("recitation_times")
    val recitationTimes: Map<String, Int>
) : ApiResponse

data class UpdateRecitationTimesRequest(
    @SerializedName("duration_seconds")
    val durationSeconds: Int
)