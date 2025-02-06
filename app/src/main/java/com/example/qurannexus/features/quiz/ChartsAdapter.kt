package com.example.qurannexus.features.quiz

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
class ChartsAdapter(
    private val context: Context,
    private val onChartsCreated: (LineChart, BarChart) -> Unit
) : RecyclerView.Adapter<ChartsAdapter.ChartViewHolder>() {

    private lateinit var dailyActivityChart: LineChart
    private lateinit var surahPerformanceChart: BarChart
    private var chartsInitialized = false
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChartViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_chart_container, parent, false
        )
        return ChartViewHolder(view)
    }
    init {
        // Initialize MPAndroidChart Utils
        com.github.mikephil.charting.utils.Utils.init(context)
    }
    override fun onBindViewHolder(holder: ChartViewHolder, position: Int) {
        when (position) {
            0 -> {
                holder.bind("Daily Activity") {
                    dailyActivityChart = LineChart(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        description.isEnabled = false
                        legend.isEnabled = true
                        setTouchEnabled(true)
                        setDrawGridBackground(false)

                        xAxis.apply {
                            setPosition(XAxis.XAxisPosition.BOTTOM)
                            setDrawGridLines(false)
                            valueFormatter = DateAxisValueFormatter()
                        }


                        axisLeft.apply {
                            setDrawGridLines(true)
                            axisMinimum = 0f
                        }

                        axisRight.isEnabled = false
                    }
                    it.addView(dailyActivityChart)
                }
            }
            1 -> {
                holder.bind("Surah Performance") {
                    surahPerformanceChart = BarChart(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        description.isEnabled = false
                        legend.isEnabled = true
                        setTouchEnabled(true)
                        setDrawGridBackground(false)

                        xAxis.apply {
                            setPosition(XAxis.XAxisPosition.BOTTOM)
                            setDrawGridLines(false)
                            valueFormatter = SurahAxisValueFormatter()
                        }
                        axisLeft.apply {
                            setDrawGridLines(true)
                            axisMinimum = 0f
                            axisMaximum = 100f
                        }

                        axisRight.isEnabled = false
                    }
                    it.addView(surahPerformanceChart)

                    if (!chartsInitialized && ::dailyActivityChart.isInitialized && ::surahPerformanceChart.isInitialized) {
                        chartsInitialized = true
                        onChartsCreated(dailyActivityChart, surahPerformanceChart)
                    }
                }
            }
        }

        // Only call onChartsCreated once both charts are initialized
        if (position == 1) {
            onChartsCreated(dailyActivityChart, surahPerformanceChart)
        }
    }


    override fun getItemCount() = 2

    class ChartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val cardView: MaterialCardView = view.findViewById(R.id.chartCardView)
        private val titleText: TextView = view.findViewById(R.id.chartTitle)
        private val chartContainer: ViewGroup = view.findViewById(R.id.chartContainer)

        fun bind(title: String, createChart: (ViewGroup) -> Unit) {
            titleText.text = title
            chartContainer.removeAllViews()
            createChart(chartContainer)
        }
    }

    private class DateAxisValueFormatter : ValueFormatter() {
        private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            return dateFormat.format(Date(value.toLong()))
        }
    }

    private class SurahAxisValueFormatter : ValueFormatter() {
        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            return "Surah ${value.toInt()}"
        }
    }
}