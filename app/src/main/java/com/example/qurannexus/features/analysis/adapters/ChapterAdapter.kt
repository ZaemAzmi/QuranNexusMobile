package com.example.qurannexus.features.analysis.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.features.analysis.ChapterInfo

class ChaptersAdapter(
    private val chapters: List<ChapterInfo>,
    private val onItemClick: (ChapterInfo) -> Unit
) : RecyclerView.Adapter<ChaptersAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val chapterCard: CardView = view.findViewById(R.id.chapterCard)
        val chapterIdTextView: TextView = view.findViewById(R.id.chapterIdTextView)
        val arabicNameTextView: TextView = view.findViewById(R.id.arabicNameTextView)
        val englishNameTextView: TextView = view.findViewById(R.id.englishNameTextView)
        val revelationPlaceTextView: TextView = view.findViewById(R.id.revelationPlaceTextView)
        val verseCountTextView: TextView = view.findViewById(R.id.verseCountTextView)
        val wordCountTextView: TextView = view.findViewById(R.id.wordCountTextView)

        fun bind(chapter: ChapterInfo, onItemClick: (ChapterInfo) -> Unit) {
            chapterIdTextView.text = chapter.id
            arabicNameTextView.text = chapter.arabicName
            englishNameTextView.text = chapter.englishName
            revelationPlaceTextView.text = chapter.revelationPlace
            verseCountTextView.text = "${chapter.verseCount} verses"
            wordCountTextView.text = "${chapter.wordCount} words"

            chapterCard.setOnClickListener {
                onItemClick(chapter)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chapter, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(chapters[position], onItemClick)
    }

    override fun getItemCount() = chapters.size
}