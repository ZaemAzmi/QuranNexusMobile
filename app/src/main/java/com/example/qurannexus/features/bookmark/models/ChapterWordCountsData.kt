package com.example.qurannexus.features.bookmark.models

data class ChapterWordCountsResponse(
    val status: String,
    val data: ChapterWordCountsData
)

data class ChapterWordCountsData(
    val wordCounts: Map<String, Int>  // chapter_id -> word_count
)