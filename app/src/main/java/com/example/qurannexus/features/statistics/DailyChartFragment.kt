package com.example.qurannexus.features.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.qurannexus.R
import com.example.qurannexus.features.statistics.interfaces.RecitationDataReceiver
import com.example.qurannexus.features.statistics.models.RecitationStreakData
import com.example.qurannexus.features.statistics.ui.DailyMarkerView
import com.example.qurannexus.features.statistics.viewmodels.HomepageStatisticsViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import java.text.SimpleDateFormat
import java.util.Locale

// Fragment for Daily Chart
class DailyChartFragment : Fragment(), RecitationDataReceiver {
    private lateinit var dailyChart: LineChart
    private val viewModel: HomepageStatisticsViewModel by viewModels({ requireParentFragment() })
    private var pendingData : RecitationStreakData? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chart_container, container, false)
        dailyChart = view.findViewById(R.id.lineChart)
        setupLineChart()
        pendingData?.let {
            onRecitationDataReceived(it)
            pendingData = null
        }
        // Observe the chart data
        viewModel.dailyRecitationData.observe(viewLifecycleOwner) { data ->
            updateDailyChart(data)
        }

        return view
    }

    private fun setupLineChart() {
        dailyChart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            setTouchEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)

            // Customize appearance
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
            animateX(1000)
            isDoubleTapToZoomEnabled = false

            // Add marker for better data display on touch
            val markerView = DailyMarkerView(
                requireContext(),
                R.layout.marker_view_daily,
                viewModel
            )
            markerView.chartView = this
            marker = markerView

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                valueFormatter = DateAxisValueFormatter(viewModel)
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

            // Highlight interaction
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    // Optional: special handling for selected value
                }

                override fun onNothingSelected() {
                    // Optional: clear any highlighted state
                }
            })
        }
    }

    private fun updateDailyChart(data: List<Pair<String, Int>>) {
        if (data.isEmpty()) {
            onEmptyState()
            return
        }

        val entries = data.mapIndexed { index, (_, value) ->
            Entry(index.toFloat(), value.toFloat())
        }

        val dataSet = LineDataSet(entries, "Minutes").apply {
            color = ContextCompat.getColor(requireContext(), R.color.primaryColor)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.primaryColor))
            lineWidth = 2.5f
            circleRadius = 4f
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(requireContext(), R.color.primaryColorLight)
            fillAlpha = 50
            mode = LineDataSet.Mode.CUBIC_BEZIER // Smooth curve
            setDrawValues(false)
        }

        dailyChart.data = LineData(dataSet)
        dailyChart.animateY(1000)
        dailyChart.invalidate()
    }
    override fun onRecitationDataReceived(data: RecitationStreakData) {

        if (!this::dailyChart.isInitialized || view == null) {
            // Store the data to process later when the view is ready
            pendingData = data
            return
        }
        
        // The viewModel will process this data and update dailyRecitationData LiveData
        // No need to do anything here as we're already observing dailyRecitationData

        // But if we want to ensure charts are visible after receiving data:
        if (dailyChart.visibility != View.VISIBLE) {
            dailyChart.visibility = View.VISIBLE
        }
    }

    override fun onEmptyState() {
        dailyChart.apply {
            setNoDataText("Start your recitation journey!")
            setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.primaryColor))
            invalidate()
        }
    }
    private inner class DateAxisValueFormatter(private val viewModel: HomepageStatisticsViewModel) : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val index = value.toInt()
            return viewModel.dailyRecitationData.value?.getOrNull(index)?.first?.let { date ->
                SimpleDateFormat("MM/dd", Locale.getDefault()).format(
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)
                )
            } ?: value.toString()
        }
    }


}