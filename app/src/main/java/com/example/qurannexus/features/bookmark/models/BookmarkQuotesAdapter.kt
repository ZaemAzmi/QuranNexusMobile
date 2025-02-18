package com.example.qurannexus.features.bookmark.models

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R

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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_item_bookmark_quote, parent, false)
        return QuoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuoteViewHolder, position: Int) {
        val quote = quotes[position]

        holder.title.text = quote.itemProperties.quoteTitle
        holder.description.text = quote.itemProperties.quoteDescription
        holder.source.text = quote.itemProperties.quoteSource

        // Handle click events
        holder.container.setOnClickListener { onQuoteClick(quote) }

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

    override fun getItemCount() = quotes.size

    fun updateData(newQuotes: List<BookmarkQuote>) {
        quotes = newQuotes
        notifyDataSetChanged()
    }
}