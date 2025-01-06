package com.example.qurannexus.features.bookmark.models

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R

class BookmarkChaptersAdapter(private var chaptersList: List<BookmarkChapter>) : RecyclerView.Adapter<BookmarkChaptersAdapter.BookmarkChapterViewHolder>() {

    class BookmarkChapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chapterNumberTextView: TextView = itemView.findViewById(R.id.bookmarkChapterNumber)
        val chapterTitleTextView: TextView = itemView.findViewById(R.id.bookmarkChapterTitle)
        val chapterInfoTextView: TextView = itemView.findViewById(R.id.bookmarkChapterRevelationPlace)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkChapterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_item_bookmark_chapter, parent, false)
        return BookmarkChapterViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookmarkChapterViewHolder, position: Int) {
        val chapter = chaptersList[position]
        holder.chapterNumberTextView.text = "${chapter.chapterNumber}"
        holder.chapterTitleTextView.text = chapter.chapterTitle
        holder.chapterInfoTextView.text = chapter.chapterInfo
    }

    override fun getItemCount() = chaptersList.size

    fun updateData(newList: List<BookmarkChapter>) {
        chaptersList = newList
        notifyDataSetChanged()
    }
}
