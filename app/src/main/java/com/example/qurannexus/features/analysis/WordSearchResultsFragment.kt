package com.example.qurannexus.features.analysis

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
// Remove QuranApi import if not directly used
import com.example.qurannexus.features.analysis.adapters.SearchResultsAdapter // Keep this
import com.example.qurannexus.features.analysis.enums.SearchType
import com.example.qurannexus.features.analysis.viewmodels.DisplayableFrequentRoot // Import this
import com.example.qurannexus.features.analysis.viewmodels.WordAnalysisViewModel
import com.example.qurannexus.features.words.WordDetailsActivity
import com.example.qurannexus.features.words.services.WordSearchService
// Remove WordOccurrenceResponse if API call is removed
import dagger.hilt.android.AndroidEntryPoint
// Remove Retrofit imports

// Data class SearchResult can be removed if DisplayableFrequentRoot is used for adapter
// data class SearchResult(...)

@AndroidEntryPoint
class WordSearchResultsFragment : Fragment() {

    // Use the shared WordAnalysisViewModel or create a dedicated one
    private val viewModel: WordAnalysisViewModel by viewModels() // Or activityViewModels() if shared with WordAnalysisFragment

    private lateinit var backButton: ImageView
    private lateinit var titleTextView: TextView
    private lateinit var noResultsTextView: TextView
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var searchResultsAdapter: SearchResultsAdapter // Adapter instance

    private var searchQuery: String = ""
    private var searchTypeEnum: SearchType = SearchType.ALL

    companion object {
        private const val ARG_SEARCH_QUERY = "search_query"
        private const val ARG_SEARCH_TYPE_STRING = "search_type_string"
        private const val TAG = "WordSearchResultsFrag"

        fun newInstance(searchQuery: String, searchTypeString: String): WordSearchResultsFragment {
            return WordSearchResultsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SEARCH_QUERY, searchQuery)
                    putString(ARG_SEARCH_TYPE_STRING, searchTypeString)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            searchQuery = it.getString(ARG_SEARCH_QUERY, "")
            val searchTypeString = it.getString(ARG_SEARCH_TYPE_STRING, SearchType.ALL.name)

            searchTypeEnum = try {
                SearchType.valueOf(searchTypeString)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Invalid searchTypeString: '$searchTypeString', defaulting to ALL.", e)
                SearchType.ALL
            }
        }
        Log.d(TAG, "onCreate: searchQuery = $searchQuery")
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
        setupRecyclerView()
        observeViewModel()

        if (searchQuery.isNotEmpty()) {
            // Update title to include the search type for clarity
            val typeDisplay = searchTypeEnum.name.replace("_", " ").lowercase()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            titleTextView.text = "Search: \"$searchQuery\" (Type: $typeDisplay)"
            // Pass the enum to the ViewModel
            viewModel.performSearch(searchQuery, searchTypeEnum)
        } else {
            Log.w(TAG, "Search query is empty.")
            showNoResults("No search query provided.")
        }

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
    }

    private fun setupRecyclerView() {
        searchResultsAdapter = SearchResultsAdapter { displayableRoot ->
            // Now displayableRoot is the correct type with rootLabel
            val intent = Intent(requireContext(), WordDetailsActivity::class.java).apply {
                putExtra(WordDetailsActivity.EXTRA_IDENTIFIER_VALUE, displayableRoot.identifierValue)
                putExtra(WordDetailsActivity.EXTRA_WORD_TEXT_FOR_PRESELECTION, displayableRoot.displayArabicText)
            }
            startActivity(intent)
        }
        resultsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        resultsRecyclerView.adapter = searchResultsAdapter
    }

    private fun observeViewModel() {
        viewModel.searchResults.observe(viewLifecycleOwner) { results -> // results is List<DisplayableFrequentRoot>
            Log.d(TAG, "Search results received: ${results.size} items")
            progressBar.visibility = View.GONE
            if (results.isNotEmpty()) {
                resultsRecyclerView.visibility = View.VISIBLE
                noResultsTextView.visibility = View.GONE
                searchResultsAdapter.submitList(results) // Directly submit the list
            } else {
                // Only show "no results" if not loading and query was actually performed
                if (viewModel.isLoading.value == false) {
                    showNoResults("No results found for \"$searchQuery\" with filter \"${searchTypeEnum.name.replace("_", " ")}\".")
                }
            }
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) {
                resultsRecyclerView.visibility = View.GONE
                noResultsTextView.visibility = View.GONE
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                progressBar.visibility = View.GONE
                showNoResults(it) // Show error message
                Log.e(TAG, "Search error LiveData: $it")
            }
        }
    }

    private fun showNoResults(message: String) {
        noResultsTextView.text = message
        noResultsTextView.visibility = View.VISIBLE
        resultsRecyclerView.visibility = View.GONE
    }

    // getSurahName and createSampleResults are likely no longer needed if using local DB
    // private fun getSurahName(chapterId: Int): String { ... }
    // private fun createSampleResults(): List<SearchResult> { ... }
}