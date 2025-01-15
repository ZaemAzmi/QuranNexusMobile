package com.example.qurannexus.features.home

import android.content.Context
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.features.words.JuzPieChartManager
import com.example.qurannexus.features.words.models.WordDetails
import com.example.qurannexus.features.words.models.WordDetailsViewModel
import com.example.qurannexus.features.words.models.WordOccurrence
import com.example.qurannexus.features.words.models.WordOccurrencesAdapter
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
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.utils.MPPointF
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException

@AndroidEntryPoint
class WordDetailsActivity : AppCompatActivity() {
    private lateinit var barChart: BarChart
    private lateinit var pieChart: PieChart
    private lateinit var bookmarkButton: ImageView
    private val viewModel: WordDetailsViewModel by viewModels()
    private var isBookmarked = false
    private var authToken: String? = null
    private var currentWord: WordDetails? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_word_details)
        Log.d("WordDetailsActivity", "onCreate called")
        // Get auth token
        authToken = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("token", null)

        // Initialize views and setup UI
        initializeViews()
        setupBookmarkButton()
        setupCharts()

        // Load word data and update UI
        loadWordData()
    }

    private fun initializeViews() {
        Log.d("WordDetailsActivity", "initializeViews called")
        barChart = findViewById(R.id.barChart)
        pieChart = findViewById(R.id.pieChart)
        bookmarkButton = findViewById(R.id.bookmarkButton)
        // Back button
        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            onBackPressed()
        }

        // Audio playback setup
        val playAudioButton = findViewById<Button>(R.id.playAudioButton)
        val audioUrl = intent.getStringExtra("AUDIO_URL")
        playAudioButton.setOnClickListener {
            playAudio(audioUrl)
        }
    }
    private fun setupBookmarkButton() {
        Log.d("WordDetailsActivity", "setupBookmarkButton called")
        bookmarkButton.setOnClickListener {
            authToken?.let { token ->
                currentWord?.let { word ->
                    if (isBookmarked) {
                        viewModel.removeWordBookmark(token)
                    } else {
                        viewModel.addWordBookmark(token, word)
                    }
                }
            } ?: run {
                Toast.makeText(this, "Please login to bookmark words", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.isBookmarked.observe(this) { bookmarked ->
            isBookmarked = bookmarked
            bookmarkButton.setImageResource(
                if (bookmarked) R.drawable.ic_heart_bookmarked
                else R.drawable.ic_heart
            )
        }
    }
    private fun loadWordData() {
        // Get word details from intent
        val wordId = intent.getStringExtra("WORD_ID") ?: return finish()

        // Load from JSON and check bookmark status
        authToken?.let { token ->
            viewModel.loadWordDetails(wordId, token)
        } ?: run {
            viewModel.loadWordDetails(wordId, "") // Load just word details without bookmark check
        }

        // Update UI with intent data
        updateWordInfoFromIntent()
    }

    private fun updateWordInfoFromIntent() {
        findViewById<TextView>(R.id.wordText).text = intent.getStringExtra("WORD_TEXT")
        findViewById<TextView>(R.id.translationText).text =
            "Translation: ${intent.getStringExtra("TRANSLATION")}"
        findViewById<TextView>(R.id.transliterationText).text =
            "Transliteration: ${intent.getStringExtra("TRANSLITERATION")}"
        findViewById<TextView>(R.id.surahNameText).text =
            "Surah: ${intent.getStringExtra("SURAH_NAME_ARABIC")} (${intent.getStringExtra("SURAH_NAME_ENGLISH")})"
        findViewById<TextView>(R.id.ayahKeyText).text =
            "Ayah Key: ${intent.getStringExtra("AYAH_KEY")}"
        findViewById<TextView>(R.id.surahNumberText).text =
            "Surah Number: ${intent.getStringExtra("SURAH_NUMBER")}"
        findViewById<TextView>(R.id.lineNumberText).text =
            "Line Number: ${intent.getIntExtra("LINE_NUMBER", -1)}"
        findViewById<TextView>(R.id.wordNumberText).text =
            "Word Number: ${intent.getStringExtra("WORD_NUMBER")}"
        findViewById<TextView>(R.id.pageIdText).text =
            "Page ID: ${intent.getStringExtra("PAGE_ID")}"


    }



    private fun setupCharts() {
        viewModel.wordDetails.observe(this) { word ->
            currentWord = word
            updateCharts(word.juz_distribution)
        }

        // Bar chart click listener
        barChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                e?.let {
                    val juzNumber = it.x.toInt()
                    viewModel.getWordOccurrencesInJuz(juzNumber)
                }
            }
            override fun onNothingSelected() {}
        })

        // Show occurrences when available
        viewModel.occurrencesInJuz.observe(this) { occurrences ->
            showOccurrencesBottomSheet(occurrences)
        }
    }

    private fun updateCharts(juzDistribution: Map<String, Int>) {
        setupBarChart(juzDistribution)
        setupPieChart(juzDistribution)
    }

    private fun setupBarChart(juzDistribution: Map<String, Int>) {
        val entries = juzDistribution.map { (juz, count) ->
            BarEntry(juz.toFloat(), count.toFloat())
        }

        val dataSet = BarDataSet(entries, "")
        dataSet.color = Color.BLUE
        dataSet.valueTextSize = 8f
        dataSet.valueTextColor = Color.TRANSPARENT

        barChart.apply {
            data = BarData(dataSet)
            description.isEnabled = false
            setFitBars(true)
            animateY(1400)

            // X-axis setup
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textSize = 10f
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(
                    (1..30).map { it.toString() } + listOf("Juz")
                )
                labelRotationAngle = 0f
            }

            // Y-axis setup
            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = (juzDistribution.values.maxOrNull() ?: 100).toFloat()
                labelCount = 5
                setDrawGridLines(false)
            }
            axisRight.isEnabled = false

            invalidate()
        }

        // Update statistics text
        val total = juzDistribution.values.sum()
        val maxEntry = juzDistribution.maxByOrNull { it.value }
        val minEntry = juzDistribution.minByOrNull { it.value }

        findViewById<TextView>(R.id.tvTotalOccurrences).text = "Total Occurrences: $total"
        findViewById<TextView>(R.id.tvMostLeastOccurrences).text =
            "Most: Juz ${maxEntry?.key} (${maxEntry?.value}), " +
                    "Least: Juz ${minEntry?.key} (${minEntry?.value})"
    }

    private fun setupPieChart(juzDistribution: Map<String, Int>) {
        // Filter out entries with count = 0
        val entries = juzDistribution
            .filter { (_, count) -> count > 0 }  // Only keep entries with count > 0
            .map { (juz, count) ->
                PieEntry(count.toFloat(), juz)
            }

        val dataSet = PieDataSet(entries, "").apply {
            colors = ColorTemplate.COLORFUL_COLORS.toList()
            sliceSpace = 2f
            valueTextSize = 0f
        }

        pieChart.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            isRotationEnabled = true
            setHoleColor(Color.WHITE)
            setEntryLabelTextSize(10f)
            setDrawEntryLabels(true)
            animateY(1000)
            invalidate()
        }
    }

    private fun showOccurrencesBottomSheet(occurrences: List<WordOccurrence>) {
        BottomSheetDialog(this).apply {
            setContentView(R.layout.layout_word_occurrences_bottom_sheet)

            findViewById<RecyclerView>(R.id.occurrencesRecyclerView)?.apply {
                layoutManager = LinearLayoutManager(this@WordDetailsActivity)
                adapter = WordOccurrencesAdapter(occurrences) { occurrence ->
                    navigateToVerse(occurrence.chapter_id, occurrence.verse_number)
                }
            }

            show()
        }
    }

    private fun playAudio(audioUrl: String?) {
        Log.d("playdio", "Audio URL: $audioUrl")
        audioUrl?.let {
            val mediaPlayer = MediaPlayer()
            try {
                mediaPlayer.setDataSource(it)
                mediaPlayer.prepare()
                mediaPlayer.start()
            } catch (e: IOException) {
                Toast.makeText(this, "Failed to play audio", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToVerse(chapterId: String, verseNumber: String) {
        // TODO: Implement navigation to verse using your existing logic
        Toast.makeText(this, "Navigating to chapter $chapterId verse $verseNumber",
            Toast.LENGTH_SHORT).show()
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



