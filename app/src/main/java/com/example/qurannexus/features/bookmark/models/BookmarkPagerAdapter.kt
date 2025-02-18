package com.example.qurannexus.features.bookmark.models

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.qurannexus.features.bookmark.*

class BookmarkPagerAdapter(fragment: BookmarkFragment) : FragmentStateAdapter(fragment) {
    private val tabCount = 6

    override fun getItemCount(): Int {
        return tabCount
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> BookmarkChaptersFragment()
            1 -> BookmarkVersesFragment()
            2 -> BookmarkPagesFragment()
            3 -> BookmarkQuotesFragment()
            4 -> BookmarkWordsFragment()
            5 -> RecentlyReadFragment()
            else -> BookmarkChaptersFragment()
        }
    }
}
