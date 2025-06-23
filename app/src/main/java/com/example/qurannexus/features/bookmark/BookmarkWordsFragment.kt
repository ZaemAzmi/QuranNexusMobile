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
import android.widget.ImageButton
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
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@AndroidEntryPoint
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
    private fun navigateToWordDetails(bookmarkWord: BookmarkWord) {
        val intent = Intent(requireContext(), WordDetailsActivity::class.java).apply {
            // Pass the ARABIC FORM TEXT as EXTRA_WORD_TEXT_FOR_PRESELECTION
            putExtra(
                WordDetailsActivity.EXTRA_WORD_TEXT_FOR_PRESELECTION,
                bookmarkWord.itemProperties.wordText
            )
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
        setupRadarChartInFragment()

        binding.radarChart.setOnClickListener{
            if(binding.radarChart.data != null && !binding.radarChart.isEmpty){
                showRadarChartInDialog()
            }else{
                Toast.makeText(context, "No chart data available", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnZoomRadarChart.setOnClickListener {
            if (binding.radarChart.data != null && !binding.radarChart.isEmpty) {
                showRadarChartInDialog()
            } else {
                Toast.makeText(requireContext(), "No chart data to display larger.", Toast.LENGTH_SHORT).show()
            }
        }
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

    private fun setupRadarChartInFragment() { // Renamed method
        // Use binding.radarChart directly
        configureRadarChartInstance(binding.radarChart, isDialog = false)
    }

    private fun showRadarChartInDialog() {
        if (context == null) return

        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.FullScreenDialogTheme)
        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_radar_chart_fullscreen, null)
        dialogBuilder.setView(dialogView)

        val dialogChartInstance = dialogView.findViewById<RadarChart>(R.id.dialogRadarChart)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btnCloseDialogRadar)

        configureRadarChartInstance(dialogChartInstance, isDialog = true) // Configure the dialog's chart

        // Set the SAME data from the fragment's chart
        if (binding.radarChart.data != null) {
            dialogChartInstance.data = binding.radarChart.data
            dialogChartInstance.animateXY(1000, 1000) // Re-animate for effect
            dialogChartInstance.invalidate() // Refresh display
        } else {
            Log.w("BookmarkWordsFragment", "Original chart data is null, cannot populate dialog chart.")
            // Optionally, show a message in the dialog's chart or don't show the dialog
        }


        val dialog = dialogBuilder.create()
        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()

        // To make the dialog truly full screen (optional, style might handle this)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    // Common configuration method for any RadarChart instance
    private fun configureRadarChartInstance(chart: RadarChart, isDialog: Boolean) {
        chart.apply {
            description.isEnabled = false
            // You can make these lines thicker and change their color to make them more prominent.
            webLineWidth = 1.5f // CHANGED: Increased from 1.2f for more emphasis
            webColor = Color.parseColor("#80CBC4") // CHANGED: A slightly darker teal color for the outer web
            webLineWidthInner = 1.0f // CHANGED: Increased from 0.8f
            webColorInner = Color.parseColor("#B2DFDB") // CHANGED: A lighter teal for the inner percentage lines
            webAlpha = 255 // CHANGED: Made fully opaque

            setTouchEnabled(true) // Allow touch for marker view in both
            isRotationEnabled = true
            isHighlightPerTapEnabled = true

            legend.apply {
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)

                textSize = if (isDialog) 14f else 12f // CHANGED: Increased text size from 12f/10f

                yOffset = 15f
                form = Legend.LegendForm.CIRCLE
                formSize = if (isDialog) 10f else 8f
                formLineWidth = 2f
                xEntrySpace = 15f
                textColor = Color.parseColor("#004D40")
            }

            xAxis.apply {
                textSize = if (isDialog) 12f else 11f // CHANGED: Increased text size from 11f/9f
                textColor = Color.parseColor("#004D40") // A darker teal for text
                // Updated XAxis Formatter for more labels in dialog
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val index = value.toInt()
                        val surahNumber = index + 1 // Assuming index is 0-based for 114 Surahs
                        if (surahNumber < 1 || surahNumber > 114) return "" // Bounds check

                        return if (isDialog) { // Show more labels in dialog
                            if (surahNumber == 1 || surahNumber == 114 || surahNumber % 10 == 0) {
                                surahNumber.toString()
                            } else {
                                ""
                            }
                        } else { // Fewer labels in fragment chart
                            if (surahNumber == 1 || surahNumber % 20 == 0) {
                                surahNumber.toString()
                            } else {
                                ""
                            }
                        }
                    }
                }
                yOffset = 0f
            }

            yAxis.apply {
                axisMinimum = 0f
                axisMaximum = 100f // Assuming percentage data
                setLabelCount(5, true)
                textSize = if (isDialog) 10f else 9f
                textColor = Color.parseColor("#1E4620")
                valueFormatter = PercentageFormatter() // Your existing formatter
            }

            // Re-assign marker. ChapterMarkerView needs context, pass it if it's not activity/fragment context.
            // Assuming ChapterMarkerView can use fragment.requireContext()
            marker = ChapterMarkerView(requireContext(), this@BookmarkWordsFragment)
            rotationAngle = 90f // Or adjust as needed
            minOffset = if (isDialog) 20f else 60f // Less offset for more space in dialog
            setExtraOffsets(10f, 10f, 10f, 10f) // Less extra offset for dialog
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
            valueTextSize = 0f // Hide values on lines for RadarChart, marker shows details
            color = Color.parseColor("#0E2E3E")
            fillColor = Color.parseColor("#108A83")
            setDrawFilled(true)
            fillAlpha = 160 // Slightly more opaque fill
            lineWidth = 2f
            isDrawHighlightCircleEnabled = true
            highlightCircleFillColor = Color.WHITE
            highlightCircleStrokeColor = Color.parseColor("#F4C430")
            highlightCircleStrokeWidth = 2f
            highlightCircleInnerRadius = 3f // Slightly larger marker points
            highlightCircleOuterRadius = 5f
        }

        binding.radarChart.apply { // Configure the fragment's chart
            data = RadarData(set)
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) { /* Marker handles this */ }
                override fun onNothingSelected() { highlightValue(null) }
            })
            highlightValue(null)
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
    private fun createChip(text: String): Chip {
        return Chip(requireContext()).apply {
            this.text = text
            isCheckable = true
            chipBackgroundColor = ColorStateList.valueOf(resources.getColor(R.color.white, null))
            textSize = resources.getDimension(R.dimen.text_size_medium) / resources.displayMetrics.density
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
            tvPercentage.text = "${it.y.toInt()}% Words Learnt"
        }
    }

    override fun getOffset(): MPPointF {
        // Position the marker above the selected point
        return MPPointF((-(width / 2)).toFloat(), (-height - 10f))
    }
}