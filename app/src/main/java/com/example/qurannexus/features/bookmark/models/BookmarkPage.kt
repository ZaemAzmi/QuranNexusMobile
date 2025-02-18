package com.example.qurannexus.features.bookmark.models

import com.google.gson.annotations.SerializedName

data class BookmarkPage(
    @SerializedName("item_properties")
    val itemProperties: PageProperties,
    val notes: String = "",
    @SerializedName("created_at")
    val createdAt: String
) {
    data class PageProperties(
        @SerializedName("page_id")
        val pageId: String,
        @SerializedName("page_number")
        val pageNumber: Int
    )
}