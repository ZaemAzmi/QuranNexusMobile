package com.example.qurannexus.features.words.models

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.qurannexus.core.database.entities.ArabicFormWithFullDetails
import com.example.qurannexus.core.database.entities.RootEntity
import com.example.qurannexus.core.database.entities.RootFullDetails
import com.example.qurannexus.core.interfaces.QuranApi
import com.example.qurannexus.core.utils.TokenManager
import com.example.qurannexus.features.analysis.data.WordRootDao
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
    private val wordRootDao : WordRootDao,
    private val quranApi: QuranApi,
    private val gson : Gson,
    private val tokenManager: TokenManager
) : AndroidViewModel(application) {

    private val _rootDetails = MutableLiveData<RootEntity?>()
    val rootDetails: LiveData<RootEntity?> = _rootDetails

    private val _arabicForms = MutableLiveData<List<ArabicFormWithFullDetails>>()
    val arabicForms: LiveData<List<ArabicFormWithFullDetails>> = _arabicForms

    // This will hold the Arabic form whose details are currently displayed (e.g., text, translation, transliteration)
    private val _selectedArabicForm = MutableLiveData<ArabicFormWithFullDetails?>()
    val selectedArabicForm: LiveData<ArabicFormWithFullDetails?> = _selectedArabicForm

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

    private var currentOccurrencePage = 1
    private val occurrencesPerPage = 20
    private var currentRootLabelForOccurrences: String? = null
    private var currentJuzForOccurrences: Int? = null
    companion object { // Add a TAG for logging
        private const val TAG = "WordDetailsVM"
    }
    // Entry point for loading data into the ViewModel.
    // Called from WordDetailsActivity.
    // `rootLabelFromIntent`: If navigating directly to a root (e.g., from search).
    // `wordTextFromRecitation`: If navigating by clicking a specific word in recitation text.
    fun loadInitialData(rootLabelFromIntent: String?, wordTextFromRecitation: String?) {
        if (_isLoading.value == true) return
        _isLoading.value = true
        _error.value = null
        clearAllDataInternal() // Clear previous state before loading new

        viewModelScope.launch {
            try {
                val targetRootLabel: String? = if (rootLabelFromIntent != null) {
                    rootLabelFromIntent
                } else if (wordTextFromRecitation != null) {
                    // Find the root for the specific Arabic word text clicked in recitation
                    val roots = wordRootDao.findRootsByArabicFormText(wordTextFromRecitation)
                    // Pick the first root if multiple (rare, but possible if a form is shared)
                    // Or implement disambiguation if necessary.
                    if (roots.isNotEmpty()) roots.first().rootLabel else null
                } else {
                    null
                }

                if (targetRootLabel == null) {
                    _error.postValue("Could not determine the root to load.")
                    return@launch
                }

                currentRootLabelForOccurrences = targetRootLabel // Save for loading occurrences later
                Log.d("ViewModelLoad", "TargetRootLabel: $targetRootLabel, WordTextFromRecitation: $wordTextFromRecitation")
                val fullDetails: RootFullDetails? = wordRootDao.getRootFullDetails(targetRootLabel)

                if (fullDetails != null) {
                    _rootDetails.postValue(fullDetails.rootEntity)
                    _arabicForms.postValue(fullDetails.arabicForms)
                    _contributingMorphForms.postValue(fullDetails.contributingMorphForms)

                    val juzDistMap = fullDetails.juzDistribution
                        .associate { it.juzId.toString() to (it.count ?: 0) }
                    _juzDistribution.postValue(juzDistMap)

                    Log.d("ViewModelLoad", "Available Arabic Forms for $targetRootLabel:")
                    fullDetails.arabicForms.forEachIndexed { index, formDetail ->
                        Log.d("ViewModelLoad", "  Form $index: ${formDetail.arabicFormEntity.arabicText}")
                    }
                    // Determine which Arabic form to select initially
                    val formToSelect = if (wordTextFromRecitation != null) {
                        Log.d("ViewModelLoad", "Attempting to match: '$wordTextFromRecitation'")
                        fullDetails.arabicForms.firstOrNull { it.arabicFormEntity.arabicText == wordTextFromRecitation }
                    } else {
                        null // No specific form was clicked, can default or wait for user selection
                    }
                    if (formToSelect != null) {
                        Log.d("ViewModelLoad", "Matched form from recitation: ${formToSelect.arabicFormEntity.arabicText}")
                    } else if (wordTextFromRecitation != null) {
                        Log.w("ViewModelLoad", "COULD NOT MATCH '$wordTextFromRecitation' with any available forms.")
                    }
                    // If no specific form from recitation, or if not found, select the first form if available
                    _selectedArabicForm.postValue(formToSelect ?: fullDetails.arabicForms.firstOrNull())
                    Log.d("ViewModelLoad", "FINAL _selectedArabicForm posted: ${ (formToSelect ?: fullDetails.arabicForms.firstOrNull())?.arabicFormEntity?.arabicText }")
                } else {
                    _error.postValue("Root details for '$targetRootLabel' not found.")
                }
            } catch (e: Exception) {
                _error.postValue("Error loading root data: ${e.localizedMessage}")
                e.printStackTrace()
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun selectArabicForm(form: ArabicFormWithFullDetails) {
        _selectedArabicForm.value = form
        // Here you could also trigger fetching specific details for this form if they aren't already loaded,
        // e.g., if transliterations/translations were very large and fetched on-demand.
        // In our current DAO, they are fetched with getArabicFormsWithDetails.
    }

    fun loadOccurrencesForJuz(juzNumber: Int, isInitialLoad: Boolean = true) {
        val rootLabel = currentRootLabelForOccurrences ?: return
        if (_isLoadingMoreOccurrences.value == true && !isInitialLoad) return

        if (isInitialLoad) {
            currentOccurrencePage = 1
            _occurrencesInJuz.value = emptyList()
            _hasMoreOccurrences.value = true
        }

        currentJuzForOccurrences = juzNumber
        _isLoadingMoreOccurrences.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val offset = (currentOccurrencePage - 1) * occurrencesPerPage
                val newDbOccurrences = wordRootDao.getOccurrencesForRootInJuz(
                    rootLabel = rootLabel,
                    juzNumber = juzNumber,
                    limit = occurrencesPerPage,
                    offset = offset
                )

                // Map DB entities to display items
                val displayItems = newDbOccurrences.map { occ ->
                    // TODO: Fetch fullVerseText if needed. This would require another DAO call
                    // or having verse text directly in RootWordOccurrenceEntity (which makes it very large).
                    // For now, keeping it null.
                    WordOccurrenceDisplayItem(
                        wordKey = occ.wordKey,
                        surahId = occ.surahId,
                        ayahIndex = occ.ayahIndex,
                        arabicText = occ.arabicText,
                        translation = occ.translation,
                        juzId = occ.juzId,// This comes from RootWordOccurrenceEntity
                        pageId = occ.pageId,
                        fullVerseText = null, // Placeholder
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
                } else if (displayItems.isEmpty() && !isInitialLoad) {
                    _hasMoreOccurrences.postValue(false) // No more items from DB
                }


            } catch (e: Exception) {
                _error.postValue("Error loading occurrences for Juz $juzNumber: ${e.localizedMessage}")
                e.printStackTrace()
                _hasMoreOccurrences.postValue(false) // Stop pagination on error
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
        return _selectedArabicForm.value?.arabicFormEntity?.charactersJson?.let { json ->
            try {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson<List<String>>(json, type)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
    }

    // Helper to parse characters from RootEntity (first occurrence)
    fun getFirstOccurrenceCharacters(): List<String> {
        return _rootDetails.value?.firstOccurrenceCharactersJson?.let { json ->
            try {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson<List<String>>(json, type)
            } catch (e: Exception) {
                emptyList()
            }
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

        val formEntity = selectedFormDetail.arabicFormEntity
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
        _rootDetails.value = null
        _arabicForms.value = emptyList()
        _selectedArabicForm.value = null
        _contributingMorphForms.value = emptyList()
        _juzDistribution.value = emptyMap()
        _occurrencesInJuz.value = emptyList()
        currentRootLabelForOccurrences = null
        currentJuzForOccurrences = null
        currentOccurrencePage = 1
        _hasMoreOccurrences.value = true
        _error.value = null
        _isLoading.value = false
        _isLoadingMoreOccurrences.value = false
        _isBookmarked.value = false // Reset bookmark status
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