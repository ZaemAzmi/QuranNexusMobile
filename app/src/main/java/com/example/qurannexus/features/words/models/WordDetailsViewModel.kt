package com.example.qurannexus.features.words.models

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.qurannexus.core.database.entities.AnalysisEntryEntity
import com.example.qurannexus.core.database.entities.EntryArabicFormWithFullDetails // Renamed
import com.example.qurannexus.core.database.entities.AnalysisEntryFullDetails // Renamed
import com.example.qurannexus.core.database.entities.AllWordOccurrenceEntity // Renamed
import com.example.qurannexus.core.database.entities.WordIdentity // New
import com.example.qurannexus.core.interfaces.QuranApi
import com.example.qurannexus.core.utils.TokenManager
import com.example.qurannexus.features.analysis.data.WordAnalysisDao
import com.example.qurannexus.features.bookmark.models.BookmarkRequest
import com.example.qurannexus.features.bookmark.models.BookmarkResponse
import com.example.qurannexus.features.bookmark.models.BookmarksResponse
import com.example.qurannexus.features.bookmark.models.RemoveBookmarkResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class WordDetailsViewModel @Inject constructor(
    private val application : Application,
    private val wordAnalysisDao : WordAnalysisDao,
    private val quranApi: QuranApi,
    private val gson : Gson,
    private val tokenManager: TokenManager
) : AndroidViewModel(application) {

    private val _analysisEntry = MutableLiveData<AnalysisEntryEntity?>()
    val analysisEntry: LiveData<AnalysisEntryEntity?> = _analysisEntry

    private val _arabicForms = MutableLiveData<List<EntryArabicFormWithFullDetails>>()
    val arabicForms: LiveData<List<EntryArabicFormWithFullDetails>> = _arabicForms

    // This will hold the Arabic form whose details are currently displayed (e.g., text, translation, transliteration)
    private val _selectedArabicForm = MutableLiveData<EntryArabicFormWithFullDetails?>()
    val selectedArabicForm: LiveData<EntryArabicFormWithFullDetails?> = _selectedArabicForm

    private val _contributingMorphForms = MutableLiveData<List<String>>()
    val contributingMorphForms: LiveData<List<String>> = _contributingMorphForms

    // For charts - Map<JuzNumber (String), Count (Int)>
    private val _juzDistribution = MutableLiveData<Map<String, Int>>()
    val juzDistribution: LiveData<Map<String, Int>> = _juzDistribution

    // For BottomSheet: occurrences of the root in a specific Juz
    private val _occurrencesInJuz = MutableLiveData<List<WordOccurrenceDisplayItem>>()
    val occurrencesInJuz: LiveData<List<WordOccurrenceDisplayItem>> = _occurrencesInJuz

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isLoadingMoreOccurrences = MutableLiveData(false)
    val isLoadingMoreOccurrences: LiveData<Boolean> = _isLoadingMoreOccurrences

    private val _hasMoreOccurrences = MutableLiveData(true)
    val hasMoreOccurrences: LiveData<Boolean> = _hasMoreOccurrences

    private val _isBookmarked = MutableLiveData<Boolean>()
    val isBookmarked: LiveData<Boolean> = _isBookmarked

    // For signaling toast messages to the Activity
    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage
    // To store the S:A:W key if navigated from recitation
    private val _currentWordKey = MutableLiveData<String?>()
    // To store the originally clicked Arabic text if provided, used for pre-selecting in spinner
    private val _clickedArabicTextForPreselection = MutableLiveData<String?>()

    private var currentOccurrencePage = 1
    private val occurrencesPerPage = 20
    // Store the identifierValue of the current AnalysisEntry for loading its occurrences
    private var currentIdentifierValueForOccurrences: String? = null
    private var currentJuzForOccurrences: Int? = null
    companion object { // Add a TAG for logging
        private const val TAG = "WordDetailsVM"
    }
    /**
     * Main entry point for loading data.
     * Determines the canonical identifier (root/lemma/form string) and its type,
     * then fetches the full details for that identifier.
     *
     * @param identifierValueFromSearch Directly provides the root/lemma/form string (e.g., from a search result).
     *                                  If this is provided, its type is assumed or needs to be found.
     * @param wordKeyFromRecitation The S:A:W key (e.g., "1:2:3") if clicked from recitation text.
     *                              This is used to find the mapped_identifier_value and _type.
     * @param wordTextForPreselection The specific Arabic text of the word that was clicked/bookmarked.
     *                                Used to pre-select the correct item in the spinner.
     */
    fun loadInitialData(
        identifierValueFromSearch: String?,
        wordKeyFromRecitation: String?,
        wordTextForPreselection: String?
    ) {
        if (_isLoading.value == true) return
        _isLoading.value = true
        _error.value = null
        clearAllDataInternal()
        _clickedArabicTextForPreselection.value = wordTextForPreselection

        viewModelScope.launch {
            var targetIdentifierValue: String? = null
            // var targetIdentifierType: String? = null // We get this from AnalysisEntryEntity

            try {
                if (identifierValueFromSearch != null) {
                    // Navigating with a known identifier (e.g. root "ktb" from search)
                    targetIdentifierValue = identifierValueFromSearch
                    Log.d(TAG, "Loading by direct identifierValueFromSearch: $targetIdentifierValue")
                } else if (wordKeyFromRecitation != null) {
                    // Navigating from recitation click (S:A:W)
                    _currentWordKey.value = wordKeyFromRecitation
                    val identity: WordIdentity? = wordAnalysisDao.getIdentityForWordKey(wordKeyFromRecitation)
                    if (identity != null) {
                        targetIdentifierValue = identity.value
                        // targetIdentifierType = identity.type // Type will be in AnalysisEntryEntity
                        Log.d(TAG, "Resolved wordKey '$wordKeyFromRecitation' to identifier: ${identity.value} (${identity.type})")
                        // If original wordTextForPreselection was null, use the one from the S:A:W occurrence
                        if (_clickedArabicTextForPreselection.value == null) {
                            val occurrence = wordAnalysisDao.getWordOccurrenceByWordKey(wordKeyFromRecitation)
                            _clickedArabicTextForPreselection.postValue(occurrence?.arabicText)
                            Log.d(TAG, "Fetched arabicText '${occurrence?.arabicText}' for preselection from wordKey '$wordKeyFromRecitation'")
                        }
                    } else {
                        _error.postValue("Could not find mapping for word key: $wordKeyFromRecitation")
                        _isLoading.postValue(false)
                        return@launch
                    }
                } else if (wordTextForPreselection != null) {
                    // Navigating from bookmark or old click mechanism (only Arabic text provided)
                    // This is less precise, relies on finding the first occurrence of this text.
                    val identity: WordIdentity? = wordAnalysisDao.getIdentityForArabicText(wordTextForPreselection)
                    if (identity != null) {
                        targetIdentifierValue = identity.value
                        // targetIdentifierType = identity.type
                        Log.d(TAG, "Resolved wordText '$wordTextForPreselection' to identifier: ${identity.value} (${identity.type})")
                    } else {
                        // Fallback: maybe the wordTextForPreselection IS an identifier_value itself
                        // (e.g. a root like "ktb" was bookmarked, not a specific form "كاتِب").
                        // We check if an entry exists with this value.
                        val directEntry = wordAnalysisDao.getAnalysisEntry(wordTextForPreselection)
                        if (directEntry != null) {
                            targetIdentifierValue = directEntry.identifierValue
                            // targetIdentifierType = directEntry.identifierType
                            Log.d(TAG, "Found direct entry for '$wordTextForPreselection' as an identifier itself.")
                        } else {
                            _error.postValue("Could not determine identifier for word: $wordTextForPreselection")
                            _isLoading.postValue(false)
                            return@launch
                        }
                    }
                } else {
                    _error.postValue("No valid parameters to load word details.")
                    _isLoading.postValue(false)
                    return@launch
                }

                if (targetIdentifierValue == null) {
                    _error.postValue("Failed to determine target identifier.")
                    _isLoading.postValue(false)
                    return@launch
                }

                currentIdentifierValueForOccurrences = targetIdentifierValue // For loading occurrences
                Log.d(TAG, "Fetching full details for identifierValue: $targetIdentifierValue")

                // Fetch the full details using the resolved targetIdentifierValue
                val fullDetails: AnalysisEntryFullDetails? = wordAnalysisDao.getAnalysisEntryFullDetails(targetIdentifierValue)

                if (fullDetails != null) {
                    _analysisEntry.postValue(fullDetails.analysisEntryEntity)
                    _arabicForms.postValue(fullDetails.arabicForms) // Uses Renamed Wrapper
                    _contributingMorphForms.postValue(fullDetails.contributingMorphForms)

                    val juzDistMap = fullDetails.juzDistribution
                        .associate { it.juzId.toString() to (it.count ?: 0) }
                    _juzDistribution.postValue(juzDistMap)

                    Log.d(TAG, "Available Arabic Forms for ${fullDetails.analysisEntryEntity.identifierValue} (${fullDetails.analysisEntryEntity.identifierType}):")
                    fullDetails.arabicForms.forEachIndexed { index, formDetail ->
                        Log.d(TAG, "  Form $index: ${formDetail.entryArabicFormEntity.arabicText}")
                    }

                    val actualPreselectionText = _clickedArabicTextForPreselection.value
                    val formToSelect = if (actualPreselectionText != null) {
                        Log.d(TAG, "Attempting to pre-select form matching: '$actualPreselectionText'")
                        fullDetails.arabicForms.firstOrNull { it.entryArabicFormEntity.arabicText == actualPreselectionText }
                    } else {
                        null
                    }

                    if (formToSelect != null) {
                        Log.d(TAG, "Matched form for pre-selection: ${formToSelect.entryArabicFormEntity.arabicText}")
                    } else if (actualPreselectionText != null) {
                        Log.w(TAG, "COULD NOT MATCH '$actualPreselectionText' with any available forms for pre-selection.")
                    }

                    _selectedArabicForm.postValue(formToSelect ?: fullDetails.arabicForms.firstOrNull())
                    Log.d(TAG, "FINAL _selectedArabicForm posted: ${(formToSelect ?: fullDetails.arabicForms.firstOrNull())?.entryArabicFormEntity?.arabicText}")

                } else {
                    _error.postValue("Details for identifier '$targetIdentifierValue' not found.")
                }
            } catch (e: Exception) {
                _error.postValue("Error loading word data: ${e.localizedMessage}")
                Log.e(TAG, "Error in loadInitialData", e)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun selectArabicForm(form: EntryArabicFormWithFullDetails) {
        _selectedArabicForm.value = form
        // Here you could also trigger fetching specific details for this form if they aren't already loaded,
        // e.g., if transliterations/translations were very large and fetched on-demand.
        // In our current DAO, they are fetched with getArabicFormsWithDetails.
    }

    fun loadOccurrencesForJuz(juzNumber: Int, isInitialLoad: Boolean = true) {
        val identifierValue = currentIdentifierValueForOccurrences ?: run {
            Log.w(TAG, "loadOccurrencesForJuz called but currentIdentifierValueForOccurrences is null")
            return
        }
        if (_isLoadingMoreOccurrences.value == true && !isInitialLoad) return

        if (isInitialLoad) {
            currentOccurrencePage = 1
            _occurrencesInJuz.value = emptyList()
            _hasMoreOccurrences.value = true
        }

        currentJuzForOccurrences = juzNumber
        _isLoadingMoreOccurrences.value = true
//        _error.value = null

        viewModelScope.launch {
            try {
                val offset = (currentOccurrencePage - 1) * occurrencesPerPage
                // Use AllWordOccurrenceEntity from DAO
                val newDbOccurrences: List<AllWordOccurrenceEntity> = wordAnalysisDao.getOccurrencesForEntryInJuz(
                    identifierValue = identifierValue, // Pass identifierValue
                    juzNumber = juzNumber,
                    limit = occurrencesPerPage,
                    offset = offset
                )

                val displayItems = newDbOccurrences.map { occ ->
                    WordOccurrenceDisplayItem(
                        wordKey = occ.wordKey,
                        surahId = occ.surahId,
                        ayahIndex = occ.ayahIndex,
                        arabicText = occ.arabicText,
                        translation = occ.translation, // This is translation of the word, not verse.
                        juzId = occ.juzId,
                        pageId = occ.pageId,
                        fullVerseText = null, // To be fetched if needed
                        chapterIdString = occ.surahId.toString(),
                        verseNumberString = occ.ayahIndex.toString()
                    )
                }

                if (isInitialLoad) {
                    _occurrencesInJuz.postValue(displayItems)
                } else {
                    _occurrencesInJuz.postValue(_occurrencesInJuz.value.orEmpty() + displayItems)
                }

                _hasMoreOccurrences.postValue(displayItems.size == occurrencesPerPage)
                if (displayItems.isNotEmpty() && displayItems.size == occurrencesPerPage) {
                    currentOccurrencePage++
                }
                // Log.d(TAG, "Loaded ${displayItems.size} occurrences for Juz $juzNumber. HasMore: ${_hasMoreOccurrences.value}")

            } catch (e: Exception) {
                // Post error specific to this operation if desired, or use general _error
                _error.postValue("Error loading occurrences for Juz $juzNumber: ${e.localizedMessage}")
                Log.e(TAG, "Error in loadOccurrencesForJuz", e)
                _hasMoreOccurrences.postValue(false)
            } finally {
                _isLoadingMoreOccurrences.postValue(false)
            }
        }
    }


    fun loadMoreOccurrencesForCurrentJuz() {
        if (currentJuzForOccurrences != null && _hasMoreOccurrences.value == true && _isLoadingMoreOccurrences.value == false) {
            loadOccurrencesForJuz(currentJuzForOccurrences!!, isInitialLoad = false)
        }
    }

    fun getCharactersForSelectedForm(): List<String> {
        // Use renamed wrapper and entity
        return _selectedArabicForm.value?.entryArabicFormEntity?.charactersJson?.let { json ->
            try {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson<List<String>>(json, type)
            } catch (e: Exception) { emptyList() }
        } ?: emptyList()
    }

    // Helper to parse characters from RootEntity (first occurrence)
    fun getFirstOccurrenceCharacters(): List<String> {
        // Use renamed LiveData
        return _analysisEntry.value?.firstOccurrenceCharactersJson?.let { json ->
            try {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson<List<String>>(json, type)
            } catch (e: Exception) { emptyList() }
        } ?: emptyList()
    }

    fun checkSelectedFormBookmarkStatus(arabicFormText: String?) {
        val token = tokenManager.getToken()
        if (token == null) {
            _isBookmarked.postValue(false)
            Log.d(TAG, "checkSelectedFormBookmarkStatus: No token, user not logged in. Setting bookmark to false.")
            return
        }
        if (arabicFormText == null) {
            _isBookmarked.postValue(false) // Cannot determine bookmark status for a null form
            Log.d(TAG, "checkSelectedFormBookmarkStatus: arabicFormText is null.")
            return
        }
        Log.d(TAG, "Checking bookmark status for form: $arabicFormText")

        quranApi.getBookmarks("Bearer $token").enqueue(object : Callback<BookmarksResponse> {
            override fun onResponse(call: Call<BookmarksResponse>, response: Response<BookmarksResponse>) {
                Log.e(TAG, "bookmark bearer : $token")
                
                if (response.isSuccessful) {
                    val bookmarked = response.body()?.bookmarks?.words?.any {
                        it.itemProperties.wordText == arabicFormText
                    } ?: false
                    _isBookmarked.postValue(bookmarked)
                    Log.d(TAG, "Form '$arabicFormText' bookmarked: $bookmarked")
                } else {
                    _error.postValue("API Error ${response.code()}: Failed to check bookmark status")
                    Log.e(TAG, "Error checking bookmark: ${response.code()} - ${response.message()}")
                }
            }
            override fun onFailure(call: Call<BookmarksResponse>, t: Throwable) {
                _error.postValue("Network error: ${t.message ?: "Failed to check bookmark status"}")
                Log.e(TAG, "Failure checking bookmark status", t)
            }
        })
    }


    fun toggleSelectedFormBookmark() {
        val token = tokenManager.getToken()
        val selectedFormDetail = _selectedArabicForm.value

        if (token == null) {
            _toastMessage.postValue("Please login to bookmark.")
            Log.d(TAG, "toggleSelectedFormBookmark: No token, login required.")
            return
        }
        if (selectedFormDetail == null) {
            _toastMessage.postValue("No Arabic form selected to bookmark.")
            Log.d(TAG, "toggleSelectedFormBookmark: selectedFormDetail is null.")
            return
        }

        val formEntity = selectedFormDetail.entryArabicFormEntity
        val wordTextToBookmark = formEntity.arabicText ?: run {
            _toastMessage.postValue("Cannot bookmark, word text is missing.")
            return
        }
        val currentlyBookmarked = _isBookmarked.value == true
        Log.d(TAG, "Toggling bookmark for form: $wordTextToBookmark. Currently: $currentlyBookmarked")

        val itemProps = mapOf(
            "word_text" to wordTextToBookmark,
            "translation" to (selectedFormDetail.translations.firstOrNull() ?: "N/A"),
            "transliteration" to (selectedFormDetail.transliterations.firstOrNull() ?: "N/A"),
            "total_occurrences" to (formEntity.occurrencesOfThisSpecificArabicForm ?: 0)
        )
        val bookmarkApiRequest = BookmarkRequest(type = "word", itemProperties = itemProps, notes = "")

        if (currentlyBookmarked) {
            quranApi.removeBookmark("Bearer $token", "word", wordTextToBookmark)
                .enqueue(object : Callback<RemoveBookmarkResponse> {
                    override fun onResponse(call: Call<RemoveBookmarkResponse>, response: Response<RemoveBookmarkResponse>) {
                        if (response.isSuccessful) {
                            _isBookmarked.postValue(false)
                            _toastMessage.postValue("Bookmark removed")
                        } else {
                            _toastMessage.postValue("Error removing bookmark: ${response.message()}")
                            Log.e(TAG, "Failed removeBookmark: ${response.code()} - ${response.message()}")
                        }
                    }
                    override fun onFailure(call: Call<RemoveBookmarkResponse>, t: Throwable) {
                        _toastMessage.postValue("Network error removing bookmark")
                        Log.e(TAG, "Failure removeBookmark", t)
                    }
                })
        } else {
            quranApi.addBookmark("Bearer $token", bookmarkApiRequest)
                .enqueue(object : Callback<BookmarkResponse> {
                    override fun onResponse(call: Call<BookmarkResponse>, response: Response<BookmarkResponse>) {
                        if (response.isSuccessful) {
                            _isBookmarked.postValue(true)
                            _toastMessage.postValue("Bookmark added")
                        } else {
                            _toastMessage.postValue("Error adding bookmark: ${response.message()}")
                            Log.e(TAG, "Failed addBookmark: ${response.code()} - ${response.message()}")
                        }
                    }
                    override fun onFailure(call: Call<BookmarkResponse>, t: Throwable) {
                        _toastMessage.postValue("Network error adding bookmark")
                        Log.e(TAG, "Failure addBookmark", t)
                    }
                })
        }
    }
    fun onToastMessageShown(){
        _toastMessage.value = null
    }

    private fun clearAllDataInternal() {
        _analysisEntry.value = null // Renamed
        _arabicForms.value = emptyList()
        _selectedArabicForm.value = null
        _contributingMorphForms.value = emptyList()
        _juzDistribution.value = emptyMap()
        _occurrencesInJuz.value = emptyList()
        currentIdentifierValueForOccurrences = null // Renamed
        _currentWordKey.value = null
        _clickedArabicTextForPreselection.value = null
        currentJuzForOccurrences = null
        currentOccurrencePage = 1
        _hasMoreOccurrences.value = true
        _error.value = null
        _isLoading.value = false
        _isLoadingMoreOccurrences.value = false
        _isBookmarked.value = false
    }
}

data class WordOccurrenceDisplayItem(
    val wordKey: String, // S:A:W
    val surahId: Int,
    val ayahIndex: Int,
    val arabicText: String?, // The specific word form at this occurrence
    val translation: String?, // Translation of this specific word/verse context
    val juzId: Int?,
    val pageId: Int?,
    val fullVerseText: String?, // If you want to show the full verse text in the bottom sheet
    // For navigation to verse:
    val chapterIdString: String,
    val verseNumberString: String
)