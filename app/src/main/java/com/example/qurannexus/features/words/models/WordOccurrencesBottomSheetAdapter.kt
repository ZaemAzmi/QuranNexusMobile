package com.example.qurannexus.features.words.models

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.core.utils.QuranMetadata

class WordOccurrencesBottomSheetAdapter(
    private val onOccurrenceClick: (WordOccurrence) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val occurrences = mutableListOf<WordOccurrence>()
    private var isLoading = false

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_LOADING = 1
    }

    fun submitList(newOccurrences: List<WordOccurrence>) {
        occurrences.clear()
        occurrences.addAll(newOccurrences)
        notifyDataSetChanged()
    }

    fun addItems(newOccurrences: List<WordOccurrence>) {
        val startPos = occurrences.size
        occurrences.addAll(newOccurrences)
        notifyItemRangeInserted(startPos, newOccurrences.size)
    }

    fun setLoading(loading: Boolean) {
        if (isLoading != loading) {
            isLoading = loading
            if (loading) {
                notifyItemInserted(occurrences.size)
            } else {
                notifyItemRemoved(occurrences.size)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < occurrences.size) VIEW_TYPE_ITEM else VIEW_TYPE_LOADING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ITEM -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_word_occurrence, parent, false)
                OccurrenceViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_loading, parent, false)
                LoadingViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is OccurrenceViewHolder -> {
                if (position < occurrences.size) {
                    holder.bind(occurrences[position])
                }
            }
            is LoadingViewHolder -> {
                holder.showLoading(isLoading)
            }
        }
    }

    override fun getItemCount() = occurrences.size + if (isLoading) 1 else 0

    inner class OccurrenceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val positionText: TextView = itemView.findViewById(R.id.positionText)
        private val surahName: TextView = itemView.findViewById(R.id.surahName)
        private val arabicSurahName: TextView = itemView.findViewById(R.id.arabicSurahName)
        private val verseNumber: TextView = itemView.findViewById(R.id.verseNumber)
        private val verseText: TextView = itemView.findViewById(R.id.verseText)
        private val container: View = itemView

        fun bind(occurrence: WordOccurrence) {
            // Get surah details safely
            val surahDetails = try {
                QuranMetadata.getInstance().getSurahDetails(occurrence.chapter_id.toInt())
            } catch (e: Exception) {
                null
            }

            // Set position/ID text
            positionText.text = occurrence.word_id

            // Set surah names with fallback
            surahName.text = surahDetails?.englishName ?: "Chapter ${occurrence.chapter_id}"
            arabicSurahName.text = surahDetails?.arabicName ?: ""

            // Set verse number and additional info
            verseNumber.text = buildString {
                append("Verse ${occurrence.verse_number}")
                append(" • Juz ${occurrence.juz_number}")
                // Only add revelation place if surahDetails is available
                surahDetails?.revelationPlace?.let { place ->
                    append(" • $place")
                }
            }

            // Set verse text with ellipsis if too long
            verseText.text = occurrence.verse_text ?: "Verse text not available"

            container.setOnClickListener { onOccurrenceClick(occurrence) }
        }
    }

    inner class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)

        fun showLoading(show: Boolean) {
            progressBar.visibility = if (show) View.VISIBLE else View.GONE
        }
    }
}