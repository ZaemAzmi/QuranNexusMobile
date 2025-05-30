package com.example.qurannexus.features.analysis.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.features.analysis.viewmodels.DisplayableFrequentRoot // Use this

// Changed to ListAdapter and uses DisplayableFrequentRoot
class SearchResultsAdapter(
    private val onItemClick: (DisplayableFrequentRoot) -> Unit
) : ListAdapter<DisplayableFrequentRoot, SearchResultsAdapter.ViewHolder>(DisplayableRootDiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Ensure these IDs match your item_search_result.xml
        val resultCard: View = view.findViewById(R.id.resultCard) // Assuming root is CardView or clickable LinearLayout
        val arabicTextView: TextView = view.findViewById(R.id.arabicTextView)
        val translationTextView: TextView = view.findViewById(R.id.translationTextView)
        val surahTextView: TextView = view.findViewById(R.id.surahTextView) // Will show root label / occurrences
        val verseTextView: TextView = view.findViewById(R.id.verseTextView) // Can be hidden or repurposed

        fun bind(result: DisplayableFrequentRoot, onItemClick: (DisplayableFrequentRoot) -> Unit) {
            arabicTextView.text = result.displayArabicText
            translationTextView.text = result.displayTranslation
            // Repurpose surahTextView to show root label and occurrences
            surahTextView.text = "Root: ${result.rootLabel} (${result.totalOccurrences} times)"
            verseTextView.visibility = View.GONE // Hide verseText as it's not directly relevant for root display

            resultCard.setOnClickListener {
                onItemClick(result)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_word_search_result, parent, false) // Ensure this layout exists and has the IDs
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }
}

// DiffUtil for DisplayableFrequentRoot (can be shared with FrequentWordsAdapter if identical)
class DisplayableRootDiffCallback : DiffUtil.ItemCallback<DisplayableFrequentRoot>() {
    override fun areItemsTheSame(oldItem: DisplayableFrequentRoot, newItem: DisplayableFrequentRoot): Boolean {
        return oldItem.rootLabel == newItem.rootLabel
    }

    override fun areContentsTheSame(oldItem: DisplayableFrequentRoot, newItem: DisplayableFrequentRoot): Boolean {
        return oldItem == newItem
    }
}