package com.example.qurannexus.features.statistics

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.features.statistics.interfaces.RecitationDataReceiver
import com.example.qurannexus.features.statistics.models.RecitationStreakData
import com.example.qurannexus.features.statistics.viewmodels.HomepageStatisticsViewModel
import com.example.qurannexus.features.words.models.DailyBreakdownAdapter
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import java.text.SimpleDateFormat
import java.util.Locale

// Fragment for Weekly Chart
class WeeklyChartFragment : Fragment(), RecitationDataReceiver {
    private lateinit var weeklyChart: BarChart
    private val viewModel: HomepageStatisticsViewModel by viewModels({ requireParentFragment() })
    private var pendingData: RecitationStreakData? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chart_container, container, false)
        weeklyChart = view.findViewById(R.id.barChart)
        setupBarChart()

        pendingData?.let {
            onRecitationDataReceived(it)
            pendingData = null
        }
        // Observe the chart data
        viewModel.weeklyRecitationData.observe(viewLifecycleOwner) { data ->
            updateWeeklyChart(data)
        }

        return view
    }

    private fun setupBarChart() {
        weeklyChart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            setTouchEnabled(true)
            setPinchZoom(false)
            setDrawGridBackground(false)

            // Customize appearance
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
            animateY(1000)

            // Enable horizontal scrolling
            setScaleEnabled(false)
            isDragEnabled = true
            isScaleXEnabled = true
            setVisibleXRangeMaximum(5f)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                valueFormatter = WeekFormatter()
                labelRotationAngle = -45f
                textColor = ContextCompat.getColor(requireContext(), R.color.textColorSecondary)
                textSize = 10f
                setAvoidFirstLastClipping(true)
            }

            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                textColor = ContextCompat.getColor(requireContext(), R.color.textColorSecondary)
                textSize = 10f
                // Only show whole numbers for minutes
                granularity = 1f
            }

            axisRight.isEnabled = false

            // Add value selected listener for details popup
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    e?.let {
                        showWeekDetailsDialog(it.x.toInt(), it.y.toInt())
                    }
                }

                override fun onNothingSelected() {}
            })
        }
    }

    override fun onRecitationDataReceived(data: RecitationStreakData) {
        // check view has been created
        if(!this::weeklyChart.isInitialized || view == null){
            pendingData = data
            return
        }
        // The viewModel will process this data and update weeklyRecitationData LiveData
        // which we're already observing

        // But we can add visibility handling
        if (weeklyChart.visibility != View.VISIBLE) {
            weeklyChart.visibility = View.VISIBLE
        }

        // If needed, we can also process the data here to generate insights
        data.recitationTimes?.let { times ->
            if (times.isNotEmpty()) {
                // Calculate any additional insights if needed
                // For example, find the best week
                val bestWeek = viewModel.weeklyRecitationData.value?.maxByOrNull { it.second }
                bestWeek?.let { (week, minutes) ->
                    Log.d("WeeklyChart", "Best week: Week $week with $minutes minutes")
                    // Could show a special indicator on this week in the chart if desired
                }
            }
        }
    }

    override fun onEmptyState() {
        weeklyChart.apply {
            setNoDataText("Weekly statistics will appear here")
            setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.primaryColor))
            invalidate()
        }
    }

    private fun updateWeeklyChart(data: List<Pair<Int, Int>>) {
        if (data.isEmpty()) {
            onEmptyState()
            return
        }

        val entries = data.map { (week, minutes) ->
            BarEntry(week.toFloat(), minutes.toFloat())
        }

        val dataSet = BarDataSet(entries, "Weekly Minutes").apply {
            color = ContextCompat.getColor(requireContext(), R.color.primaryColor)
            valueTextSize = 10f
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.textColorPrimary)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "${value.toInt()} mins"
                }
            }

            // Add color gradient based on value
            colors = entries.map { entry ->
                val maxValue = entries.maxOfOrNull { it.y } ?: 1f
                val ratio = entry.y / maxValue

                ContextCompat.getColor(
                    requireContext(),
                    when {
                        ratio > 0.8 -> R.color.primaryColorDark
                        ratio > 0.5 -> R.color.primaryColor
                        else -> R.color.primaryColorLight
                    }
                )
            }
        }

        val barData = BarData(dataSet)
        barData.barWidth = 0.7f
        weeklyChart.data = barData

        // Focus on recent data
        if (entries.size <= 5) {
            weeklyChart.fitScreen()
        } else {
            weeklyChart.moveViewToX(entries.last().x - 4)
        }

        weeklyChart.animateY(1000)
        weeklyChart.invalidate()
    }

    private fun showWeekDetailsDialog(weekNumber: Int, totalMinutes: Int) {
        // Get the week data
        val weekData = viewModel.getWeekDetails(weekNumber)

        // Calculate stats
        val daysWithRecitation = weekData.dailyRecitations.count { it.minutes > 0 }
        val avgMinutes = if (daysWithRecitation > 0) {
            weekData.dailyRecitations.sumOf { it.minutes } / daysWithRecitation
        } else 0

        // Use MaterialAlertDialogBuilder for a modern look
        val dialogView = layoutInflater.inflate(R.layout.layout_dialog_recitation_week_details, null)

        dialogView.apply {
            // Set header information with improved formatting
            findViewById<TextView>(R.id.tvWeekDate).text = getWeekDateRange(weekNumber)
            findViewById<TextView>(R.id.tvTotalMinutes).text = "$totalMinutes mins"
            findViewById<TextView>(R.id.tvAverageDuration).text = "$avgMinutes mins/day"
            findViewById<TextView>(R.id.tvDaysRecited).text = "$daysWithRecitation of 7 days"

            // Add visual indicator for days recited (progress circle)
            findViewById<CircularProgressIndicator>(R.id.progressDaysRecited)?.apply {
                progress = (daysWithRecitation * 100) / 7
                setIndicatorColor(ContextCompat.getColor(context, R.color.primaryColor))
            }

            // Set up RecyclerView for daily breakdown
            val rvDailyBreakdown = findViewById<RecyclerView>(R.id.rvDailyBreakdown)
            rvDailyBreakdown.layoutManager = LinearLayoutManager(context)
            rvDailyBreakdown.adapter = DailyBreakdownAdapter(weekData.dailyRecitations)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun getWeekDateRange(weekNumber: Int): String {
        // Get the first and last date of the week
        val weekData = viewModel.getWeekDateRange(weekNumber)
        return SimpleDateFormat("MMM dd", Locale.getDefault()).format(weekData.first) + " - " +
                SimpleDateFormat("MMM dd", Locale.getDefault()).format(weekData.second)
    }

    // Formatter class for week labels
    private inner class WeekFormatter : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return getWeekDateRange(value.toInt())
        }
    }
}