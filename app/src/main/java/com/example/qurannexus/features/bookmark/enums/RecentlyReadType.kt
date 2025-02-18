package com.example.qurannexus.features.bookmark.enums

enum class RecentlyReadType {
    CHAPTER, PAGE, JUZ;

    fun toApiString(): String = name.lowercase()
}