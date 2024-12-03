package com.example.qurannexus.features.quiz.models

data class Question(
    val text : String,
    val options : List<String>,
    val answer : String
)
