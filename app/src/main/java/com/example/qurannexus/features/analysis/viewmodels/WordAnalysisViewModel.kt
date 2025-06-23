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
        _searchResults.value = emptyList()
        Log.d(TAG, "performSearch called with query: '$query', type: ${searchType.name}")

        viewModelScope.launch {
            try {
                val resultsSet = mutableSetOf<AnalysisEntryEntity>()

                when (searchType) {
                    SearchType.ALL -> {
                        Log.d(TAG, "SearchType: ALL")
                        // Search by identifier value (covers roots, lemmas, forms directly)
                        wordAnalysisDao.searchEntriesByIdentifierValue(query, limit = 15).forEach { resultsSet.add(it) }
                        // Search by Arabic form text (finds entries whose forms match)
                        wordAnalysisDao.findAnalysisEntriesByExactArabicForm(query).forEach { resultsSet.add(it) }
                        // Search by translation of forms
                        wordAnalysisDao.findAnalysisEntriesByTranslation(query, limit = 15).forEach { resultsSet.add(it) }
                        // More comprehensive generic query
                        wordAnalysisDao.searchEntriesByGenericQuery(query, limit = 20).forEach { resultsSet.add(it) }
                    }
                    SearchType.ROOT_LABEL -> { // This now means search by IDENTIFIER_VALUE where type might be ROOT
                        Log.d(TAG, "SearchType: ROOT_LABEL (searching identifier_value)")
                        wordAnalysisDao.searchEntriesByIdentifierValue(query, limit = 20)
                            .filter { it.identifierType == "ROOT" } // Filter for actual roots
                            .forEach { resultsSet.add(it) }
                        // Also include exact match if the query IS a root identifier
                        wordAnalysisDao.getAnalysisEntry(query)?.let { if(it.identifierType == "ROOT") resultsSet.add(it) }

                    }
                    SearchType.ARABIC_FORM -> {
                        Log.d(TAG, "SearchType: ARABIC_FORM")
                        wordAnalysisDao.findAnalysisEntriesByExactArabicForm(query).forEach { resultsSet.add(it) }
                        // You might want a prefix search for Arabic forms too:
                        // Create a DAO method like:
                        // @Query("SELECT DISTINCT ae.* FROM analysis_entries ae JOIN entry_arabic_forms eaf ON ae.identifier_value = eaf.parent_identifier_value WHERE eaf.arabic_text LIKE :query || '%'")
                        // suspend fun findAnalysisEntriesByArabicFormPrefix(query: String): List<AnalysisEntryEntity>
                        // wordAnalysisDao.findAnalysisEntriesByArabicFormPrefix(query).forEach { resultsSet.add(it) }
                    }
                    SearchType.TRANSLATION -> {
                        Log.d(TAG, "SearchType: TRANSLATION")
                        wordAnalysisDao.findAnalysisEntriesByTranslation(query, limit = 20).forEach { resultsSet.add(it) }
                    }
                }

                val displayableResults = mapAnalysisEntitiesToDisplayable(resultsSet.toList())
                    .sortedByDescending { it.totalOccurrences } // Sort results

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