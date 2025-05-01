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
    val first_occurrence: FirstWordOccurrence
)

data class DailyQuoteResponse(
    val status: String,
    val quote: DailyQuote
)

data class DailyQuote(
    val Id: String,
    val Title: String,
    val Description: String,
    val Source: String
)

// This class handles the list response from your API
data class DailyQuoteListResponse(
    val data: List<DailyQuote>
)