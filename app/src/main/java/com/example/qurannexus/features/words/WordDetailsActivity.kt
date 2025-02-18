package com.example.qurannexus.features.words

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.core.activities.MainActivity
import com.example.qurannexus.core.interfaces.QuranApi
import com.example.qurannexus.core.network.ApiService
import com.example.qurannexus.features.words.models.WordDetails
import com.example.qurannexus.features.words.models.WordDetailsViewModel
import com.example.qurannexus.features.words.models.WordOccurrence
import com.example.qurannexus.features.words.models.WordOccurrenceResponse
import com.example.qurannexus.features.words.models.WordOccurrencesBottomSheetAdapter
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.utils.MPPointF
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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
    private lateinit var quranApi: QuranApi

    private var currentJuzNumber: Int = 0
    private var currentPage = 1
    private lateinit var occurrencesAdapter: WordOccurrencesBottomSheetAdapter
    private var bottomSheetDialog: BottomSheetDialog? = null

    private var isLoadingMore = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_word_details)
        // Get auth token
        authToken = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("token", null)
        quranApi = ApiService.getQuranClient().create(QuranApi::class.java)
        initializeViews()
        setupBookmarkButton()
        setupCharts()
        // Initialize adapter early
        occurrencesAdapter = WordOccurrencesBottomSheetAdapter { occurrence ->
            navigateToVerse(occurrence.chapter_id, occurrence.verse_number)
        }
        // Get word text from intent
        val wordText = intent.getStringExtra("WORD_TEXT") ?: return finish()

        // Observe data
        viewModel.wordDistribution.observe(this) { distribution ->
            updateCharts(distribution.juz_distribution)
        }

        viewModel.occurrences.observe(this) { occurrences ->
            showOccurrencesBottomSheet(occurrences)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            showLoading(isLoading)
        }

        viewModel.error.observe(this) { error ->
            showError(error)
        }
        // Add observation of new ViewModel states
        viewModel.isLoadingMore.observe(this) { isLoading ->
            occurrencesAdapter.setLoading(isLoading && viewModel.hasMorePages.value == true)
        }

        viewModel.hasMorePages.observe(this) { hasMore ->
            if (hasMore && currentPage > 1) {
                currentPage++
            }
        }
        viewModel.loadWordData(wordText)
        updateWordInfoFromIntent() // Keep this to update UI with intent data

        // Check bookmark status if user is logged in
        authToken?.let { token ->
            viewModel.checkBookmarkStatus(token, wordText)
        }
    }

    // Update when user clicks on chart
