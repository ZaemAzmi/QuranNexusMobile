package com.example.qurannexus.features.analysis.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView // If your item_frequent_word.xml uses CardView
import androidx.recyclerview.widget.ListAdapter // Changed to ListAdapter for DiffUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.features.analysis.viewmodels.DisplayableFrequentRoot // Updated import

class FrequentWordsAdapter(
    private val onItemClick: (DisplayableFrequentRoot) -> Unit
) : ListAdapter<DisplayableFrequentRoot, FrequentWordsAdapter.ViewHolder>(FrequentRootDiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Ensure these IDs match your item_frequent_word.xml
        private val wordCard: View = view.findViewById(R.id.wordCard) // Assuming root is a CardView or clickable LinearLayout
        private val arabicTextView: TextView = view.findViewById(R.id.arabicTextView)
        private val translationTextView: TextView = view.findViewById(R.id.translationTextView)
        private val occurrencesTextView: TextView = view.findViewById(R.id.occurrencesTextView)
        // private val rootLabelTextView: TextView? = view.findViewById(R.id.rootLabelTextView) // Optional: if you want to show root label on card

        fun bind(entry: DisplayableFrequentRoot, onItemClick: (DisplayableFrequentRoot) -> Unit) { // Renamed root to entry
            arabicTextView.text = entry.displayArabicText
            translationTextView.text = entry.displayTranslation
            // Display identifier type along with occurrences
            val displayType = entry.identifierType.uppercase()
            occurrencesTextView.text = "${displayType}: ${entry.totalOccurrences} occurrences"
            // rootLabelTextView?.text = "Identifier: ${entry.identifierValue}" // If you want to show the raw identifier

            wordCard.setOnClickListener {
                onItemClick(entry)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_frequent_word, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }
}

class FrequentRootDiffCallback : DiffUtil.ItemCallback<DisplayableFrequentRoot>() {
    override fun areItemsTheSame(oldItem: DisplayableFrequentRoot, newItem: DisplayableFrequentRoot): Boolean {
        return oldItem.identifierValue == newItem.identifierValue
    }

    override fun areContentsTheSame(oldItem: DisplayableFrequentRoot, newItem: DisplayableFrequentRoot): Boolean {
        return oldItem == newItem
    }
}