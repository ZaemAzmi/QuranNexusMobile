package com.example.qurannexus.features.recitation.models;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.qurannexus.R;
import com.example.qurannexus.core.activities.MainActivity;
import com.example.qurannexus.features.recitation.ByAyatRecitationFragment;

import java.util.ArrayList;

@UnstableApi
public class VersesPaginationAdapter extends FragmentStateAdapter {

    private ArrayList<ArrayList<ChapterAyah>> paginatedVerses = new ArrayList<>();

    public VersesPaginationAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    public void setData(ArrayList<ArrayList<ChapterAyah>> data) {
        this.paginatedVerses = data;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return paginatedVerses.size();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        PageFragment fragment = new PageFragment();
        Bundle args = new Bundle();
        args.putInt("page_position", position);
        fragment.setArguments(args);
        return fragment;
    }

    @UnstableApi
    public static class PageFragment extends Fragment {
        private ArrayList<ChapterAyah> pageVerses;
        public RecyclerView versesRecyclerView;
        private SurahRecitationByAyatAdapter adapter;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_verses_page, container, false);

            versesRecyclerView = view.findViewById(R.id.versesRecyclerView);
            versesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

            int pagePosition = getArguments() != null ? getArguments().getInt("page_position", 0) : 0;

            // Get the parent fragment (ByAyatRecitationFragment)
            Fragment parentFragment = getParentFragment();
            if (parentFragment instanceof ByAyatRecitationFragment) {
                ByAyatRecitationFragment byAyatFragment = (ByAyatRecitationFragment) parentFragment;

                // Access the adapter through the parent fragment
                if (byAyatFragment.getPaginationAdapter() != null &&
                        pagePosition < byAyatFragment.getPaginationAdapter().paginatedVerses.size()) {
                    pageVerses = byAyatFragment.getPaginationAdapter().paginatedVerses.get(pagePosition);
//                    adapter = new SurahRecitationByAyatAdapter(getContext(), pageVerses);
//                    versesRecyclerView.setAdapter(adapter);

                    // Add scroll listener here as well for immediate effect
                    setupScrollListener();
                }
            }

            return view;
        }

        public void scrollToVerse(int versePosition) {
            if (versesRecyclerView != null && adapter != null && versePosition < pageVerses.size()) {
                versesRecyclerView.smoothScrollToPosition(versePosition);

                // Highlight the verse briefly
                View view = versesRecyclerView.getLayoutManager().findViewByPosition(versePosition);
                if (view != null) {
                    view.setBackgroundColor(getResources().getColor(R.color.light_gray));
                    view.postDelayed(() -> {
                        view.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                    }, 1500);
                }
            }
        }

        private void setupScrollListener() {
            versesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                private int scrollThreshold = 10;
                private int scrolledDistance = 0;
                private boolean isScrollingUp = false;

                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);

                    if (Math.abs(dy) > 0) {
                        if (dy > 0) { // Scrolling down
                            if (!isScrollingUp) {
                                scrolledDistance += dy;
                                if (scrolledDistance > scrollThreshold) {
                                    hideBottomNavigation();
                                    scrolledDistance = 0;
                                }
                            } else {
                                isScrollingUp = false;
                                scrolledDistance = 0;
                            }
                        } else { // Scrolling up
                            if (isScrollingUp) {
                                scrolledDistance += Math.abs(dy);
                                if (scrolledDistance > scrollThreshold) {
                                    showBottomNavigation();
                                    scrolledDistance = 0;
                                }
                            } else {
                                isScrollingUp = true;
                                scrolledDistance = 0;
                            }
                        }
                    }
                }
            });
        }

        private void hideBottomNavigation() {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).setBottomNavigationVisibility(false);
            }
        }

        private void showBottomNavigation() {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).setBottomNavigationVisibility(true);
            }
        }
    }
}