package com.example.qurannexus.features.bookmark.models

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.qurannexus.features.bookmark.*

class BookmarkPagerAdapter(activity: BookmarkFragment) : FragmentStateAdapter(activity) {
    private val tabCount = 4

    override fun getItemCount(): Int {
        return tabCount
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> BookmarkChaptersFragment()
            1 -> BookmarkVersesFragment()
            2 -> BookmarkQuotesFragment()
            3 -> BookmarkWordsFragment()
            4 -> BookmarkHistoryFragment()
            else -> BookmarkChaptersFragment()
        }
    }
}
