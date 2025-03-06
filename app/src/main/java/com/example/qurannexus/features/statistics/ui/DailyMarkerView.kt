package com.example.qurannexus.features.statistics.ui

import android.content.Context
import android.widget.TextView
import com.example.qurannexus.R
import com.example.qurannexus.features.statistics.viewmodels.HomepageStatisticsViewModel
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.SimpleDateFormat
import java.util.Locale

class DailyMarkerView(
    context: Context,
    layoutResource: Int,
    private val viewModel: HomepageStatisticsViewModel
) : MarkerView(context, layoutResource) {

    private val tvDate: TextView = findViewById(R.id.tvMarkerDate)
    private val tvValue: TextView = findViewById(R.id.tvMarkerValue)

    // This is called every time the MarkerView is redrawn
    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let { entry ->
            val index = entry.x.toInt()

            // Get date from the viewModel
            val dateString = viewModel.dailyRecitationData.value?.getOrNull(index)?.first

            if (dateString != null) {
                // Format the date
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)
                val formattedDate = SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
                tvDate.text = formattedDate
            } else {
                tvDate.text = "Unknown date"
            }

            // Format the value (minutes)
            val minutes = entry.y.toInt()
            tvValue.text = "$minutes mins"
        }

        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        // Center the marker on top of the highlighted value
        return MPPointF(-(width / 2f), -height.toFloat() - 10f)
    }
}