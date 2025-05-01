package com.example.qurannexus.features.home.models

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.features.home.dailyQuote.QuoteBookmarkService
import com.example.qurannexus.features.home.dailyQuote.ShareCustomQuoteActivity
import com.example.qurannexus.features.words.models.DailyQuote

class DailyInspirationAdapter(
    private val inspirationQuotes: List<DailyQuote>,
    private val context: Context,
    private val bookmarkService: QuoteBookmarkService? = null,
    private val token: String? = null,
    private val bookmarkedQuoteIds: MutableSet<String> = mutableSetOf()
) : RecyclerView.Adapter<DailyInspirationAdapter.InspirationViewHolder>() {

    // SharedPreferences for caching bookmark state
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "QuotePreferences", Context.MODE_PRIVATE
    )

    class InspirationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val quoteTextView: TextView = itemView.findViewById(R.id.tvQuote)
        val sourceTextView: TextView = itemView.findViewById(R.id.tvSource)
        val btnBookmark: ImageButton = itemView.findViewById(R.id.btnBookmark)
        val btnShare: Button = itemView.findViewById(R.id.btnShare)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InspirationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_item_daily_quote, parent, false)
        return InspirationViewHolder(view)
    }

    override fun onBindViewHolder(holder: InspirationViewHolder, position: Int) {
        val quote = inspirationQuotes[position]

        // Log to debug
        Log.d("DailyInspirationAdapter", "Binding quote ID: ${quote.Id}")

        // Set the quote description
        holder.quoteTextView.text = quote.Description

        // Set the quote source
        holder.sourceTextView.text = quote.Source

        // Check if this quote is bookmarked and update icon immediately
        val isBookmarked = isQuoteBookmarked(quote.Id)
        Log.d("DailyInspirationAdapter", "Quote ${quote.Id} is bookmarked: $isBookmarked")
        updateBookmarkIcon(holder.btnBookmark, isBookmarked)

        // Set up bookmark button click listener
        holder.btnBookmark.setOnClickListener {
            Log.d("DailyInspirationAdapter", "Bookmark button clicked for quote ID: ${quote.Id}")
            toggleBookmark(quote, holder.btnBookmark)
        }

        // Set up share button click listener
        holder.btnShare.setOnClickListener {
            navigateToCustomizationActivity(quote)
        }
    }

    private fun updateBookmarkIcon(bookmarkButton: ImageButton, isBookmarked: Boolean) {
        Log.d("DailyInspirationAdapter", "Updating icon to: ${if (isBookmarked) "bookmarked" else "not bookmarked"}")
        val iconRes = if (isBookmarked) R.drawable.ic_heart_bookmarked else R.drawable.ic_heart
        bookmarkButton.setImageResource(iconRes)
    }

    private fun isQuoteBookmarked(quoteId: String): Boolean {
        // First check local cache
        val cachedValue = sharedPreferences.getBoolean("quote_$quoteId", false)

        // Also check against the bookmarked quote IDs from the API
        val isBookmarkedInApi = bookmarkedQuoteIds.contains(quoteId)

        Log.d("DailyInspirationAdapter", "Quote $quoteId - Cached: $cachedValue, API: $isBookmarkedInApi")

        // Sync local state with API state if they differ
        if (isBookmarkedInApi && !cachedValue) {
            toggleLocalBookmarkState(quoteId, true)
            Log.d("DailyInspirationAdapter", "Syncing local state to match API for quote $quoteId")
        }

        return cachedValue || isBookmarkedInApi
    }

    private fun toggleBookmark(quote: DailyQuote, bookmarkButton: ImageButton) {
        val quoteId = quote.Id
        val isCurrentlyBookmarked = isQuoteBookmarked(quoteId)

        Log.d("DailyInspirationAdapter", "Toggling bookmark for quote $quoteId, current state: $isCurrentlyBookmarked")

        // If we don't have a bookmark service or token, just toggle local state
        if (bookmarkService == null || token == null) {
            val newState = !isCurrentlyBookmarked
            toggleLocalBookmarkState(quoteId, newState)
            updateBookmarkIcon(bookmarkButton, newState)

            // Show message
            val message = if (newState) "Quote bookmarked locally (login to sync)" else "Bookmark removed locally"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            return
        }

        // We have a bookmark service, so use the API
        if (!isCurrentlyBookmarked) {
            // Add bookmark
            bookmarkService.bookmarkQuote(token, quote, object : QuoteBookmarkService.BookmarkCallback {
                override fun onSuccess(message: String) {
                    // Update local state
                    toggleLocalBookmarkState(quoteId, true)
                    bookmarkedQuoteIds.add(quoteId)
                    Log.d("DailyInspirationAdapter", "Successfully bookmarked quote $quoteId")

                    // Update UI on main thread
                    val activity = (context as? androidx.activity.ComponentActivity)
                    activity?.runOnUiThread {
                        updateBookmarkIcon(bookmarkButton, true)
                        Toast.makeText(context, "Quote bookmarked", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(message: String) {
                    Log.e("DailyInspirationAdapter", "Error bookmarking quote $quoteId: $message")
                    // Show error message
                    val activity = (context as? androidx.activity.ComponentActivity)
                    activity?.runOnUiThread {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            })
        } else {
            // Remove bookmark
            bookmarkService.removeQuoteBookmark(token, quoteId, object : QuoteBookmarkService.BookmarkCallback {
                override fun onSuccess(message: String) {
                    // Update local state
                    toggleLocalBookmarkState(quoteId, false)
                    bookmarkedQuoteIds.remove(quoteId)
                    Log.d("DailyInspirationAdapter", "Successfully removed bookmark for quote $quoteId")

                    // Update UI on main thread
                    val activity = (context as? androidx.activity.ComponentActivity)
                    activity?.runOnUiThread {
                        updateBookmarkIcon(bookmarkButton, false)
                        Toast.makeText(context, "Bookmark removed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(message: String) {
                    Log.e("DailyInspirationAdapter", "Error removing bookmark for quote $quoteId: $message")
                    // Show error message
                    val activity = (context as? androidx.activity.ComponentActivity)
                    activity?.runOnUiThread {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }

    private fun toggleLocalBookmarkState(quoteId: String, isBookmarked: Boolean) {
        Log.d("DailyInspirationAdapter", "Setting local bookmark state for quote $quoteId to: $isBookmarked")
        sharedPreferences.edit()
            .putBoolean("quote_$quoteId", isBookmarked)
            .apply()
    }

    private fun navigateToCustomizationActivity(quote: DailyQuote) {
        val intent = Intent(context, ShareCustomQuoteActivity::class.java)
        intent.putExtra("DAILY_QUOTE_ID", quote.Id)
        intent.putExtra("DAILY_QUOTE_DESCRIPTION", quote.Description)
        intent.putExtra("DAILY_QUOTE_SOURCE", quote.Source)
        context.startActivity(intent)
    }

    override fun getItemCount(): Int = inspirationQuotes.size
}