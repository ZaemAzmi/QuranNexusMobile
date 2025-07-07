package com.example.qurannexus.features.recitation.models;


import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import androidx.media3.common.util.UnstableApi;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.example.qurannexus.core.database.entities.QuranAyahDetailEntity;
import com.example.qurannexus.features.recitation.SingleAyahPageFragment;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.util.SparseArray;

public class AyahPageAdapter extends FragmentStateAdapter {
    public static final int TOTAL_PAGES = 604;
    private Set<String> bookmarkedVerseIds; // NEW: Hold the bookmark data

    // MODIFIED: Update the constructor
    public AyahPageAdapter(Fragment fragment, Set<String> bookmarkedVerseIds) {
        super(fragment);
        this.bookmarkedVerseIds = bookmarkedVerseIds;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        int pageNumber = TOTAL_PAGES - position;
        // MODIFIED: Pass the bookmark data to the fragment instance
        return SingleAyahPageFragment.newInstance(pageNumber, new ArrayList<>(bookmarkedVerseIds));
    }

    @Override
    public int getItemCount() {
        return TOTAL_PAGES;
    }
}