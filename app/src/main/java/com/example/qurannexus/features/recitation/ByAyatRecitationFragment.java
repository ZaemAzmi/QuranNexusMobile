package com.example.qurannexus.features.recitation;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.qurannexus.R;
import com.example.qurannexus.core.activities.MainActivity;
import com.example.qurannexus.core.interfaces.QuranApi;
import com.example.qurannexus.core.utils.UtilityService;
import com.example.qurannexus.features.bookmark.models.BookmarkVerse;
import com.example.qurannexus.features.bookmark.models.BookmarksResponse;
import com.example.qurannexus.features.home.HomeFragment;
import com.example.qurannexus.features.home.achievement.AchievementService;
import com.example.qurannexus.features.home.achievement.StreakCheckCallback;
import com.example.qurannexus.features.recitation.models.AyahRecitationModel;
import com.example.qurannexus.features.recitation.models.ChapterAyah;
import com.example.qurannexus.features.recitation.models.SurahRecitationByAyatAdapter;
import com.example.qurannexus.core.network.ApiService;
import com.example.qurannexus.features.recitation.models.VersesPaginationAdapter;
import com.example.qurannexus.features.recitation.models.Word;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
@AndroidEntryPoint
@UnstableApi
public class ByAyatRecitationFragment extends Fragment {
    private static final String ARG_SURAH_NUMBER = "surah_number";
    private static final String ARG_SCROLL_TO_VERSE = "scroll_to_verse";

    private int surahNumber;
    private int scrollToVerse = -1;
    private int currentPage = 0;

    private ArrayList<ChapterAyah> allAyatModels = new ArrayList<>();
    private ArrayList<ArrayList<ChapterAyah>> paginatedAyahs = new ArrayList<>();

    private QuranApi quranApi;
    private Set<String> bookmarkedAyahIds = new HashSet<>();
    private String authToken;
    private AchievementService achievementService;

    // UI components
    public ViewPager2 versesPager;
    private TabLayout paginationIndicator;
    private ProgressBar loadingProgressBar;
    private TextView pageInfoTextView;
    private MaterialButton prevPageButton;
    private MaterialButton nextPageButton;

    private VersesPaginationAdapter paginationAdapter;

    public ByAyatRecitationFragment() {}

