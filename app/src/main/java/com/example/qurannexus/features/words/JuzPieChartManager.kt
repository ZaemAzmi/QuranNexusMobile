package com.example.qurannexus.features.words
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.example.qurannexus.R
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF

class JuzPieChartManager(private val pieChart: PieChart) {

    // Islamic-inspired color palette
    private val COLORS = intArrayOf(
        Color.parseColor("#26A69A"), // Teal
        Color.parseColor("#5C6BC0"), // Indigo
        Color.parseColor("#7E57C2"), // Deep Purple
        Color.parseColor("#FF7043"), // Deep Orange
        Color.parseColor("#66BB6A"), // Green
        Color.parseColor("#FFA726"), // Orange
        Color.parseColor("#42A5F5"), // Blue
        Color.parseColor("#EC407A")  // Pink
    )

    init {
        setupPieChart()
    }

    private fun setupPieChart() {
        // Basic chart setup
        pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            setExtraOffsets(5f, 10f, 5f, 5f)

            // Center hole setup
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            holeRadius = 45f
            transparentCircleRadius = 50f

            // Center text
            centerText = "بسم الله"
            setCenterTextSize(20f)
            setCenterTextTypeface(Typeface.DEFAULT_BOLD)

            // Legend setup
            legend.apply {
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                orientation = Legend.LegendOrientation.VERTICAL
                setDrawInside(false)
                xEntrySpace = 7f
                yEntrySpace = 0f
                yOffset = 0f
            }

            // Animation
            animateY(1400, Easing.EaseInOutQuad)
        }
    }

    fun updateChartData(occurrences: List<Int>) {
        val entries = ArrayList<PieEntry>()

        // Create entries
        occurrences.forEachIndexed { index, value ->
            entries.add(PieEntry(value.toFloat(), "Juz ${index + 1}"))
        }

        // Create and style the dataset
        val dataSet = PieDataSet(entries, "").apply {
            colors = COLORS.toList()
            sliceSpace = 3f
            selectionShift = 5f

            // Value text styling
            valueTextSize = 12f
            valueTypeface = Typeface.DEFAULT_BOLD
            valueTextColor = Color.WHITE
            valueFormatter = PercentFormatter(pieChart)
        }

        // Apply data to chart
        val data = PieData(dataSet)
        pieChart.data = data

        // Custom marker view
        val markerView = CustomMarkerView(
            pieChart.context,
            R.layout.marker_view,
            occurrences
        ).apply {
            chartView = pieChart
        }
        pieChart.marker = markerView

        pieChart.invalidate()
    }
}

// Enhanced MarkerView
class CustomMarkerView(
    context: Context,
    layoutResource: Int,
    private val occurrences: List<Int>
) : MarkerView(context, layoutResource) {

    private val tvContent: TextView = findViewById(R.id.tvContent)
    private val cardView: CardView = findViewById(R.id.markerCardView)

    init {
        // Style the marker card
        cardView.apply {
            radius = 12f
            cardElevation = 8f
            setCardBackgroundColor(Color.WHITE)
        }
    }

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e is PieEntry) {
            val juzNumber = e.label.replace("Juz ", "").toInt()
            val occurrence = occurrences[juzNumber - 1]

            tvContent.text = buildString {
                append("Juz $juzNumber\n")
                append("Occurrences: $occurrence")
            }
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat() - 10) // Offset above the selected slice
    }
}