package com.example.qurannexus.features.bookmark

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.qurannexus.R
import com.example.qurannexus.core.interfaces.QuranApi
import com.example.qurannexus.core.network.ApiService
import com.example.qurannexus.core.utils.QuranMetadata
import com.example.qurannexus.databinding.FragmentBookmarkWordsBinding
import com.example.qurannexus.features.bookmark.interfaces.BookmarkApi
import com.example.qurannexus.features.bookmark.models.BookmarkWord
import com.example.qurannexus.features.bookmark.models.BookmarksResponse
import com.example.qurannexus.features.bookmark.models.ChapterWordCountsResponse
import com.example.qurannexus.features.words.WordDetailsActivity
import com.example.qurannexus.features.words.models.AccordionAdapter
import com.example.qurannexus.features.words.models.AccordionSection
import com.example.qurannexus.features.words.models.WordsChaptersDistributionResponse
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.MPPointF
import com.google.android.material.chip.Chip
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BookmarkWordsFragment : Fragment() {

    private var _binding: FragmentBookmarkWordsBinding? = null
    private val binding get() = _binding!!
    private lateinit var accordionAdapter: AccordionAdapter
    private lateinit var quranApi: QuranApi
    private lateinit var bookmarkApi : BookmarkApi
    private var authToken: String? = null
    private lateinit var listViewChip: Chip
    private lateinit var cloudViewChip: Chip
    private var chapterWordCounts: Map<String, Int> = emptyMap()
    private lateinit var radarChart: RadarChart
    private val colors = listOf(
        Color.parseColor("#2196F3"),  // Blue
        Color.parseColor("#4CAF50"),  // Green
        Color.parseColor("#FFC107"),  // Amber
        Color.parseColor("#9C27B0")   // Purple
    )
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookmarkWordsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupWindowInsets()
        setupViews()
        setupApis()
        fetchBookmarkedWords()
        fetchChapterWordCounts()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val navigationBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val meowNavHeight = resources.getDimensionPixelSize(R.dimen.meow_bottom_nav_height)

            // Add extra padding if device has navigation bar
            val totalBottomPadding = meowNavHeight + navigationBars.bottom

            // Apply the padding to the content container
            binding.wordCloudView.setPadding(0, 0, 0, totalBottomPadding)

            windowInsets
        }
    }
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Apply padding to content to account for system bars (including bottom nav)
            view.updatePadding(
                bottom = insets.bottom
            )

            WindowInsetsCompat.CONSUMED
        }
    }
    private fun navigateToWordDetails(word: BookmarkWord) {
        val intent = Intent(requireContext(), WordDetailsActivity::class.java).apply {
            putExtra("WORD_TEXT", word.itemProperties.wordText)
            putExtra("TRANSLATION", word.itemProperties.translation)
            putExtra("TRANSLITERATION", word.itemProperties.transliteration)
            putExtra("TOTAL_OCCURRENCES", word.itemProperties.totalOccurrences)

            // First occurrence details
            putExtra("CHAPTER_ID", word.itemProperties.firstOccurrence.chapterId)
            putExtra("VERSE_NUMBER", word.itemProperties.firstOccurrence.verseNumber)
            putExtra("SURAH_NAME", word.itemProperties.firstOccurrence.surahName)
            putExtra("PAGE_ID", word.itemProperties.firstOccurrence.pageId)
            putExtra("JUZ_NUMBER", word.itemProperties.firstOccurrence.juzId)
            putExtra("VERSE_TEXT", word.itemProperties.firstOccurrence.verseText)
            putExtra("AUDIO_URL", word.itemProperties.firstOccurrence.audioUrl)
        }
        startActivity(intent)
    }

    private fun setupViews() {
        // Setup RecyclerView
        accordionAdapter = AccordionAdapter { wordDetail ->
            navigateToWordDetails(wordDetail)
        }
        binding.recyclerViewAccordion.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = accordionAdapter
        }

        // Setup view type chips
        setupChips()

        // Setup word cloud and controls
        setupWordCloudControls()
        setupRadarChart()
    }
    private fun setupChips() {
        // Create view type chips
        binding.viewTypeChipGroup.apply {
            addView(createChip("List View"))
            addView(createChip("Word Cloud"))
            addView(createChip("Analytics"))
            // Set initial selection
            check(getChildAt(0).id)

            // Handle chip selection
            setOnCheckedStateChangeListener { group, checkedIds ->
                if (checkedIds.isNotEmpty()) {
                    when (group.findViewById<Chip>(checkedIds.first())?.text) {
                        "List View" -> showListView()
                        "Word Cloud" -> showWordCloudView()
                        "Analytics" -> showAnalyticsView()
                    }
                } else {
                    // Ensure at least one chip is always selected
                    check(getChildAt(0).id)
                }
            }
        }

//        // Setup filter chips if needed
//        binding.chipGroupFilter.apply {
//            binding.chipArabic.isChecked = true
//        }
    }

    private fun setupRadarChart() {
        radarChart = binding.radarChart
        radarChart.apply {
            description.isEnabled = false
            webLineWidth = 1.2f
            webColor = Color.parseColor("#A5D6A7") // Light green for web lines
            webLineWidthInner = 0.8f
            webColorInner = Color.parseColor("#A5D6A7") // Same light green
            webAlpha = 150 // More visible

            setTouchEnabled(true)
            isRotationEnabled = true
            isHighlightPerTapEnabled = true

            legend.apply {
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                textSize = 12f
                yOffset = 15f
                form = Legend.LegendForm.CIRCLE
                formSize = 10f
                formLineWidth = 2f
                xEntrySpace = 15f
                textColor = Color.parseColor("#1E4620") // Dark green for better visibility
            }

            xAxis.apply {
                textSize = 9f
                textColor = Color.parseColor("#1E4620") // Dark green
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val index = value.toInt()
                        return if (index % 20 == 0) (index + 1).toString() else ""
                    }
                }
                yOffset = 0f
            }

            yAxis.apply {
                axisMinimum = 0f
                axisMaximum = 100f
                setLabelCount(5, true)
                textSize = 9f
                textColor = Color.parseColor("#1E4620") // Dark green
                valueFormatter = PercentageFormatter()
            }

            marker = ChapterMarkerView(context, this@BookmarkWordsFragment)
            rotationAngle = 90f
            minOffset = 60f
            setExtraOffsets(20f, 20f, 20f, 20f)
        }
    }
    private fun updateRadarChart(words: List<BookmarkWord>) {
        // Get unique words
        val uniqueWords = words.map { it.itemProperties.wordText }.distinct()

        // Fetch distribution for all words in one call
        quranApi.getWordsChaptersDistribution(uniqueWords)
            .enqueue(object : Callback<WordsChaptersDistributionResponse> {
                override fun onResponse(
                    call: Call<WordsChaptersDistributionResponse>,
                    response: Response<WordsChaptersDistributionResponse>
                ) {
                    if (response.isSuccessful) {
                        val chaptersDistribution = response.body()?.data?.chapters ?: emptyMap()
                        updateRadarChartWithDistribution(chaptersDistribution)
                    }
                }

                override fun onFailure(call: Call<WordsChaptersDistributionResponse>, t: Throwable) {
                    Toast.makeText(context, "Failed to load distribution: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
    private fun updateRadarChartWithDistribution(bookmarkedWords: Map<String, Int>) {
        val entries = ArrayList<RadarEntry>()
        val labels = ArrayList<String>()
        val chapterIds = ArrayList<String>()

        (1..114).forEach { chapterId ->
            val chapterIdStr = chapterId.toString()
            val bookmarkedCount = bookmarkedWords[chapterIdStr] ?: 0
            val totalWords = chapterWordCounts[chapterIdStr] ?: 0
            val percentage = if (totalWords > 0) {
                (bookmarkedCount.toFloat() / totalWords) * 100
            } else {
                0f
            }
            entries.add(RadarEntry(percentage))
            labels.add(chapterIdStr)
            chapterIds.add(chapterIdStr)
        }

        val set = RadarDataSet(entries, "Words Learned (%)").apply {
            color = Color.parseColor("#0288D1") // Darker blue
            fillColor = Color.parseColor("#B3E5FC") // Light blue
            setDrawFilled(true)
            fillAlpha = 40 // Lower alpha
            lineWidth = 2f
            valueTextSize = 0f
            isDrawHighlightCircleEnabled = true
            highlightCircleFillColor = Color.WHITE
            highlightCircleStrokeColor = Color.parseColor("#0288D1")
            highlightCircleStrokeWidth = 2f
            highlightCircleInnerRadius = 2f
            highlightCircleOuterRadius = 4f
        }

        radarChart.apply {
            data = RadarData(set)
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    e?.let {
                        // The marker view will be shown automatically
                        // No need to call any additional methods
                    }
                }
                override fun onNothingSelected() {
                    highlightValue(null) // Clear highlight when clicking away
                }
            })
            highlightValue(null) // Clear any existing highlights
            animateXY(1000, 1000)
            invalidate()
        }
    }

    fun getChapterWordCount(chapterId: String): Int {
        return chapterWordCounts[chapterId] ?: 0
    }
    private class PercentageFormatter : ValueFormatter() {
        override fun getFormattedValue(value: Float): String = "${value.toInt()}%"
    }
    private fun fetchChapterWordCounts() {
        bookmarkApi.getChapterWordCounts().enqueue(object : Callback<ChapterWordCountsResponse> {
            override fun onResponse(
                call: Call<ChapterWordCountsResponse>,
                response: Response<ChapterWordCountsResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    chapterWordCounts = response.body()?.data?.wordCounts ?: emptyMap()
                }
            }

            override fun onFailure(call: Call<ChapterWordCountsResponse>, t: Throwable) {
                Toast.makeText(context, "Failed to load chapter word counts: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun showChapterStats(
        chapterId: String,
        bookmarkedCount: Int,
        totalWords: Int,
        percentage: Float
    ) {
        val context = requireContext()
        val dialog = AlertDialog.Builder(context, R.style.TransparentDialog)
        val dialogView = layoutInflater.inflate(R.layout.dialog_chapter_stats_enhanced, null)
        val surahDetails = QuranMetadata.getInstance().getSurahDetails(chapterId.toInt())

        dialogView.apply {
            findViewById<TextView>(R.id.tvChapterName).text =
                "Chapter ${chapterId}: ${surahDetails?.englishName}"
            findViewById<TextView>(R.id.tvBookmarkedWords).text =
                "$bookmarkedCount words bookmarked"
            findViewById<TextView>(R.id.tvProgressLabel).text =
                "${percentage.toInt()}% Complete"
            findViewById<ProgressBar>(R.id.progressBar).progress = percentage.toInt()
        }

        dialog.apply {
            setView(dialogView)
            create().apply {
                window?.setBackgroundDrawableResource(android.R.color.transparent)
                window?.attributes?.windowAnimations = R.style.DialogAnimation
                show()
            }
        }
    }
    private fun createChip(text: String): Chip {
        return Chip(requireContext()).apply {
            this.text = text
            isCheckable = true
            chipBackgroundColor = ColorStateList.valueOf(resources.getColor(R.color.white, null))
        }
    }
    private fun setupWordCloudControls() {
        binding.wordCloudView.apply {
            onWordClickListener = { word ->
                navigateToWordDetails(word)
            }
            setRotationSpeed(0.5f)
        }

        // Toggle rotation button
        binding.toggleRotationButton.setOnClickListener {
            binding.wordCloudView.toggleRotation()
            binding.toggleRotationButton.setImageResource(
                if (binding.wordCloudView.isRotating) R.drawable.ic_pause
                else R.drawable.ic_play_audio
            )
        }

        // Speed control slider
        binding.speedSlider.apply {
            value = 0.5f
            addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    binding.wordCloudView.setRotationSpeed(value)
                }
            }
        }
    }

    private fun showListView() {
        binding.recyclerViewAccordion.visibility = View.VISIBLE
        binding.wordCloudContainer.visibility = View.GONE
        binding.radarChart.visibility = View.GONE
    }

    private fun showWordCloudView() {
        binding.recyclerViewAccordion.visibility = View.GONE
        binding.wordCloudContainer.visibility = View.VISIBLE
        binding.radarChart.visibility = View.GONE
    }

    private fun showAnalyticsView() {
        binding.recyclerViewAccordion.visibility = View.GONE
        binding.wordCloudContainer.visibility = View.GONE
        binding.radarChart.visibility = View.VISIBLE
    }
    private fun setupApis() {
        quranApi = ApiService.getQuranClient().create(QuranApi::class.java)
        bookmarkApi = ApiService.getQuranClient().create(BookmarkApi::class.java)

        authToken = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("token", null)
    }

    private fun fetchBookmarkedWords() {
        if (authToken == null) {
            Toast.makeText(context, "Please login to view bookmarks", Toast.LENGTH_SHORT).show()
            return
        }

        quranApi.getBookmarks("Bearer $authToken").enqueue(object : Callback<BookmarksResponse> {
            override fun onResponse(
                call: Call<BookmarksResponse>,
                response: Response<BookmarksResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val bookmarksResponse = response.body()!!
                    if (bookmarksResponse.status == "success") {
                        val words = bookmarksResponse.bookmarks.words
                        if (words.isEmpty()) {
                            Toast.makeText(context, "No bookmarked words found", Toast.LENGTH_SHORT).show()
                        } else {
                            updateViews(words)
                        }
                    }
                } else {
                    Toast.makeText(context, "Failed to load bookmarks", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<BookmarksResponse>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateViews(words: List<BookmarkWord>) {
        Log.d("BookmarkWordsFragment", "Updating views with ${words.size} words")

        // Update word cloud
        binding.wordCloudView.visibility = View.VISIBLE  // Make sure view is visible
        binding.wordCloudView.setWords(words)

        // Update radar chart
        updateRadarChart(words)

        // Update accordion list
        val groupedWords = words.groupBy { it.itemProperties.wordText.first().toString() }
        val sections = groupedWords.map { (letter, wordsList) ->
            AccordionSection(letter, wordsList)
        }.sortedBy { it.title }

        accordionAdapter.submitList(sections)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
// Custom marker view for better touch feedback
private class ChapterMarkerView(
    context: Context,
    private val fragment : BookmarkWordsFragment
) : MarkerView(context, R.layout.layout_radar_chart_marker_view) {
    private val tvChapterName: TextView = findViewById(R.id.tvChapterName)
    private val tvChapterNumber: TextView = findViewById(R.id.tvChapterNumber)
    private val tvWordsProgress: TextView = findViewById(R.id.tvWordsProgress)
    private val progressBar: ProgressBar = findViewById(R.id.progressBar)
    private val tvPercentage: TextView = findViewById(R.id.tvPercentage)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let {
            val chapterNumber = (highlight?.x?.toInt() ?: 0) + 1
            val surahDetails = QuranMetadata.getInstance().getSurahDetails(chapterNumber)
            val totalWords = fragment.getChapterWordCount(chapterNumber.toString())
            val learnedWords = (it.y * totalWords / 100).toInt()

            // Set chapter name (in Arabic and English)
            tvChapterName.text = surahDetails?.englishName ?: ""

            // Set chapter number and type
            tvChapterNumber.text = "Chapter ${chapterNumber} • ${surahDetails?.revelationPlace}"

            // Set words progress
            tvWordsProgress.text = "$learnedWords/$totalWords words"

            // Set progress bar
            progressBar.max = 100
            progressBar.progress = it.y.toInt()

            // Set percentage
            tvPercentage.text = "${it.y.toInt()}% Completed"
        }
    }

    override fun getOffset(): MPPointF {
        // Position the marker above the selected point
        return MPPointF((-(width / 2)).toFloat(), (-height - 10f))
    }
}