package com.example.qurannexus.features.analysis.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.features.analysis.SearchResult

class SearchResultsAdapter(
    private val results: List<SearchResult>,
    private val onItemClick: (SearchResult) -> Unit
) : RecyclerView.Adapter<SearchResultsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val resultCard: CardView = view.findViewById(R.id.resultCard)
        val arabicTextView: TextView = view.findViewById(R.id.arabicTextView)
        val translationTextView: TextView = view.findViewById(R.id.translationTextView)
        val surahTextView: TextView = view.findViewById(R.id.surahTextView)
        val verseTextView: TextView = view.findViewById(R.id.verseTextView)

        fun bind(result: SearchResult, onItemClick: (SearchResult) -> Unit) {
            arabicTextView.text = result.wordText
            translationTextView.text = result.translation
            surahTextView.text = "${result.surahName} (${result.chapterId}:${result.verseNumber})"
            verseTextView.text = result.verseText

            resultCard.setOnClickListener {
                onItemClick(result)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(results[position], onItemClick)
    }

    override fun getItemCount() = results.size
}