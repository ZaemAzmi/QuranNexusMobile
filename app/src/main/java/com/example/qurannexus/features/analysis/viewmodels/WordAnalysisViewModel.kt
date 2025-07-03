package com.example.qurannexus.features.analysis.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qurannexus.core.database.entities.AnalysisEntryEntity
import com.example.qurannexus.features.analysis.WordSearchResultsFragment
import com.example.qurannexus.features.analysis.data.WordAnalysisDao
import com.example.qurannexus.features.analysis.enums.FilterType
import com.example.qurannexus.features.analysis.enums.SearchType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// Data class for displaying frequent roots in the UI
data class DisplayableFrequentRoot( // Or DisplayableAnalysisEntry
    val identifierValue: String,         // Was rootLabel, now holds the root/lemma/form string
    val identifierType: String,        // NEW: "ROOT", "LEMMA", "OTHERS"
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

    // --- The Single Source of Truth for the UI ---
    private val _uiState = MutableLiveData<WordSearchResultsFragment.WordAnalysisUiState>(
        WordSearchResultsFragment.WordAnalysisUiState.Idle)
    val uiState: LiveData<WordSearchResultsFragment.WordAnalysisUiState> = _uiState
    private var currentResultsList = mutableListOf<DisplayableFrequentRoot>()

    // --- PAGINATION STATE VARIABLES ---
    private var currentPage = 0
    private var isLastPage = false
    private var currentQuery: String = ""
    private var initialSearchScope: SearchType = SearchType.GENERAL
    private var currentFilter: FilterType = FilterType.ALL
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
    fun startSearch(query: String, searchScope: SearchType) {
        _uiState.value = WordSearchResultsFragment.WordAnalysisUiState.LoadingInitial
        currentPage = 0
        isLastPage = false
        currentResultsList.clear()

        currentQuery = query
        initialSearchScope = searchScope
        currentFilter = FilterType.ALL // Always start with "All" filter selected

        fetchData()
    }
    fun applyFilter(newFilter: FilterType) {
        if (newFilter == currentFilter) return

        _uiState.value = WordSearchResultsFragment.WordAnalysisUiState.LoadingInitial
        currentPage = 0
        isLastPage = false
        currentResultsList.clear()

        currentFilter = newFilter
        fetchData()
    }

    fun loadMoreResults() {
        // Get the current state
        val currentState = _uiState.value
        // Guard against multiple calls if we are already loading or on the last page.
        if (isLastPage || (currentState is WordSearchResultsFragment.WordAnalysisUiState.HasResults && currentState.isLoadingMore)) {
            return
        }

        if (currentState is WordSearchResultsFragment.WordAnalysisUiState.HasResults) {
            // Correctly update the state to show we are now loading more.
            _uiState.value = currentState.copy(isLoadingMore = true)
        }

        // Proceed to fetch the data
        fetchData()
    }
    private fun fetchData() {
        viewModelScope.launch {
            try {
                val offset = currentPage * WordAnalysisDao.SEARCH_PAGE_SIZE
                val limit = WordAnalysisDao.SEARCH_PAGE_SIZE
                val count: Int = if (currentPage == 0) {
                    // --- COUNT LOGIC ---
                    if (currentFilter == FilterType.ALL) {
                        when (initialSearchScope) {
                            SearchType.GENERAL -> wordAnalysisDao.countGeneral(currentQuery)
                            SearchType.ARABIC_FORM -> wordAnalysisDao.countArabicForm(currentQuery)
                            SearchType.TRANSLATION -> wordAnalysisDao.countTranslation(currentQuery)
                            SearchType.IDENTIFIER -> wordAnalysisDao.countIdentifier(currentQuery)
                        }
                    } else {
                        val typeString = when(currentFilter) {
                            FilterType.ROOT -> "ROOT"
                            FilterType.LEMMA -> "LEMMA"
                            FilterType.OTHERS -> "OTHERS"
                            else -> ""
                        }
                        when (initialSearchScope) {
                            SearchType.GENERAL -> wordAnalysisDao.countGeneralAndFilterByType(currentQuery, typeString)
                            SearchType.ARABIC_FORM -> wordAnalysisDao.countArabicFormAndFilterByType(currentQuery, typeString)
                            SearchType.TRANSLATION -> wordAnalysisDao.countTranslationAndFilterByType(currentQuery, typeString)
                            SearchType.IDENTIFIER -> wordAnalysisDao.countIdentifierAndFilterByType(currentQuery, typeString)
                        }
                    }
                } else {
                    (_uiState.value as? WordSearchResultsFragment.WordAnalysisUiState.HasResults)?.totalCount ?: 0
                }

                if (currentPage == 0 && count == 0) {
                    _uiState.postValue(WordSearchResultsFragment.WordAnalysisUiState.NoResults("No results found for \"$currentQuery\"."))
                    return@launch
                }

                val newEntities: List<AnalysisEntryEntity> = if (currentFilter == FilterType.ALL) {
                    // --- PAGINATED FETCH LOGIC (ALL Filter) ---
                    when (initialSearchScope) {
                        SearchType.GENERAL -> wordAnalysisDao.searchGeneralPaginated(currentQuery, limit, offset)
                        SearchType.ARABIC_FORM -> wordAnalysisDao.searchArabicFormPaginated(currentQuery, limit, offset)
                        SearchType.TRANSLATION -> wordAnalysisDao.searchTranslationPaginated(currentQuery, limit, offset)
                        SearchType.IDENTIFIER -> wordAnalysisDao.searchIdentifierPaginated(currentQuery, limit, offset)
                    }
                } else {
                    // --- PAGINATED FETCH LOGIC (Specific Filter) ---
                    val typeString = when(currentFilter) {
                        FilterType.ROOT -> "ROOT"
                        FilterType.LEMMA -> "LEMMA"
                        FilterType.OTHERS -> "OTHERS"
                        else -> ""
                    }
                    when (initialSearchScope) {
                        SearchType.GENERAL -> wordAnalysisDao.searchGeneralAndFilterByTypePaginated(currentQuery, typeString, limit, offset)
                        SearchType.ARABIC_FORM -> wordAnalysisDao.searchArabicFormAndFilterByTypePaginated(currentQuery, typeString, limit, offset)
                        SearchType.TRANSLATION -> wordAnalysisDao.searchTranslationAndFilterByTypePaginated(currentQuery, typeString, limit, offset)
                        SearchType.IDENTIFIER -> wordAnalysisDao.searchIdentifierAndFilterByTypePaginated(currentQuery, typeString, limit, offset)
                    }
                }
                if (newEntities.size < WordAnalysisDao.SEARCH_PAGE_SIZE) {
                    isLastPage = true
                }
                currentResultsList.addAll(mapAnalysisEntitiesToDisplayable(newEntities))
                _uiState.postValue(
                    WordSearchResultsFragment.WordAnalysisUiState.HasResults(
                    results = ArrayList(currentResultsList),
                    totalCount = count,
                    isLoadingMore = false
                ))
                currentPage++

            } catch (e: Exception) {
                Log.e("ViewModel", "Error fetching data", e)
                _uiState.postValue(WordSearchResultsFragment.WordAnalysisUiState.Error("An error occurred."))
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
                    uniqueFormCount = if (entry.identifierType != "OTHERS" && uniqueFormsFromEntity != null && uniqueFormsFromEntity > 0) uniqueFormsFromEntity else null
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
                        uniqueFormCount = if (entry.identifierType != "OTHERS" && uniqueFormsFromEntity != null && uniqueFormsFromEntity > 0) uniqueFormsFromEntity else null
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

}