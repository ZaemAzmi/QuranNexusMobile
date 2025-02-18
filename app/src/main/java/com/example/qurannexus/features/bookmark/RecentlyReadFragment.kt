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
import com.example.qurannexus.features.recitation.RecitationPageFragment
import com.example.qurannexus.features.recitation.models.SurahModel
import com.google.android.material.chip.Chip
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RecentlyReadFragment : Fragment() {
    private var _binding: FragmentRecentlyReadBinding? = null
    private val binding get() = _binding!!

    private lateinit var recentlyReadAdapter: RecentlyReadAdapter
    private lateinit var bookmarkApi: BookmarkApi
    private var authToken: String? = null
    private var currentType = RecentlyReadType.CHAPTER

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
        setupApi()
        setupViews()
        fetchRecentlyRead()
    }

    private fun setupApi() {
        bookmarkApi = ApiService.getQuranClient().create(BookmarkApi::class.java)
        authToken = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("token", null)
    }
    private fun setupViews() {
        // Setup RecyclerView
        recentlyReadAdapter = RecentlyReadAdapter(
            currentType = RecentlyReadType.CHAPTER,
            onItemClick = { item, type -> handleItemClick(item, type) },
            onDeleteClick = { item, type -> handleDeleteClick(item, type) }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recentlyReadAdapter
        }

        // Setup ChipGroup for filtering
        binding.filterChipGroup.apply {
            addView(createFilterChip("Chapters"))
            addView(createFilterChip("Pages"))
            addView(createFilterChip("Juz"))

            setOnCheckedStateChangeListener { group, checkedIds ->
                if (checkedIds.isNotEmpty()) {
                    when (group.findViewById<Chip>(checkedIds.first())?.text) {
                        "Chapters" -> updateType(RecentlyReadType.CHAPTER)
                        "Pages" -> updateType(RecentlyReadType.PAGE)
                        "Juz" -> updateType(RecentlyReadType.JUZ)
                    }
                }
            }

            // Set initial selection
            check(getChildAt(0).id)
        }
    }

    private fun createFilterChip(text: String): Chip {
        return Chip(requireContext()).apply {
            this.text = text
            isCheckable = true
            chipBackgroundColor = ColorStateList.valueOf(resources.getColor(R.color.white, null))
        }
    }

    private fun updateType(type: RecentlyReadType) {
        currentType = type
        fetchRecentlyRead()
    }

    private fun fetchRecentlyRead() {
        if (authToken == null) {
            Toast.makeText(context, "Please login to view recently read", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        bookmarkApi.getRecentlyRead("Bearer $authToken").enqueue(object :
            Callback<RecentlyReadResponse> {
            override fun onResponse(
                call: Call<RecentlyReadResponse>,
                response: Response<RecentlyReadResponse>
            ) {
                showLoading(false)
                if (response.isSuccessful && response.body() != null) {
                    val recentlyReadResponse = response.body()!!
                    if (recentlyReadResponse.status == "success") {
                        updateRecentlyReadList(recentlyReadResponse.recentlyRead)
                    } else {
                        showError("Failed to load recently read items")
                    }
                } else {
                    showError("Error: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<RecentlyReadResponse>, t: Throwable) {
                showLoading(false)
                showError("Error: ${t.message}")
            }
        })
    }

    private fun updateRecentlyReadList(recentlyRead: RecentlyReadList) {
        val items = when (currentType) {
            RecentlyReadType.CHAPTER -> recentlyRead.chapters
            RecentlyReadType.PAGE -> recentlyRead.pages
            RecentlyReadType.JUZ -> recentlyRead.juzs
        }

        if (items.isEmpty()) {
            binding.emptyView.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.emptyView.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
            recentlyReadAdapter.updateData(items, currentType)
        }
    }

    private fun handleItemClick(item: RecentlyRead, type: RecentlyReadType) {
        when (type) {
            RecentlyReadType.CHAPTER -> navigateToChapter(item.itemId.toInt())
            RecentlyReadType.PAGE -> navigateToPage(item.itemId.toInt())
            RecentlyReadType.JUZ -> navigateToJuz(item.itemId.toInt())
        }
    }

    private fun handleDeleteClick(item: RecentlyRead, type: RecentlyReadType) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Recently Read")
            .setMessage("Remove this item from recently read?")
            .setPositiveButton("Delete") { dialog, _ ->
                deleteRecentlyRead(item, type)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun deleteRecentlyRead(item: RecentlyRead, type: RecentlyReadType) {
        bookmarkApi.removeRecentlyRead(
            "Bearer $authToken",
            type.toApiString(),
            item.itemId
        ).enqueue(object : Callback<SimpleResponse> {
            override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    fetchRecentlyRead() // Refresh list
                    Toast.makeText(context, "Item removed from recently read", Toast.LENGTH_SHORT).show()
                } else {
                    showError("Failed to remove item")
                }
            }

            override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                showError("Error: ${t.message}")
            }
        })
    }

    private fun navigateToChapter(chapterNumber: Int) {
        val surahDetails = QuranMetadata.getInstance().getSurahDetails(chapterNumber)
        val surahModel = SurahModel(
            surahDetails?.englishName ?: "",
            surahDetails?.arabicName ?: "",
            chapterNumber.toString(),
            surahDetails?.translationName ?: "",
            surahDetails?.numberOfVerses.toString(),
            false
        )

        val fragment = RecitationPageFragment.newInstance(
            surahModel,
            "verseByVerse",
            chapterNumber - 1
        )

        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.mainFragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToPage(pageNumber: Int) {
        // We need to get the surah that starts on this page
        val surahNumber = QuranMetadata.getInstance().getSurahNumberForPage(pageNumber)
        val surahDetails = QuranMetadata.getInstance().getSurahDetails(surahNumber)
        Log.d("RecentlyReadFragment", "Navigating to Page: $pageNumber, Surah: $surahNumber, Details: $surahDetails")

        val surahModel = SurahModel(
            surahDetails?.englishName ?: "",
            surahDetails?.arabicName ?: "",
            surahNumber.toString(),
            surahDetails?.translationName ?: "",
            surahDetails?.numberOfVerses.toString(),
            false
        )

        val fragment = RecitationPageFragment.newInstance(
            surahModel,
            "pageByPage",
            surahNumber - 1
        )

        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.mainFragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToJuz(juzNumber: Int) {
        // Navigate to the first page of the juz
        val firstPage = QuranMetadata.getInstance().getJuzStartPage(juzNumber)
        navigateToPage(firstPage)
    }
    private fun showLoading(isLoading: Boolean) {
        // Check if binding is available
        _binding?.let {
            it.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun showError(message: String) {
        _binding?.let{
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}