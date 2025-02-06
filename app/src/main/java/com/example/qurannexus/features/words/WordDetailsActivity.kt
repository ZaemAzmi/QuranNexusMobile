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
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
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
                        word_id = intent.getStringExtra("WORD_ID") ?: "", //#
                        word_text = wordText ?: "",
                        translation = intent.getStringExtra("TRANSLATION") ?: "",
                        transliteration = intent.getStringExtra("TRANSLITERATION"),
                        chapter_id = intent.getStringExtra("CHAPTER_ID") ?: "",
                        verse_number = intent.getStringExtra("VERSE_NUMBER") ?: "",
                        verse_text = intent.getStringExtra("VERSE_TEXT"),
                        ayah_key = intent.getStringExtra("AYAH_KEY") ?: "", //#
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
        // Bar chart click listener
        barChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                e?.let {
//                    val juzNumber = it.x.toInt()
//                    viewModel.getWordOccurrencesInJuz(juzNumber)
                    currentJuzNumber = it.x.toInt()
                    currentPage = 1 // Reset pagination when selecting new juz
                    showOccurrencesBottomSheet(emptyList()) // Show empty bottom sheet first
                    loadOccurrences(currentJuzNumber) // Load first page
                }
            }
            override fun onNothingSelected() {}
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
    private fun showLoading(isLoading: Boolean) {
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
    private fun loadOccurrences(juzNumber: Int) {
        if (isLoadingMore) return
        isLoadingMore = true
        val wordText = intent.getStringExtra("WORD_TEXT") ?: return

        quranApi.getWordOccurrences(
            wordText = wordText,  // Using the word text as the search query
            juzNumber = juzNumber,
            page = currentPage,
            perPage = 20
        ).enqueue(object : Callback<WordOccurrenceResponse> {
            override fun onResponse(
                call: Call<WordOccurrenceResponse>,
                response: Response<WordOccurrenceResponse>
            ) {
                Log.d("WordDetails", "Response: ${response.isSuccessful}, Code: ${response.code()}")
                Log.d("WordDetails", "Error body: ${response.errorBody()?.string()}")

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!.data
                    val hasMorePages = data.pagination.current_page < data.pagination.total_pages

                    if (currentPage == 1) {
                        occurrencesAdapter.submitList(data.words)
                    } else {
                        occurrencesAdapter.addItems(data.words)
                    }
                    occurrencesAdapter.setLoading(hasMorePages)

                    if (hasMorePages) currentPage++
                } else {
                    Toast.makeText(
                        this@WordDetailsActivity,
                        "Failed to load occurrences: ${response.errorBody()?.string()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                isLoadingMore = false
            }

            override fun onFailure(call: Call<WordOccurrenceResponse>, t: Throwable) {
                Log.e("WordDetails", "Network error", t)
                Toast.makeText(
                    this@WordDetailsActivity,
                    "Error: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
                isLoadingMore = false
            }
        })
    }

    private fun showOccurrencesBottomSheet(occurrences: List<WordOccurrence>) {
        if (bottomSheetDialog == null) {
            bottomSheetDialog = BottomSheetDialog(this).apply {
                setContentView(R.layout.layout_word_occurrences_bottom_sheet)

                // Initialize the adapter
                occurrencesAdapter = WordOccurrencesBottomSheetAdapter { occurrence ->
                    navigateToVerse(occurrence.chapter_id, occurrence.verse_number)
                }

                findViewById<RecyclerView>(R.id.occurrencesRecyclerView)?.apply {
                    layoutManager = LinearLayoutManager(this@WordDetailsActivity)
                    adapter = occurrencesAdapter

                    // Add scroll listener for pagination
                    addOnScrollListener(object : RecyclerView.OnScrollListener() {
                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                            val visibleItemCount = layoutManager.childCount
                            val totalItemCount = layoutManager.itemCount
                            val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                            if (!isLoadingMore &&
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
        }

        startActivity(intent)
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



