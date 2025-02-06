package com.example.qurannexus.features.words.models

import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qurannexus.core.interfaces.QuranApi
import com.example.qurannexus.features.bookmark.models.BookmarkRequest
import com.example.qurannexus.features.bookmark.models.BookmarkResponse
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
            _isLoading.value = true

            quranApi.getWordOccurrences(
                wordText = wordText,
                juzNumber = juzNumber,
                page = page
            ).enqueue(object : Callback<WordOccurrenceResponse> {
                override fun onResponse(
                    call: Call<WordOccurrenceResponse>,
                    response: Response<WordOccurrenceResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        _occurrences.value = response.body()!!.data.words
                    } else {
                        _error.value = "Failed to load occurrences"
                    }
                    _isLoading.value = false
                }

                override fun onFailure(call: Call<WordOccurrenceResponse>, t: Throwable) {
                    _error.value = t.message ?: "Unknown error occurred"
                    _isLoading.value = false
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
                            it.word_text == wordText
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
                val request = BookmarkRequest(
                    type = "word",
                    word_text = wordOccurrence.word_text,
                    translation = wordOccurrence.translation,
                    transliteration = wordOccurrence.transliteration,
                    total_occurrences = totalOccurrences,
                    first_occurrence = FirstOccurrence(
                        word_key = "${wordOccurrence.chapter_id}:${wordOccurrence.verse_number}:${wordOccurrence.position}",
                        chapter_id = wordOccurrence.chapter_id,
                        verse_number = wordOccurrence.verse_number,
                        surah_name = "", // Need to get this from your metadata
                        page_id = "1", // Need to get this from occurrence
                        juz_id = wordOccurrence.juz_number,
                        verse_text = wordOccurrence.verse_text ?: "",
                        audio_url = wordOccurrence.audio_url
                    )
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