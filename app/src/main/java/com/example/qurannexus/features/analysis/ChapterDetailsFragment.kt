package com.example.qurannexus.features.analysis

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.qurannexus.R
import com.example.qurannexus.core.interfaces.QuranApi
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ChapterDetailsFragment : Fragment() {

    @Inject
    lateinit var quranApi: QuranApi

    private lateinit var backButton: ImageView
    private lateinit var chapterTitleTextView: TextView
    private lateinit var chapterSubtitleTextView: TextView
    private lateinit var chapterInfoTextView: TextView
    private lateinit var pieChart: PieChart

    private var chapterId: String = "1"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            chapterId = it.getString(ARG_CHAPTER_ID, "1")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chapter_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        loadChapterDetails()
        setupPieChart()
    }

    private fun initViews(view: View) {
        backButton = view.findViewById(R.id.backButton)
        chapterTitleTextView = view.findViewById(R.id.chapterTitleTextView)
        chapterSubtitleTextView = view.findViewById(R.id.chapterSubtitleTextView)
        chapterInfoTextView = view.findViewById(R.id.chapterInfoTextView)
        pieChart = view.findViewById(R.id.pieChart)

        backButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun loadChapterDetails() {
        // In a real app, fetch data from API
        // For this demo, we'll use hardcoded sample data

        when (chapterId) {
            "1" -> {
                chapterTitleTextView.text = "Al-Fatiha (الفاتحة)"
                chapterSubtitleTextView.text = "The Opening - Chapter 1"
                chapterInfoTextView.text = "Al-Fatiha is the first chapter of the Quran. It consists of 7 verses and is a Meccan surah. It is known as 'The Opening' because it opens the Quran and is recited at the beginning of every prayer cycle (rak'ah). It is also called Umm Al-Kitab (The Mother of the Book) and Sab'a al-Mathani (The Seven Oft-Repeated Verses)."
            }
            "2" -> {
                chapterTitleTextView.text = "Al-Baqarah (البقرة)"
                chapterSubtitleTextView.text = "The Cow - Chapter 2"
                chapterInfoTextView.text = "Al-Baqarah is the second and longest chapter of the Quran with 286 verses. It is a Medinan surah, revealed shortly after the migration to Medina. The name 'Al-Baqarah' (The Cow) is derived from the story of Moses and the Israelites mentioned in verses 67-73, which tells of a cow they sacrificed as commanded by Allah."
            }
            else -> {
                chapterTitleTextView.text = "Chapter $chapterId"
                chapterSubtitleTextView.text = "Chapter Details"
                chapterInfoTextView.text = "Information about this chapter will be displayed here."
            }
        }
    }

    private fun setupPieChart() {
        // Sample data for word categories in the chapter
        val entries = ArrayList<PieEntry>()

        when (chapterId) {
            "1" -> {
                entries.add(PieEntry(25f, "Nouns"))
                entries.add(PieEntry(20f, "Verbs"))
                entries.add(PieEntry(30f, "Particles"))
                entries.add(PieEntry(15f, "Pronouns"))
                entries.add(PieEntry(10f, "Other"))
            }
            "2" -> {
                entries.add(PieEntry(30f, "Nouns"))
                entries.add(PieEntry(25f, "Verbs"))
                entries.add(PieEntry(20f, "Particles"))
                entries.add(PieEntry(15f, "Pronouns"))
                entries.add(PieEntry(10f, "Other"))
            }
            else -> {
                entries.add(PieEntry(30f, "Nouns"))
                entries.add(PieEntry(25f, "Verbs"))
                entries.add(PieEntry(20f, "Particles"))
                entries.add(PieEntry(15f, "Pronouns"))
                entries.add(PieEntry(10f, "Other"))
            }
        }

        val colors = ArrayList<Int>().apply {
            add(Color.parseColor("#3F51B5"))
            add(Color.parseColor("#4CAF50"))
            add(Color.parseColor("#FFC107"))
            add(Color.parseColor("#FF5722"))
            add(Color.parseColor("#9C27B0"))
        }

        val dataSet = PieDataSet(entries, "Word Categories").apply {
            this.colors = colors
            valueTextSize = 12f
            valueTextColor = Color.WHITE
            valueFormatter = PercentFormatter(pieChart)
        }

        val data = PieData(dataSet)

        pieChart.apply {
            this.data = data
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 40f
            setHoleColor(Color.WHITE)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            setUsePercentValues(true)
            legend.isEnabled = true
            setEntryLabelColor(Color.WHITE)
            animateY(1000)
            invalidate()
        }
    }

    companion object {
        private const val ARG_CHAPTER_ID = "chapter_id"

        fun newInstance(chapterId: String): ChapterDetailsFragment {
            return ChapterDetailsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CHAPTER_ID, chapterId)
                }
            }
        }
    }
}