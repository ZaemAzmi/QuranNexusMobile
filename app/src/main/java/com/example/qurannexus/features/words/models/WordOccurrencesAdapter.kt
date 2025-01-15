package com.example.qurannexus.features.words.models

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R

class WordOccurrencesAdapter(
    private val occurrences: List<WordOccurrence>,
    private val onOccurrenceClick: (WordOccurrence) -> Unit
) : RecyclerView.Adapter<WordOccurrencesAdapter.OccurrenceViewHolder>() {

    class OccurrenceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val surahName: TextView = itemView.findViewById(R.id.surahName)
        val verseNumber: TextView = itemView.findViewById(R.id.verseNumber)
        val container: View = itemView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OccurrenceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_word_occurrence, parent, false)
        return OccurrenceViewHolder(view)
    }

    override fun onBindViewHolder(holder: OccurrenceViewHolder, position: Int) {
        val occurrence = occurrences[position]
        holder.surahName.text = occurrence.surah_name
        holder.verseNumber.text = "Verse ${occurrence.verse_number}"
        holder.container.setOnClickListener { onOccurrenceClick(occurrence) }
    }

    override fun getItemCount() = occurrences.size
}