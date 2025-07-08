package com.example.qurannexus.features.bookmark

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.core.network.ApiService
import com.example.qurannexus.core.utils.QuranMetadata
import com.example.qurannexus.databinding.FragmentRecentlyReadBinding
import com.example.qurannexus.features.bookmark.enums.RecentlyReadType
import com.example.qurannexus.features.bookmark.interfaces.BookmarkApi
import com.example.qurannexus.features.bookmark.models.RecentlyRead
import com.example.qurannexus.features.bookmark.models.RecentlyReadAdapter
import com.example.qurannexus.features.bookmark.models.RecentlyReadList
import com.example.qurannexus.features.bookmark.models.RecentlyReadResponse
import com.example.qurannexus.features.bookmark.models.SimpleResponse
import com.example.qurannexus.features.bookmark.viewmodels.RecentlyReadViewModel
import com.example.qurannexus.features.recitation.RecitationPageFragment
import com.example.qurannexus.features.recitation.models.SurahModel
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@AndroidEntryPoint
class RecentlyReadFragment : Fragment() {
    private var _binding: FragmentRecentlyReadBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecentlyReadViewModel by viewModels()
    private lateinit var recentlyReadAdapter: RecentlyReadAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecentlyReadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        setupRecyclerView()
        setupFilterChips()
    }

    private fun setupRecyclerView() {
        recentlyReadAdapter = RecentlyReadAdapter(
            currentType = RecentlyReadType.CHAPTER,
            onItemClick = { item, type -> viewModel.handleItemClick(item, type) },
            onDeleteClick = { item, type -> showDeleteConfirmationDialog(item, type) }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recentlyReadAdapter
        }
    }

    private fun setupFilterChips() {
        binding.filterChipGroup.apply {
            addView(createFilterChip("Chapters"))
            addView(createFilterChip("Pages"))
            addView(createFilterChip("Juz"))

            setOnCheckedStateChangeListener { group, checkedIds ->
                if (checkedIds.isNotEmpty()) {
                    when (group.findViewById<Chip>(checkedIds.first())?.text) {
                        "Chapters" -> viewModel.updateType(RecentlyReadType.CHAPTER)
                        "Pages" -> viewModel.updateType(RecentlyReadType.PAGE)
                        "Juz" -> viewModel.updateType(RecentlyReadType.JUZ)
                    }
                }
            }

            // Set initial selection
            check(getChildAt(0).id)
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is RecentlyReadViewModel.UiState.Loading -> showLoading(true)
                is RecentlyReadViewModel.UiState.Empty -> showEmptyState()
                is RecentlyReadViewModel.UiState.Content -> showContent(state.items)
                is RecentlyReadViewModel.UiState.Error -> showError(state.message)
            }
        }

        viewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            handleNavigation(event)
        }

        viewModel.currentType.observe(viewLifecycleOwner) { type ->
            recentlyReadAdapter.updateCurrentType(type)
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.recyclerView.isVisible = !isLoading
        binding.emptyView.isVisible = false
    }

    private fun showEmptyState() {
        binding.progressBar.isVisible = false
        binding.recyclerView.isVisible = false
        binding.emptyView.isVisible = true
    }

    private fun showContent(items: List<RecentlyRead>) {
        binding.progressBar.isVisible = false
        binding.recyclerView.isVisible = true
        binding.emptyView.isVisible = false
        recentlyReadAdapter.updateData(items, viewModel.currentType.value!!)
    }

    private fun showError(message: String) {
        binding.progressBar.isVisible = false
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun handleNavigation(event: RecentlyReadViewModel.NavigationEvent) {
        when (event) {
            is RecentlyReadViewModel.NavigationEvent.ToChapter -> navigateToChapter(event.chapterNumber)
            is RecentlyReadViewModel.NavigationEvent.ToPage -> navigateToPage(event.pageNumber)
            is RecentlyReadViewModel.NavigationEvent.ToJuz -> navigateToJuz(event.juzNumber)
        }
    }

    private fun navigateToChapter(chapterNumber: Int) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        val isByPage = sharedPreferences.getBoolean("recitation_layout_by_page", false)

        val fragment: RecitationPageFragment

        if (isByPage) {
            // User prefers Page-by-Page view. Find the starting page of the chapter.
            val startingPage = QuranMetadata.getInstance().getStartingPage(chapterNumber)
            fragment = RecitationPageFragment.newInstanceForNavigation(
                true, chapterNumber.toString(), null, startingPage, null, null
            )
        } else {
            // User prefers Verse-by-Verse view.
            fragment = RecitationPageFragment.newInstanceForNavigation(
                false, chapterNumber.toString(), null, null, null, chapterNumber - 1
            )
        }

        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.mainFragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    // --- REPLACEMENT for navigateToPage ---
    private fun navigateToPage(pageNumber: Int) {
        // When navigating to a specific page, the layout preference doesn't matter as much,
        // but we should still respect it for consistency.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        val isByPage = sharedPreferences.getBoolean("recitation_layout_by_page", false)

        val fragment = RecitationPageFragment.newInstanceForNavigation(
            isByPage,
            null,
            null,
            pageNumber,
            null,
            null
        )

        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.mainFragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToJuz(juzNumber: Int) {
        val firstPage = QuranMetadata.getInstance().getJuzStartPage(juzNumber)
        // This function is now correct because it calls our fixed navigateToPage
        navigateToPage(firstPage)
    }

    private fun showDeleteConfirmationDialog(item: RecentlyRead, type: RecentlyReadType) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Recently Read")
            .setMessage("Remove this item from recently read?")
            .setPositiveButton("Delete") { dialog, _ ->
                viewModel.deleteRecentlyRead(item, type)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun createFilterChip(text: String): Chip {
        return Chip(requireContext()).apply {
            this.text = text
            isCheckable = true
            chipBackgroundColor = ColorStateList.valueOf(
                resources.getColor(R.color.white, null)
            )
            textSize = resources.getDimension(R.dimen.text_size_medium) / resources.displayMetrics.density
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}