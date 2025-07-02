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
import com.google.android.material.button.MaterialButtonToggleGroup
// Remove WordOccurrenceResponse if API call is removed
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WordSearchResultsFragment : Fragment() {

    private val viewModel: WordAnalysisViewModel by viewModels()

    private lateinit var backButton: ImageView
    private lateinit var titleTextView: TextView
    private lateinit var noResultsLayout: View
    private lateinit var noResultsTextView: TextView // We still need this to change the text
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var searchResultsAdapter: SearchResultsAdapter // Adapter instance
    private lateinit var totalResultsTextView: TextView
    private lateinit var filterToggleGroup: MaterialButtonToggleGroup
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
        setupFilterListeners()
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
        noResultsLayout = view.findViewById(R.id.noResultsLayout)
        noResultsTextView = view.findViewById(R.id.noResultsTextView)
        resultsRecyclerView = view.findViewById(R.id.resultsRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        totalResultsTextView = view.findViewById(R.id.totalResultsTextView)
        filterToggleGroup = view.findViewById(R.id.filterToggleGroup)
        backButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    // --- THIS IS THE CORRECTED FUNCTION ---
    private fun setupRecyclerView() {
        searchResultsAdapter = SearchResultsAdapter { displayableRoot ->
            val intent = Intent(requireContext(), WordDetailsActivity::class.java).apply {
                putExtra(WordDetailsActivity.EXTRA_IDENTIFIER_VALUE, displayableRoot.identifierValue)
                putExtra(WordDetailsActivity.EXTRA_WORD_TEXT_FOR_PRESELECTION, displayableRoot.displayArabicText)
            }
            startActivity(intent)
        }

        val linearLayoutManager = LinearLayoutManager(requireContext()) // Must store manager in a variable
        resultsRecyclerView.layoutManager = linearLayoutManager
        resultsRecyclerView.adapter = searchResultsAdapter

        // *** CRITICAL FIX: ADD THE SCROLL LISTENER ***
        resultsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                // This logic triggers the loading of the next page
                val visibleItemCount = linearLayoutManager.childCount
                val totalItemCount = linearLayoutManager.itemCount
                val firstVisibleItemPosition = linearLayoutManager.findFirstVisibleItemPosition()

                // Check if we're near the end, not currently loading, and the search type is one that supports pagination
                val isLoading = viewModel.isLoading.value ?: false
                if (!isLoading &&
                    (visibleItemCount + firstVisibleItemPosition) >= totalItemCount &&
                    firstVisibleItemPosition >= 0) {
                    viewModel.loadMoreResults()
                }
            }
        })
    }
    private fun setupFilterListeners() {
        // Set the initial checked button based on the search type passed to the fragment
        when(searchTypeEnum) {
            SearchType.ALL -> filterToggleGroup.check(R.id.filterButtonAll)
            SearchType.ROOT_LABEL -> filterToggleGroup.check(R.id.filterButtonRoot)
            SearchType.ARABIC_FORM -> {
                // Arabic form is not a type filter, it's a content filter.
                // We should disable the toggle group for this search type.
                filterToggleGroup.visibility = View.GONE
            }
            SearchType.TRANSLATION -> filterToggleGroup.check(R.id.filterButtonAll) // Default to all
            SearchType.LEMMA -> filterToggleGroup.check(R.id.filterButtonLemma)
            SearchType.FORM -> filterToggleGroup.check(R.id.filterButtonOthers)
        }

        filterToggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                // Determine the new search type based on the button clicked
                val newFilterType = when (checkedId) {
                    R.id.filterButtonRoot -> SearchType.ROOT_LABEL
                    R.id.filterButtonLemma -> SearchType.LEMMA // Assuming you add this to SearchType enum
                    R.id.filterButtonOthers -> SearchType.FORM // Assuming you add this to SearchType enum
                    else -> SearchType.ALL
                }

                // Tell the ViewModel to apply the new filter
                // Note: You need to add LEMMA and FORM to your SearchType enum
                viewModel.applyFilter(newFilterType)
            }
        }
    }
    private fun observeViewModel() {
        // Observer 1: Manages the main progress bar's visibility.
        // It only shows the bar for the very first load.
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // If we are loading AND the list is currently empty, show the main spinner.
            if (isLoading && searchResultsAdapter.itemCount == 0) {
                progressBar.visibility = View.VISIBLE
                resultsRecyclerView.visibility = View.GONE
                noResultsLayout.visibility = View.GONE
            } else if (!isLoading) {
                // Once loading is finished for a page, the main spinner's job is done.
                // The searchResults observer will handle showing/hiding content.
                progressBar.visibility = View.GONE
            }
        }

        viewModel.totalResultsCount.observe(viewLifecycleOwner) { count ->
            if (count != null && count > 0) {
                totalResultsTextView.text = "$count results found"
                totalResultsTextView.visibility = View.VISIBLE
            } else {
                totalResultsTextView.visibility = View.GONE
            }
        }
        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            Log.d(TAG, "Search results updated: ${results.size} items")

            searchResultsAdapter.submitList(results)

            // Get the final count. This is our source of truth.
            val finalCount = viewModel.totalResultsCount.value
            // The search is officially "over" and has "no results" ONLY when
            // the count has been calculated and is zero.
            if (finalCount != null && finalCount == 0) {
                showNoResults("No results found for \"$searchQuery\".")
            } else {
                // Otherwise, show the list. It's either populated or will be populated.
                resultsRecyclerView.visibility = View.VISIBLE
                noResultsLayout.visibility = View.GONE
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                progressBar.visibility = View.GONE
                // The error observer should also trigger the "no results" view, but with an error message.
                showNoResults(it)
                Log.e(TAG, "Search error LiveData: $it")
            }
        }
    }

    private fun showNoResults(message: String) {
        noResultsTextView.text = message
        noResultsLayout.visibility = View.VISIBLE // Show the whole layout
        resultsRecyclerView.visibility = View.GONE
    }

}