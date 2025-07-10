package com.example.qurannexus.features.home.models

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
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
    // MODIFIED: Added a Handler for main thread operations to avoid casting context to an activity.
    // This is a cleaner and safer approach.
    private val mainThreadHandler = Handler(Looper.getMainLooper())

    class InspirationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val quoteTextView: TextView = itemView.findViewById(R.id.tvQuote)
        val sourceTextView: TextView = itemView.findViewById(R.id.tvSource)
        val btnBookmark: ImageView = itemView.findViewById(R.id.btnBookmark)
        val btnShare: Button = itemView.findViewById(R.id.btnShare)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InspirationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_item_daily_quote, parent, false)
        return InspirationViewHolder(view)
    }

    override fun onBindViewHolder(holder: InspirationViewHolder, position: Int) {
        val quote = inspirationQuotes[position]

        Log.d("DailyInspirationAdapter", "Binding quote ID: ${quote.Id}")

        holder.quoteTextView.text = quote.Description
        holder.sourceTextView.text = quote.Source
        // 1. Determine if the current quote is a default/placeholder quote.
        val isDefaultQuote = quote.Id.startsWith("default")

        // 2. Control the visibility and functionality of the bookmark button.
        if (isDefaultQuote || token == null) {
            // Hide the bookmark button entirely if it's a default quote or the user is not logged in.
            holder.btnBookmark.visibility = View.GONE
        } else {
            // Only show the button and set up listeners for real quotes when a user is logged in.
            holder.btnBookmark.visibility = View.VISIBLE

            // Check if this quote is bookmarked and update icon immediately
            val isBookmarked = isQuoteBookmarked(quote.Id)
            Log.d("DailyInspirationAdapter", "Quote ${quote.Id} is bookmarked: $isBookmarked")
            updateBookmarkIcon(holder.btnBookmark, isBookmarked)

            // Set up bookmark button click listener
            holder.btnBookmark.setOnClickListener {
                // Ensure the position is valid before proceeding
                if (holder.adapterPosition != RecyclerView.NO_POSITION) {
                    // MODIFIED: Pass the adapter position instead of the button view.
                    toggleBookmark(quote, holder.adapterPosition)
                }
            }
        }
        // Set up share button click listener (this part is fine)
        holder.btnShare.setOnClickListener {
            navigateToCustomizationActivity(quote)
        }
    }

    private fun updateBookmarkIcon(bookmarkButton: ImageView, isBookmarked: Boolean) {
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

    private fun toggleBookmark(quote: DailyQuote, position: Int) {
        val quoteId = quote.Id
        val isCurrentlyBookmarked = isQuoteBookmarked(quoteId)

        // Optimistic UI Update: Change the state immediately for a responsive feel.
        // We will revert it if the API call fails.
        val newBookmarkState = !isCurrentlyBookmarked
        updateLocalState(quoteId, newBookmarkState)
        notifyItemChanged(position) // This triggers onBindViewHolder to redraw the item instantly.

        // If no service or token, the local update is all we do.
        if (bookmarkService == null || token == null) {
            val message = if (newBookmarkState) "Quote bookmarked locally" else "Bookmark removed locally"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            return
        }

        // Now, perform the network operation.
        if (newBookmarkState) {
            // Add bookmark via API
            bookmarkService.bookmarkQuote(token, quote, object : QuoteBookmarkService.BookmarkCallback {
                override fun onSuccess(message: String) {
                    mainThreadHandler.post {
                        Log.d("DailyInspirationAdapter", "Successfully bookmarked quote $quoteId on server.")
                        Toast.makeText(context, "Quote bookmarked", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(message: String) {
                    mainThreadHandler.post {
                        Log.e("DailyInspirationAdapter", "Error bookmarking quote $quoteId: $message")
                        // Revert the state since the API call failed
                        updateLocalState(quoteId, false)
                        notifyItemChanged(position)
                        Toast.makeText(context, "Failed to bookmark: $message", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        } else {
            // Remove bookmark via API
            bookmarkService.removeQuoteBookmark(token, quoteId, object : QuoteBookmarkService.BookmarkCallback {
                override fun onSuccess(message: String) {
                    mainThreadHandler.post {
                        Log.d("DailyInspirationAdapter", "Successfully removed bookmark for quote $quoteId on server.")
                        Toast.makeText(context, "Bookmark removed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(message: String) {
                    mainThreadHandler.post {
                        Log.e("DailyInspirationAdapter", "Error removing bookmark for quote $quoteId: $message")
                        // Revert the state since the API call failed
                        updateLocalState(quoteId, true)
                        notifyItemChanged(position)
                        Toast.makeText(context, "Failed to remove bookmark: $message", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }
    private fun updateLocalState(quoteId: String, isBookmarked: Boolean) {
        // Update both sources of truth
        if (isBookmarked) {
            bookmarkedQuoteIds.add(quoteId)
        } else {
            bookmarkedQuoteIds.remove(quoteId)
        }
        sharedPreferences.edit().putBoolean("quote_$quoteId", isBookmarked).apply()
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