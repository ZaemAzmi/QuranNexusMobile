package com.example.qurannexus.features.bookmark

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.qurannexus.databinding.FragmentBookmarkWordsBinding
import com.example.qurannexus.features.words.WordDetailsActivity
import android.content.Intent
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.qurannexus.R
import com.example.qurannexus.core.interfaces.QuranApi
import com.example.qurannexus.core.network.ApiService
import com.example.qurannexus.core.utils.QuranMetadata
import com.example.qurannexus.features.bookmark.models.BookmarkWord
import com.example.qurannexus.features.bookmark.models.BookmarksResponse
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
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.formatter.ValueFormatter

import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
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
    private var authToken: String? = null
    private lateinit var listViewChip: Chip
    private lateinit var cloudViewChip: Chip

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
        setupQuranApi()
        fetchBookmarkedWords()

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
            putExtra("WORD_TEXT", word.word_text)
            putExtra("TRANSLATION", word.translation)
            putExtra("TRANSLITERATION", word.transliteration)
            putExtra("TOTAL_OCCURRENCES", word.total_occurrences)

            // First occurrence details
            putExtra("CHAPTER_ID", word.first_occurrence.chapter_id)
            putExtra("VERSE_NUMBER", word.first_occurrence.verse_number)
            putExtra("SURAH_NAME", word.first_occurrence.surah_name)
            putExtra("PAGE_ID", word.first_occurrence.page_id)
            putExtra("JUZ_NUMBER", word.first_occurrence.juz_id)
            putExtra("VERSE_TEXT", word.first_occurrence.verse_text)
            putExtra("AUDIO_URL", word.first_occurrence.audio_url)
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
            webLineWidth = 1f
            webColor = Color.LTGRAY
            webLineWidthInner = 1f
            webColorInner = Color.LTGRAY
            webAlpha = 100

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
            }

            xAxis.apply {
                textSize = 8f
                textColor = Color.BLACK
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val index = value.toInt()
                        return if (index % 10 == 0) (index + 1).toString() else ""
                    }
                }
                yOffset = 5f
            }

            yAxis.apply {
                axisMinimum = 0f
                axisMaximum = 100f
                setLabelCount(4, true)
                textSize = 9f
                valueFormatter = PercentageFormatter()
            }

            marker = ChapterMarkerView(context)
            rotationAngle = 90f
            minOffset = 80f
            setExtraOffsets(30f, 30f, 30f, 30f)
        }
    }
    private fun updateRadarChart(words: List<BookmarkWord>) {
        // Get unique words
        val uniqueWords = words.map { it.word_text }.distinct()

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
        val totalWordsPerChapter = (1..114).associateWith { 1000 }

        (1..114).forEach { chapterId ->
            val bookmarkedCount = bookmarkedWords[chapterId.toString()] ?: 0
            val totalWords = totalWordsPerChapter[chapterId] ?: 1000
            val percentage = (bookmarkedCount.toFloat() / totalWords) * 100

            entries.add(RadarEntry(percentage))
            labels.add(chapterId.toString())
            chapterIds.add(chapterId.toString())
        }

        val set = RadarDataSet(entries, "Words Learned (%)").apply {
            color = Color.parseColor("#2196F3")
            fillColor = Color.parseColor("#2196F3")
            setDrawFilled(true)
            fillAlpha = 120
            lineWidth = 2f
            valueTextSize = 0f
            isDrawHighlightCircleEnabled = true
            highlightCircleFillColor = Color.WHITE
            highlightCircleStrokeColor = Color.parseColor("#2196F3")
            highlightCircleStrokeWidth = 2f
        }

        radarChart.apply {
            data = RadarData(set)
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    e?.let {
                        val index = h?.x?.toInt() ?: return
                        val chapterId = chapterIds[index]
                        showChapterStats(
                            chapterId,
                            bookmarkedWords[chapterId] ?: 0,
                            totalWordsPerChapter[chapterId.toInt()] ?: 0,
                            it.y
                        )
                    }
                }
                override fun onNothingSelected() {}
            })
            animateXY(1000, 1000)
            invalidate()
        }
    }
    private class PercentageFormatter : ValueFormatter() {
        override fun getFormattedValue(value: Float): String = "${value.toInt()}%"
    }

    private fun showChapterStats(
        chapterId: String,
        bookmarkedCount: Int,
        totalWords: Int,
        percentage: Float
    ) {
        val context = requireContext()
        val dialog = AlertDialog.Builder(context)
        val dialogView = layoutInflater.inflate(R.layout.dialog_chapter_stats, null)
        val surahDetails = QuranMetadata.getInstance().getSurahDetails(chapterId.toInt())

        dialogView.apply {
            findViewById<TextView>(R.id.tvChapterName).text =
                "Chapter ${chapterId}: ${surahDetails?.englishName}"
            findViewById<TextView>(R.id.tvBookmarkedWords).text =
                "Progress: ${percentage.toInt()}% ($bookmarkedCount/$totalWords words)"
        }

        dialog.apply {
            setView(dialogView)
            setPositiveButton("Close", null)
            create().apply {
                window?.attributes?.windowAnimations = android.R.style.Animation_Dialog
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
    private fun setupQuranApi() {
        quranApi = ApiService.getQuranClient().create(QuranApi::class.java)
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
        val groupedWords = words.groupBy { it.word_text.first().toString() }
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
private class ChapterMarkerView(context: Context) : MarkerView(context, R.layout.layout_radar_chart_marker_view) {
    private val tvChapter: TextView = findViewById(R.id.tvChapter)
    private val tvPercentage: TextView = findViewById(R.id.tvPercentage)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let {
            tvChapter.text = "Chapter ${(highlight?.x?.toInt() ?: 0) + 1}"
            tvPercentage.text = "${it.y.toInt()}%"
        }
    }

    override fun getOffset(): MPPointF {
        return MPPointF((-(width / 2)).toFloat(), (-height).toFloat())
    }
}
