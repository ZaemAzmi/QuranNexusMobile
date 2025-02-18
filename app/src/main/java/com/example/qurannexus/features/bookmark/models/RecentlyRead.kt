package com.example.qurannexus.features.bookmark.models

import com.example.qurannexus.core.interfaces.ApiResponse
import com.google.gson.annotations.SerializedName

data class RecentlyRead(
    @SerializedName("item_id")
    val itemId: String,
    @SerializedName("read_at")
    val readAt: String
)
data class RecentlyReadResponse(
    override val status: String,
    override val message: String? = null,
    @SerializedName("recently_read")
    val recentlyRead: RecentlyReadList
) : ApiResponse

data class RecentlyReadList(
    val chapters: List<RecentlyRead>,
    val pages: List<RecentlyRead>,
    val juzs: List<RecentlyRead>
)

data class AddRecentlyReadRequest(
    val type: String, // "chapter", "page", or "juz"
    @SerializedName("item_id")
    val itemId: String,
    @SerializedName("duration_seconds")
    val durationSeconds: Long
)

data class SimpleResponse(
    override val status: String,
    override val message: String? = null
) : ApiResponse
