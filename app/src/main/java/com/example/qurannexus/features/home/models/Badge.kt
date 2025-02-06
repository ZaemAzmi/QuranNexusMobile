package com.example.qurannexus.features.home.models

data class Badge(
    val id: String,
    val title: String,
    val description: String,
    val iconRes: Int,
    val status: String? = null
)
