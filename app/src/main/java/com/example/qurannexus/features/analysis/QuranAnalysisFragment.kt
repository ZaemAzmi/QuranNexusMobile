package com.example.qurannexus.features.analysis

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.features.analysis.adapters.QuranStat
import com.example.qurannexus.features.analysis.adapters.QuranStatsAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QuranAnalysisFragment : Fragment() {

    private lateinit var wordAnalysisCard: CardView
    private lateinit var chapterAnalysisCard: CardView
    private lateinit var statsRecyclerView: RecyclerView
    private lateinit var funFactTextView: TextView
    private lateinit var nextFactButton: Button

    private val funFacts = listOf(
        "The word 'Allah' (الله) appears 2,699 times in the Quran, making it one of the most frequently mentioned words.",
        "The shortest chapter in the Quran is Surah Al-Kawthar with only 3 verses.",
        "The longest chapter is Surah Al-Baqarah with 286 verses.",
        "The middle chapter of the Quran is Surah Al-Hadid (Iron).",
        "The word 'Jannah' (paradise) is mentioned 77 times in the Quran.",
        "The word 'Jahannam' (hellfire) is mentioned 77 times in the Quran.",
        "The most mentioned prophet in the Quran is Prophet Musa (Moses) who is mentioned 136 times.",
        "The Quran contains approximately 77,430 words."
    )
    private var currentFactIndex = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_quran_analysis, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupClickListeners()
        setupStatsRecyclerView()
        displayRandomFact()
    }

    private fun initViews(view: View) {
        wordAnalysisCard = view.findViewById(R.id.wordAnalysisCard)
        chapterAnalysisCard = view.findViewById(R.id.chapterAnalysisCard)
        statsRecyclerView = view.findViewById(R.id.statsRecyclerView)
        funFactTextView = view.findViewById(R.id.tvFunFact)
        nextFactButton = view.findViewById(R.id.btnNextFact)
    }

    private fun setupClickListeners() {
        // Word Analysis card click
        wordAnalysisCard.setOnClickListener {
            val fragment = WordAnalysisFragment.newInstance()
            parentFragmentManager.beginTransaction()
                .replace(R.id.mainFragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }

        // Chapter Analysis card click
        chapterAnalysisCard.setOnClickListener {
            val fragment = ChapterAnalysisFragment.newInstance()
            parentFragmentManager.beginTransaction()
                .replace(R.id.mainFragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }

        // Next fact button click
        nextFactButton.setOnClickListener {
            displayRandomFact()
        }
    }
    private fun setupStatsRecyclerView() {
        // Create adapter with Quran statistics
        val statsAdapter = QuranStatsAdapter(getQuranStats())
        statsRecyclerView.adapter = statsAdapter
    }

    private fun getQuranStats(): List<QuranStat> {
        return listOf(
            QuranStat("114", "Chapters"),
            QuranStat("6,236", "Verses"),
            QuranStat("~77,430", "Words"),
            QuranStat("~330,000", "Letters"),
            QuranStat("30", "Parts (Juz)"),
            QuranStat("23", "Years of Revelation")
        )
    }

    private fun displayRandomFact() {
        funFactTextView.text = funFacts[currentFactIndex]
        currentFactIndex = (currentFactIndex + 1) % funFacts.size
    }
}