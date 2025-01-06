package com.example.qurannexus.features.home

import android.content.Context
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.qurannexus.R
import com.example.qurannexus.features.words.JuzPieChartManager
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.utils.MPPointF
import java.io.IOException

class WordDetailsActivity : AppCompatActivity() {
    private lateinit var barChart: BarChart
    private lateinit var pieChart: PieChart
    private lateinit var chartManager: JuzPieChartManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_word_details)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressed()
        }
        val wordTextView = findViewById<TextView>(R.id.wordText)
        val translationTextView = findViewById<TextView>(R.id.translationText)
        val transliterationTextView = findViewById<TextView>(R.id.transliterationText)
        val surahNameTextView = findViewById<TextView>(R.id.surahNameText)
        val ayahKeyTextView = findViewById<TextView>(R.id.ayahKeyText)
        val surahNumberText = findViewById<TextView>(R.id.surahNumberText)
        val lineNumberText = findViewById<TextView>(R.id.lineNumberText)
        val wordNumberText = findViewById<TextView>(R.id.wordNumberText)
        val pageIdText = findViewById<TextView>(R.id.pageIdText)
        val playAudioButton = findViewById<Button>(R.id.playAudioButton)

        // Retrieve data from the intent
        val wordText = intent.getStringExtra("WORD_TEXT")
        val translation = intent.getStringExtra("TRANSLATION")
        val transliteration = intent.getStringExtra("TRANSLITERATION")
        val surahNameArabic = intent.getStringExtra("SURAH_NAME_ARABIC")
        val surahNameEnglish = intent.getStringExtra("SURAH_NAME_ENGLISH")
        val ayahKey = intent.getStringExtra("AYAH_KEY")
        val audioUrl = intent.getStringExtra("AUDIO_URL")
        val surahNumber = intent.getStringExtra("SURAH_NUMBER")
        val lineNumber = intent.getIntExtra("LINE_NUMBER", -1)
        val wordNumber = intent.getStringExtra("WORD_NUMBER")
        val pageId = intent.getStringExtra("PAGE_ID")

        // Set data to views
        wordTextView.text = wordText
        translationTextView.text = "Translation: $translation"
        transliterationTextView.text = "Transliteration: $transliteration"
        surahNameTextView.text = "Surah: $surahNameArabic ($surahNameEnglish)"
        ayahKeyTextView.text = "Ayah Key: $ayahKey"
        surahNumberText.text = "Surah Number: $surahNumber"
        lineNumberText.text = "Line Number: $lineNumber"
        wordNumberText.text = "Word Number: $wordNumber"
        pageIdText.text = "Page ID: $pageId"

        // Set up audio playback
        playAudioButton.setOnClickListener {
            val mediaPlayer = MediaPlayer()
            try {
                mediaPlayer.setDataSource(audioUrl)
                mediaPlayer.prepare()
                mediaPlayer.start()
            } catch (e: IOException) {
                Toast.makeText(this, "Failed to play audio", Toast.LENGTH_SHORT).show()
            }
        }
        pieChart = findViewById(R.id.pieChart)
        barChart = findViewById(R.id.barChart)

        setupBarChart()
        setupPieChart()

    }

    private fun setupBarChart() {
        val entries = ArrayList<BarEntry>()

        // Generate temporary data for all 30 Juz
        val occurrences = (1..30).map { (10..100).random() }
        for (i in occurrences.indices) {
            entries.add(BarEntry((i + 1).toFloat(), occurrences[i].toFloat()))
        }

        val dataSet = BarDataSet(entries, "")
        dataSet.color = Color.BLUE
        dataSet.valueTextSize = 8f // Hide value texts
        dataSet.valueTextColor = Color.TRANSPARENT

        val data = BarData(dataSet)
        data.barWidth = 0.8f

        barChart.data = data
        barChart.description.isEnabled = false
        barChart.setFitBars(true)
        barChart.animateY(1400)

        // Customize x-axis
        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textSize = 10f
        xAxis.granularity = 1f
        xAxis.valueFormatter = IndexAxisValueFormatter(
            (1..30).map { it.toString() } + listOf("Juz") // Add "Juz" at the end
        )
        xAxis.labelRotationAngle = 0f // Keep labels horizontal

        // Customize y-axis
        barChart.axisLeft.axisMinimum = 0f
        barChart.axisLeft.axisMaximum = 100f // Example range
        barChart.axisLeft.labelCount = 5
        barChart.axisRight.isEnabled = false // Hide right axis
        barChart.axisLeft.setDrawGridLines(false)

        // Add a MarkerView for long click
        val marker = CustomMarkerView(this, R.layout.marker_view, occurrences)
        barChart.marker = marker

        barChart.invalidate()

        // Update TextViews
        val tvTotalOccurrences: TextView = findViewById(R.id.tvTotalOccurrences)
        val tvMostLeastOccurrences: TextView = findViewById(R.id.tvMostLeastOccurrences)

        val total = occurrences.sum()
        val mostIndex = occurrences.indexOf(occurrences.maxOrNull() ?: 0)
        val leastIndex = occurrences.indexOf(occurrences.minOrNull() ?: 0)

        tvTotalOccurrences.text = "Total Occurrences: $total"
        tvMostLeastOccurrences.text =
            "Most: Juz ${mostIndex + 1} (${occurrences[mostIndex]}), " +
                    "Least: Juz ${leastIndex + 1} (${occurrences[leastIndex]})"
    }


    private fun setupPieChart() {
        val entries = ArrayList<PieEntry>()

        // Generate temporary data for all 30 Juz
        val occurrences = (1..30).map { (10..100).random() }
        for (i in occurrences.indices) {
            entries.add(PieEntry(occurrences[i].toFloat(), (i + 1).toString())) // Use index as label
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = ColorTemplate.COLORFUL_COLORS.toList()
        dataSet.sliceSpace = 2f
        dataSet.valueTextSize = 0f // Hide values on pie sections

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.isRotationEnabled = true
        pieChart.setHoleColor(Color.WHITE)
        pieChart.setEntryLabelTextSize(10f) // Keep only numbers (1-30)
        pieChart.setDrawEntryLabels(true)

        // Add MarkerView for long click
        val markerPie = CustomMarkerViewPie(this, R.layout.marker_view, occurrences)
        pieChart.marker = markerPie

        pieChart.invalidate()
    }


}
class CustomMarkerView(
    context: Context,
    layoutResource: Int,
    private val occurrences: List<Int>
) : MarkerView(context, layoutResource) {
    private val tvContent: TextView = findViewById(R.id.tvContent)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e is BarEntry) {
            val index = e.x.toInt() - 1 // Adjust for 0-based indexing
            tvContent.text = "Juz ${index + 1}: ${occurrences[index]} Occurrences"
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF((-width / 2).toFloat(), (-height).toFloat())
    }
}

class CustomMarkerViewPie(
    context: Context,
    layoutResource: Int,
    private val occurrences: List<Int>
) : MarkerView(context, layoutResource) {
    private val tvContent: TextView = findViewById(R.id.tvContent)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e is PieEntry) {
            val index = e.label.toInt() - 1 // Convert label back to index
            tvContent.text = "Juz ${index + 1}: ${occurrences[index]} Occurrences"
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF((-width / 2).toFloat(), (-height).toFloat())
    }
}



