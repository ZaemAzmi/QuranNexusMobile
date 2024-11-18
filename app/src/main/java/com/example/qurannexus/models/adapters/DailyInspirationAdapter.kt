package com.example.qurannexus.models.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.activities.ShareCustomQuoteActivity


class  DailyInspirationAdapter(
    private val inspirationQuoteList: List<String>,
    private val context : Context
) :
    RecyclerView.Adapter<DailyInspirationAdapter.InspirationViewHolder>() {
    class InspirationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val quoteTextView: TextView = itemView.findViewById(R.id.tvQuote)
        val btnBookmark: ImageButton = itemView.findViewById(R.id.btnBookmark)
        val btnShare: Button = itemView.findViewById(R.id.btnShare)

        fun bind(quote: String) {
            quoteTextView.text = quote
        }
    }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InspirationViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.card_item_daily_quote, parent, false)
            return InspirationViewHolder(view)
        }

        override fun onBindViewHolder(holder: InspirationViewHolder, position: Int) {
            val quoteText = inspirationQuoteList[position]
            holder.bind(quoteText)

            holder.btnShare.setOnClickListener{
                 navigateToCustomizationActivity(quoteText)
            }

            holder.btnBookmark.setOnClickListener {
                // Handle bookmarking logic here
            }
        }
    private fun navigateToCustomizationActivity(quote: String) {
        val intent = Intent(context, ShareCustomQuoteActivity::class.java)
        intent.putExtra("DAILY_QUOTE", quote)
        context.startActivity(intent)
    }
        override fun getItemCount(): Int = inspirationQuoteList.size
}
