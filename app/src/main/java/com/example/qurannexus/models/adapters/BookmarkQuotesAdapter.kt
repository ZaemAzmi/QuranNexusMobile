package com.example.qurannexus.models.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.models.BookmarkQuote

class BookmarkQuotesAdapter (private val chaptersList: List<BookmarkQuote>) : RecyclerView.Adapter<BookmarkQuotesAdapter.BookmarkQuoteViewHolder>() {

    class BookmarkQuoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chapterNumberTextView: TextView = itemView.findViewById(R.id.bookmarkQuoteCardTitle)
        val chapterTitleTextView: TextView = itemView.findViewById(R.id.bookmarkQuoteDescription)
        val chapterInfoTextView: TextView = itemView.findViewById(R.id.bookmarkQuoteSource)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkQuoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_item_bookmark_quote, parent, false)
        return BookmarkQuoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookmarkQuoteViewHolder, position: Int) {
        val chapter = chaptersList[position]
        holder.chapterNumberTextView.text = "${chapter.chapterNumber}"
        holder.chapterTitleTextView.text = chapter.chapterTitle
        holder.chapterInfoTextView.text = chapter.chapterInfo
    }

    override fun getItemCount() = chaptersList.size
}
