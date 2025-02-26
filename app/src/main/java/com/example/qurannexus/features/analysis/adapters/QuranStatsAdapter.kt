package com.example.qurannexus.features.analysis.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R

data class QuranStat(val value: String, val label: String)

class QuranStatsAdapter(private val stats: List<QuranStat>) :
    RecyclerView.Adapter<QuranStatsAdapter.StatViewHolder>() {

    class StatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val valueTextView: TextView = itemView.findViewById(R.id.statValue)
        val labelTextView: TextView = itemView.findViewById(R.id.statLabel)

        fun bind(stat: QuranStat) {
            valueTextView.text = stat.value
            labelTextView.text = stat.label
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quran_stat, parent, false)
        return StatViewHolder(view)
    }

    override fun onBindViewHolder(holder: StatViewHolder, position: Int) {
        holder.bind(stats[position])
    }

    override fun getItemCount(): Int = stats.size
}