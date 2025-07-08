package com.example.qurannexus.features.bookmark.models

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.features.home.dailyQuote.ShareCustomQuoteActivity

class BookmarkQuotesAdapter(
    private var quotes: List<BookmarkQuote>,
    private val onQuoteClick: (BookmarkQuote) -> Unit,
    private val onDeleteClick: (BookmarkQuote) -> Unit
) : RecyclerView.Adapter<BookmarkQuotesAdapter.QuoteViewHolder>() {

    inner class QuoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.bookmarkQuoteCardTitle)
        val description: TextView = itemView.findViewById(R.id.bookmarkQuoteDescription)
        val source: TextView = itemView.findViewById(R.id.bookmarkQuoteSource)
        val container: CardView = itemView.findViewById(R.id.quoteCard)
        val shareButton: ImageView = itemView.findViewById(R.id.bookmarkQuoteCardArrowIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_item_bookmark_quote, parent, false)
        return QuoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuoteViewHolder, position: Int) {
        val quote = quotes[position]

        try {
            // Set title with null safety
            val title = if (quote.itemProperties.quoteTitle.isNullOrEmpty()) {
                "Daily Quote"
            } else {
                quote.itemProperties.quoteTitle
            }
            holder.title.text = title

            // Set description with null safety
            holder.description.text = quote.itemProperties.quoteDescription ?: "No description available"

            // Set source with null safety
            holder.source.text = quote.itemProperties.quoteSource ?: ""

            // Log for debugging
            Log.d("BookmarkQuotesAdapter", "Quote: ${quote.itemProperties.quoteDescription}, Source: ${quote.itemProperties.quoteSource}")
        } catch (e: Exception) {
            // Catch any exceptions and log them
            Log.e("BookmarkQuotesAdapter", "Error binding quote data", e)
            holder.title.text = "Daily Quote"
            holder.description.text = "Could not load quote"
            holder.source.text = ""
        }

        // Handle click events
        holder.container.setOnClickListener {
            onQuoteClick(quote)
        }

        // Add share functionality using arrow icon
        holder.shareButton.setOnClickListener {
            navigateToShareActivity(holder.itemView.context, quote)
        }
        //TODO : change item touch helper to declare once in fragment, so it wont be created everytime it binds the items
        // Setup swipe-to-delete
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                onDeleteClick(quotes[position])
            }
        }).attachToRecyclerView(holder.container.parent as? RecyclerView)
    }

    private fun navigateToShareActivity(context: Context, quote: BookmarkQuote) {
        val intent = Intent(context, ShareCustomQuoteActivity::class.java)
        intent.putExtra("DAILY_QUOTE_ID", quote.itemProperties.quoteId)
        intent.putExtra("DAILY_QUOTE_DESCRIPTION", quote.itemProperties.quoteDescription)
        intent.putExtra("DAILY_QUOTE_SOURCE", quote.itemProperties.quoteSource)
        context.startActivity(intent)
    }

    override fun getItemCount() = quotes.size

    fun updateData(newQuotes: List<BookmarkQuote>) {
        quotes = newQuotes
        notifyDataSetChanged()
    }
}