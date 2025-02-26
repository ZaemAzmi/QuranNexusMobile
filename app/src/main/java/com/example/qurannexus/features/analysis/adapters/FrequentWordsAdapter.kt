package com.example.qurannexus.features.analysis.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.features.analysis.FrequentWord

class FrequentWordsAdapter(
    private val words: List<FrequentWord>,
    private val onItemClick: (FrequentWord) -> Unit
) : RecyclerView.Adapter<FrequentWordsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val wordCard: CardView = view.findViewById(R.id.wordCard)
        val arabicTextView: TextView = view.findViewById(R.id.arabicTextView)
        val translationTextView: TextView = view.findViewById(R.id.translationTextView)
        val occurrencesTextView: TextView = view.findViewById(R.id.occurrencesTextView)

        fun bind(word: FrequentWord, onItemClick: (FrequentWord) -> Unit) {
            arabicTextView.text = word.text
            translationTextView.text = word.translation
            occurrencesTextView.text = "${word.occurrences} times"

            wordCard.setOnClickListener {
                onItemClick(word)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_frequent_word, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(words[position], onItemClick)
    }

    override fun getItemCount() = words.size
}