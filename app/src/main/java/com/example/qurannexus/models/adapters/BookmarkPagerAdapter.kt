package com.example.qurannexus.models.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.qurannexus.fragments.BookmarkChaptersFragment
import com.example.qurannexus.fragments.BookmarkFragment
import com.example.qurannexus.fragments.BookmarkHistoryFragment
import com.example.qurannexus.fragments.BookmarkQuotesFragment
import com.example.qurannexus.fragments.BookmarkVersesFragment

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
