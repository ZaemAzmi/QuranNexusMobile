package com.example.qurannexus.models.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.models.BookmarkHistory

class BookmarkHistoriesAdapter (private val chaptersList: List<BookmarkHistory>) : RecyclerView.Adapter<BookmarkHistoriesAdapter.BookmarkHistoryViewHolder>() {

    class BookmarkHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chapterNumberTextView: TextView = itemView.findViewById(R.id.bookmarkHistoryChapterNumber)
        val chapterTitleTextView: TextView = itemView.findViewById(R.id.bookmarkHistoryChapterTitle)
        val chapterInfoTextView: TextView = itemView.findViewById(R.id.bookmarkHistoryDateRead)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkHistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_item_bookmark_history, parent, false)
        return BookmarkHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookmarkHistoryViewHolder, position: Int) {
        val chapter = chaptersList[position]
        holder.chapterNumberTextView.text = "${chapter.chapterNumber}"
        holder.chapterTitleTextView.text = chapter.chapterTitle
        holder.chapterInfoTextView.text = chapter.chapterInfo
    }

    override fun getItemCount() = chaptersList.size
}
