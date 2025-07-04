package com.example.qurannexus.features.recitation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
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
import androidx.lifecycle.FlowLiveDataConversions;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.example.qurannexus.R;
import com.example.qurannexus.core.database.entities.QuranAyahDetailEntity;
import com.example.qurannexus.core.database.entities.WordData;
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
import com.example.qurannexus.features.recitation.viewModels.PageDataState;
import com.example.qurannexus.features.recitation.viewModels.RecitationViewModel;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.UnstableApi;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
@AndroidEntryPoint
@androidx.media3.common.util.UnstableApi
public class ByPageRecitationFragment extends Fragment {
    private static final String ARG_PAGE_NUMBER = "page_number";
    private static final String ARG_SCROLL_TO_VERSE_ON_PAGE = "arg_scroll_to_verse_on_page";
    private static final String ARG_HIGHLIGHT_CHAPTER_ID = "arg_highlight_chapter_id";

    private static final String ARG_AYAH_LIST_JSON = "arg_ayah_list_json";
    private RecitationViewModel sharedViewModel; // NEW
    private int initialPageNumber;
    private ArrayList<QuranAyahDetailEntity> initialPageAyahs; // Data for the first page shown
    private int receivedScrollToVerseOnPage = -1;
    private String receivedHighlightChapterId = null;
    public static final int TOTAL_PAGES = 604;
    private Context context;
    private int currentPageNumber;
    private QuranApi quranApi;
    private UtilityService utilityService;
    public ViewPager2 viewPager;
    private PageAdapter pageAdapter;
    private TextView pageNumberTextView;
//    private PageAdapter.PageContentCallback contentCallback;
    private AudioPlayerManager audioPlayerManager;
    private DraggableFloatingActionButton audioFab;
    private MaterialCardView expandedAudioPlayer;
    private boolean isPlayerExpanded = false;
    private PageVerseResponse.PageData responseData;
    private AchievementService achievementService;

//    public static ByPageRecitationFragment newInstance(int pageNumber, int scrollToVerseOnPage, String highlightChapterId) {
//        ByPageRecitationFragment fragment = new ByPageRecitationFragment();
//        Bundle args = new Bundle();
//        args.putInt(ARG_PAGE_NUMBER, pageNumber);
//        if (scrollToVerseOnPage > 0) {
//            args.putInt(ARG_SCROLL_TO_VERSE_ON_PAGE, scrollToVerseOnPage);
//            if (highlightChapterId != null && !highlightChapterId.isEmpty()) {
//                args.putString(ARG_HIGHLIGHT_CHAPTER_ID, highlightChapterId);
//            }
//        }
//        fragment.setArguments(args);
//        return fragment;
//    }

