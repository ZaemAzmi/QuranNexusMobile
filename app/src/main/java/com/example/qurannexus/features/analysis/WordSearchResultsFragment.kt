package com.example.qurannexus.features.analysis

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.core.interfaces.QuranApi
import com.example.qurannexus.features.analysis.adapters.SearchResultsAdapter
import com.example.qurannexus.features.words.WordDetailsActivity
import com.example.qurannexus.features.words.models.WordOccurrenceResponse
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

@AndroidEntryPoint
class WordSearchResultsFragment : Fragment() {

    @Inject
    lateinit var quranApi: QuranApi

    private lateinit var backButton: ImageView
    private lateinit var titleTextView: TextView
    private lateinit var noResultsTextView: TextView
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar

    private var searchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            searchQuery = it.getString(ARG_SEARCH_QUERY, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_word_search_results, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        searchWords()
    }

    private fun initViews(view: View) {
        backButton = view.findViewById(R.id.backButton)
        titleTextView = view.findViewById(R.id.titleTextView)
        noResultsTextView = view.findViewById(R.id.noResultsTextView)
        resultsRecyclerView = view.findViewById(R.id.resultsRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)

        backButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        titleTextView.text = "Search: \"$searchQuery\""

        // Set up recycler view
        resultsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun searchWords() {
        progressBar.visibility = View.VISIBLE
        noResultsTextView.visibility = View.GONE
        resultsRecyclerView.visibility = View.GONE

        // Call API to search words
        quranApi.searchWords(
            query = searchQuery,
            type = "all",
            page = 1,
            perPage = 50
        ).enqueue(object : Callback<WordOccurrenceResponse> {
            override fun onResponse(call: Call<WordOccurrenceResponse>, response: Response<WordOccurrenceResponse>) {
                progressBar.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    // Check if there are results
                    val wordOccurrences = response.body()?.data?.words ?: emptyList()

                    if (wordOccurrences.isNotEmpty()) {
                        // Convert API response to our SearchResult objects
                        val searchResults = wordOccurrences.map { word ->
                            SearchResult(
                                wordText = word.word_text,
                                translation = word.translation ?: "",
                                verseText = word.verse_text ?: "",
                                chapterId = word.chapter_id,
                                verseNumber = word.verse_number,
                                surahName = getSurahName(word.chapter_id.toIntOrNull() ?: 1)
                            )
                        }
                        displayResults(searchResults)
                    } else {
                        // Show sample results for demonstration if needed
                        // val sampleResults = createSampleResults()
                        // displayResults(sampleResults)

                        // Or just show no results message
                        showNoResults("No words found matching \"$searchQuery\"")
                    }
                } else {
                    showNoResults("Error searching words. Please try again.")
                }
            }

            override fun onFailure(call: Call<WordOccurrenceResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                showNoResults("Network error: ${t.message}")
            }
        })
    }

    // Helper function to get surah name from chapter ID
    private fun getSurahName(chapterId: Int): String {
        // In a real app, you would use a proper lookup from your Quran metadata
        // This is just a simplified example
        return when (chapterId) {
            1 -> "Al-Fatiha"
            2 -> "Al-Baqarah"
            3 -> "Aali Imran"
            4 -> "An-Nisa"
            5 -> "Al-Ma'idah"
            // Add more chapters as needed
            else -> "Chapter $chapterId"
        }
    }

    private fun displayResults(results: List<SearchResult>) {
        if (results.isEmpty()) {
            showNoResults("No words found matching \"$searchQuery\"")
            return
        }

        // Display results
        resultsRecyclerView.visibility = View.VISIBLE

        val adapter = SearchResultsAdapter(results) { result ->
            // Navigate to word details
            val intent = Intent(requireContext(), WordDetailsActivity::class.java).apply {
                putExtra("WORD_TEXT", result.wordText)
                putExtra("TRANSLATION", result.translation)
                putExtra("CHAPTER_ID", result.chapterId)
                putExtra("VERSE_NUMBER", result.verseNumber)
                putExtra("VERSE_TEXT", result.verseText)
            }
            startActivity(intent)
        }

        resultsRecyclerView.adapter = adapter
    }

    private fun showNoResults(message: String) {
        noResultsTextView.visibility = View.VISIBLE
        resultsRecyclerView.visibility = View.GONE
        noResultsTextView.text = message
    }

    private fun createSampleResults(): List<SearchResult> {
        // Create sample results for demonstration
        return listOf(
            SearchResult(
                wordText = "رحمن",
                translation = "Merciful",
                verseText = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                chapterId = "1",
                verseNumber = "1",
                surahName = "Al-Fatiha"
            ),
            SearchResult(
                wordText = "رحيم",
                translation = "Compassionate",
                verseText = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                chapterId = "1",
                verseNumber = "1",
                surahName = "Al-Fatiha"
            ),
            SearchResult(
                wordText = "الله",
                translation = "Allah",
                verseText = "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ",
                chapterId = "1",
                verseNumber = "2",
                surahName = "Al-Fatiha"
            )
        )
    }

    companion object {
        private const val ARG_SEARCH_QUERY = "search_query"

        fun newInstance(searchQuery: String): WordSearchResultsFragment {
            return WordSearchResultsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SEARCH_QUERY, searchQuery)
                }
            }
        }
    }
}

data class SearchResult(
    val wordText: String,
    val translation: String,
    val verseText: String,
    val chapterId: String,
    val verseNumber: String,
    val surahName: String
)