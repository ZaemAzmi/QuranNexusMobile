package com.example.qurannexus.models

data class Badge(
    val title: String,
    val description: String,
    val iconRes: Int,
    val status: String? = null
)
