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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qurannexus.R;
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
import com.example.qurannexus.features.recitation.models.Word;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
@AndroidEntryPoint
@UnstableApi
public class ByAyatRecitationFragment extends Fragment {
    private static final String ARG_SURAH_NUMBER = "surah_number";
    private int surahNumber;
    private ArrayList<ChapterAyah> ayatModels = new ArrayList<>();
    private SurahRecitationByAyatAdapter byAyatAdapter;
    private RecyclerView byAyatRecyclerView;
    private QuranApi quranApi;
    private Set<String> bookmarkedAyahIds = new HashSet<>();
    private String authToken;
    private static final String ARG_SCROLL_TO_VERSE = "scroll_to_verse";
    private int scrollToVerse = -1;
    private AchievementService achievementService;
    private ProgressBar loadingProgressBar;
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
        achievementService = new AchievementService(requireContext());
        byAyatRecyclerView = view.findViewById(R.id.byAyatRecyclerView);
        byAyatRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar);
        byAyatAdapter = new SurahRecitationByAyatAdapter(getContext(), ayatModels);
        byAyatRecyclerView.setAdapter(byAyatAdapter);
        UtilityService utilityService = new UtilityService();
        utilityService.setupBottomNavPadding(this, byAyatRecyclerView);

        if (surahNumber != 0) {
            fetchBookmarksAndVerses(surahNumber);
            fetchVersesByAyat(surahNumber);
        }
        return view;
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

    private void fetchVersesByAyat(int surahIndex) {
        loadingProgressBar.setVisibility(View.VISIBLE);
        byAyatRecyclerView.setVisibility(View.GONE);
        quranApi.getVersesBySurah(surahIndex).enqueue(new Callback<AyahRecitationModel>() {
            @Override
            public void onResponse(Call<AyahRecitationModel> call, Response<AyahRecitationModel> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        List<ChapterAyah> ayahs = response.body().getData();
                        ayatModels.clear();
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
                                ayatModels.add(ayah);
                            } catch (Exception e) {
                                Log.w("ByAyatRecitationFragment",
                                        "Skipping problematic ayah: " + ayah.getAyahKey(), e);
                                // Continue processing other ayahs
                                continue;
                            }
                        }
                        byAyatAdapter.notifyDataSetChanged();
                        loadingProgressBar.setVisibility(View.GONE);
                        byAyatRecyclerView.setVisibility(View.VISIBLE);
                        // Scroll handling
                        if (scrollToVerse > 0 && byAyatRecyclerView != null) {
                            for (int i = 0; i < ayatModels.size(); i++) {
                                try {
                                    if (Integer.parseInt(ayatModels.get(i).getAyahIndex()) == scrollToVerse) {
                                        final int position = i;
                                        byAyatRecyclerView.post(() -> {
                                            byAyatRecyclerView.scrollToPosition(position);
                                            highlightVerse(position);
                                        });
                                        break;
                                    }
                                } catch (NumberFormatException e) {
                                    Log.w("ByAyatRecitationFragment",
                                            "Error parsing ayah index", e);
                                    continue;
                                }
                            }
                        }
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
    private void highlightVerse(int position) {
        // Optional: Add visual highlight to the scrolled verse
        View view = byAyatRecyclerView.getLayoutManager().findViewByPosition(position);
        if (view != null) {
            view.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray));
            new Handler().postDelayed(() -> {
                view.setBackgroundColor(Color.TRANSPARENT);
            }, 1500); // Remove highlight after 1.5 seconds
        }
    }


}
