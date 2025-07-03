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
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
// Remove QuranApi import if not directly used
import com.example.qurannexus.features.analysis.adapters.SearchResultsAdapter // Keep this
import com.example.qurannexus.features.analysis.enums.FilterType
import com.example.qurannexus.features.analysis.enums.SearchType
import com.example.qurannexus.features.analysis.viewmodels.DisplayableFrequentRoot
import com.example.qurannexus.features.analysis.viewmodels.WordAnalysisViewModel
import com.example.qurannexus.features.words.WordDetailsActivity
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
    private var searchTypeEnum: SearchType = SearchType.GENERAL
    private var isFilterListenerReady = false
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
            val searchTypeString = it.getString(ARG_SEARCH_TYPE_STRING, SearchType.GENERAL.name)

            searchTypeEnum = try {
                SearchType.valueOf(searchTypeString)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Invalid searchTypeString: '$searchTypeString', defaulting to ALL.", e)
                SearchType.GENERAL
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
        setupFilterListeners()
        observeViewModel()
        setupSearchQuery()
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

    private fun setupSearchQuery(){
        if (searchQuery.isNotEmpty()) {
            // ... update title text ...
            filterToggleGroup.visibility = View.VISIBLE
            // Call the new, clear method in the ViewModel
            viewModel.startSearch(searchQuery, searchTypeEnum)
        } else {
            Log.w(TAG, "Search query is empty.")
            showNoResults("No search query provided.")
        }
    }

    private fun setupRecyclerView() {
        searchResultsAdapter = SearchResultsAdapter { displayableRoot ->
            val intent = Intent(requireContext(), WordDetailsActivity::class.java).apply {
                putExtra(WordDetailsActivity.EXTRA_IDENTIFIER_VALUE, displayableRoot.identifierValue)
                putExtra(WordDetailsActivity.EXTRA_WORD_TEXT_FOR_PRESELECTION, displayableRoot.displayArabicText)
            }
            startActivity(intent)
        }

        val linearLayoutManager = LinearLayoutManager(requireContext())
        resultsRecyclerView.layoutManager = linearLayoutManager
        resultsRecyclerView.adapter = searchResultsAdapter

        // The OnScrollListener is updated to use the new UIState
        resultsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                // Get the current state from the ViewModel
                val currentState = viewModel.uiState.value

                // We only proceed if the current state is HasResults and we are NOT already loading more.
                if (currentState is WordAnalysisUiState.HasResults && !currentState.isLoadingMore) {
                    val visibleItemCount = linearLayoutManager.childCount
                    val totalItemCount = linearLayoutManager.itemCount
                    val firstVisibleItemPosition = linearLayoutManager.findFirstVisibleItemPosition()

                    // The classic pagination check: are we near the end of the list?
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0) {
                        viewModel.loadMoreResults()
                    }
                }
            }
        })
    }
    private fun setupFilterListeners() {
        filterToggleGroup.check(R.id.filterButtonAll)
        filterToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && isFilterListenerReady) {
                val newFilter = when (checkedId) {
                    R.id.filterButtonRoot -> FilterType.ROOT
                    R.id.filterButtonLemma -> FilterType.LEMMA
                    R.id.filterButtonOthers -> FilterType.OTHERS
                    else -> FilterType.ALL
                }
                viewModel.applyFilter(newFilter)
            }
        }
        isFilterListenerReady = true
    }
    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            // Hide all components by default, then show the correct one.
            progressBar.visibility = View.GONE
            resultsRecyclerView.visibility = View.GONE
            noResultsLayout.visibility = View.GONE
            totalResultsTextView.visibility = View.GONE

            when (state) {
                is WordAnalysisUiState.Idle -> {
                    // Do nothing, wait for search to start
                }
                is WordAnalysisUiState.LoadingInitial -> {
                    progressBar.visibility = View.VISIBLE
                }
                is WordAnalysisUiState.HasResults -> {
                    resultsRecyclerView.visibility = View.VISIBLE
                    totalResultsTextView.visibility = View.VISIBLE
                    totalResultsTextView.text = "${state.totalCount} results found"
                    searchResultsAdapter.submitList(state.results)

                    // You would also update your adapter here to show/hide a loading footer
                    // searchResultsAdapter.setLoadingFooter(state.isLoadingMore)
                }
                is WordAnalysisUiState.NoResults -> {
                    noResultsLayout.visibility = View.VISIBLE
                    noResultsTextView.text = state.message
                }
                is WordAnalysisUiState.Error -> {
                    noResultsLayout.visibility = View.VISIBLE
                    noResultsTextView.text = state.message
                }
            }
        }
    }

    // This method is now only called by the observer.
    private fun showNoResults(message: String) {
        noResultsTextView.text = message
        noResultsLayout.visibility = View.VISIBLE
        resultsRecyclerView.visibility = View.GONE
    }

    sealed class WordAnalysisUiState{
        data object Idle : WordAnalysisUiState()

        // State when the very first page is being loaded
        object LoadingInitial : WordAnalysisUiState()

        // State when we have results to show
        data class HasResults(
            val results: List<DisplayableFrequentRoot>,
            val totalCount: Int,
            val isLoadingMore: Boolean // To show a loading footer in the RecyclerView
        ) : WordAnalysisUiState()

        // State when a search/filter completes with no results
        data class NoResults(val message: String) : WordAnalysisUiState()

        // State when an error occurs
        data class Error(val message: String) : WordAnalysisUiState()
    }
}