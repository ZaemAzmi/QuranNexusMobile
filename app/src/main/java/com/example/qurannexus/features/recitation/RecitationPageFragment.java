package com.example.qurannexus.features.recitation;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.media3.common.util.UnstableApi;
import androidx.preference.PreferenceManager;
import androidx.lifecycle.LifecycleKt;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.qurannexus.R;
import com.example.qurannexus.core.interfaces.QuranApi;
import com.example.qurannexus.core.network.ApiService;
import com.example.qurannexus.core.utils.CoroutinesHelper;
import com.example.qurannexus.core.utils.ReadingTracker;
import com.example.qurannexus.core.utils.Result;
import com.example.qurannexus.core.utils.SurahDetails;
import com.example.qurannexus.core.utils.TokenManager;
import com.example.qurannexus.core.utils.UtilityService;
import com.example.qurannexus.features.bookmark.enums.RecentlyReadType;
import com.example.qurannexus.features.bookmark.interfaces.BookmarkApi;
import com.example.qurannexus.features.bookmark.models.AddRecentlyReadRequest;
import com.example.qurannexus.features.bookmark.models.BookmarkChapter;
import com.example.qurannexus.features.bookmark.models.BookmarkPage;
import com.example.qurannexus.features.bookmark.models.BookmarkRequest;
import com.example.qurannexus.features.bookmark.models.BookmarkResponse;
import com.example.qurannexus.features.bookmark.models.BookmarksResponse;
import com.example.qurannexus.features.bookmark.models.RemoveBookmarkResponse;
import com.example.qurannexus.features.bookmark.models.SimpleResponse;
import com.example.qurannexus.features.bookmark.repositories.RecentlyReadRepository;
import com.example.qurannexus.features.recitation.models.PageAdapter;
import com.example.qurannexus.features.recitation.models.SurahModel;
import com.example.qurannexus.core.utils.QuranMetadata;
import com.example.qurannexus.features.statistics.interfaces.StatisticsApi;
import com.example.qurannexus.features.statistics.models.UpdateRecitationTimesRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import kotlin.Unit;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class RecitationPageFragment extends Fragment {
    SurahModel surahModel;
     int startPosition;
     String mode;
    String layoutType = "verseByVerse";
    private int scrollToVerse = -1;  // Add this line
    private boolean isPageBookmarked = false;
    private ImageView pageBookmarkIcon;
    private int currentPageNumber = 1;
    private static final String KEY_LAYOUT_TYPE = "recitation_layout_by_page";
    private int currentSurahIndex;
    private View rootView;
    private QuranApi quranApi;
    private BookmarkApi bookmarkApi;
    private StatisticsApi statisticsApi;
    private boolean isChapterBookmarked = false;
    private ImageView bookmarkIcon;
    private String authToken;
    private static final String ARG_SCROLL_TO_VERSE = "scrollToVerse";
    private TextView surahNameTextView;
    private TextView surahNameEnglishTextView;
    private QuranMetadata quranMetadata;
    private long readingStartTime;
    @Inject
    RecentlyReadRepository recentlyReadRepository;
    private CoroutineScope coroutineScope;
    public RecitationPageFragment() {
    }

    public static RecitationPageFragment newInstance(SurahModel surahModel, String fragmentType, int currentSurahIndex) {
        RecitationPageFragment fragment = new RecitationPageFragment();
        Bundle args = new Bundle();
        if (surahModel != null) {
            args.putParcelable("surah_model", surahModel);
        }

        args.putString("fragment_type", fragmentType);
        args.putInt("current_surah_index", currentSurahIndex);

        Activity activity = fragment.getActivity();
        if (activity != null) {
            Intent intent = activity.getIntent();
            if (intent != null && intent.hasExtra("SCROLL_TO_VERSE")) {
                args.putInt("scroll_to_verse", intent.getIntExtra("SCROLL_TO_VERSE", -1));
            }
        }

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        readingStartTime = System.currentTimeMillis();
        if (getArguments() != null) {
            surahModel = getArguments().getParcelable("surah_model");
            startPosition = getArguments().getInt("start_position");
            mode = getArguments().getString("mode");
            layoutType = getArguments().getString("fragment_type");
            currentSurahIndex = getArguments().getInt("current_surah_index");

            // Get initial page if available
            if (getArguments().containsKey("initial_page")) {
                currentPageNumber = getArguments().getInt("initial_page");
            }
            // Get scroll to verse if available
            if (getArguments().containsKey("scroll_to_verse")) {
                scrollToVerse = getArguments().getInt("scroll_to_verse", -1);
            }
        }
        // Get layout type from preferences if not specified
        if (layoutType == null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
            layoutType = prefs.getBoolean("recitation_layout_by_page", false) ?
                    "pageByPage" : "verseByVerse";
        }
        quranApi = ApiService.getQuranClient().create(QuranApi.class);
        bookmarkApi = ApiService.getQuranClient().create(BookmarkApi.class);
        statisticsApi = ApiService.getQuranClient().create(StatisticsApi.class);

//        coroutineScope = LifecycleKt.getLifecycleScope(this);

        // Initialize repository with dependencies
//        recentlyReadRepository = new RecentlyReadRepository(
//                ApiService.getQuranClient().create(BookmarkApi.class),
//                new TokenManager(requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)),
//                Dispatchers.getIO()
//        );
        authToken = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                .getString("token", null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_recitation_page, container, false);
        surahNameTextView = rootView.findViewById(R.id.surahNameTextView);
        surahNameEnglishTextView = rootView.findViewById(R.id.englishSurahNameTextView);
        quranMetadata = QuranMetadata.Companion.getInstance();

        setupUI();
        checkBookmarkStatus();
        return rootView;
    }
    @Override
    public void onPause() {
        super.onPause();
        long durationInSeconds = (System.currentTimeMillis() - readingStartTime) / 1000;

        // Check if reading duration is valid
        if (ReadingTracker.INSTANCE.isValidReadingDuration(durationInSeconds)) {
            try {
                // First, record the primary reading type (chapter or page)
                String primaryItemId;
                RecentlyReadType primaryType;

                if ("verseByVerse".equals(layoutType)) {
                    primaryType = RecentlyReadType.CHAPTER;
                    // Handle potential null surahModel
                    if (surahModel != null) {
                        primaryItemId = surahModel.getSurahNumber();
                    } else if (currentSurahIndex >= 0) {
                        // Use currentSurahIndex + 1 as fallback
                        // (adding 1 because indices are 0-based but Surah numbers are 1-based)
                        primaryItemId = String.valueOf(currentSurahIndex + 1);
                    } else {
                        // Can't determine chapter, skip recording
                        Log.w("RecitationPage", "Cannot determine chapter ID, skipping recording");
                        return;
                    }
                } else if ("pageByPage".equals(layoutType)) {
                    primaryType = RecentlyReadType.PAGE;
                    primaryItemId = String.valueOf(currentPageNumber);
                } else {
                    Log.w("RecitationPage", "Invalid layout type: " + layoutType);
                    return; // Exit if mode is invalid
                }

                // Record primary reading type
                recordRecentlyRead(primaryType, primaryItemId, durationInSeconds);
                recordRecitationTimes(durationInSeconds);

                // Now record the Juz
                int pageNumber;
                if ("pageByPage".equals(layoutType)) {
                    pageNumber = currentPageNumber;
                } else {
                    int surahNumber;
                    if (surahModel != null) {
                        surahNumber = Integer.parseInt(surahModel.getSurahNumber());
                    } else {
                        surahNumber = currentSurahIndex + 1; // Fallback
                    }
                    pageNumber = QuranMetadata.Companion.getInstance().getStartingPage(surahNumber);
                }

                int juzNumber = QuranMetadata.Companion.getInstance().getJuzForPage(pageNumber);
                recordRecentlyRead(RecentlyReadType.JUZ, String.valueOf(juzNumber), durationInSeconds);
            } catch (Exception e) {
                Log.e("RecitationPage", "Error recording recently read: " + e.getMessage());
            }
        }
    }
    private void recordRecentlyRead(RecentlyReadType type, String itemId, long durationSeconds) {
        CoroutinesHelper.addRecentlyRead(
                recentlyReadRepository,
                type,
                itemId,
                durationSeconds,
                () -> {
                    // On Success
                    Log.d("RecitationPage", "Recorded " + type + ": " + itemId);
                    return null;
                },
                error -> {
                    // On Error
                    Log.e("RecitationPage", "Failed to record " + type + ": " + error);
                    return null;
                }
        );
    }
    private void recordRecitationTimes(long durationInSeconds){
        UpdateRecitationTimesRequest timesRequest = new UpdateRecitationTimesRequest((int)durationInSeconds);

        statisticsApi.updateRecitationTimes("Bearer " + authToken, timesRequest)
                .enqueue(new Callback<SimpleResponse>() {
                    @Override
                    public void onResponse(Call<SimpleResponse> call, Response<SimpleResponse> response) {
                        Log.d("RecitationPage", "Recitation times updated");
                    }

                    @Override
                    public void onFailure(Call<SimpleResponse> call, Throwable t) {
                        Log.e("RecitationPage", "Failed to update recitation times: " + t.getMessage());
                    }
                });
    }
    private void setupUI() {
        // Retrieve the user's layout preference
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean isByPage = sharedPreferences.getBoolean(KEY_LAYOUT_TYPE, false);
        layoutType = isByPage ? "pageByPage" : "verseByVerse";
        fetchVerses();
//        displayFragment(layoutType);
//        if (QuranMetadata.Companion.getInstance() == null) {
//            Log.e("RecitationPageFragment", "QuranMetadata is not initialized!");
//        }
//        SurahDetails surahDetails = QuranMetadata.Companion.getInstance().getSurahDetails(currentSurahIndex+1);
//
//        TextView surahNameTextView = rootView.findViewById(R.id.surahNameTextView);
//        TextView surahNameEnglishTextView = rootView.findViewById(R.id.englishSurahNameTextView);
//
//        surahNameTextView.setText(surahDetails.getEnglishName());
//        surahNameEnglishTextView.setText(surahDetails.getTranslationName());

//        previousSurahButton.setOnClickListener(v -> navigateToSurah(currentSurahIndex - 1));
//        nextSurahButton.setOnClickListener(v -> navigateToSurah(currentSurahIndex + 1));

        // Setup bookmark functionality
        bookmarkIcon = rootView.findViewById(R.id.bookmarkIcon);
        bookmarkIcon.setOnClickListener(v -> toggleBookmarkStatus());

        pageBookmarkIcon = rootView.findViewById(R.id.pageBookmarkIcon);
        pageBookmarkIcon.setOnClickListener(v -> togglePageBookmarkStatus());

        // Show/hide page bookmark based on layout type
        pageBookmarkIcon.setVisibility("pageByPage".equals(layoutType) ? View.VISIBLE : View.GONE);
    }

        private void checkBookmarkStatus() {
            if (authToken == null) {
                Log.e("RecitationPageFragment", "No auth token available");
                return;
            }

            quranApi.getBookmarks("Bearer " + authToken).enqueue(new Callback<BookmarksResponse>() {
                @Override
                public void onResponse(Call<BookmarksResponse> call, Response<BookmarksResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        BookmarksResponse bookmarksResponse = response.body();
                        // Check chapter bookmarks
                        List<BookmarkChapter> chapterBookmarks = bookmarksResponse.getBookmarks().getChapters();
                        isChapterBookmarked = false;
                        for (BookmarkChapter chapter : chapterBookmarks) {
                            if (String.valueOf(currentSurahIndex + 1).equals(chapter.getItemProperties().getChapterId())) {
                                isChapterBookmarked = true;
                                break;
                            }
                        }

                        // Check page bookmarks
                        List<BookmarkPage> pageBookmarks = bookmarksResponse.getBookmarks().getPages();
                        isPageBookmarked = false;
                        for (BookmarkPage page : pageBookmarks) {
                            if (page.getItemProperties().getPageNumber() == currentPageNumber) {
                                isPageBookmarked = true;
                                break;
                            }
                        }
                        updateBookmarkIcons();
                    }
                }

                @Override
                public void onFailure(Call<BookmarksResponse> call, Throwable t) {
                    Log.e("RecitationPageFragment", "Failed to check bookmark status", t);
                }
            });
         }

    private void togglePageBookmarkStatus() {
        if (authToken == null) {
            Toast.makeText(getContext(), "Please login to bookmark", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isPageBookmarked) {
            // Remove page bookmark
            quranApi.removeBookmark("Bearer " + authToken, "page", String.valueOf(currentPageNumber))
                    .enqueue(new Callback<RemoveBookmarkResponse>() {
                        @Override
                        public void onResponse(Call<RemoveBookmarkResponse> call, Response<RemoveBookmarkResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                isPageBookmarked = false;
                                updateBookmarkIcons();
                                Toast.makeText(getContext(), "Page bookmark removed", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "Failed to remove page bookmark", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<RemoveBookmarkResponse> call, Throwable t) {
                            Toast.makeText(getContext(), "Error removing page bookmark", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            // Add page bookmark
            Map<String, Object> itemProperties = new HashMap<>();
            itemProperties.put("page_id", String.valueOf(currentPageNumber));
            itemProperties.put("page_number", currentPageNumber);
            Log.e("page number", String.valueOf(currentPageNumber));
            BookmarkRequest request = new BookmarkRequest(
                    "page",
                    itemProperties,
                    ""  // notes
            );
            quranApi.addBookmark("Bearer " + authToken, request)
                    .enqueue(new Callback<BookmarkResponse>() {
                        @Override
                        public void onResponse(Call<BookmarkResponse> call, Response<BookmarkResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                isPageBookmarked = true;
                                updateBookmarkIcons();
                                Toast.makeText(getContext(), "Page bookmark added", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "Failed to add page bookmark", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<BookmarkResponse> call, Throwable t) {
                            Toast.makeText(getContext(), "Error adding page bookmark", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void toggleBookmarkStatus() {

            if (authToken == null) {
                Toast.makeText(getContext(), "Please login to bookmark", Toast.LENGTH_SHORT).show();
                return;
            }
            // Get the correct surah number (add 1 since index starts at 0)
            String surahNumber = String.valueOf(currentSurahIndex + 1);

            if (isChapterBookmarked) {
                // Remove bookmark - use the adjusted surah number
                quranApi.removeBookmark("Bearer " + authToken, "chapter", surahNumber)
                        .enqueue(new Callback<RemoveBookmarkResponse>() {
                            @Override
                            public void onResponse(Call<RemoveBookmarkResponse> call, Response<RemoveBookmarkResponse> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    isChapterBookmarked = false;
                                    updateBookmarkIcons();
                                    Toast.makeText(getContext(), "Bookmark removed", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getContext(), "Failed to remove bookmark", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<RemoveBookmarkResponse> call, Throwable t) {
                                Toast.makeText(getContext(), "Error removing bookmark", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                // Add bookmark - use the adjusted surah number
                // For Chapter Bookmark
                Map<String, Object> itemProperties = new HashMap<>();
                itemProperties.put("chapter_id", String.valueOf(currentSurahIndex + 1));

                BookmarkRequest request = new BookmarkRequest(
                        "chapter",  // type
                        itemProperties,  // itemProperties
                        ""  // notes (empty string as default)
                );

                quranApi.addBookmark("Bearer " + authToken, request)
                        .enqueue(new Callback<BookmarkResponse>() {
                    @Override
                    public void onResponse(Call<BookmarkResponse> call, Response<BookmarkResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                        isChapterBookmarked = true;
                        updateBookmarkIcons();
                        Toast.makeText(getContext(), "Bookmark added", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Failed to add bookmark", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<BookmarkResponse> call, Throwable t) {
                    Toast.makeText(getContext(), "Error adding bookmark", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateBookmarkIcons() {
        if (bookmarkIcon != null) {
            bookmarkIcon.setImageResource(isChapterBookmarked ?
                    R.drawable.ic_bookmarked : R.drawable.ic_bookmark);
        }
        if (pageBookmarkIcon != null) {
            pageBookmarkIcon.setImageResource(isPageBookmarked ?
                    R.drawable.ic_bookmarked : R.drawable.ic_bookmark);
        }
    }
    private void fetchVerses() {
        if (surahModel == null) {
            int surahNumber = currentSurahIndex + 1; // Since index is 0-based
            int scrollToVerse = getArguments() != null ? getArguments().getInt("scroll_to_verse", -1) : -1;

            if ("verseByVerse".equals(layoutType)) {
                displayByAyatRecitationFragment(surahNumber, scrollToVerse);
                updateSurahHeader(surahNumber);
            } else if ("pageByPage".equals(layoutType)) {
                // Use initial_page if specified, otherwise use starting page of surah
                int pageToShow = getArguments() != null && getArguments().containsKey("initial_page")
                        ? getArguments().getInt("initial_page")
                        : QuranMetadata.Companion.getInstance().getStartingPage(surahNumber);

                displayByPageRecitationFragment(pageToShow);  // Passing the page number
                setPageContentCallback(pageToShow);
            }
        } else {
            // Use surahModel if available
            int surahNumber = Integer.parseInt(surahModel.getSurahNumber());
            final int scrollToVerse = getArguments() != null ? getArguments().getInt("scrollToVerse", -1) : -1;

            if ("verseByVerse".equals(layoutType)) {
                displayByAyatRecitationFragment(surahNumber, scrollToVerse);
                updateSurahHeader(surahNumber);
            } else if ("pageByPage".equals(layoutType)) {
                // Use initial_page if specified, otherwise use starting page of surah
                int pageToShow = getArguments() != null && getArguments().containsKey("initial_page")
                        ? getArguments().getInt("initial_page")
                        : QuranMetadata.Companion.getInstance().getStartingPage(surahNumber);

                displayByPageRecitationFragment(pageToShow);  // Passing the page number
                setPageContentCallback(pageToShow);
            }
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    private void setPageContentCallback(int pageNumber) {
        // Create callback for page content
        PageAdapter.PageContentCallback callback = new PageAdapter.PageContentCallback() {
            @Override
            public void onPageContentFetched(SpannableStringBuilder content) {
                // Get first surah in the page from QuranMetadata
                int surahNumber = quranMetadata.getSurahNumberForPage(pageNumber);
                updateSurahHeader(surahNumber);
            }

            @Override
            public void onPageContentFetchFailed(SpannableStringBuilder error) {
                // Handle error
            }
        };

        // Pass callback to ByPageRecitationFragment
        Fragment currentFragment = getChildFragmentManager()
                .findFragmentById(R.id.recitationFragmentContainerView);
        if (currentFragment instanceof ByPageRecitationFragment) {
            ((ByPageRecitationFragment) currentFragment).setPageContentCallback(callback);
        }
    }

    private void updateSurahHeader(int surahNumber) {
        if (surahNumber > 0 && surahNumber <= 114) {
            SurahDetails surahDetails = quranMetadata.getSurahDetails(surahNumber);
            if (surahDetails != null) {
                surahNameTextView.setText(surahDetails.getArabicName());
                surahNameEnglishTextView.setText(surahDetails.getEnglishName() +
                        " (" + surahDetails.getTranslationName() + ")");
            }
        }
    }

    // Update the existing onPageChanged method
    public void onPageChanged(int newPage) {
        currentPageNumber = newPage;
        int surahNumber = quranMetadata.getSurahNumberForPage(newPage);
        updateSurahHeader(surahNumber);
        checkBookmarkStatus(); // Check bookmark status when page changes
    }

    @OptIn(markerClass = UnstableApi.class)
    private void displayByAyatRecitationFragment(int surahNumber, int scrollToVerse) {
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        ByAyatRecitationFragment fragment = ByAyatRecitationFragment.newInstance(surahNumber, scrollToVerse);
        transaction.replace(R.id.recitationFragmentContainerView, fragment);
        transaction.commit();
    }

    // In RecitationPageFragment.java
    @OptIn(markerClass = UnstableApi.class)
    private void displayByPageRecitationFragment(int pageNumber) {
        Log.d("RecitationPageFragment", "Displaying page: " + pageNumber);
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        ByPageRecitationFragment fragment = ByPageRecitationFragment.newInstance(pageNumber);
        transaction.replace(R.id.recitationFragmentContainerView, fragment);
        transaction.commit();
    }
    // Add this method to RecitationPageFragment.java
    public void navigateToSpecificPage(int pageNumber) {
        Log.d("RecitationPageFragment", "Navigating to specific page: " + pageNumber);
        // Update the current page number
        currentPageNumber = pageNumber;

        // Ensure we're using the pageByPage layout
        layoutType = "pageByPage";

        // Display the page
        displayByPageRecitationFragment(pageNumber);

        // Update the page content callback
        setPageContentCallback(pageNumber);

        // Update bookmark status for the new page
        checkBookmarkStatus();
    }
//
//    private void navigateToSurah(int newIndex) {
//        if (newIndex >= 0 && newIndex < 114) {
//            currentSurahIndex = newIndex;
//
//            // Create a new fragment with the updated index
//            FragmentTransaction transaction = requireActivity()
//                    .getSupportFragmentManager()
//                    .beginTransaction();
//
//            RecitationPageFragment newFragment = RecitationPageFragment.newInstance(
//                    null,  // We don't need surahModel for navigation
//                    layoutType,
//                    newIndex
//            );
//
//            transaction.replace(R.id.mainFragmentContainer, newFragment);
//            transaction.addToBackStack(null);
//            transaction.commit();
//        }
//    }

}