    public static ByAyatRecitationFragment newInstance(int surahNumber, int scrollToVerse) {
        ByAyatRecitationFragment fragment = new ByAyatRecitationFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SURAH_NUMBER, surahNumber);
        args.putInt(ARG_SCROLL_TO_VERSE, scrollToVerse);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            surahNumber = getArguments().getInt(ARG_SURAH_NUMBER);
            scrollToVerse = getArguments().getInt(ARG_SCROLL_TO_VERSE, -1);
        }
        quranApi = ApiService.getQuranClient().create(QuranApi.class);
        achievementService = new AchievementService(requireContext());
        authToken = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                .getString("token", null);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_by_ayat_recitation, container, false);

        // Initialize UI components
        versesPager = view.findViewById(R.id.versesPager);
        paginationIndicator = view.findViewById(R.id.paginationIndicator);
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar);
        pageInfoTextView = view.findViewById(R.id.pageInfoTextView);
        prevPageButton = view.findViewById(R.id.prevPageButton);
        nextPageButton = view.findViewById(R.id.nextPageButton);

        // Setup pagination controls
        prevPageButton.setOnClickListener(v -> navigateToPreviousPage());
        nextPageButton.setOnClickListener(v -> navigateToNextPage());

        // Setup ViewPager2
        paginationAdapter = new VersesPaginationAdapter(this);
        versesPager.setAdapter(paginationAdapter);

        versesPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPage = position;
                updatePageControls();
                setupScrollListenerForCurrentPage();
                // Notify parent fragment about page change
                if (getParentFragment() instanceof RecitationPageFragment) {
                    String pageId = getCurrentPageId();
                    if (pageId != null) {
                        try {
                            int pageNumber = Integer.parseInt(pageId);
                            ((RecitationPageFragment) getParentFragment()).onPageChanged(pageNumber);
                        } catch (NumberFormatException e) {
                            Log.e("ByAyatRecitationFragment", "Invalid page ID: " + pageId);
                        }
                    }
                }
            }
        });

        // Set bottom nav padding
        UtilityService utilityService = new UtilityService();
        utilityService.setupBottomNavPadding(this, versesPager);

        if (surahNumber != 0) {
            fetchBookmarksAndVerses(surahNumber);
        }

        return view;
    }

    public VersesPaginationAdapter getPaginationAdapter() {
        return paginationAdapter;
    }

    private void fetchBookmarksAndVerses(int surahIndex) {
        if (authToken == null) {
            Log.e("ByAyatRecitationFragment", "No auth token available");
            fetchVersesByAyat(surahIndex);
            return;
        }

        quranApi.getBookmarks("Bearer " + authToken).enqueue(new Callback<BookmarksResponse>() {
            @Override
            public void onResponse(Call<BookmarksResponse> call, Response<BookmarksResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BookmarksResponse bookmarksResponse = response.body();
                    if ("success".equals(bookmarksResponse.getStatus())) {
                        bookmarkedAyahIds.clear();
                        List<BookmarkVerse> verses = bookmarksResponse.getBookmarks().getVerses();
                        for (BookmarkVerse verse : verses) {
                            bookmarkedAyahIds.add(verse.getItemProperties().getVerseId());
                        }
                    }
                }
                fetchVersesByAyat(surahIndex);
            }

            @Override
            public void onFailure(Call<BookmarksResponse> call, Throwable t) {
                Log.e("ByAyatRecitationFragment", "Failed to fetch bookmarks", t);
                fetchVersesByAyat(surahIndex);
            }
        });
    }
    private void setupScrollListenerForCurrentPage() {
        if (paginationAdapter != null) {
            new Handler().postDelayed(() -> {
                String fragmentTag = "f" + currentPage;
                Fragment pageFragment = getChildFragmentManager().findFragmentByTag(fragmentTag);

                if (pageFragment instanceof VersesPaginationAdapter.PageFragment) {
                    VersesPaginationAdapter.PageFragment fragment =
                            (VersesPaginationAdapter.PageFragment) pageFragment;

                    RecyclerView recyclerView = fragment.getView().findViewById(R.id.versesRecyclerView);
                    if (recyclerView != null) {
                        // Add scroll listener to control bottom navigation visibility
                        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
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
                }
            }, 100); // Short delay to ensure fragment is attached
        }
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

    private void fetchVersesByAyat(int surahIndex) {
        loadingProgressBar.setVisibility(View.VISIBLE);
        versesPager.setVisibility(View.GONE);

        quranApi.getVersesBySurah(surahIndex).enqueue(new Callback<AyahRecitationModel>() {
            @Override
            public void onResponse(Call<AyahRecitationModel> call, Response<AyahRecitationModel> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        List<ChapterAyah> ayahs = response.body().getData();
                        allAyatModels.clear();

                        if (!ayahs.isEmpty()) {
                            ChapterAyah firstAyah = ayahs.get(0);
                            trackChapterRead(firstAyah.getSurahId());
                        }

                        for (ChapterAyah ayah : ayahs) {
                            try {
                                if (ayah.getWords() != null) {
                                    for (Word word : ayah.getWords()) {
                                        if (word != null) {
                                            word.getText();
                                            word.getTranslation();
                                        }
                                    }
                                }
                                ayah.setBookmarked(bookmarkedAyahIds.contains(ayah.getId()));
                                allAyatModels.add(ayah);
                            } catch (Exception e) {
                                Log.w("ByAyatRecitationFragment",
                                        "Skipping problematic ayah: " + ayah.getAyahKey(), e);
                                continue;
                            }
                        }

                        // Group verses by page
                        organizeVersesByPage();

                        // Initialize the ViewPager
                        paginationAdapter.setData(paginatedAyahs);

                        // Calculate initial page for scrolling to a specific verse
                        if (scrollToVerse > 0) {
                            int pageIndex = findPageForVerse(scrollToVerse);
                            if (pageIndex >= 0) {
                                currentPage = pageIndex;
                                versesPager.setCurrentItem(pageIndex, false);

                                // Find verse position within the page
                                final int versePositionInPage = findVersePositionInPage(scrollToVerse, pageIndex);
                                if (versePositionInPage >= 0) {
                                    // Allow time for fragment to be created
                                    new Handler().postDelayed(() -> {
                                        VersesPaginationAdapter.PageFragment fragment =
                                                (VersesPaginationAdapter.PageFragment) getChildFragmentManager()
                                                        .findFragmentByTag("f" + pageIndex);

                                        if (fragment != null) {
                                            fragment.scrollToVerse(versePositionInPage);
                                        }
                                    }, 300);
                                }
                            }
                        }

                        loadingProgressBar.setVisibility(View.GONE);
                        versesPager.setVisibility(View.VISIBLE);
                        updatePageControls();

                    } else {
                        Log.e("ByAyatRecitationFragment",
                                "Error response: " + response.code() + " " + response.message());
                        showError("Failed to load verses. Please try again.");
                    }
                } catch (Exception e) {
                    Log.e("ByAyatRecitationFragment",
                            "Error processing verse data", e);
                    showError("Error loading verses. Please try again.");
                }
                loadingProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(Call<AyahRecitationModel> call, Throwable t) {
                Log.e("ByAyatRecitationFragment", "Failed to fetch verses", t);
                showError("Network error. Please check your connection.");
                loadingProgressBar.setVisibility(View.GONE);
            }
        });
    }
    private void organizeVersesByPage() {
        paginatedAyahs.clear();

        // Group verses by page ID
        Map<String, ArrayList<ChapterAyah>> pageMap = new HashMap<>();

        for (ChapterAyah ayah : allAyatModels) {
            String pageId = ayah.getPageId();
            if (!pageMap.containsKey(pageId)) {
                pageMap.put(pageId, new ArrayList<>());
            }
            pageMap.get(pageId).add(ayah);
        }

        // Sort the pages by their numerical values
        List<String> sortedPageIds = new ArrayList<>(pageMap.keySet());
        java.util.Collections.sort(sortedPageIds, (a, b) -> {
            try {
                return Integer.parseInt(a) - Integer.parseInt(b);
            } catch (NumberFormatException e) {
                return a.compareTo(b);
            }
        });

        // Add the pages in order to the paginated list
        for (String pageId : sortedPageIds) {
            paginatedAyahs.add(pageMap.get(pageId));
        }
    }

    private int findPageForVerse(int verseNumber) {
        for (int pageIndex = 0; pageIndex < paginatedAyahs.size(); pageIndex++) {
            ArrayList<ChapterAyah> pageVerses = paginatedAyahs.get(pageIndex);
            for (ChapterAyah ayah : pageVerses) {
                try {
                    if (Integer.parseInt(ayah.getAyahIndex()) == verseNumber) {
                        return pageIndex;
                    }
                } catch (NumberFormatException e) {
                    Log.w("ByAyatRecitationFragment", "Error parsing ayah index", e);
                }
            }
        }
        return 0; // Default to first page if verse not found
    }
    private int findVersePositionInPage(int verseNumber, int pageIndex) {
        if (pageIndex >= 0 && pageIndex < paginatedAyahs.size()) {
            ArrayList<ChapterAyah> pageVerses = paginatedAyahs.get(pageIndex);
            for (int i = 0; i < pageVerses.size(); i++) {
                try {
                    if (Integer.parseInt(pageVerses.get(i).getAyahIndex()) == verseNumber) {
                        return i;
                    }
                } catch (NumberFormatException e) {
                    Log.w("ByAyatRecitationFragment", "Error parsing ayah index", e);
                }
            }
        }
        return -1;
    }

    private void updatePageControls() {
        int totalPages = paginatedAyahs.size();

        // Update page info text
        pageInfoTextView.setText(String.format("Page %d of %d", currentPage + 1, totalPages));

        // Update navigation buttons
        prevPageButton.setEnabled(currentPage > 0);
        nextPageButton.setEnabled(currentPage < totalPages - 1);

        // Fade out disabled buttons
        prevPageButton.setAlpha(prevPageButton.isEnabled() ? 1.0f : 0.5f);
        nextPageButton.setAlpha(nextPageButton.isEnabled() ? 1.0f : 0.5f);
    }

    private void navigateToPreviousPage() {
        if (currentPage > 0) {
            versesPager.setCurrentItem(currentPage - 1, true);
        }
    }

    private void navigateToNextPage() {
        if (currentPage < paginatedAyahs.size() - 1) {
            versesPager.setCurrentItem(currentPage + 1, true);
        }
    }

    private void trackChapterRead(String surahId) {
        if (surahId.equals("2")) { // Al-Baqarah
            achievementService.unlockAchievement("longest_chapter", success -> {
                if (success) {
                    refreshHomeFragment();
                }
                return null; // Required for Java lambda
            });
        } else if (surahId.equals("108")) { // Al-Kawthar
            achievementService.unlockAchievement("shortest_chapter", success -> {
                if (success) {
                    refreshHomeFragment();
                }
                return null;
            });
        }

        // Check streak achievement
        achievementService.checkStreakEligibility(new StreakCheckCallback() {
            @Override
            public void onStreakChecked(boolean isEligible, int currentStreak) {
                if (isEligible) {
                    achievementService.unlockAchievement("weekly_streak", success -> {
                        if (success) {
                            refreshHomeFragment();
                        }
                        return null;
                    });
                }
            }
        });
    }
    public String getCurrentPageId() {
        if (currentPage >= 0 && currentPage < paginatedAyahs.size() && !paginatedAyahs.get(currentPage).isEmpty()) {
            return paginatedAyahs.get(currentPage).get(0).getPageId();
        }
        return null;
    }
    private void refreshHomeFragment() {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            for (Fragment fragment : activity.getSupportFragmentManager().getFragments()) {
                if (fragment instanceof HomeFragment) {
                    ((HomeFragment) fragment).setupAchievements(AchievementService.PREDEFINED_BADGES);
                }
            }
        }
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

}
