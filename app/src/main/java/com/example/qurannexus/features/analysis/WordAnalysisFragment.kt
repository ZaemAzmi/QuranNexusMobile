package com.example.qurannexus.features.analysis

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.core.interfaces.QuranApi
import com.example.qurannexus.features.analysis.adapters.FrequentWordsAdapter
import com.example.qurannexus.features.analysis.enums.SearchType
import com.example.qurannexus.features.analysis.viewmodels.DisplayableFrequentRoot
import com.example.qurannexus.features.analysis.viewmodels.WordAnalysisViewModel
import com.example.qurannexus.features.words.WordDetailsActivity
import com.example.qurannexus.features.words.models.WordOccurrenceResponse
import com.google.android.material.chip.ChipGroup
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject


@AndroidEntryPoint
class WordAnalysisFragment : Fragment() {

    @Inject
    lateinit var quranApi: QuranApi
    private val viewModel: WordAnalysisViewModel by viewModels() // Inject ViewModel
    private lateinit var backButton: ImageView
    private lateinit var searchEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var heavenCategoryCard: CardView
    private lateinit var hellCategoryCard: CardView
    private lateinit var prophetsCategoryCard: CardView
    private lateinit var wordFactTextView: TextView
    private lateinit var wordFactButton: Button
    private lateinit var frequentWordsRecyclerView: RecyclerView
    private lateinit var frequentWordsAdapter: FrequentWordsAdapter
    private lateinit var chipGroupSearchFilter : ChipGroup
    private var isInSearchMode = false // To track if recycler is showing search results or frequent words
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_word_analysis, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupRecyclerView() // Renamed from setupFrequentWords
        setupClickListeners()
        observeViewModel()
        displayRandomFact() // Keep if you want this
        viewModel.fetchFrequentRoots() // Fetch data
    }

    private fun initViews(view: View) {
        backButton = view.findViewById(R.id.backButton)
        searchEditText = view.findViewById(R.id.searchEditText)
        searchButton = view.findViewById(R.id.searchButton)
        chipGroupSearchFilter = view.findViewById(R.id.chipGroupSearchFilter)
        frequentWordsRecyclerView = view.findViewById(R.id.frequentWordsRecyclerView)
        heavenCategoryCard = view.findViewById(R.id.heavenCategoryCard)
        hellCategoryCard = view.findViewById(R.id.hellCategoryCard)
        prophetsCategoryCard = view.findViewById(R.id.prophetsCategoryCard)
        wordFactTextView = view.findViewById(R.id.wordFactTextView)
        wordFactButton = view.findViewById(R.id.wordFactButton)
    }
    private fun setupRecyclerView() {
        frequentWordsAdapter = FrequentWordsAdapter { displayableRoot ->
            navigateToWordDetails(displayableRoot)
        }
        frequentWordsRecyclerView.adapter = frequentWordsAdapter
        // You might want to set a LayoutManager if not set in XML
        // frequentWordsRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false) // Example
    }


    private fun observeViewModel() {
        viewModel.frequentRoots.observe(viewLifecycleOwner) { roots ->
            if (!isInSearchMode) { // Only update if not in search mode
                frequentWordsAdapter.submitList(roots)
            }
        }
        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            if (isInSearchMode) { // Only update if in search mode
                if (results.isEmpty()) {
                    Toast.makeText(requireContext(), "No results found for your search.", Toast.LENGTH_SHORT).show()
                }
                frequentWordsAdapter.submitList(results)
            }
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Handle loading state (e.g., for a general progress bar)
        }
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        }
    }
    private fun setupClickListeners() {
        // Back button click
        backButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        searchButton.setOnClickListener {
            val query = searchEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                val selectedChipId = chipGroupSearchFilter.checkedChipId
                Log.d("WordAnalysisFragment", "Selected Chip ID: $selectedChipId") // LOG THIS
                val searchType = when (selectedChipId) {
                    R.id.chipFilterRootLabel -> SearchType.ROOT_LABEL
                    R.id.chipFilterArabicForm -> SearchType.ARABIC_FORM
                    R.id.chipFilterTranslation -> SearchType.TRANSLATION
                    R.id.chipFilterAll -> SearchType.ALL
                    else -> {
                        Log.w(
                            "WordAnalysisFragment",
                            "Unknown chip ID: $selectedChipId, defaulting to ALL"
                        )
                        SearchType.ALL // Default
                    }
                }
                Log.d("WordAnalysisFragment", "Determined SearchType: ${searchType.name}") // LOG THIS
                navigateToSearchResults(query, searchType)
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

    // Renamed to distinguish from future local search
//    private fun searchWordsApi(query: String) {
//        Toast.makeText(requireContext(), "Searching (API): $query", Toast.LENGTH_SHORT).show()
//        quranApi.searchWords(query = query, type = "all", page = 1, perPage = 20)
//            .enqueue(object : Callback<WordOccurrenceResponse> {
//                override fun onResponse(call: Call<WordOccurrenceResponse>, response: Response<WordOccurrenceResponse>) {
//                    if (response.isSuccessful) {
//                        navigateToSearchResults(query) // This likely needs update too if SearchResultsFragment expects different data
//                    } else {
//                        Toast.makeText(requireContext(), "Search failed (API).", Toast.LENGTH_SHORT).show()
//                    }
//                }
//                override fun onFailure(call: Call<WordOccurrenceResponse>, t: Throwable) {
//                    Toast.makeText(requireContext(), "Network error (API): ${t.message}", Toast.LENGTH_SHORT).show()
//                }
//            })
//    }

    // Modified to take DisplayableFrequentRoot
    private fun navigateToWordDetails(selectedRoot: DisplayableFrequentRoot) {
        // No API call needed here anymore, WordDetailsActivity will use the rootLabel
        val intent = Intent(requireContext(), WordDetailsActivity::class.java).apply {
            putExtra(WordDetailsActivity.EXTRA_ROOT_LABEL, selectedRoot.rootLabel)
            // Optionally, pass the specific arabic text that was displayed, so WordDetailsActivity
            // can try to select it in its spinner initially.
            putExtra(WordDetailsActivity.EXTRA_WORD_TEXT_FROM_RECITATION, selectedRoot.displayArabicText)
        }
        startActivity(intent)
    }
    private fun displayRandomFact() {
        wordFactTextView.text = wordFacts[currentFactIndex]
        currentFactIndex = (currentFactIndex + 1) % wordFacts.size
    }
    private fun navigateToWordCategory(category: String) {
        // Navigate to category fragment
        val fragment = WordCategoryFragment.newInstance(category)
        parentFragmentManager.beginTransaction()
            .replace(R.id.mainFragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToSearchResults(query: String, searchType: SearchType) {
        Log.d("WordAnalysisFragment", "Navigating to search results with query: '$query', type: ${searchType.name}")
        val fragment = WordSearchResultsFragment.newInstance(query, searchType.name) // Pass searchType.name (String)
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
    override fun onResume() { // Or use a specific back handling mechanism
        super.onResume()
        if (searchEditText.text.isEmpty()) {
            if (isInSearchMode) { // If we were in search mode but now query is empty
                isInSearchMode = false
                viewModel.fetchFrequentRoots() // Refresh frequent words
            }
        }
    }
}