//    private fun onJuzSelected(juzNumber: Int) {
//        val wordText = intent.getStringExtra("WORD_TEXT") ?: return
//        viewModel.loadWordData(wordText, juzNumber)
//    }

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
                val wordText = intent.getStringExtra("WORD_TEXT")
                if (isBookmarked) {
                    wordText?.let { text -> viewModel.removeWordBookmark(token, text) }
                } else {
                    // Get current word occurrence from intent extras
                    val currentOccurrence = WordOccurrence(
//                        word_id = intent.getStringExtra("WORD_ID") ?: "", //#
                        word_id = wordText ?: "",
                        word_text = wordText ?: "",
                        translation = intent.getStringExtra("TRANSLATION") ?: "",
                        transliteration = intent.getStringExtra("TRANSLITERATION"),
                        chapter_id = intent.getStringExtra("CHAPTER_ID") ?: "",
                        verse_number = intent.getStringExtra("VERSE_NUMBER") ?: "",
                        verse_text = intent.getStringExtra("VERSE_TEXT"),
                        ayah_key = "${intent.getStringExtra("CHAPTER_ID")}:${intent.getStringExtra("VERSE_NUMBER")}",
                        juz_number = intent.getStringExtra("JUZ_NUMBER") ?: "",
                        position = intent.getIntExtra("POSITION", 0),
                        audio_url = intent.getStringExtra("AUDIO_URL")
                    )

                    // Get total occurrences from distribution data
                    val totalOccurrences = viewModel.wordDistribution.value?.total_occurrences ?: 0
                    viewModel.addWordBookmark(token, currentOccurrence, totalOccurrences)
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
//        findViewById<TextView>(R.id.surahNumberText).text =
//            "Surah Number: ${intent.getStringExtra("SURAH_NUMBER")}"
//        findViewById<TextView>(R.id.lineNumberText).text =
//            "Line Number: ${intent.getIntExtra("LINE_NUMBER", -1)}"
//        findViewById<TextView>(R.id.wordNumberText).text =
//            "Word Number: ${intent.getStringExtra("WORD_NUMBER")}"
        findViewById<TextView>(R.id.verseText).text =
            "Full Verse: ${intent.getStringExtra("VERSE_TEXT")}"
        findViewById<TextView>(R.id.pageIdText).text =
            "Page ID: ${intent.getStringExtra("PAGE_ID")}"


    }

    private fun setupCharts() {
        // Bar chart click listener
        barChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                e?.let {
                    currentJuzNumber = it.x.toInt()
                    currentPage = 1
                    loadOccurrences(currentJuzNumber)
                    showOccurrencesBottomSheet(emptyList())
                }
            }

            override fun onNothingSelected() {}
        })

        // Pie chart click listener
        pieChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                Log.d("PieChart", "Value selected: ${e?.toString()}")
                if (e is PieEntry) {
                    // Extract juz number by removing "Juz " prefix and parsing remaining number
                    val juzStr = e.label?.replace("Juz ", "")?.trim()
                    Log.d("PieChart", "Extracted juz string: $juzStr")
                    // Extract juz number from label
                    try {
                        val juzNumber = juzStr?.toInt()
                        if (juzNumber != null) {
                            Log.d("PieChart", "Successfully parsed juz number: $juzNumber")
                            currentJuzNumber = juzNumber
                            currentPage = 1
                            loadOccurrences(juzNumber)
                            showOccurrencesBottomSheet(emptyList())
                        } else {
                            Log.e("PieChart", "Juz number was null after parsing")
                        }
                    } catch (ex: NumberFormatException) {
                        Log.e("PieChart", "Error parsing juz number from: $juzStr", ex)
                    }
                }
            }

            override fun onNothingSelected() {
                Log.d("PieChart", "Nothing selected")
            }
        })

        // Show occurrences when available
        viewModel.occurrences.observe(this) { occurrences ->
            showOccurrencesBottomSheet(occurrences)
        }
    }

    private fun updateCharts(juzDistribution: Map<String, Int>) {
        setupBarChart(juzDistribution)
        setupPieChart(juzDistribution)
    }

    private fun
            showLoading(isLoading: Boolean) {
        // Implement loading UI - you might want to show/hide a ProgressBar
        findViewById<ProgressBar>(R.id.progressBar)?.visibility =
            if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showError(error: String) {
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
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
        try {
            val total = juzDistribution.values.sum().toFloat()

            // Create entries
            val entries = juzDistribution
                .filter { (_, count) -> count > 0 }
                .map { (juz, count) ->
                    val percentage = (count.toFloat() / total) * 100f
                    PieEntry(count.toFloat(), "Juz $juz", percentage)
                }
                .sortedByDescending { it.value }

            // Set up colors and data
            val dataSet = PieDataSet(entries, "").apply {
                colors = mutableListOf<Int>().apply {
                    entries.forEach { entry ->
                        val percentage = entry.data as Float
                        add(
                            when {
                                percentage > 10f -> Color.parseColor("#4CAF50")  // Green
                                percentage > 5f -> Color.parseColor("#FFC107")   // Yellow
                                percentage > 2f -> Color.parseColor("#F44336")   // Red
                                else -> Color.parseColor("#9C27B0")             // Purple
                            }
                        )
                    }
                }

                setValueTextColors(listOf(Color.BLACK))
                valueTextSize = 11f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val percentage = (value / total) * 100f
                        return if (percentage >= 1f) String.format("%.1f%%", percentage) else ""
                    }
                }

                yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                valueLinePart1Length = 0.6f
                valueLinePart2Length = 0.3f
                valueLineWidth = 1.5f
                valueLineColor = Color.GRAY
                sliceSpace = 2f
            }

            // Configure pie chart
            pieChart.apply {
                setExtraOffsets(
                    30f,
                    30f,
                    30f,
                    100f
                )  // Important: Give extra space at bottom for legend
                data = PieData(dataSet)
                description.isEnabled = false
                isRotationEnabled = true

                // Center settings
                isDrawHoleEnabled = true
                setHoleColor(Color.WHITE)
                setTransparentCircleColor(Color.WHITE)
                setTransparentCircleAlpha(110)
                holeRadius = 35f
                transparentCircleRadius = 40f

                // Configure legend - THIS IS THE KEY PART
                legend.apply {
                    isEnabled = true
                    verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                    horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                    orientation = Legend.LegendOrientation.VERTICAL
                    setDrawInside(false)
                    yOffset = 10f
                    xOffset = 10f
                    yEntrySpace = 10f
                    textSize = 12f
                    formSize = 12f
                    form = Legend.LegendForm.CIRCLE
                    textColor = Color.BLACK

                    // Create custom legend entries
                    val customEntries = arrayOf(
                        LegendEntry(
                            "Frequent (>10%)",
                            Legend.LegendForm.CIRCLE,
                            Float.NaN,
                            Float.NaN,
                            null,
                            Color.parseColor("#4CAF50")
                        ),
                        LegendEntry(
                            "Common (5-10%)",
                            Legend.LegendForm.CIRCLE,
                            Float.NaN,
                            Float.NaN,
                            null,
                            Color.parseColor("#FFC107")
                        ),
                        LegendEntry(
                            "Occasional (2-5%)",
                            Legend.LegendForm.CIRCLE,
                            Float.NaN,
                            Float.NaN,
                            null,
                            Color.parseColor("#F44336")
                        ),
                        LegendEntry(
                            "Rare (<2%)",
                            Legend.LegendForm.CIRCLE,
                            Float.NaN,
                            Float.NaN,
                            null,
                            Color.parseColor("#9C27B0")
                        )
                    )
                    setCustom(customEntries)
                }

                // Center text and animation
                centerText = "Total\n${total.toInt()}"
                setCenterTextSize(14f)

                // Important: Set minimum height to ensure legend fits
                minimumHeight = 600

                // Animate
                animateY(800, Easing.EaseInOutQuad)
                invalidate()
            }

        } catch (e: Exception) {
            Log.e("PieChart", "Error setting up pie chart", e)
        }
    }

    private fun setupPieChartLegend(pieChart: PieChart) {
        pieChart.legend.apply {
            isEnabled = true
            verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            orientation = Legend.LegendOrientation.VERTICAL
            setDrawInside(false)
            direction = Legend.LegendDirection.LEFT_TO_RIGHT
            textSize = 12f
            formSize = 12f
            xEntrySpace = 10f
            yEntrySpace = 10f
            setWordWrapEnabled(true)
            maxSizePercent = 0.95f  // Allow legend to take up more space
            textColor = Color.BLACK
            form = Legend.LegendForm.CIRCLE

            // Create legend entries manually
            setCustom(
                arrayOf(
                    LegendEntry(
                        "Frequent (>10%)", Legend.LegendForm.CIRCLE,
                        12f, 2f, null, Color.parseColor("#4CAF50")
                    ),
                    LegendEntry(
                        "Common (5-10%)", Legend.LegendForm.CIRCLE,
                        12f, 2f, null, Color.parseColor("#FFC107")
                    ),
                    LegendEntry(
                        "Occasional (2-5%)", Legend.LegendForm.CIRCLE,
                        12f, 2f, null, Color.parseColor("#F44336")
                    ),
                    LegendEntry(
                        "Rare (<2%)", Legend.LegendForm.CIRCLE,
                        12f, 2f, null, Color.parseColor("#9C27B0")
                    )
                )
            )
        }

        // Add extra bottom offset to accommodate legend
        pieChart.setExtraOffsets(20f, 20f, 20f, 140f)  // Increased bottom offset
    }

    private fun updateStatisticsText(juzDistribution: Map<String, Int>, total: Float) {
        val maxEntry = juzDistribution.maxByOrNull { it.value }
        val minEntry = juzDistribution.filterValues { it > 0 }.minByOrNull { it.value }

        findViewById<TextView>(R.id.tvTotalOccurrences).text =
            "Total Occurrences: ${total.toInt()}"

        findViewById<TextView>(R.id.tvMostLeastOccurrences).text = buildString {
            append("Most: Juz ${maxEntry?.key} (")
            append("%.1f%%".format((maxEntry?.value?.toFloat() ?: 0f) / total * 100))
            append(")\nLeast: Juz ${minEntry?.key} (")
            append("%.1f%%".format((minEntry?.value?.toFloat() ?: 0f) / total * 100))
            append(")")
        }
    }

    private fun loadOccurrences(juzNumber: Int) {
        viewModel.loadOccurrencesForJuz(juzNumber, currentPage)
    }


    private fun showOccurrencesBottomSheet(occurrences: List<WordOccurrence>) {
        if(isFinishing)return

        if (bottomSheetDialog == null) {
            bottomSheetDialog = BottomSheetDialog(this).apply {
                setContentView(R.layout.layout_word_occurrences_bottom_sheet)

//                // Initialize the adapter
//                occurrencesAdapter = WordOccurrencesBottomSheetAdapter { occurrence ->
//                    navigateToVerse(occurrence.chapter_id, occurrence.verse_number)
//                }

                findViewById<RecyclerView>(R.id.occurrencesRecyclerView)?.apply {
                    layoutManager = LinearLayoutManager(this@WordDetailsActivity)
                    adapter = occurrencesAdapter

                    // Add scroll listener for pagination
                    addOnScrollListener(object : RecyclerView.OnScrollListener() {
                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                            val visibleItemCount = layoutManager.childCount
                            val totalItemCount = layoutManager.itemCount
                            val firstVisibleItemPosition =
                                layoutManager.findFirstVisibleItemPosition()

                            if (!viewModel.isLoadingMore.value!! &&
                                viewModel.hasMorePages.value == true &&
                                (visibleItemCount + firstVisibleItemPosition) >= totalItemCount &&
                                firstVisibleItemPosition >= 0
                            ) {
                                loadOccurrences(currentJuzNumber)
                            }
                        }
                    })
                }
            }
        }
        // Update title every time bottom sheet is shown
        bottomSheetDialog?.findViewById<TextView>(R.id.titleText)?.text =
            "Occurrences in Juz $currentJuzNumber"
        // Update adapter with initial data
        occurrencesAdapter.submitList(occurrences)
        bottomSheetDialog?.show()
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
        bottomSheetDialog?.dismiss()
        // Get user's layout preference
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val isByPage = sharedPreferences.getBoolean("recitation_layout_by_page", false)

        // Create navigation intent
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("NAVIGATE_TO_RECITATION", true)
            putExtra("CHAPTER_ID", chapterId)
            putExtra("VERSE_NUMBER", verseNumber)
            putExtra("IS_BY_PAGE", isByPage)

            if(isByPage){
                putExtra("CURRENT_SURAH_INDEX", chapterId.toInt() - 1)
                putExtra("SCROLL_TO_VERSE", verseNumber.toInt())
            }
        }

        startActivity(intent)
        finish()
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



