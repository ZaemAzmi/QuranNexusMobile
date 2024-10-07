package com.example.qurannexus.models.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R

class DailyInspirationAdapter(private val inspirationQuoteList: List<String>) :
    RecyclerView.Adapter<DailyInspirationAdapter.InspirationViewHolder>() {

    class InspirationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val quoteTextView: TextView = itemView.findViewById(R.id.tvQuote)

        fun bind(quote: String) {
            quoteTextView.text = quote
        }
    }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InspirationViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.quote_card_item, parent, false)
            return InspirationViewHolder(view)
        }

        override fun onBindViewHolder(holder: InspirationViewHolder, position: Int) {
            holder.bind(inspirationQuoteList[position])
        }

        override fun getItemCount(): Int = inspirationQuoteList.size
}
