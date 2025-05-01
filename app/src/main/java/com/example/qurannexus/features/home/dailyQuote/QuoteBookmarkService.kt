package com.example.qurannexus.features.home.dailyQuote

import android.util.Log
import com.example.qurannexus.core.interfaces.QuranApi
import com.example.qurannexus.features.bookmark.models.BookmarkRequest
import com.example.qurannexus.features.bookmark.models.BookmarkResponse
import com.example.qurannexus.features.bookmark.models.RemoveBookmarkResponse
import com.example.qurannexus.features.words.models.DailyQuote
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuoteBookmarkService @Inject constructor(private val quranApi: QuranApi) {

    interface BookmarkCallback {
        fun onSuccess(message: String)
        fun onError(message: String)
    }

    fun bookmarkQuote(token: String, quote: DailyQuote, callback: BookmarkCallback) {
        // Create item properties for the quote
        val itemProperties = mapOf(
            "quote_id" to quote.Id,
            "title" to quote.Title,
            "description" to quote.Description,
            "source" to quote.Source
        )

        // Create the request
        val request = BookmarkRequest(
            type = "quote",
            itemProperties = itemProperties,
            notes = ""
        )

        // Make the API call
        val call = quranApi.addBookmark("Bearer $token", request)
        call.enqueue(object : Callback<BookmarkResponse> {
            override fun onResponse(call: Call<BookmarkResponse>, response: Response<BookmarkResponse>) {
                if (response.isSuccessful) {
                    val bookmarkResponse = response.body()
                    if (bookmarkResponse != null && bookmarkResponse.status == "success") {
                        callback.onSuccess(bookmarkResponse.message)
                    } else {
                        callback.onError("Failed to bookmark quote: ${bookmarkResponse?.message ?: "Unknown error"}")
                    }
                } else {
                    // Check if it's a duplicate
                    if (response.code() == 409) {
                        callback.onError("This quote is already bookmarked")
                    } else {
                        callback.onError("API Error: ${response.code()}")
                    }
                }
            }

            override fun onFailure(call: Call<BookmarkResponse>, t: Throwable) {
                callback.onError("Network error: ${t.localizedMessage}")
                Log.e("QuoteBookmarkService", "Error bookmarking quote", t)
            }
        })
    }

    fun removeQuoteBookmark(token: String, quoteId: String, callback: BookmarkCallback) {
        val call = quranApi.removeBookmark("Bearer $token", "quote", quoteId)
        call.enqueue(object : Callback<RemoveBookmarkResponse> {
            override fun onResponse(call: Call<RemoveBookmarkResponse>, response: Response<RemoveBookmarkResponse>) {
                if (response.isSuccessful) {
                    val unbookmarkResponse = response.body()
                    if (unbookmarkResponse != null && unbookmarkResponse.status == "success") {
                        callback.onSuccess(unbookmarkResponse.message)
                    } else {
                        callback.onError("Failed to remove bookmark: ${unbookmarkResponse?.message ?: "Unknown error"}")
                    }
                } else {
                    callback.onError("API Error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<RemoveBookmarkResponse>, t: Throwable) {
                callback.onError("Network error: ${t.localizedMessage}")
                Log.e("QuoteBookmarkService", "Error removing quote bookmark", t)
            }
        })
    }

    fun isQuoteBookmarked(quoteId: String, bookmarks: List<Map<String, Any>>?): Boolean {
        if (bookmarks == null) return false

        return bookmarks.any { bookmark ->
            val itemProperties = bookmark["item_properties"] as? Map<*, *>
            itemProperties?.get("quote_id") == quoteId
        }
    }
}