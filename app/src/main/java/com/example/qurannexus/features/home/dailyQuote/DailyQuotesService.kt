package com.example.qurannexus.features.home.dailyQuote

import android.util.Log
import com.example.qurannexus.core.interfaces.QuranApi
import com.example.qurannexus.features.words.models.DailyQuote
import com.example.qurannexus.features.words.models.DailyQuoteListResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyQuotesService @Inject constructor(private val quranApi: QuranApi) {

    interface DailyQuotesCallback {
        fun onQuotesReceived(quotes: List<DailyQuote>)
        fun onError(message: String)
    }

    fun fetchDailyQuotes(callback: DailyQuotesCallback) {
        val call = quranApi.getDailyQuotes()
        call.enqueue(object : Callback<DailyQuoteListResponse> {
            override fun onResponse(call: Call<DailyQuoteListResponse>, response: Response<DailyQuoteListResponse>) {
                if (response.isSuccessful) {
                    val quoteResponse = response.body()
                    if (quoteResponse != null) {
                        val quotes = quoteResponse.data ?: emptyList()
                        callback.onQuotesReceived(quotes)
                    } else {
                        callback.onError("Failed to get quotes: Response body was null")
                    }
                } else {
                    callback.onError("API Error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<DailyQuoteListResponse>, t: Throwable) {
                callback.onError("Network error: ${t.localizedMessage}")
                Log.e("DailyQuotesService", "Error fetching quotes", t)
            }
        })
    }
}