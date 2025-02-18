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
class WordManagementViewModel @Inject constructor(
    private val quranApi: QuranApi,
    private val wordJsonService: WordJsonService
) : ViewModel() {

    private val _dailyWord = MutableLiveData<WordDetails>()
    val dailyWord: LiveData<WordDetails> = _dailyWord

    private val _bookmarkStatus = MutableLiveData<BookmarkResponse>()
    val bookmarkStatus: LiveData<BookmarkResponse> = _bookmarkStatus
    private val _bookmarks = MutableLiveData<BookmarksResponse>()
    val bookmarks: LiveData<BookmarksResponse> = _bookmarks
    fun getDailyWord(userId: String) {
        viewModelScope.launch {
            try {
                wordJsonService.getDailyWord(userId)?.let { word ->
                    _dailyWord.value = word
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun addBookmark(token: String, request: BookmarkRequest) {
        viewModelScope.launch {
            try {
                quranApi.addBookmark(token, request).enqueue(object : Callback<BookmarkResponse> {
                    override fun onResponse(call: Call<BookmarkResponse>, response: Response<BookmarkResponse>) {
                        _bookmarkStatus.value = response.body()
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
    fun getBookmarks(token: String): LiveData<BookmarksResponse> {
        viewModelScope.launch {
            try {
                quranApi.getBookmarks("Bearer $token").enqueue(object : Callback<BookmarksResponse> {
                    override fun onResponse(
                        call: Call<BookmarksResponse>,
                        response: Response<BookmarksResponse>
                    ) {
                        if (response.isSuccessful) {
                            _bookmarks.value = response.body()
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
        return bookmarks
    }
    fun removeBookmark(token: String, type: String, itemId: String) {
        viewModelScope.launch {
            try {
                quranApi.removeBookmark(token, type, itemId).enqueue(object : Callback<RemoveBookmarkResponse> {
                    override fun onResponse(call: Call<RemoveBookmarkResponse>, response: Response<RemoveBookmarkResponse>) {
                        if (response.isSuccessful) {
                            // Handle success
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

    fun checkBookmarkStatus(token: String, wordText: String): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        viewModelScope.launch {
            try {
                quranApi.getBookmarks(token).enqueue(object : Callback<BookmarksResponse> {
                    override fun onResponse(call: Call<BookmarksResponse>, response: Response<BookmarksResponse>) {
                        if (response.isSuccessful) {
                            val isBookmarked = response.body()?.bookmarks?.words?.any {
                                it.itemProperties.wordText == wordText
                            } ?: false
                            result.value = isBookmarked
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
        return result
    }
}