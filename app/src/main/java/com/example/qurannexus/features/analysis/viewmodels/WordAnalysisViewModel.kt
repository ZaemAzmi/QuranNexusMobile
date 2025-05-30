package com.example.qurannexus.features.analysis.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qurannexus.core.database.entities.RootEntity
import com.example.qurannexus.features.analysis.data.WordRootDao
import com.example.qurannexus.features.analysis.enums.SearchType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// Data class for displaying frequent roots in the UI
data class DisplayableFrequentRoot(
    val rootLabel: String,
    val displayArabicText: String, // e.g., "ٱللَّه" - the most common form or first occurrence text
    val displayTranslation: String, // e.g., "Allah" - translation of the displayArabicText
    val totalOccurrences: Int
)

@HiltViewModel
class WordAnalysisViewModel @Inject constructor(
    private val wordRootDao: WordRootDao
) : ViewModel() {

    private val _frequentRoots = MutableLiveData<List<DisplayableFrequentRoot>>()
    val frequentRoots: LiveData<List<DisplayableFrequentRoot>> = _frequentRoots

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    private val _searchResults = MutableLiveData<List<DisplayableFrequentRoot>>() // Reuse DisplayableFrequentRoot for consistency
    val searchResults: LiveData<List<DisplayableFrequentRoot>> = _searchResults

    companion object {
        private const val TAG = "WordAnalysisVM"
    }
    fun fetchFrequentRoots(limit: Int = 8) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val rootEntities = wordRootDao.getMostFrequentRoots(limit)
                val displayableRoots = rootEntities.mapNotNull { root ->
                    // For display, we use the first_occurrence_arabic_text and its translation.
                    // Alternatively, you could fetch the most common ArabicForm for this root.
                    if (root.firstOccurrenceArabicText != null && root.firstOccurrenceTranslation != null) {
                        DisplayableFrequentRoot(
                            rootLabel = root.rootLabel,
                            displayArabicText = root.firstOccurrenceArabicText,
                            displayTranslation = root.firstOccurrenceTranslation,
                            totalOccurrences = root.totalOccurrences ?: 0
                        )
                    } else if (root.firstOccurrenceArabicText != null) { // Fallback if translation is missing
                        DisplayableFrequentRoot(
                            rootLabel = root.rootLabel,
                            displayArabicText = root.firstOccurrenceArabicText,
                            displayTranslation = "N/A", // Or fetch from its forms
                            totalOccurrences = root.totalOccurrences ?: 0
                        )
                    }
                    else {
                        // If first_occurrence details are insufficient, you might skip or fetch its forms.
                        // For simplicity, skipping if essential display text is missing.
                        null
                    }
                }
                _frequentRoots.postValue(displayableRoots)
                _error.postValue(null)
            } catch (e: Exception) {
                _error.postValue("Failed to load frequent roots: ${e.localizedMessage}")
                _frequentRoots.postValue(emptyList())
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun performSearch(query: String, searchType: SearchType) {
        _isLoading.value = true
        _searchResults.value = emptyList() // Clear previous results for a new search
        Log.d(TAG, "performSearch called with query: '$query', type: ${searchType.name}")

        viewModelScope.launch {
            try {
                val resultsSet = mutableSetOf<RootEntity>() // Use Set to handle distinct roots easily

                when (searchType) {
                    SearchType.ALL -> {
                        Log.d(TAG, "SearchType: ALL - Querying all types")
                        wordRootDao.getRoot(query)?.let { resultsSet.add(it) }
                        wordRootDao.searchRootsByLabel(query, limit = 10).forEach { resultsSet.add(it) }
                        wordRootDao.findRootsByExactArabicForm(query).forEach { resultsSet.add(it) }
                        // Consider if findRootsByArabicFormPrefix is needed for 'ALL'
                        // wordRootDao.findRootsByArabicFormPrefix(query).forEach { resultsSet.add(it) }
                        wordRootDao.findRootsByTranslation(query, limit = 10).forEach { resultsSet.add(it) }
                    }
                    SearchType.ROOT_LABEL -> {
                        Log.d(TAG, "SearchType: ROOT_LABEL")
                        wordRootDao.getRoot(query)?.let { resultsSet.add(it) } // Exact match
                        wordRootDao.searchRootsByLabel(query, limit = 20).forEach { resultsSet.add(it) } // Prefix match
                    }
                    SearchType.ARABIC_FORM -> {
                        Log.d(TAG, "SearchType: ARABIC_FORM")
                        wordRootDao.findRootsByExactArabicForm(query).forEach { resultsSet.add(it) }
                        // Add prefix search for Arabic forms if DAO method exists and is desired
                        // wordRootDao.findRootsByArabicFormPrefix(query, limit = 20).forEach { resultsSet.add(it) }
                    }
                    SearchType.TRANSLATION -> {
                        Log.d(TAG, "SearchType: TRANSLATION")
                        wordRootDao.findRootsByTranslation(query, limit = 20).forEach { resultsSet.add(it) }
                    }
                }

                val displayableResults = mapRootEntitiesToDisplayable(resultsSet.toList())
                    .sortedByDescending { it.totalOccurrences }

                Log.d(TAG, "Search completed. Found ${displayableResults.size} displayable results.")
                _searchResults.postValue(displayableResults)
                _error.postValue(null)

            } catch (e: Exception) {
                Log.e(TAG, "Search failed for query: '$query', type: $searchType", e)
                _error.postValue("Search failed: ${e.localizedMessage}")
                _searchResults.postValue(emptyList())
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    // Helper function to map RootEntity list to DisplayableFrequentRoot list
    private suspend fun mapRootEntitiesToDisplayable(roots: List<RootEntity>): List<DisplayableFrequentRoot> {
        return roots.mapNotNull { root ->
            if (root.firstOccurrenceArabicText != null) {
                DisplayableFrequentRoot(
                    rootLabel = root.rootLabel,
                    displayArabicText = root.firstOccurrenceArabicText,
                    displayTranslation = root.firstOccurrenceTranslation ?: "N/A",
                    totalOccurrences = root.totalOccurrences ?: 0
                )
            } else {
                // Fallback: If firstOccurrenceArabicText is null, try to get the first Arabic form
                val forms = wordRootDao.getArabicFormsForRoot(root.rootLabel)
                if (forms.isNotEmpty() && forms.first().arabicText != null) {
                    val firstForm = forms.first()
                    val firstFormTranslations = wordRootDao.getTranslationsForForm(firstForm.arabicFormId)
                    DisplayableFrequentRoot(
                        rootLabel = root.rootLabel,
                        displayArabicText = firstForm.arabicText!!, // Not null due to check
                        displayTranslation = firstFormTranslations.firstOrNull()?.translation ?: "N/A",
                        totalOccurrences = root.totalOccurrences ?: 0
                    )
                } else {
                    Log.w(TAG, "Could not find displayable text/translation for root: ${root.rootLabel}")
                    null // Skip this root if no displayable information can be found
                }
            }
        }
    }
    // Clear search results when needed (e.g., when search query is cleared)
    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }
}