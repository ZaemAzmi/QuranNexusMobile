package com.example.qurannexus.features.analysis.adapters

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.features.analysis.viewmodels.DisplayableFrequentRoot // Use this
import com.google.android.material.card.MaterialCardView

// Changed to ListAdapter and uses DisplayableFrequentRoot
class SearchResultsAdapter(
    private val onItemClick: (DisplayableFrequentRoot) -> Unit
) : ListAdapter<DisplayableFrequentRoot, SearchResultsAdapter.ViewHolder>(DisplayableRootDiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Ensure these IDs match your item_search_result.xml
        private val resultCard: View = view.findViewById(R.id.resultCard)
        private val arabicTextView: TextView = view.findViewById(R.id.arabicTextView)
        private val translationTextView: TextView = view.findViewById(R.id.translationTextView)

        // New/Renamed TextViews based on the updated XML
        private val identifierTypeTextView: TextView = view.findViewById(R.id.identifierTypeTextView)
        private val identifierValueTextView: TextView = view.findViewById(R.id.identifierValueTextView)
        private val occurrencesTextView: TextView = view.findViewById(R.id.occurrencesTextView)
        // Optional: for unique forms count
        private val uniqueFormsCountTextView: TextView = view.findViewById(R.id.uniqueFormsCountTextView)

        fun bind(result: DisplayableFrequentRoot, onItemClick: (DisplayableFrequentRoot) -> Unit) {
            arabicTextView.text = result.displayArabicText
            translationTextView.text = result.displayTranslation

            identifierTypeTextView.text = result.identifierType.uppercase()

            identifierValueTextView.text = result.identifierValue
            occurrencesTextView.text = "Occurred ${result.totalOccurrences} times"

            // Dynamically set background color for identifierTypeTextView chip
            val chipBackgroundColor = when (result.identifierType) {
                "ROOT" -> ContextCompat.getColor(itemView.context, R.color.appPrimaryVariant)
                "LEMMA" -> ContextCompat.getColor(itemView.context, R.color.accent_dark_teal_cyan)
                "OTHERS" -> ContextCompat.getColor(itemView.context, R.color.coolSlateBlue)
                else -> ContextCompat.getColor(itemView.context, R.color.textTertiary) // A fallback gray
            }

            // Get the background drawable and cast it to GradientDrawable to change its color
            val background = identifierTypeTextView.background
            if (background is GradientDrawable) {
                background.setColor(chipBackgroundColor)
            } else {
                // Fallback for other drawable types or if you just want to set a solid color
                identifierTypeTextView.setBackgroundColor(chipBackgroundColor)
            }
            // 2. UPDATED: SET CARD BACKGROUND FROM YOUR EXISTING COLORS
            val cardBackgroundColor = when (result.identifierType) {
                "ROOT" -> ContextCompat.getColor(itemView.context, R.color.backgroundCardSubtle)
                "LEMMA" -> ContextCompat.getColor(itemView.context, R.color.appSecondary)
                "OTHERS" -> ContextCompat.getColor(itemView.context, R.color.accentSoftLavender)
                else -> ContextCompat.getColor(itemView.context, R.color.backgroundCard) // Default is white
            }
            (itemView as MaterialCardView).setCardBackgroundColor(cardBackgroundColor)

            // Handle uniqueFormsCountTextView visibility (assuming you've added the field to DisplayableFrequentRoot)
            if (result.uniqueFormCount != null && result.uniqueFormCount > 0) {
                val uniqueFormsText = when (result.identifierType) {
                    "ROOT", "LEMMA", "OTHERS" -> if (result.uniqueFormCount > 1) "Has ${result.uniqueFormCount} distinct Arabic forms" else "Has 1 distinct Arabic form"
                    else -> ""
                }
                if (uniqueFormsText.isNotEmpty()) {
                    uniqueFormsCountTextView.text = uniqueFormsText
                    uniqueFormsCountTextView.visibility = View.VISIBLE
                } else {
                    uniqueFormsCountTextView.visibility = View.GONE
                }
            } else {
                uniqueFormsCountTextView.visibility = View.GONE
            }


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
        return oldItem.identifierValue == newItem.identifierValue
    }

    override fun areContentsTheSame(oldItem: DisplayableFrequentRoot, newItem: DisplayableFrequentRoot): Boolean {
        return oldItem == newItem
    }
}