    public static ByPageRecitationFragment newInstance(int pageNumber, String ayahsJson, int scrollToVerseOnPage, String highlightChapterId) {
        ByPageRecitationFragment fragment = new ByPageRecitationFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE_NUMBER, pageNumber);
        args.putString(ARG_AYAH_LIST_JSON, ayahsJson);
        if (scrollToVerseOnPage > 0) {
            args.putInt(ARG_SCROLL_TO_VERSE_ON_PAGE, scrollToVerseOnPage);
            if (highlightChapterId != null && !highlightChapterId.isEmpty()) {
                args.putString(ARG_HIGHLIGHT_CHAPTER_ID, highlightChapterId);
            }
        }
        fragment.setArguments(args);
        return fragment;
    }
    // Overload newInstance for when scroll info is not needed
    public static ByPageRecitationFragment newInstance(int pageNumber) {
        return newInstance(pageNumber,"", -1, null);
    }
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        if (getArguments() != null) {
//            currentPageNumber = getArguments().getInt(ARG_PAGE_NUMBER);
//            receivedScrollToVerseOnPage = getArguments().getInt(ARG_SCROLL_TO_VERSE_ON_PAGE, -1);
//            receivedHighlightChapterId = getArguments().getString(ARG_HIGHLIGHT_CHAPTER_ID);
//        }
//        context = getContext();
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            initialPageNumber = getArguments().getInt(ARG_PAGE_NUMBER);
            String ayahsJson = getArguments().getString(ARG_AYAH_LIST_JSON);
            sharedViewModel = new ViewModelProvider(requireParentFragment()).get(RecitationViewModel.class);
            Type listType = new TypeToken<ArrayList<QuranAyahDetailEntity>>() {}.getType();
            initialPageAyahs = new Gson().fromJson(ayahsJson, listType);
            currentPageNumber = getArguments().getInt(ARG_PAGE_NUMBER);
            receivedScrollToVerseOnPage = getArguments().getInt(ARG_SCROLL_TO_VERSE_ON_PAGE, -1);
            receivedHighlightChapterId = getArguments().getString(ARG_HIGHLIGHT_CHAPTER_ID);
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
        pageNumberTextView = view.findViewById(R.id.pageInfoTextView);
        utilityService = new UtilityService();
        achievementService = new AchievementService(requireContext());
        setupViewPager();
        observeSharedViewModel();
        UtilityService utilityService = new UtilityService();
        utilityService.setupBottomNavPadding(this, viewPager);

        // Initialize audio views
        audioPlayerManager = new AudioPlayerManager(requireContext());
        View audioLayout = view.findViewById(R.id.audioPlayerLayout);
        audioFab = audioLayout.findViewById(R.id.audioFab);
        expandedAudioPlayer = audioLayout.findViewById(R.id.expandedAudioPlayer);
        setupAudioControls(); // Setup listeners for the player
        isPlayerExpanded = false;

        audioFab.setOnClickListener(v -> {
            if (isPlayerExpanded) {
                hidePlayer();
            } else {
                // If player is closed, the FAB starts playback and shows the player
                PageDataState state = sharedViewModel.getPageData().getValue();
                if (state instanceof PageDataState.Success) {
                    List<QuranAyahDetailEntity> ayahs = ((PageDataState.Success) state).getAyahs();
                    if (!ayahs.isEmpty()) {
                        audioPlayerManager.playPageAyahs(ayahs);
                    }
                } else {
                    Toast.makeText(getContext(), "Page data not ready.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        setupViewPager();
        return view;
    }
    // NEW: This fragment now listens for data changes itself.
    private void observeSharedViewModel() {
        LiveData<PageDataState> pageDataLiveData = FlowLiveDataConversions.asLiveData(sharedViewModel.getPageData());
        pageDataLiveData.observe(getViewLifecycleOwner(), state -> {
            if (state instanceof PageDataState.Success) {
                PageDataState.Success successState = (PageDataState.Success) state;
                if (!successState.getAyahs().isEmpty()) {
                    // When new data arrives, tell the adapter to update its cache.
                    int pageNumber = successState.getAyahs().get(0).getPageId();
                    if (pageAdapter != null) {
                        pageAdapter.updatePageContent(pageNumber, successState.getAyahs());
                    }
                }
            }
        });
    }

//    private void setupViewPager() {
//        pageAdapter = new PageAdapter(this);
//        viewPager.setAdapter(pageAdapter);
//
//        int initialPosition = TOTAL_PAGES - currentPageNumber;
//        viewPager.setCurrentItem(initialPosition, false);
//
//        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
//            @Override
//            public void onPageSelected(int position) {
//                super.onPageSelected(position);
//                currentPageNumber = TOTAL_PAGES - position;
//                updatePageNumber();
//                audioPlayerManager.handlePageChange(currentPageNumber);
//
//                // Notify parent fragment about page change
//                if (getParentFragment() instanceof RecitationPageFragment) {
//                    ((RecitationPageFragment) getParentFragment()).onPageChanged(currentPageNumber);
//                }
//            }
//        });
//
//        updatePageNumber();
//    }

    private void setupViewPager() {
        // Pass the initial data to the adapter
        pageAdapter = new PageAdapter(this, initialPageAyahs, initialPageNumber);
        viewPager.setAdapter(pageAdapter);

        int initialPosition = TOTAL_PAGES - initialPageNumber;
        viewPager.setCurrentItem(initialPosition, false);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                int newPageNumber = TOTAL_PAGES - position;
                updatePageNumber(newPageNumber);

                // IMPORTANT: Notify the parent fragment to load data for the new page
                if (getParentFragment() instanceof RecitationPageFragment) {
                    ((RecitationPageFragment) getParentFragment()).onPageChanged(newPageNumber);
                }
            }
        });
        updatePageNumber(initialPageNumber);
    }

    // NEW: Helper method to render content
    private SpannableStringBuilder renderPageContent(List<QuranAyahDetailEntity> ayahs) {
        if (ayahs == null || ayahs.isEmpty()) {
            return new SpannableStringBuilder("No content for this page.");
        }

        SpannableStringBuilder pageBuilder = new SpannableStringBuilder();
        Gson gson = new Gson();
        int lastLineNumber = -1;

        for (QuranAyahDetailEntity ayahEntity : ayahs) {
            // TODO: Logic to add Surah Name Calligraphy if it's a new surah
            // if (ayahEntity.getAyahIndex() == 1) { ... }

            Type wordListType = new TypeToken<List<WordData>>() {}.getType();
            List<WordData> words = gson.fromJson(ayahEntity.getWordsDataJson(), wordListType);

            if (words != null) {
                for (WordData word : words) {
                    // Handle line breaks based on the data from our Python script
                    if (word.getLine_number() != lastLineNumber && lastLineNumber != -1) {
                        pageBuilder.append("\n");
                    }
                    lastLineNumber = word.getLine_number();

                    // Create a clickable span for each word
                    SpannableString wordSpannable = new SpannableString(word.getText() + " ");

                    // You can add a ClickableSpan here later for word analysis
                    // ClickableSpan clickableSpan = new ClickableSpan() { ... };
                    // wordSpannable.setSpan(clickableSpan, 0, word.getText().length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    pageBuilder.append(wordSpannable);
                }
            }

            // Append the Ayah number symbol
            String ayahNumberInArabic = new com.example.qurannexus.core.utils.UtilityService()
                    .convertToArabicNumber(ayahEntity.getAyahIndex());
            pageBuilder.append(String.format(" %s ", ayahNumberInArabic));
        }

        return pageBuilder;
    }

    // NEW METHOD: This is called by the parent RecitationPageFragment when new page data arrives.
    public void updateAdapterData(List<QuranAyahDetailEntity> ayahs) {
        if (pageAdapter != null && ayahs != null && !ayahs.isEmpty()) {
            // Get the page number from the data itself
            int pageNumber = ayahs.get(0).getPageId();
            pageAdapter.updatePageContent(pageNumber, ayahs);
        }
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
//        audioPlayerManager.isPlaying().observe(getViewLifecycleOwner(), isPlaying -> {
//            playPauseButton.setImageResource(
//                    isPlaying ? R.drawable.ic_pause : R.drawable.ic_play_audio_black
//            );
//            audioFab.setImageResource(
//                    isPlaying ? R.drawable.ic_pause : R.drawable.ic_play_audio
//            );
//        });
        audioPlayerManager.isPlaying().observe(getViewLifecycleOwner(), isPlaying -> {
            playPauseButton.setImageResource(
                    isPlaying ? R.drawable.ic_pause : R.drawable.ic_play_audio_black
            );
            // Don't change the FAB icon here, it's for opening/closing
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

        // Observe LiveData from the manager to update the UI
        audioPlayerManager.getShouldShowPlayer().observe(getViewLifecycleOwner(), shouldShow -> {
            if (shouldShow) {
                showPlayer();
            } else {
                hidePlayer();
            }
        });
    }
//    public void fetchPageVerses(int pageNumber, PageAdapter.PageContentCallback callback) {
//        quranApi.getPageVerses(pageNumber, true,true).enqueue(new Callback<PageVerseResponse>() {
//            @Override
//            public void onResponse(Call<PageVerseResponse> call, Response<PageVerseResponse> response) {
//                if (response.isSuccessful() && response.body() != null) {
//                    // Store the response data
//                    responseData = response.body().getData();
//                    List<PageAyah> verseList = responseData.getAyahs();
//                    setupAudioForPage(verseList);
//
//                    if (!verseList.isEmpty()) {
//                        PageAyah firstVerse = verseList.get(0);
//                        trackChapterRead(firstVerse.getSurahId());
//                    }
//
////                    List<PageAyah> verseList = response.body().getData().getAyahs();
//                    SpannableStringBuilder pageContent = new SpannableStringBuilder();
//                    int previousSurahId = -1;
//
//                    for (PageAyah ayah : verseList) {
//                        int currentSurahId = Integer.parseInt(ayah.getSurahId());
//                        int currentAyahNumber = Integer.parseInt(ayah.getAyahIndex());
//
//                        // Insert calligraphy at the start of a new surah
//                        if (currentAyahNumber == 1) {
//                            pageContent.append("\n");
//                            appendAndCenterCalligraphy(pageContent, "surah_0", "surah_" + currentSurahId);
//                            pageContent.append("\n");
//                        }
//
//                        // Insert Bismillah if present
//                        if (ayah.getBismillah() != null && !ayah.getBismillah().isEmpty()) {
////                            pageContent.append(ayah.getBismillah()).append("\n");
//                            SpannableString bismillah = new SpannableString(ayah.getBismillah());
//                            bismillah.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
//                                    0, bismillah.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//                            pageContent.append(bismillah).append("\n\n");
//
//                        }
//
//                        // Append text from each word
//                        StringBuilder ayahText = new StringBuilder();
//                        if (ayah.getWords() != null) {
//                            List<Word> words = ayah.getWords();
//                            // Only iterate until the second-to-last word to skip the waqaf
//                            for (int i = 0; i < words.size() - 1; i++) {
//                                Word word = words.get(i);
//                                if (word.getText() != null) {
//                                    ayahText.append(word.getText()).append(" ");
//                                }
//                            }
//                        }
//
//                        // Append the constructed ayah text and number
//                        String arabicNumber = utilityService.convertToArabicNumber(currentAyahNumber);
//                        pageContent.append(ayahText.toString().trim())
//                                .append(" ")
//                                .append(arabicNumber)
//                                .append(" ");
//
//                        previousSurahId = currentSurahId;
//                    }
//                    pageContent.append("\n\n");
//                    if (getParentFragment() instanceof RecitationPageFragment) {
//                        ((RecitationPageFragment) getParentFragment()).onPageChanged(pageNumber);
//                    }
//
//                    // Call the content callback if set
//                    if (contentCallback != null) {
//                        contentCallback.onPageContentFetched(pageContent);
//                    }
//                    callback.onPageContentFetched(pageContent);
//
//                    if (receivedScrollToVerseOnPage > 0 && receivedHighlightChapterId != null) {
//                        Log.d("ByPageRecitation", "Attempting to highlight/scroll to: " + receivedHighlightChapterId + ":" + receivedScrollToVerseOnPage);
//                        // TODO: Implement the actual logic to find this verse in the 'pageContent' SpannableStringBuilder
//                        // and apply a highlight (e.g., BackgroundColorSpan) or attempt to scroll the TextView to it.
//                        // This is complex because 'pageContent' is one large block.
//                        // You would need to find the start/end index of the target verse text within pageContent.
//                        // One way: Iterate through `verseList` again, reconstruct text as it was added to `pageContent`,
//                        // keeping track of start/end indices, then find the one matching receivedHighlightChapterId/receivedScrollToVerseOnPage.
//                        // For now, a Toast or Log is a good placeholder.
//                        Toast.makeText(context, "Should highlight " + receivedHighlightChapterId + ":" + receivedScrollToVerseOnPage, Toast.LENGTH_LONG).show();
//                    }
//                } else {
//                    callback.onPageContentFetchFailed(SpannableStringBuilder.valueOf("Failed to fetch page content"));
//                }
//            }
//
//            @Override
//            public void onFailure(Call<PageVerseResponse> call, Throwable t) {
//                callback.onPageContentFetchFailed(SpannableStringBuilder.valueOf("Error fetching page: " + t.getMessage()));
//            }
//        });
//    }
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
            // Get the intrinsic width and height
            int intrinsicWidth = calligraphyDrawable.getIntrinsicWidth();
            int intrinsicHeight = calligraphyDrawable.getIntrinsicHeight();

            // Scale factor - increase this number to make the calligraphy larger
            float scaleFactor;
            if (getResources().getDisplayMetrics().widthPixels >=
                    getResources().getDisplayMetrics().density * 600) {
                scaleFactor = 2.5f; // For tablets
            } else {
                scaleFactor = 1.5f; // For phones
            }
            // Apply the scale factor to the width and height
            int scaledWidth = (int)(intrinsicWidth * scaleFactor);
            int scaledHeight = (int)(intrinsicHeight * scaleFactor);

            // Set the bounds with scaled dimensions
            calligraphyDrawable.setBounds(0, 0, scaledWidth, scaledHeight);
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

    private void playFullPage(List<QuranAyahDetailEntity> ayahs) {
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String selectedReciterKey = prefs.getString("selected_reciter_key", "Alafasy");

        ArrayList<String> relativeUrls = new ArrayList<>();
        ArrayList<String> ayahKeys = new ArrayList<>();
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, String>>(){}.getType();

        // 1. Build the list of URLs and keys for the whole page
        for (QuranAyahDetailEntity ayah : ayahs) {
            Map<String, String> audioUrls = gson.fromJson(ayah.getAyahAudioUrlsJson(), type);
            if (audioUrls != null) {
                String url = audioUrls.get(selectedReciterKey);
                if (url != null && !url.isEmpty()) {
                    relativeUrls.add(url);
                    ayahKeys.add(ayah.getAyahKey());
                }
            }
        }

        // 2. Tell the manager to play the entire sequence
        if (!relativeUrls.isEmpty()) {
            audioPlayerManager.playPageSequence(relativeUrls, ayahKeys);
        } else {
            Toast.makeText(getContext(), "No audio available for this page.", Toast.LENGTH_SHORT).show();
        }
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
    public void onPause() {
        super.onPause();
        // Tell the manager to stop everything when this fragment is no longer visible
        if (audioPlayerManager != null) {
            audioPlayerManager.stopAndHidePlayer();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // It's also good practice to release resources in onDestroy
        if (audioPlayerManager != null) {
            audioPlayerManager.release();
        }
    }
    private void updatePageNumber(int pageNum) {
        // Update the member variable for consistency
        this.currentPageNumber = pageNum;
        if (pageNumberTextView != null) {
            // Use the correct format string
            pageNumberTextView.setText(String.format("Page %d of %d", pageNum, TOTAL_PAGES));
        }
    }

//    public void setPageContentCallback(PageAdapter.PageContentCallback callback) {
//        this.contentCallback = callback;
//    }

    public PageVerseResponse.PageData getResponseData() {
        return responseData;
    }
}