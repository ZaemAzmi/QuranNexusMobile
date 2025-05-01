package com.example.qurannexus.features.bookmark.models

import com.google.gson.annotations.SerializedName

data class BookmarkQuote(
    @SerializedName("item_properties")
    val itemProperties: QuoteProperties,
    val notes: String = "",
    @SerializedName("created_at")
    val createdAt: String
) {
    data class QuoteProperties(
        @SerializedName("quote_id")
        val quoteId: String,
        @SerializedName("title")
        val quoteTitle: String,
        @SerializedName("description")
        val quoteDescription: String,
        @SerializedName("source")
        val quoteSource: String
    )
}