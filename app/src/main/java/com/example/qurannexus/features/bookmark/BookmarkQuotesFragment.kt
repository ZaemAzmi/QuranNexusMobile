package com.example.qurannexus.features.bookmark

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.core.interfaces.QuranApi
import com.example.qurannexus.features.bookmark.models.BookmarkQuote
import com.example.qurannexus.features.bookmark.models.BookmarkQuotesAdapter
import com.example.qurannexus.features.bookmark.models.BookmarksResponse
import com.example.qurannexus.features.bookmark.models.RemoveBookmarkResponse
import com.example.qurannexus.features.home.dailyQuote.ShareCustomQuoteActivity
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

@AndroidEntryPoint
class BookmarkQuotesFragment : Fragment() {

    @Inject
    lateinit var quranApi: QuranApi

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateView: TextView
    private lateinit var adapter: BookmarkQuotesAdapter

    private var userToken: String? = null
    private var bookmarkedQuotes = listOf<BookmarkQuote>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_bookmark_quotes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        recyclerView = view.findViewById(R.id.bookmarkQuotesRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        emptyStateView = view.findViewById(R.id.emptyStateTextView)

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Initialize adapter with empty list and click handlers
        adapter = BookmarkQuotesAdapter(
            emptyList(),
            onQuoteClick = { quote -> handleQuoteClick(quote) },
            onDeleteClick = { quote -> removeBookmark(quote) }
        )
        recyclerView.adapter = adapter

        // Get user token
        getUserToken()

        // Fetch bookmarked quotes
        fetchBookmarkedQuotes()
    }

    private fun getUserToken() {
        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        userToken = sharedPreferences.getString("token", null)
    }

    private fun fetchBookmarkedQuotes() {
        // Show loading state
        showLoading(true)

        // Check if we have a token
        if (userToken == null) {
            showEmptyState("Please log in to view your bookmarked quotes")
            showLoading(false)
            return
        }

        // Make API call to get bookmarks
        val call = quranApi.getBookmarks("Bearer $userToken")
        call.enqueue(object : Callback<BookmarksResponse> {
            override fun onResponse(call: Call<BookmarksResponse>, response: Response<BookmarksResponse>) {
                if (response.isSuccessful) {
                    val bookmarksResponse = response.body()
                    if (bookmarksResponse != null && bookmarksResponse.status == "success") {
                        // Get quotes from the response
                        bookmarkedQuotes = bookmarksResponse.bookmarks.quotes

                        Log.d("BookmarkQuotesFragment", "Fetched ${bookmarkedQuotes.size} quotes")
                        bookmarkedQuotes.forEach { quote ->
                            Log.d("BookmarkQuotesFragment", "Quote: ${quote.itemProperties.quoteDescription}, Source: ${quote.itemProperties.quoteSource}")
                        }

                        // Update UI
                        activity?.runOnUiThread {
                            if (bookmarkedQuotes.isEmpty()) {
                                showEmptyState("No bookmarked quotes yet")
                            } else {
                                hideEmptyState()
                                adapter.updateData(bookmarkedQuotes)
                            }
                            showLoading(false)
                        }
                    } else {
                        showEmptyState("Failed to load bookmarked quotes")
                        showLoading(false)
                    }
                } else {
                    showEmptyState("Error: ${response.code()}")
                    showLoading(false)
                    Log.e("BookmarkQuotes", "API Error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<BookmarksResponse>, t: Throwable) {
                showEmptyState("Network error. Please try again.")
                showLoading(false)
                Log.e("BookmarkQuotes", "Network error", t)
            }
        })
    }

    private fun removeBookmark(quote: BookmarkQuote) {
        if (userToken == null) {
            return
        }

        val quoteId = quote.itemProperties.quoteId
        val call = quranApi.removeBookmark("Bearer $userToken", "quote", quoteId)

        call.enqueue(object : Callback<RemoveBookmarkResponse> {
            override fun onResponse(call: Call<RemoveBookmarkResponse>, response: Response<RemoveBookmarkResponse>) {
                if (response.isSuccessful) {
                    // Remove the quote from our local list
                    val updatedQuotes = bookmarkedQuotes.filter { it.itemProperties.quoteId != quoteId }
                    bookmarkedQuotes = updatedQuotes

                    // Update the UI
                    activity?.runOnUiThread {
                        if (updatedQuotes.isEmpty()) {
                            showEmptyState("No bookmarked quotes yet")
                        }
                        adapter.updateData(updatedQuotes)
                        Toast.makeText(context, "Quote removed from bookmarks", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Failed to remove bookmark", Toast.LENGTH_SHORT).show()
                    }
                    Log.e("BookmarkQuotes", "Failed to remove bookmark: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<RemoveBookmarkResponse>, t: Throwable) {
                activity?.runOnUiThread {
                    Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                }
                Log.e("BookmarkQuotes", "Network error removing bookmark", t)
            }
        })
    }

    private fun handleQuoteClick(quote: BookmarkQuote) {
        // Navigate to ShareCustomQuoteActivity
        val intent = Intent(context, ShareCustomQuoteActivity::class.java)
        intent.putExtra("DAILY_QUOTE_ID", quote.itemProperties.quoteId)
        intent.putExtra("DAILY_QUOTE_DESCRIPTION", quote.itemProperties.quoteDescription)
        intent.putExtra("DAILY_QUOTE_SOURCE", quote.itemProperties.quoteSource)
        startActivity(intent)
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showEmptyState(message: String) {
        emptyStateView.text = message
        emptyStateView.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun hideEmptyState() {
        emptyStateView.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }

    // Refresh the quotes when the fragment becomes visible
    override fun onResume() {
        super.onResume()
        fetchBookmarkedQuotes()
    }
}