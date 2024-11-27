package com.example.qurannexus.features.bookmark.models

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.qurannexus.features.bookmark.BookmarkChaptersFragment
import com.example.qurannexus.features.bookmark.BookmarkFragment
import com.example.qurannexus.features.bookmark.BookmarkHistoryFragment
import com.example.qurannexus.features.bookmark.BookmarkQuotesFragment
import com.example.qurannexus.features.bookmark.BookmarkVersesFragment

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
            3 -> BookmarkHistoryFragment()
            else -> BookmarkChaptersFragment()
        }
    }
}
