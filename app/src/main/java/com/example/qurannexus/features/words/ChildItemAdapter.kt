package com.example.qurannexus.features.words

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.features.home.models.WordDetails

class ChildItemAdapter : RecyclerView.Adapter<ChildItemAdapter.ChildViewHolder>() {

    private val items = mutableListOf<WordDetails>()

    fun submitList(newItems: List<WordDetails>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChildViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_item_bookmark_word, parent, false)
        return ChildViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChildViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ChildViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val wordArabic: TextView = itemView.findViewById(R.id.bookmarkWordArabicText)
        private val wordEnglish: TextView = itemView.findViewById(R.id.bookmarkWordEnglishText)

        fun bind(wordDetails: WordDetails) {
            wordArabic.text = wordDetails.text
            wordEnglish.text = wordDetails.translation
        }
    }
}

