package com.example.qurannexus.features.analysis

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.core.interfaces.QuranApi
import com.example.qurannexus.features.analysis.adapters.ChaptersAdapter
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

data class ChapterInfo(
    val id: String,
    val arabicName: String,
    val englishName: String,
    val revelationPlace: String,
    val verseCount: Int,
    val wordCount: Int
)

@AndroidEntryPoint
class ChapterAnalysisFragment : Fragment() {

    @Inject
    lateinit var quranApi: QuranApi

    private lateinit var backButton: ImageView
    private lateinit var chartContainer: FrameLayout
    private lateinit var chaptersRecyclerView: RecyclerView
    private lateinit var chapterFactTextView: TextView
    private lateinit var chapterFactButton: Button

    private val chapterFacts = listOf(
        "The longest chapter in the Quran is Al-Baqarah (The Cow) with 286 verses, while the shortest is Al-Kawthar with only 3 verses.",
        "There are 114 chapters (surahs) in the Quran.",
        "The first chapter revealed was Surah Al-Alaq (The Clot), verses 1-5.",
        "The last chapter revealed in full was Surah An-Nasr (The Victory).",
        "86 chapters were revealed in Mecca, while 28 were revealed in Medina.",
        "The 29 chapters that begin with the Arabic letters (like 'Alif-Lam-Mim') are known as the 'Muqatta'at'.",
        "Surah Ya-Sin is often referred to as the 'Heart of the Quran'."
    )
    private var currentFactIndex = 0

    // Sample data for chapters
    private val chapters = listOf(
        ChapterInfo("1", "الفاتحة", "Al-Fatiha", "Meccan", 7, 29),
        ChapterInfo("2", "البقرة", "Al-Baqarah", "Medinan", 286, 6144),
        ChapterInfo("3", "آل عمران", "Aali Imran", "Medinan", 200, 3503),
        ChapterInfo("4", "النساء", "An-Nisa", "Medinan", 176, 3765),
        ChapterInfo("5", "المائدة", "Al-Ma'idah", "Medinan", 120, 2837),
        ChapterInfo("6", "الأنعام", "Al-An'am", "Meccan", 165, 3055),
        ChapterInfo("7", "الأعراف", "Al-A'raf", "Meccan", 206, 3341),
        ChapterInfo("8", "الأنفال", "Al-Anfal", "Medinan", 75, 1243),
        ChapterInfo("9", "التوبة", "At-Tawbah", "Medinan", 129, 2506),
        ChapterInfo("10", "يونس", "Yunus", "Meccan", 109, 1841)
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chapter_analysis, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupClickListeners()
        displayChart()
        setupChaptersList()
        displayRandomFact()
    }

    private fun initViews(view: View) {
        backButton = view.findViewById(R.id.backButton)
        chartContainer = view.findViewById(R.id.chartContainer)
        chaptersRecyclerView = view.findViewById(R.id.chaptersRecyclerView)
        chapterFactTextView = view.findViewById(R.id.chapterFactTextView)
        chapterFactButton = view.findViewById(R.id.chapterFactButton)

        // Set up recycler view
        chaptersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        chapterFactButton.setOnClickListener {
            displayRandomFact()
        }
    }

    private fun displayChart() {
        // Create a bar chart for chapter word counts
        val barChart = BarChart(requireContext())
        chartContainer.addView(barChart)

        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        // Add data points
        chapters.forEachIndexed { index, chapter ->
            entries.add(BarEntry(index.toFloat(), chapter.wordCount.toFloat()))
            labels.add(chapter.englishName)
        }

        val dataSet = BarDataSet(entries, "Word Count")
        dataSet.color = Color.parseColor("#3F51B5")
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 10f

        val barData = BarData(dataSet)
        barChart.data = barData

        // Customize chart appearance
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.setDrawGridBackground(false)
        barChart.setFitBars(true)

        // X-axis customization
        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.textSize = 8f
        xAxis.labelRotationAngle = 45f

        // Y-axis customization
        barChart.axisLeft.setDrawGridLines(false)
        barChart.axisRight.isEnabled = false

        barChart.invalidate()
    }

    private fun setupChaptersList() {
        val adapter = ChaptersAdapter(chapters) { chapter ->
            // Navigate to chapter detail view
            navigateToChapterDetail(chapter)
        }
        chaptersRecyclerView.adapter = adapter
    }

    private fun displayRandomFact() {
        chapterFactTextView.text = chapterFacts[currentFactIndex]
        currentFactIndex = (currentFactIndex + 1) % chapterFacts.size
    }

    private fun navigateToChapterDetail(chapter: ChapterInfo) {
        // Navigate to chapter detail fragment
        val fragment = ChapterDetailsFragment.newInstance(chapter.id)
        parentFragmentManager.beginTransaction()
            .replace(R.id.mainFragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        fun newInstance(): ChapterAnalysisFragment {
            return ChapterAnalysisFragment()
        }
    }
}