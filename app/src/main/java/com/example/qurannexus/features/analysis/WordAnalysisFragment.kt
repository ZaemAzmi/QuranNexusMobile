package com.example.qurannexus.features.analysis

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.core.interfaces.QuranApi
import com.example.qurannexus.features.analysis.adapters.FrequentWordsAdapter
import com.example.qurannexus.features.words.WordDetailsActivity
import com.example.qurannexus.features.words.models.WordOccurrenceResponse
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

data class FrequentWord(
    val text: String,
    val translation: String,
    val occurrences: Int
)

@AndroidEntryPoint
class WordAnalysisFragment : Fragment() {

    @Inject
    lateinit var quranApi: QuranApi

    private lateinit var backButton: ImageView
    private lateinit var searchEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var frequentWordsRecyclerView: RecyclerView
    private lateinit var heavenCategoryCard: CardView
    private lateinit var hellCategoryCard: CardView
    private lateinit var prophetsCategoryCard: CardView
    private lateinit var wordFactTextView: TextView
    private lateinit var wordFactButton: Button

    private val wordFacts = listOf(
        "The word 'Rahman' (الرحمن) and 'Raheem' (الرحيم) which refer to Allah's mercy appear 57 and 114 times respectively in the Quran.",
        "The word 'Salat' (prayer) is mentioned 67 times in the Quran.",
        "The word 'Jannah' (paradise) is mentioned 77 times in the Quran.",
        "The word 'Jahannam' (hellfire) is mentioned 77 times in the Quran.",
        "The word 'Yawm' (day) is mentioned 365 times in the Quran.",
        "The word 'Shahr' (month) is mentioned 12 times in the Quran.",
        "The word 'Allah' is mentioned 2,699 times in the Quran."
    )
    private var currentFactIndex = 0

    // List of most frequent words in the Quran
    private val frequentWords = listOf(
        FrequentWord("الله", "Allah", 2699),
        FrequentWord("رب", "Lord", 970),
        FrequentWord("قال", "Said", 529),
        FrequentWord("نفس", "Soul", 295),
        FrequentWord("يوم", "Day", 365),
        FrequentWord("سماء", "Sky/Heaven", 310),
        FrequentWord("أرض", "Earth", 461),
        FrequentWord("قلب", "Heart", 168)
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_word_analysis, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupClickListeners()
        setupFrequentWords()
        displayRandomFact()
    }

    private fun initViews(view: View) {
        backButton = view.findViewById(R.id.backButton)
        searchEditText = view.findViewById(R.id.searchEditText)
        searchButton = view.findViewById(R.id.searchButton)
        frequentWordsRecyclerView = view.findViewById(R.id.frequentWordsRecyclerView)
        heavenCategoryCard = view.findViewById(R.id.heavenCategoryCard)
        hellCategoryCard = view.findViewById(R.id.hellCategoryCard)
        prophetsCategoryCard = view.findViewById(R.id.prophetsCategoryCard)
        wordFactTextView = view.findViewById(R.id.wordFactTextView)
        wordFactButton = view.findViewById(R.id.wordFactButton)
    }

    private fun setupClickListeners() {
        // Back button click
        backButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // Search button click
        searchButton.setOnClickListener {
            val query = searchEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                searchWords(query)
            } else {
                Toast.makeText(requireContext(), "Please enter a search query", Toast.LENGTH_SHORT).show()
            }
        }

        // Category card clicks
        heavenCategoryCard.setOnClickListener {
            navigateToWordCategory("heaven")
        }

        hellCategoryCard.setOnClickListener {
            navigateToWordCategory("hell")
        }

        prophetsCategoryCard.setOnClickListener {
            navigateToWordCategory("prophets")
        }

        // Next fact button click
        wordFactButton.setOnClickListener {
            displayRandomFact()
        }
    }

    private fun setupFrequentWords() {
        // Set up RecyclerView with frequent words adapter
        val adapter = FrequentWordsAdapter(frequentWords) { word ->
            // Navigate to word details
            navigateToWordDetails(word)
        }
        frequentWordsRecyclerView.adapter = adapter
    }

    private fun displayRandomFact() {
        wordFactTextView.text = wordFacts[currentFactIndex]
        currentFactIndex = (currentFactIndex + 1) % wordFacts.size
    }

    private fun searchWords(query: String) {
        // Show progress
        Toast.makeText(requireContext(), "Searching for: $query", Toast.LENGTH_SHORT).show()

        // Use the search API endpoint
        quranApi.searchWords(
            query = query,
            type = "all",
            page = 1,
            perPage = 20
        ).enqueue(object : Callback<WordOccurrenceResponse> {
            override fun onResponse(call: Call<WordOccurrenceResponse>, response: Response<WordOccurrenceResponse>) {
                if (response.isSuccessful) {
                    // Navigate to search results fragment
                    navigateToSearchResults(query)
                } else {
                    Toast.makeText(requireContext(), "Search failed. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<WordOccurrenceResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun navigateToWordDetails(word: FrequentWord) {
        val intent = Intent(requireContext(), WordDetailsActivity::class.java).apply {
            putExtra("WORD_TEXT", word.text)
            putExtra("TRANSLATION", word.translation)
        }
        startActivity(intent)
    }

    private fun navigateToWordCategory(category: String) {
        // Navigate to category fragment
        val fragment = WordCategoryFragment.newInstance(category)
        parentFragmentManager.beginTransaction()
            .replace(R.id.mainFragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToSearchResults(query: String) {
        // Navigate to search results fragment
        val fragment = WordSearchResultsFragment.newInstance(query)
        parentFragmentManager.beginTransaction()
            .replace(R.id.mainFragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        fun newInstance(): WordAnalysisFragment {
            return WordAnalysisFragment()
        }
    }
}