package com.example.qurannexus.features.words.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qurannexus.core.interfaces.QuranApi
import com.example.qurannexus.features.bookmark.models.BookmarkRequest
import com.example.qurannexus.features.bookmark.models.BookmarkResponse
import com.example.qurannexus.features.bookmark.models.BookmarksResponse
import com.example.qurannexus.features.bookmark.models.RemoveBookmarkResponse
import com.example.qurannexus.features.words.services.WordJsonService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class WordDetailsViewModel @Inject constructor(
    private val wordJsonService: WordJsonService,
    private val quranApi: QuranApi
) : ViewModel() {

    private val _wordDetails = MutableLiveData<WordDetails>()
    val wordDetails: LiveData<WordDetails> = _wordDetails

    private val _isBookmarked = MutableLiveData<Boolean>()
    val isBookmarked: LiveData<Boolean> = _isBookmarked

    private val _occurrencesInJuz = MutableLiveData<List<WordOccurrence>>()
    val occurrencesInJuz: LiveData<List<WordOccurrence>> = _occurrencesInJuz

    private var wordId: String? = null

    fun loadWordDetails(wordId: String, token: String) {
        this.wordId = wordId
        viewModelScope.launch {
            try {
                // Load word details from JSON
                wordJsonService.getWordDetails(wordId)?.let { word ->
                    _wordDetails.value = word
                }

                // Check bookmark status via API
                checkBookmarkStatus(token)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun checkBookmarkStatus(token: String) {
        viewModelScope.launch {
            try {
                quranApi.getBookmarks("Bearer $token").enqueue(object : Callback<BookmarksResponse> {
                    override fun onResponse(call: Call<BookmarksResponse>, response: Response<BookmarksResponse>) {
                        if (response.isSuccessful) {
                            val bookmarks = response.body()?.bookmarks?.words ?: emptyList()
                            _isBookmarked.value = bookmarks.any { it.word_id == wordId }
                        }
                    }

                    override fun onFailure(call: Call<BookmarksResponse>, t: Throwable) {
                        // Handle error
                    }
                })
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun addWordBookmark(token: String, word: WordDetails) {
        viewModelScope.launch {
            try {
                val request = BookmarkRequest(
                    type = "word",
                    item_id = wordId!!,
                    word_text = word.word_text,
                    translation = word.translation,
                    transliteration = word.transliteration,
                    surah_name = word.first_occurrence.surah_name,
                    ayah_key = word.first_occurrence.ayah_key
                )
                quranApi.addBookmark("Bearer $token", request).enqueue(object : Callback<BookmarkResponse> {
                    override fun onResponse(call: Call<BookmarkResponse>, response: Response<BookmarkResponse>) {
                        if (response.isSuccessful) {
                            _isBookmarked.value = true
                        }
                    }

                    override fun onFailure(call: Call<BookmarkResponse>, t: Throwable) {
                        // Handle error
                    }
                })
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun removeWordBookmark(token: String) {
        viewModelScope.launch {
            try {
                quranApi.removeBookmark("Bearer $token", "word", wordId!!).enqueue(object : Callback<RemoveBookmarkResponse> {
                    override fun onResponse(call: Call<RemoveBookmarkResponse>, response: Response<RemoveBookmarkResponse>) {
                        if (response.isSuccessful) {
                            _isBookmarked.value = false
                        }
                    }

                    override fun onFailure(call: Call<RemoveBookmarkResponse>, t: Throwable) {
                        // Handle error
                    }
                })
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun getWordOccurrencesInJuz(juzNumber: Int) {
        viewModelScope.launch {
            try {
                wordId?.let { id ->
                    val occurrences = wordJsonService.getWordOccurrencesInJuz(id, juzNumber)
                    _occurrencesInJuz.value = occurrences
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun loadJuzDistribution(){

    }
}