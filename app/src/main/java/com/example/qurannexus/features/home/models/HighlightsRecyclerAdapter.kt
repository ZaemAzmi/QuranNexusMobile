package com.example.qurannexus.features.home.models

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.core.interfaces.HighlightClickListener

class HighlightsRecyclerAdapter(
    private val highlightsList: List<HighlightItem>,
    private val listener : HighlightClickListener
)
    : RecyclerView.Adapter<HighlightsRecyclerAdapter.HighlightViewHolder>() {

    inner class HighlightViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.highlightIcon)
        val label: TextView = itemView.findViewById(R.id.highlightText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HighlightViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_item_circle_highlight, parent, false)
        return HighlightViewHolder(view)
    }

    override fun onBindViewHolder(holder: HighlightViewHolder, position: Int) {
        val item = highlightsList[position]
        holder.icon.setImageResource(item.iconResId)
        holder.label.text = item.label
        holder.itemView.setOnClickListener{
            listener.onHighlightClick(position)
        }

    }
    override fun getItemCount(): Int = highlightsList.size
}
