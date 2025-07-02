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

    private val _totalResultsCount = MutableLiveData<Int?>()
    val totalResultsCount: LiveData<Int?> = _totalResultsCount
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
        _isLoading.value = true
        _totalResultsCount.value = null // Reset count for new search

        // Reset pagination state
        currentPage = 0
        isLastPage = false
        currentQuery = query
        currentSearchType = searchType
        _searchResults.value = emptyList() // Clear old results

        // Fetch total count and first page of results concurrently
        viewModelScope.launch {
            launch { // This is the count-fetching coroutine
                try {
                    val count = when (searchType) {
                        // Use the new, more accurate count method for type filters
                        SearchType.ROOT_LABEL -> wordAnalysisDao.countAllAndFilterByType(query, "ROOT")
                        SearchType.LEMMA -> wordAnalysisDao.countAllAndFilterByType(query, "LEMMA")
                        SearchType.FORM -> wordAnalysisDao.countAllAndFilterByType(query, "FORM")
                        SearchType.ALL -> wordAnalysisDao.countAll(query)
                        SearchType.ARABIC_FORM -> wordAnalysisDao.countByExactArabicForm(query)
                        SearchType.TRANSLATION -> wordAnalysisDao.countByTranslation(query)
                    }
                    _totalResultsCount.postValue(count)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get count for query '$query'", e)
                    _totalResultsCount.postValue(0) // Show 0 on error
                }
            }

            // --- Feature C (and pagination): Fetch first page ---
            loadMoreResults()
        }
    }
    fun applyFilter(newSearchType: SearchType) {
        // If the filter is the same, do nothing.
        if (newSearchType == currentSearchType) return

        Log.d(TAG, "Applying new filter: ${newSearchType.name}")
        // Re-run the entire search with the original query but the new filter type.
        performSearch(currentQuery, newSearchType)
    }
    fun loadMoreResults() {
        if (isLoadingMore || isLastPage) return

        viewModelScope.launch {
            isLoadingMore = true
            try {
                val offset = currentPage * WordAnalysisDao.SEARCH_PAGE_SIZE
                val newEntities = when (currentSearchType) {
                    SearchType.ALL -> wordAnalysisDao.searchAllPaginated(currentQuery, WordAnalysisDao.SEARCH_PAGE_SIZE, offset)
                    SearchType.ROOT_LABEL -> wordAnalysisDao.searchAllAndFilterByTypePaginated(currentQuery, "ROOT", WordAnalysisDao.SEARCH_PAGE_SIZE, offset)
                    SearchType.LEMMA -> wordAnalysisDao.searchAllAndFilterByTypePaginated(currentQuery, "LEMMA", WordAnalysisDao.SEARCH_PAGE_SIZE, offset)
                    SearchType.FORM -> wordAnalysisDao.searchAllAndFilterByTypePaginated(currentQuery, "FORM", WordAnalysisDao.SEARCH_PAGE_SIZE, offset)
                    SearchType.ARABIC_FORM -> wordAnalysisDao.findAnalysisEntriesByExactArabicFormPaginated(currentQuery, WordAnalysisDao.SEARCH_PAGE_SIZE, offset)
                    SearchType.TRANSLATION -> wordAnalysisDao.findAnalysisEntriesByTranslationPaginated(currentQuery, WordAnalysisDao.SEARCH_PAGE_SIZE, offset)
                }

                if (newEntities.isEmpty() || newEntities.size < WordAnalysisDao.SEARCH_PAGE_SIZE) {
                    isLastPage = true
                }

                val newDisplayableResults = mapAnalysisEntitiesToDisplayable(newEntities)
                val currentList = _searchResults.value ?: emptyList()
                _searchResults.postValue(currentList + newDisplayableResults)
                currentPage++

            } catch (e: Exception) {
                Log.e(TAG, "Failed to load more results", e)
                _error.postValue("Failed to load more results: ${e.message}")
            } finally {
                isLoadingMore = false
                _isLoading.postValue(false)
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