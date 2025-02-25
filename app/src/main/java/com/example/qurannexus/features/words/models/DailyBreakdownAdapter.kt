package com.example.qurannexus.features.words.models

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.features.statistics.models.DailyRecitation
import java.text.SimpleDateFormat
import java.util.Locale

class DailyBreakdownAdapter(
    private val dailyRecitations: List<DailyRecitation>
) : RecyclerView.Adapter<DailyBreakdownAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvMinutes: TextView = view.findViewById(R.id.tvMinutes)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_daily_recitation_breakdown, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dailyRecitations[position]
        val dateFormat = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())

        holder.apply {
            tvDate.text = dateFormat.format(item.date)
            tvMinutes.text = "${item.minutes} mins"

            // Calculate progress percentage (assuming max is around 120 minutes)
            val progress = (item.minutes.toFloat() / 120 * 100).toInt().coerceAtMost(100)
            progressBar.progress = progress
        }
    }

    override fun getItemCount() = dailyRecitations.size
}