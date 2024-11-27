package com.example.qurannexus.features.bookmark.models

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R

class BookmarkVersesAdapter (private val chaptersList: List<BookmarkVerse>) : RecyclerView.Adapter<BookmarkVersesAdapter.BookmarkVerseViewHolder>() {

    class BookmarkVerseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chapterNumberTextView: TextView = itemView.findViewById(R.id.bookmarkVerseTitle)
        val chapterTitleTextView: TextView = itemView.findViewById(R.id.bookmarkVerseDescription)
        val chapterInfoTextView: TextView = itemView.findViewById(R.id.bookmarkVerseChapterAndVerseNumber)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkVerseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_item_bookmark_verse, parent, false)
        return BookmarkVerseViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookmarkVerseViewHolder, position: Int) {
        val chapter = chaptersList[position]
        holder.chapterNumberTextView.text = "${chapter.chapterNumber}"
        holder.chapterTitleTextView.text = chapter.chapterTitle
        holder.chapterInfoTextView.text = chapter.chapterInfo
    }

    override fun getItemCount() = chaptersList.size
}
