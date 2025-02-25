package com.example.qurannexus.features.bookmark

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.qurannexus.R
import com.example.qurannexus.features.bookmark.models.BookmarkPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BookmarkFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_bookmark, container, false)
        val activity = requireActivity() as AppCompatActivity
        activity.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_previous)
        }

        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPager)

        // Set up the ViewPager adapter
        val adapter = BookmarkPagerAdapter(this)
        viewPager.adapter = adapter

        // Link TabLayout with ViewPager2 using TabLayoutMediator
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Chapters"
                1 -> "Verses"
                2 -> "Pages" //
                3 -> "Daily Quotes"
                4 -> "Vocabulary"
                5 -> "Recently Read"
                else -> "Chapters"
            }
            tab.view.background = ContextCompat.getDrawable(requireContext(), R.drawable.tab_background_selector)
        }.attach()

        setupSearchBar(view)
        return view
    }
    private fun setupSearchBar(view : View){
        val searchView = view.findViewById<SearchView>(R.id.searchView)
        searchView.setOnSearchClickListener {
            // When the user clicks on the search icon, the search bar will expand
            searchView.layoutParams.width = ConstraintLayout.LayoutParams.MATCH_PARENT
        }

        searchView.setOnCloseListener {
            // When the search is closed, reset to icon-only state
            searchView.layoutParams.width = ConstraintLayout.LayoutParams.WRAP_CONTENT
            false
        }

    }

}
