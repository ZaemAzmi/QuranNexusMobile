package com.example.qurannexus.features.words.models

data class DailyWordResponse(
    val status: String,
    val word: DailyWord
)

data class DailyWord(
    val word_id: String,
    val word_text: String,
    val translation: String,
    val transliteration: String,
    val explanation: String,
    val first_occurrence: FirstOccurrence
)

data class DailyQuoteResponse(
    val status: String,
    val quote: DailyQuote
)

data class DailyQuote(
    val quote_id: String,
    val title: String,
    val description: String,
    val source: String
)