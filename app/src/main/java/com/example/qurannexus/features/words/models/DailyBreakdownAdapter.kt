package com.example.qurannexus.features.words.models

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
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

        // Find the maximum minutes for better progress bar scaling
        val maxMinutes = dailyRecitations.maxOfOrNull { it.minutes }?.coerceAtLeast(30) ?: 30

        holder.apply {
            tvDate.text = dateFormat.format(item.date)
            tvMinutes.text = "${item.minutes} mins"

            // Calculate progress percentage based on max value among all days
            val progress = if (maxMinutes > 0) {
                (item.minutes.toFloat() / maxMinutes * 100).toInt().coerceAtMost(100)
            } else {
                0
            }

            progressBar.max = 100
            progressBar.progress = progress

            // Set colors based on whether there was recitation that day
            val context = itemView.context
            if (item.minutes > 0) {
                progressBar.progressTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.primaryColor)
                )
                tvMinutes.setTextColor(ContextCompat.getColor(context, R.color.primaryColor))
            } else {
                progressBar.progressTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.light_gray)
                )
                tvMinutes.setTextColor(ContextCompat.getColor(context, R.color.gray))
            }
        }
    }

    override fun getItemCount() = dailyRecitations.size
}