package com.example.qurannexus.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.qurannexus.R
import com.example.qurannexus.models.adapters.BookmarkPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [BookmarkFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BookmarkFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_bookmark, container, false)

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
                2 -> "Daily Quotes"
                3 -> "History"
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
    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            BookmarkFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
