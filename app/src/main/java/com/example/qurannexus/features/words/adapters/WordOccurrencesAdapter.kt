package com.example.qurannexus.features.words.adapters


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.core.utils.QuranMetadata // If needed for Surah names
import com.example.qurannexus.features.words.models.WordOccurrenceDisplayItem

class WordOccurrencesAdapter(
    private val onOccurrenceClick: (WordOccurrenceDisplayItem) -> Unit
) : ListAdapter<WordOccurrenceDisplayItem, RecyclerView.ViewHolder>(WordOccurrenceDiffCallback()) {

    private var isLoadingFooter = false

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_LOADING = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < super.getItemCount()) VIEW_TYPE_ITEM else VIEW_TYPE_LOADING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ITEM -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_word_occurrence, parent, false) // Reuse existing layout
                OccurrenceViewHolder(view)
            }
            else -> { // VIEW_TYPE_LOADING
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_loading, parent, false)
                LoadingViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is OccurrenceViewHolder) {
            holder.bind(getItem(position))
        } else if (holder is LoadingViewHolder) {
            holder.showLoading(isLoadingFooter)
        }
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + if (isLoadingFooter) 1 else 0
    }

    fun setLoading(loading: Boolean) {
        if (isLoadingFooter != loading) {
            isLoadingFooter = loading
            if (loading) {
                notifyItemInserted(super.getItemCount()) // Insert after current items
            } else {
                notifyItemRemoved(super.getItemCount()) // Remove from after current items
            }
        }
    }
    fun addItems(newItems: List<WordOccurrenceDisplayItem>) {
        val currentList = currentList.toMutableList()
        currentList.addAll(newItems)
        submitList(currentList)
    }


    inner class OccurrenceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val positionText: TextView = itemView.findViewById(R.id.positionText) // e.g., Word Key
        private val surahName: TextView = itemView.findViewById(R.id.surahName) // English Surah Name
        private val arabicSurahName: TextView = itemView.findViewById(R.id.arabicSurahName)
        private val verseNumber: TextView = itemView.findViewById(R.id.verseNumber) // Ayah num, Juz, Page
        private val verseText: TextView = itemView.findViewById(R.id.verseText) // Arabic word form
        private val container: View = itemView

        fun bind(item: WordOccurrenceDisplayItem) {
            val quranMetadata = QuranMetadata.getInstance()
            val surahDetails = quranMetadata.getSurahDetails(item.surahId)

            positionText.text = "Key: ${item.wordKey}"
            surahName.text = surahDetails?.englishName ?: "Surah ${item.surahId}"
            arabicSurahName.text = surahDetails?.arabicName ?: ""
            verseNumber.text = "Verse ${item.ayahIndex} • Juz ${item.juzId ?: "N/A"} • Page ${item.pageId ?: "N/A"}"
            verseText.text = item.arabicText ?: "N/A" // Display the specific Arabic form of the occurrence

            container.setOnClickListener { onOccurrenceClick(item) }
        }
    }

    inner class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        fun showLoading(show: Boolean) {
            progressBar.visibility = if (show) View.VISIBLE else View.GONE
        }
    }
}

class WordOccurrenceDiffCallback : DiffUtil.ItemCallback<WordOccurrenceDisplayItem>() {
    override fun areItemsTheSame(oldItem: WordOccurrenceDisplayItem, newItem: WordOccurrenceDisplayItem): Boolean {
        return oldItem.wordKey == newItem.wordKey // Assuming wordKey is unique per occurrence
    }

    override fun areContentsTheSame(oldItem: WordOccurrenceDisplayItem, newItem: WordOccurrenceDisplayItem): Boolean {
        return oldItem == newItem
    }
}