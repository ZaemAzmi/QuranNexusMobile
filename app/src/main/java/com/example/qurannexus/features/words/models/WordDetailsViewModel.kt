package com.example.qurannexus.features.words.models

import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qurannexus.core.interfaces.QuranApi
import com.example.qurannexus.core.utils.QuranMetadata
import com.example.qurannexus.core.utils.SurahDetails
import com.example.qurannexus.features.bookmark.models.BookmarkRequest
import com.example.qurannexus.features.bookmark.models.BookmarkResponse
import com.example.qurannexus.features.bookmark.models.BookmarkWord
import com.example.qurannexus.features.bookmark.models.BookmarksResponse
import com.example.qurannexus.features.bookmark.models.FirstOccurrence
import com.example.qurannexus.features.bookmark.models.RemoveBookmarkResponse
import com.example.qurannexus.features.words.services.WordJsonService
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class WordDetailsViewModel @Inject constructor(
    private val quranApi: QuranApi
) : ViewModel() {

    private val _wordDistribution = MutableLiveData<WordDistributionData>()
    val wordDistribution: LiveData<WordDistributionData> = _wordDistribution

    private val _occurrences = MutableLiveData<List<WordOccurrence>>()
    val occurrences: LiveData<List<WordOccurrence>> = _occurrences

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isBookmarked = MutableLiveData<Boolean>()
    val isBookmarked: LiveData<Boolean> = _isBookmarked

    private val _isLoadingMore = MutableLiveData<Boolean>()
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private val _hasMorePages = MutableLiveData<Boolean>()
    val hasMorePages: LiveData<Boolean> = _hasMorePages

    private var currentWordText: String? = null

    fun loadWordData(wordText: String) {
        _isLoading.value = true
        currentWordText = wordText

        // Load distribution
        quranApi.getWordDistribution(wordText)
            .enqueue(object : Callback<WordDistributionResponse> {
                override fun onResponse(
                    call: Call<WordDistributionResponse>,
                    response: Response<WordDistributionResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        _wordDistribution.value = response.body()!!.data
                    } else {
                        _error.value = "Failed to load word distribution"
                    }
                    _isLoading.value = false
                }

                override fun onFailure(call: Call<WordDistributionResponse>, t: Throwable) {
                    _error.value = t.message ?: "Unknown error occurred"
                    _isLoading.value = false
                }
            })
    }

    fun loadOccurrencesForJuz(juzNumber: Int, page: Int = 1) {
        currentWordText?.let { wordText ->
            _isLoadingMore.value = true

            quranApi.getWordOccurrences(
                wordText = wordText,
                juzNumber = juzNumber,
                page = page,
                perPage = 20
            ).enqueue(object : Callback<WordOccurrenceResponse> {
                override fun onResponse(
                    call: Call<WordOccurrenceResponse>,
                    response: Response<WordOccurrenceResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val data = response.body()!!.data
                        _hasMorePages.value = data.pagination.current_page < data.pagination.total_pages
                        _occurrences.value = if (page == 1) {
                            data.words
                        } else {
                            // Combine existing and new occurrences for pagination
                            _occurrences.value?.plus(data.words) ?: data.words
                        }
                    } else {
                        _error.value = "Failed to load occurrences"
                    }
                    _isLoadingMore.value = false
                }

                override fun onFailure(call: Call<WordOccurrenceResponse>, t: Throwable) {
                    _error.value = t.message ?: "Unknown error occurred"
                    _isLoadingMore.value = false
                }
            })
        }
    }

    fun checkBookmarkStatus(token: String, wordText: String) {
        viewModelScope.launch {
            quranApi.getBookmarks("Bearer $token").enqueue(object : Callback<BookmarksResponse> {
                override fun onResponse(call: Call<BookmarksResponse>, response: Response<BookmarksResponse>) {
                    if (response.isSuccessful) {
                        _isBookmarked.value = response.body()?.bookmarks?.words?.any {
                            it.itemProperties.wordText == wordText
                        } ?: false
                    }
                }

                override fun onFailure(call: Call<BookmarksResponse>, t: Throwable) {
                    _error.value = t.message ?: "Failed to check bookmark status"
                }
            })
        }
    }

    fun addWordBookmark(token: String, wordOccurrence: WordOccurrence, totalOccurrences: Int) {
        viewModelScope.launch {
            try {
                val surahDetails = QuranMetadata.getInstance().getSurahDetails(Integer.valueOf(wordOccurrence.chapter_id))
                // First create the first_occurrence map with non-null values
                val firstOccurrence = mapOf(
                    "word_key" to "${wordOccurrence.chapter_id}:${wordOccurrence.verse_number}:${wordOccurrence.position}",
                    "chapter_id" to wordOccurrence.chapter_id,
                    "verse_number" to wordOccurrence.verse_number,
                    "surah_name" to (surahDetails?.englishName ?: ""), // Provide default empty string
                    "page_id" to (surahDetails?.startingPage ?: "1"),
                    "juz_id" to wordOccurrence.juz_number,
                    "verse_text" to (wordOccurrence.verse_text ?: ""),
                    "audio_url" to (wordOccurrence.audio_url ?: "")
                )

                // Then create the main properties map
                val itemProperties = hashMapOf<String, Any>(
                    "word_text" to (wordOccurrence.word_text ?: ""),
                    "translation" to (wordOccurrence.translation ?: ""),
                    "transliteration" to (wordOccurrence.transliteration ?: ""),
                    "total_occurrences" to totalOccurrences,
                    "first_occurrence" to firstOccurrence
                )

                val request = BookmarkRequest(
                    type = "word",
                    itemProperties = itemProperties,
                    notes = ""
                )

                quranApi.addBookmark("Bearer $token", request).enqueue(object : Callback<BookmarkResponse> {
                    override fun onResponse(call: Call<BookmarkResponse>, response: Response<BookmarkResponse>) {
                        if (response.isSuccessful) {
                            _isBookmarked.value = true
                        } else {
                            _error.value = "Failed to bookmark word"
                        }
                    }

                    override fun onFailure(call: Call<BookmarkResponse>, t: Throwable) {
                        _error.value = t.message ?: "Error bookmarking word"
                    }
                })
            } catch (e: Exception) {
                _error.value = e.message ?: "Error bookmarking word"
            }
        }
    }


    fun removeWordBookmark(token: String, wordText: String) {
        viewModelScope.launch {
            try {
                // Now using word_text instead of word_id
                quranApi.removeBookmark("Bearer $token", "word", wordText)
                    .enqueue(object : Callback<RemoveBookmarkResponse> {
                        override fun onResponse(call: Call<RemoveBookmarkResponse>, response: Response<RemoveBookmarkResponse>) {
                            if (response.isSuccessful) {
                                _isBookmarked.value = false
                            } else {
                                _error.value = "Failed to remove bookmark"
                            }
                        }

                        override fun onFailure(call: Call<RemoveBookmarkResponse>, t: Throwable) {
                            _error.value = t.message ?: "Error removing bookmark"
                        }
                    })
            } catch (e: Exception) {
                _error.value = e.message ?: "Error removing bookmark"
            }
        }
    }

}