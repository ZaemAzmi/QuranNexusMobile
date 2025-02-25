package com.example.qurannexus.features.statistics

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.features.statistics.models.RecitationStreakData
import com.example.qurannexus.features.statistics.viewmodels.HomepageStatisticsViewModel
import com.example.qurannexus.features.words.models.DailyBreakdownAdapter
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
@AndroidEntryPoint
class HomepageStatisticsFragment : Fragment() {

    private val viewModel: HomepageStatisticsViewModel by viewModels()

    private lateinit var recitationChart: LineChart
    private lateinit var weeklyRecitationChart: BarChart
    private lateinit var currentStreakValue: TextView
    private lateinit var longestStreakValue: TextView
    private lateinit var consistencyScoreValue: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_homepage_recitation_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupCharts()
        observeViewModel()
        fetchData()
    }

    private fun initializeViews(view: View) {
        recitationChart = view.findViewById(R.id.recitationChart)
        weeklyRecitationChart = view.findViewById(R.id.weeklyRecitationChart)
        currentStreakValue = view.findViewById(R.id.currentStreakValue)
        longestStreakValue = view.findViewById(R.id.longestStreakValue)
        consistencyScoreValue = view.findViewById(R.id.consistencyScoreValue)
    }

    private fun setupCharts() {
        setupLineChart()
        setupBarChart()
    }
    private fun updateStats(streakData: RecitationStreakData) {
        // Update text views
        currentStreakValue.text = streakData.currentStreak.toString()
        longestStreakValue.text = streakData.longestStreak.toString()
        consistencyScoreValue.text = "${streakData.consistencyScore}%"

        // If you want to trigger chart updates manually here
        viewModel.processRecitationData(streakData)
    }

    private fun setupLineChart() {
        recitationChart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            setTouchEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                valueFormatter = DateAxisValueFormatter()
            }

            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
            }

            axisRight.isEnabled = false
        }
    }

    private fun setupBarChart() {
        weeklyRecitationChart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            setTouchEnabled(true)
            setPinchZoom(false)
            setDrawGridBackground(false)

            // Enable horizontal scrolling
            setScaleEnabled(false)
            setHorizontalScrollBarEnabled(true)
            isDragEnabled = true
            isScaleXEnabled = true
            setVisibleXRangeMaximum(5f) // Show 5 bars at a time

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        // Get the week's date range
                        return getWeekDateRange(value.toInt())
                    }
                }
                labelRotationAngle = -45f // Angle labels for better readability
            }

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
    private fun getWeekDateRange(weekNumber: Int): String {
        // Get the first and last date of the week
        val weekData = viewModel.getWeekDateRange(weekNumber)
        return SimpleDateFormat("MMM dd", Locale.getDefault()).format(weekData.first) + " - " +
                SimpleDateFormat("MMM dd", Locale.getDefault()).format(weekData.second)
    }

    private fun showWeekDetailsDialog(weekNumber: Int, totalMinutes: Int) {
        val weekData = viewModel.getWeekDetails(weekNumber)
        val dialogView = layoutInflater.inflate(R.layout.layout_dialog_recitation_week_details, null)

        dialogView.apply {
            findViewById<TextView>(R.id.tvWeekDate).text = getWeekDateRange(weekNumber)
            findViewById<TextView>(R.id.tvTotalMinutes).text = "$totalMinutes mins"
            findViewById<TextView>(R.id.tvAverageDuration).text =
                "${weekData.averageMinutes.toInt()} mins/day"
            findViewById<TextView>(R.id.tvDaysRecited).text =
                "${weekData.daysRecited} days"

            // Add RecyclerView for daily breakdown
            val rvDailyBreakdown = findViewById<RecyclerView>(R.id.rvDailyBreakdown)
            rvDailyBreakdown.layoutManager = LinearLayoutManager(context)
            rvDailyBreakdown.adapter = DailyBreakdownAdapter(weekData.dailyRecitations)
        }

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()
    }
    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is HomepageStatisticsViewModel.UiState.Loading -> {
                    // Show loading state if needed
                }
                is HomepageStatisticsViewModel.UiState.Success -> {
                    // Always update basic stats
                    currentStreakValue.text = state.streakData.currentStreak.toString()
                    longestStreakValue.text = state.streakData.longestStreak.toString()
                    consistencyScoreValue.text = "${state.streakData.consistencyScore}%"

                    // Setup empty chart states if no data
                    if (state.recitationTimes.isEmpty()) {
                        setupEmptyChartStates()
                    }
                }
                is HomepageStatisticsViewModel.UiState.Error -> {
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                }
                HomepageStatisticsViewModel.UiState.Empty -> {
                    // Update UI with empty/default values
                    setupEmptyStates()
                }
            }
        }

        viewModel.dailyRecitationData.observe(viewLifecycleOwner) { data ->
            updateDailyChart(data)
        }

        viewModel.weeklyRecitationData.observe(viewLifecycleOwner) { data ->
            updateWeeklyChart(data)
        }
    }
    private fun setupEmptyChartStates() {
        recitationChart.apply {
            setNoDataText("Start your recitation journey!")
            setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.primaryColor))
            setPadding(16, 16, 16, 16)
            invalidate()
        }

        weeklyRecitationChart.apply {
            setNoDataText("Weekly statistics will appear here")
            setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.primaryColor))
            setPadding(16, 16, 16, 16)
            invalidate()
        }
    }
    private fun setupEmptyStates() {
        currentStreakValue.text = "0"
        longestStreakValue.text = "0"
        consistencyScoreValue.text = "0%"
        setupEmptyChartStates()
    }
    private fun fetchData() {
        val token = requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE)
            .getString("token", null)

        if (token != null) {
            viewModel.fetchStatistics(token)
        }
    }

    private fun updateDailyChart(data: List<Pair<String, Int>>) {
        if (data.isEmpty()) {
            recitationChart.apply {
                setNoDataText("Start your recitation journey!")
                setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.primaryColor))
                invalidate()
            }
            return
        }

        val entries = data.mapIndexed { index, (_, value) ->
            Entry(index.toFloat(), value.toFloat())
        }

        val dataSet = LineDataSet(entries, "Daily Recitation").apply {
            color = ContextCompat.getColor(requireContext(), R.color.primaryColor)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.primaryColor))
            lineWidth = 2f
            circleRadius = 4f
            setDrawValues(false)
        }

        recitationChart.data = LineData(dataSet)
        recitationChart.invalidate()
    }

    private fun updateWeeklyChart(data: List<Pair<Int, Int>>) {
        if (data.isEmpty()) {
            weeklyRecitationChart.apply {
                setNoDataText("Weekly statistics will appear here")
                setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.primaryColor))
                invalidate()
            }
            return
        }

        val entries = data.map { (week, minutes) ->
            BarEntry(week.toFloat(), minutes.toFloat())
        }

        val dataSet = BarDataSet(entries, "Weekly Recitation").apply {
            color = ContextCompat.getColor(requireContext(), R.color.primaryColor)
            valueTextSize = 10f
        }

        weeklyRecitationChart.data = BarData(dataSet)
        weeklyRecitationChart.invalidate()
    }

    private inner class DateAxisValueFormatter : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val index = value.toInt()
            return viewModel.dailyRecitationData.value?.getOrNull(index)?.first?.let { date ->
                SimpleDateFormat("MM/dd", Locale.getDefault()).format(
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)
                )
            } ?: value.toString()
        }
    }

    private inner class WeekAxisValueFormatter : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return "Week ${value.toInt()}"
        }
    }
}