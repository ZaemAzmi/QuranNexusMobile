package com.example.qurannexus.features.analysis.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qurannexus.core.database.entities.AnalysisEntryEntity
import com.example.qurannexus.features.analysis.data.WordAnalysisDao
import com.example.qurannexus.features.analysis.enums.SearchType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// Data class for displaying frequent roots in the UI
data class DisplayableFrequentRoot( // Or DisplayableAnalysisEntry
    val identifierValue: String,         // Was rootLabel, now holds the root/lemma/form string
    val identifierType: String,        // NEW: "ROOT", "LEMMA", "FORM"
    val displayArabicText: String,     // e.g., "ٱللَّه" - the most common form or first occurrence text
    val displayTranslation: String,    // e.g., "Allah" - translation of the displayArabicText
    val totalOccurrences: Int,
    val uniqueFormCount: Int? = null // Added this as optional
)

@HiltViewModel
class WordAnalysisViewModel @Inject constructor(
    private val wordAnalysisDao: WordAnalysisDao
) : ViewModel() {

    // Renamed for clarity, though the displayable item structure is similar
    private val _frequentEntries = MutableLiveData<List<DisplayableFrequentRoot>>()
    val frequentEntries: LiveData<List<DisplayableFrequentRoot>> = _frequentEntries

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _searchResults = MutableLiveData<List<DisplayableFrequentRoot>>()
    val searchResults: LiveData<List<DisplayableFrequentRoot>> = _searchResults

    companion object {
        private const val TAG = "WordAnalysisVM"
    }
    // --- PAGINATION STATE VARIABLES ---
    private var currentPage = 0
    private var isLastPage = false
    private var isLoadingMore = false
    private var currentQuery: String = ""
    private var currentSearchType: SearchType = SearchType.ALL

    fun fetchFrequentEntries(limit: Int = 10) { // Renamed method
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // Uses the renamed DAO method that returns List<AnalysisEntryEntity>
                val entryEntities = wordAnalysisDao.getMostFrequentEntries(limit)
                val displayableEntries = mapAnalysisEntitiesToDisplayable(entryEntities)
                _frequentEntries.postValue(displayableEntries)
                _error.postValue(null)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load frequent entries", e)
                _error.postValue("Failed to load frequent entries: ${e.localizedMessage}")
                _frequentEntries.postValue(emptyList())
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun performSearch(query: String, searchType: SearchType) {
        // Reset pagination state for a new search
        currentPage = 0
        isLastPage = false
        currentQuery = query
        currentSearchType = searchType
        _searchResults.value = emptyList() // Clear old results immediately

        // If the search is paginated, start the loading process
        if (searchType == SearchType.ALL) {
            _isLoading.value = true // Show loading spinner for the first page
            loadMoreResults()
        } else {
            // For other search types, use the original, non-paginated logic
            _isLoading.value = true
            viewModelScope.launch {
                try {
                    val resultsSet = mutableSetOf<AnalysisEntryEntity>()
                    when (searchType) {
                        SearchType.ROOT_LABEL -> {
                            wordAnalysisDao.searchEntriesByIdentifierValue(query, limit = 20)
                                .filter { it.identifierType == "ROOT" }
                                .forEach { resultsSet.add(it) }
                            wordAnalysisDao.getAnalysisEntry(query)?.let { if(it.identifierType == "ROOT") resultsSet.add(it) }
                        }
                        SearchType.ARABIC_FORM -> {
                            wordAnalysisDao.findAnalysisEntriesByExactArabicForm(query).forEach { resultsSet.add(it) }
                        }
                        SearchType.TRANSLATION -> {
                            wordAnalysisDao.findAnalysisEntriesByTranslation(query, limit = 20).forEach { resultsSet.add(it) }
                        }
                        else -> { /* ALL is handled by pagination */ }
                    }
                    val displayableResults = mapAnalysisEntitiesToDisplayable(resultsSet.toList())
                        .sortedByDescending { it.totalOccurrences }
                    _searchResults.postValue(displayableResults)
                } catch (e: Exception) {
                    Log.e(TAG, "Search failed for non-paginated query: '$query'", e)
                    _error.postValue("Search failed: ${e.localizedMessage}")
                } finally {
                    _isLoading.postValue(false)
                }
            }
        }
    }
    fun loadMoreResults() {
        // Prevent multiple simultaneous loads or loading after the end is reached
        if (isLoadingMore || isLastPage || currentSearchType != SearchType.ALL) return

        viewModelScope.launch {
            isLoadingMore = true
            // The main spinner is already active for the first page (currentPage == 0)

            try {
                val offset = currentPage * WordAnalysisDao.SEARCH_PAGE_SIZE
                // Use the new paginated DAO method
                val newEntities = wordAnalysisDao.searchAllPaginated(
                    query = currentQuery,
                    limit = WordAnalysisDao.SEARCH_PAGE_SIZE,
                    offset = offset
                )

                if (newEntities.isEmpty() || newEntities.size < WordAnalysisDao.SEARCH_PAGE_SIZE) {
                    isLastPage = true // Reached the end
                }

                val newDisplayableResults = mapAnalysisEntitiesToDisplayable(newEntities)
                val currentList = _searchResults.value ?: emptyList()
                _searchResults.postValue(currentList + newDisplayableResults) // Append new results

                currentPage++ // Increment page for the next call

            } catch (e: Exception) {
                Log.e(TAG, "Failed to load more results for query: '$currentQuery'", e)
                _error.postValue("Failed to load more results: ${e.message}")
            } finally {
                isLoadingMore = false
                _isLoading.postValue(false) // Hide loading spinner after any load
            }
        }
    }

    // Helper function to map AnalysisEntryEntity list to DisplayableFrequentRoot list
    private suspend fun mapAnalysisEntitiesToDisplayable(entries: List<AnalysisEntryEntity>): List<DisplayableFrequentRoot> {
        return entries.mapNotNull { entry ->
            // Use firstOccurrenceArabicText if available
            val displayArabic = entry.firstOccurrenceArabicText
            val displayTrans = entry.firstOccurrenceTranslation
            val uniqueFormsFromEntity = entry.totalNumberOfUniqueArabicForms // Get it from the entity

            // Add a Log here to check the value from the entity:
            Log.d("ViewModelMap", "Entry: ${entry.identifierValue}, Type: ${entry.identifierType}, UniqueFormsFromEntity: $uniqueFormsFromEntity")

            if (displayArabic != null) {
                DisplayableFrequentRoot(
                    identifierValue = entry.identifierValue,
                    identifierType = entry.identifierType,
                    displayArabicText = displayArabic,
                    displayTranslation = displayTrans ?: "N/A",
                    totalOccurrences = entry.totalOccurrences ?: 0,
                    // Only assign uniqueFormCount if it's meaningful (not for FORM type typically, and > 0)
                    uniqueFormCount = if (entry.identifierType != "FORM" && uniqueFormsFromEntity != null && uniqueFormsFromEntity > 0) uniqueFormsFromEntity else null
                )
            } else {
                // Fallback: If firstOccurrenceArabicText is null, try to get the first (or most common) Arabic form
                val forms = wordAnalysisDao.getArabicFormsForEntry(entry.identifierValue)
                if (forms.isNotEmpty() && forms.first().arabicText != null) {
                    val firstForm = forms.first() // Or sort by occurrencesOfThisSpecificArabicForm and pick most common
                    val firstFormTranslations = wordAnalysisDao.getTranslationsForForm(firstForm.arabicFormId)
                    DisplayableFrequentRoot(
                        identifierValue = entry.identifierValue,
                        identifierType = entry.identifierType,
                        displayArabicText = firstForm.arabicText!!,
                        displayTranslation = firstFormTranslations.firstOrNull()?.translation ?: "N/A",
                        totalOccurrences = entry.totalOccurrences ?: 0,
                        uniqueFormCount = if (entry.identifierType != "FORM" && uniqueFormsFromEntity != null && uniqueFormsFromEntity > 0) uniqueFormsFromEntity else null
                    )
                } else {
                    // Last resort: use the identifier value itself if it's an Arabic script (common for FORMs)
                    // This is a heuristic.
                    if (entry.identifierValue.matches(Regex("\\p{InArabic}+"))) {
                        DisplayableFrequentRoot(
                            identifierValue = entry.identifierValue,
                            identifierType = entry.identifierType,
                            displayArabicText = entry.identifierValue, // Display the identifier itself
                            displayTranslation = "Definition/Type: ${entry.identifierType.lowercase()}", // Generic translation
                            totalOccurrences = entry.totalOccurrences ?: 0
                        )
                    } else {
                        Log.w(TAG, "Could not find displayable text/translation for entry: ${entry.identifierValue} (${entry.identifierType})")
                        null // Skip this entry if no displayable information
                    }
                }
            }
        }
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }
}