package com.example.qurannexus.features.recitation;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.AlignmentSpan;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.qurannexus.R;
import com.example.qurannexus.core.interfaces.QuranApi;
import com.example.qurannexus.features.home.HomeFragment;
import com.example.qurannexus.features.home.achievement.AchievementService;
import com.example.qurannexus.features.home.achievement.StreakCheckCallback;
import com.example.qurannexus.features.recitation.audio.AudioPlayerManager;
import com.example.qurannexus.features.recitation.audio.ui.DraggableFloatingActionButton;
import com.example.qurannexus.features.recitation.models.PageAyah;
import com.example.qurannexus.features.recitation.models.PageVerseResponse;
import com.example.qurannexus.features.recitation.models.PageAdapter;
import com.example.qurannexus.core.utils.UtilityService;
import com.example.qurannexus.core.network.ApiService;
import com.example.qurannexus.features.recitation.models.Word;
import com.google.android.material.card.MaterialCardView;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.UnstableApi;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
@AndroidEntryPoint
@androidx.media3.common.util.UnstableApi
public class ByPageRecitationFragment extends Fragment {
    private static final String ARG_PAGE_NUMBER = "page_number";
    private static final int TOTAL_PAGES = 604;
    private Context context;
    private int currentPageNumber;
    private QuranApi quranApi;
    private UtilityService utilityService;
    private ViewPager2 viewPager;
    private PageAdapter pageAdapter;
    private TextView pageNumberTextView;
    private PageAdapter.PageContentCallback contentCallback;
    private AudioPlayerManager audioPlayerManager;
    private DraggableFloatingActionButton audioFab;
    private MaterialCardView expandedAudioPlayer;
    private boolean isPlayerExpanded = false;
    private PageVerseResponse.PageData responseData;
    private AchievementService achievementService;
    public static ByPageRecitationFragment newInstance(int pageNumber) {
        ByPageRecitationFragment fragment = new ByPageRecitationFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE_NUMBER, pageNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentPageNumber = getArguments().getInt(ARG_PAGE_NUMBER);
        }
        context = getContext();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_by_page_recitation, container, false);
        quranApi = ApiService.getQuranClient().create(QuranApi.class);
        viewPager = view.findViewById(R.id.fragmentByPageRecitationViewPager);
        pageNumberTextView = view.findViewById(R.id.pageNumber);
        utilityService = new UtilityService();
        achievementService = new AchievementService(requireContext());
        setupViewPager();

        UtilityService utilityService = new UtilityService();
        utilityService.setupBottomNavPadding(this, viewPager);
        // Initialize audio views
        View audioLayout = view.findViewById(R.id.audioPlayerLayout);
        audioFab = audioLayout.findViewById(R.id.audioFab);
        expandedAudioPlayer = audioLayout.findViewById(R.id.expandedAudioPlayer);
        audioPlayerManager = new AudioPlayerManager(requireContext(), quranApi);

        isPlayerExpanded = false;
        if (expandedAudioPlayer != null) {
            expandedAudioPlayer.setVisibility(View.GONE);
            expandedAudioPlayer.setAlpha(0f);
            expandedAudioPlayer.setTranslationX(expandedAudioPlayer.getWidth());
        }

        setupAudioControls();
        return view;
    }

    private void setupAudioForPage(List<PageAyah> pageAyahs) {
        audioPlayerManager.playPageAyahs(pageAyahs);
    }

    private void setupViewPager() {
        pageAdapter = new PageAdapter(this);
        viewPager.setAdapter(pageAdapter);

        int initialPosition = TOTAL_PAGES - currentPageNumber;
        viewPager.setCurrentItem(initialPosition, false);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPageNumber = TOTAL_PAGES - position;
                updatePageNumber();
                audioPlayerManager.handlePageChange(currentPageNumber);

                // Notify parent fragment about page change
                if (getParentFragment() instanceof RecitationPageFragment) {
                    ((RecitationPageFragment) getParentFragment()).onPageChanged(currentPageNumber);
                }
            }
        });

        updatePageNumber();
    }

    private void setupAudioControls() {
        if (audioFab == null || expandedAudioPlayer == null) return;

        ImageButton playPauseButton = expandedAudioPlayer.findViewById(R.id.playPauseButton);
        ImageButton speedMenuButton = expandedAudioPlayer.findViewById(R.id.speedMenuButton);
        SeekBar seekBar = expandedAudioPlayer.findViewById(R.id.audioSeekBar);
        TextView currentTimeText = expandedAudioPlayer.findViewById(R.id.currentTimeText);
//        TextView durationText = expandedAudioPlayer.findViewById(R.id.durationText);

        // Observe loading state
        audioPlayerManager.isLoadingDuration().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
//                durationText.setText("-/-");
                currentTimeText.setText("-/-");
                seekBar.setEnabled(false);
            } else {
                seekBar.setEnabled(true);
            }
        });

        // Set click listeners
        audioFab.setOnClickListener(v -> togglePlayer());

        playPauseButton.setOnClickListener(v -> {
            if (Boolean.TRUE.equals(audioPlayerManager.isPlaying().getValue())) {
                audioPlayerManager.togglePlayPause();
            } else {
                audioPlayerManager.startPlayback();
            }
        });

        // Setup speed menu
        speedMenuButton.setOnClickListener(v -> showSpeedMenu(speedMenuButton));

        // Update UI based on playback state
        audioPlayerManager.isPlaying().observe(getViewLifecycleOwner(), isPlaying -> {
            playPauseButton.setImageResource(
                    isPlaying ? R.drawable.ic_pause : R.drawable.ic_play_audio_black
            );
            audioFab.setImageResource(
                    isPlaying ? R.drawable.ic_pause : R.drawable.ic_play_audio
            );
        });

        // Handle progress updates
        audioPlayerManager.getCurrentPosition().observe(getViewLifecycleOwner(), position -> {
            if (position != null && !seekBar.isPressed()) {
                seekBar.setProgress(position);
            }
        });

        audioPlayerManager.getDuration().observe(getViewLifecycleOwner(), duration -> {
            if (duration != null) {
                seekBar.setMax(duration);
            }
        });

        audioPlayerManager.getCurrentTimeText().observe(getViewLifecycleOwner(), text -> {
            if (text != null) {
                currentTimeText.setText(text);
            }
        });

        // Handle seek bar changes
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentTimeText.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Boolean isLoading = audioPlayerManager.isLoadingDuration().getValue();
                if (isLoading != null && isLoading) {
                    currentTimeText.setText("-/-");
                    return;
                }
                audioPlayerManager.seekTo(seekBar.getProgress());
            }
        });

        // Observe player visibility
        audioPlayerManager.getShouldShowPlayer().observe(getViewLifecycleOwner(), shouldShow -> {
            audioFab.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
            if (!shouldShow && isPlayerExpanded) {
                hidePlayer();
            }
        });
    }
    public void fetchPageVerses(int pageNumber, PageAdapter.PageContentCallback callback) {
//        Log.e("page num", String.valueOf(pageNumber));
        quranApi.getPageVerses(pageNumber, true,true).enqueue(new Callback<PageVerseResponse>() {
            @Override
            public void onResponse(Call<PageVerseResponse> call, Response<PageVerseResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Store the response data
                    responseData = response.body().getData();
                    List<PageAyah> verseList = responseData.getAyahs();
                    setupAudioForPage(verseList);

                    if (!verseList.isEmpty()) {
                        PageAyah firstVerse = verseList.get(0);
                        trackChapterRead(firstVerse.getSurahId());
                    }

//                    List<PageAyah> verseList = response.body().getData().getAyahs();
                    SpannableStringBuilder pageContent = new SpannableStringBuilder();
                    int previousSurahId = -1;

                    for (PageAyah ayah : verseList) {
                        int currentSurahId = Integer.parseInt(ayah.getSurahId());
                        int currentAyahNumber = Integer.parseInt(ayah.getAyahIndex());

                        // Insert calligraphy at the start of a new surah
                        if (currentAyahNumber == 1) {
                            pageContent.append("\n");
                            appendAndCenterCalligraphy(pageContent, "surah_0", "surah_" + currentSurahId);
                            pageContent.append("\n");
                        }

                        // Insert Bismillah if present
                        if (ayah.getBismillah() != null && !ayah.getBismillah().isEmpty()) {
//                            pageContent.append(ayah.getBismillah()).append("\n");
                            SpannableString bismillah = new SpannableString(ayah.getBismillah());
                            bismillah.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                                    0, bismillah.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            pageContent.append(bismillah).append("\n\n");

                        }

                        // Append text from each word
                        StringBuilder ayahText = new StringBuilder();
                        if (ayah.getWords() != null) {
                            List<Word> words = ayah.getWords();
                            // Only iterate until the second-to-last word to skip the waqaf
                            for (int i = 0; i < words.size() - 1; i++) {
                                Word word = words.get(i);
                                if (word.getText() != null) {
                                    ayahText.append(word.getText()).append(" ");
                                }
                            }
                        }

                        // Append the constructed ayah text and number
                        String arabicNumber = utilityService.convertToArabicNumber(currentAyahNumber);
                        pageContent.append(ayahText.toString().trim())
                                .append(" ")
                                .append(arabicNumber)
                                .append(" ");

                        previousSurahId = currentSurahId;
                    }
                    pageContent.append("\n\n");
                    if (getParentFragment() instanceof RecitationPageFragment) {
                        ((RecitationPageFragment) getParentFragment()).onPageChanged(pageNumber);
                    }

                    // Call the content callback if set
                    if (contentCallback != null) {
                        contentCallback.onPageContentFetched(pageContent);
                    }
                    callback.onPageContentFetched(pageContent);
                } else {
                    callback.onPageContentFetchFailed(SpannableStringBuilder.valueOf("Failed to fetch page content"));
                }
            }

            @Override
            public void onFailure(Call<PageVerseResponse> call, Throwable t) {
                callback.onPageContentFetchFailed(SpannableStringBuilder.valueOf("Error fetching page: " + t.getMessage()));
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
    private void appendCalligraphyToContent(SpannableStringBuilder pageContent, String fileName) {
        Drawable calligraphyDrawable = getSurahCalligraphyDrawable(fileName);
        if (calligraphyDrawable != null) {
            calligraphyDrawable.setBounds(0, 0, calligraphyDrawable.getIntrinsicWidth(), calligraphyDrawable.getIntrinsicHeight());
            calligraphyDrawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
            // Create an ImageSpan with centered alignment
            ImageSpan calligraphySpan = new ImageSpan(calligraphyDrawable, DynamicDrawableSpan.ALIGN_BASELINE);

            pageContent.append(" ");  // Add leading spaces for centering
            int start = pageContent.length();
            pageContent.append(" "); // Placeholder for the image
            pageContent.setSpan(calligraphySpan, start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Apply white color to the image span
            ForegroundColorSpan whiteSpan = new ForegroundColorSpan(ContextCompat.getColor(context, android.R.color.white));
            pageContent.setSpan(whiteSpan, start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
    private Drawable getSurahCalligraphyDrawable(String fileName) {
        // Convert drawable resource name to resource ID
        int resId = context.getResources().getIdentifier(fileName, "drawable", context.getPackageName());
        if (resId != 0) {
            return ContextCompat.getDrawable(context, resId);
        }
        return null;
    }


    private void appendAndCenterCalligraphy(SpannableStringBuilder pageContent, String fileName1, String fileName2) {
        int startIndex = pageContent.length();
        appendCalligraphyToContent(pageContent, fileName2);
        appendCalligraphyToContent(pageContent, fileName1);

        int endIndex = pageContent.length();

        // Apply center alignment to the entire span containing both calligraphy images
        pageContent.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    private void showSpeedMenu(View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenu().add(Menu.NONE, 1, Menu.NONE, "0.5x");
        popup.getMenu().add(Menu.NONE, 2, Menu.NONE, "0.75x");
        popup.getMenu().add(Menu.NONE, 3, Menu.NONE, "1.0x");
        popup.getMenu().add(Menu.NONE, 4, Menu.NONE, "1.5x");
        popup.getMenu().add(Menu.NONE, 5, Menu.NONE, "2.0x");

        popup.setOnMenuItemClickListener(item -> {
            float speed = 1.0f;
            switch (item.getItemId()) {
                case 1: speed = 0.5f; break;
                case 2: speed = 0.75f; break;
                case 3: speed = 1.0f; break;
                case 4: speed = 1.5f; break;
                case 5: speed = 2.0f; break;
            }
            audioPlayerManager.setPlaybackSpeed(speed);
            return true;
        });

        popup.show();
    }

    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60));
        return String.format("%d:%02d", minutes, seconds);
    }

    private void togglePlayer() {
        if (expandedAudioPlayer == null || audioFab == null) return;

        if (isPlayerExpanded) {
            hidePlayer();
        } else {
            showPlayer();
        }
    }

    private void showPlayer() {
        expandedAudioPlayer.setVisibility(View.VISIBLE);
        expandedAudioPlayer.setAlpha(0f);

        expandedAudioPlayer.post(() -> {
            float fabCenterY = audioFab.getY() + (audioFab.getHeight() / 2);
            float playerHeight = expandedAudioPlayer.getHeight();
            float playerY = fabCenterY - (playerHeight / 2);

            expandedAudioPlayer.setY(playerY);
            expandedAudioPlayer.setTranslationX(expandedAudioPlayer.getWidth());

            expandedAudioPlayer.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(200)
                    .withStartAction(() -> {
                        isPlayerExpanded = true;
                        audioFab.setImageResource(R.drawable.ic_close);
                    })
                    .start();
        });
    }


    private void hidePlayer() {
        expandedAudioPlayer.animate()
                .alpha(0f)
                .translationX(expandedAudioPlayer.getWidth())
                .setDuration(200)
                .withEndAction(() -> {
                    expandedAudioPlayer.setVisibility(View.GONE);
                    isPlayerExpanded = false;
                    audioFab.setImageResource(
                            Boolean.TRUE.equals(audioPlayerManager.isPlaying().getValue()) ?
                                    R.drawable.ic_pause : R.drawable.ic_play_audio
                    );
                })
                .start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (audioPlayerManager != null) {
            audioPlayerManager.release();
        }
    }

    private void updatePageNumber() {
        pageNumberTextView.setText(String.format("Page: %d", currentPageNumber));
    }

    public void setPageContentCallback(PageAdapter.PageContentCallback callback) {
        this.contentCallback = callback;
    }

    public PageVerseResponse.PageData getResponseData() {
        return responseData;
    }
}