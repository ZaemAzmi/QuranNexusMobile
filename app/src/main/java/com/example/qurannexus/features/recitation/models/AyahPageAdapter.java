package com.example.qurannexus.features.recitation.models;


import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import androidx.media3.common.util.UnstableApi;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.example.qurannexus.core.database.entities.QuranAyahDetailEntity;
import com.example.qurannexus.features.recitation.SingleAyahPageFragment;
import com.google.gson.Gson;
import java.util.List;
import android.util.SparseArray;

public class AyahPageAdapter extends FragmentStateAdapter {

    public static final int TOTAL_PAGES = 604;

    public AyahPageAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @OptIn(markerClass = UnstableApi.class)
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // The adapter's only job is to tell the new fragment which page it is responsible for.
        int pageNumber = TOTAL_PAGES - position;
        return SingleAyahPageFragment.newInstance(pageNumber);
    }

    @Override
    public int getItemCount() {
        return TOTAL_PAGES;
    }

    // REMOVE the pageDataCache and setPageData methods. They are no longer needed here.
}