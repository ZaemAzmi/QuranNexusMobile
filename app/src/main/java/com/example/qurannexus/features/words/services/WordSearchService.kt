package com.example.qurannexus.features.words.services

import com.example.qurannexus.core.interfaces.QuranApi
import com.example.qurannexus.features.words.models.WordSearch

class WordSearchService(private val quranApi: QuranApi) {
    sealed class SearchResult {
        data class Success(val wordSearches: List<WordSearch>, val hasMorePages: Boolean) : SearchResult()
        data class Error(val message: String) : SearchResult()
    }

//    suspend fun searchWords(
//        query: String,
//        page: Int = 1,
//        perPage: Int = 20,
//        type: String = "all"
//    ): SearchResult {
//        return try {
//            val response = quranApi.searchWords(query, page, perPage, type)
//            if (response.isSuccessful && response.body() != null) {
//                val data = response.body()!!.data
//                SearchResult.Success(
//                    words = data.words,
//                    hasMorePages = data.pagination.current_page < data.pagination.total_pages
//                )
//            } else {
//                SearchResult.Error("Failed to search words: ${response.message()}")
//            }
//        } catch (e: Exception) {
//            SearchResult.Error("Error searching words: ${e.message}")
//        }
//    }
}