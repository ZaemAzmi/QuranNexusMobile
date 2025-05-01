package com.example.qurannexus.features.recitation;

import static com.example.qurannexus.features.recitation.ByPageRecitationFragment.TOTAL_PAGES;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.cardview.widget.CardView;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.media3.common.util.UnstableApi;
import androidx.preference.PreferenceManager;
import androidx.lifecycle.LifecycleKt;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.example.qurannexus.R;
import com.example.qurannexus.core.activities.MainActivity;
import com.example.qurannexus.core.interfaces.QuranApi;
import com.example.qurannexus.core.network.ApiService;
import com.example.qurannexus.core.utils.CoroutinesHelper;
import com.example.qurannexus.core.utils.IconPopupMenu;
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
    private ImageView bookmarkMenuIcon;
    private CardView bookmarkDropdownMenu;
    private LinearLayout chapterBookmarkLayout;
    private LinearLayout pageBookmarkLayout;
    private boolean isBookmarkMenuOpen = false;
    private SwipeRefreshLayout swipeRefreshLayout;
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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupScrollListeners();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_recitation_page, container, false);
        surahNameTextView = rootView.findViewById(R.id.surahNameTextView);
        surahNameEnglishTextView = rootView.findViewById(R.id.englishSurahNameTextView);
        quranMetadata = QuranMetadata.Companion.getInstance();
        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = rootView.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> refreshCurrentContent());
        setupUI();
        checkBookmarkStatus();
        setupBookmarkMenu();

        UtilityService utilityService = new UtilityService();
        utilityService.setupBottomNavPadding(this, rootView);
        return rootView;
    }
    @Override
    public void onPause() {
        super.onPause();
        if (isBookmarkMenuOpen && bookmarkDropdownMenu != null) {
            bookmarkDropdownMenu.setVisibility(View.GONE);
            isBookmarkMenuOpen = false;
        }
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

    @OptIn(markerClass = UnstableApi.class)
    private void refreshCurrentContent() {
        // Start refresh animation
        swipeRefreshLayout.setRefreshing(true);
        Log.d("RecitationPage", "Refreshing content. Layout type: " + layoutType);

        if ("verseByVerse".equals(layoutType)) {
            // If verse by verse mode
            int surahNumber = currentSurahIndex + 1;
            Log.d("RecitationPage", "Refreshing surah: " + surahNumber);

            // Re-create the fragment with the SAME surah number
            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
            ByAyatRecitationFragment newFragment = ByAyatRecitationFragment.newInstance(surahNumber, -1);
            transaction.replace(R.id.recitationFragmentContainerView, newFragment);
            transaction.commit();

            // Update the header to show the correct surah
            updateSurahHeader(surahNumber);

            // Stop refreshing after a delay to allow fragment to load
            new Handler().postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1000);
        }
        else if ("pageByPage".equals(layoutType)) {
            // If page by page mode - maintain the current page number
            Log.d("RecitationPage", "Refreshing page: " + currentPageNumber);

            // Re-create the fragment with the SAME page number
            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
            ByPageRecitationFragment newFragment = ByPageRecitationFragment.newInstance(currentPageNumber);
            transaction.replace(R.id.recitationFragmentContainerView, newFragment);
            transaction.commit();

            // Stop refreshing after a delay to allow fragment to load
            new Handler().postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1000);
        }
        Log.d("RecitationPage", "Created new fragment and committed transaction");
    }
    @OptIn(markerClass = UnstableApi.class)
    private void setupScrollListeners() {
        // When switching to a fragment, we need to attach scroll listeners
        if ("verseByVerse".equals(layoutType)) {
            Fragment fragment = getChildFragmentManager().findFragmentById(R.id.recitationFragmentContainerView);
            if (fragment instanceof ByAyatRecitationFragment) {
                attachScrollListenerToVerseFragment((ByAyatRecitationFragment) fragment);
            }
        } else if ("pageByPage".equals(layoutType)) {
            Fragment fragment = getChildFragmentManager().findFragmentById(R.id.recitationFragmentContainerView);
            if (fragment instanceof ByPageRecitationFragment) {
                attachScrollListenerToPageFragment((ByPageRecitationFragment) fragment);
            }
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    private void attachScrollListenerToVerseFragment(ByAyatRecitationFragment fragment) {
        // Assuming fragment.versesPager is accessible or has a getter
        if (fragment.versesPager != null) {
            // We need to wait for the ViewPager to fully initialize
            fragment.versesPager.post(() -> {
                // Get the current page fragment
                int currentItem = fragment.versesPager.getCurrentItem();
                // Find the RecyclerView inside the current page fragment
                View pagerChildAt = fragment.versesPager.getChildAt(0);
                if (pagerChildAt instanceof RecyclerView) {
                    RecyclerView recyclerView = (RecyclerView) pagerChildAt;
                    attachScrollListenerToRecyclerView(recyclerView);
                }

                // Also register a callback to handle page changes
                fragment.versesPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        super.onPageSelected(position);
                        // Find the RecyclerView in the newly selected page
                        new Handler().postDelayed(() -> {
                            for (int i = 0; i < fragment.versesPager.getChildCount(); i++) {
                                View child = fragment.versesPager.getChildAt(i);
                                if (child instanceof RecyclerView) {
                                    RecyclerView recyclerView = (RecyclerView) child;
                                    attachScrollListenerToRecyclerView(recyclerView);
                                    break;
                                }
                            }
                        }, 100);
                    }
                });
            });
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    private void attachScrollListenerToPageFragment(ByPageRecitationFragment fragment) {
        // Similar logic for the page fragment's ViewPager
        if (fragment.viewPager != null) {
            fragment.viewPager.post(() -> {
                // Find the TextView or ScrollView in the ViewPager
                View contentView = fragment.viewPager.getChildAt(0);
                if (contentView != null) {
                    // If it's a ScrollView or NestedScrollView, attach listener
                    if (contentView instanceof NestedScrollView) {
                        NestedScrollView scrollView = (NestedScrollView) contentView;
                        attachScrollListenerToScrollView(scrollView);
                    }
                }
            });
        }
    }

    private void attachScrollListenerToRecyclerView(RecyclerView recyclerView) {
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

    private void attachScrollListenerToScrollView(NestedScrollView scrollView) {
        scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener)
                (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    int dy = scrollY - oldScrollY;
                    if (dy > 10) { // Scrolling down
                        hideBottomNavigation();
                    } else if (dy < -10) { // Scrolling up
                        showBottomNavigation();
                    }
                }
        );
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
        // Always show both bookmark options since we have pagination in both modes now
        if (pageBookmarkLayout != null) {
            pageBookmarkLayout.setVisibility(View.VISIBLE);
        }
    }

    // New method for bookmark menu functionality
    private void setupBookmarkMenu() {
        // Initialize the menu icon
        bookmarkMenuIcon = rootView.findViewById(R.id.bookmarkMenuIcon);

        // Set click listener to show popup menu
        bookmarkMenuIcon.setOnClickListener(v -> showBookmarkMenu(v));

        // We still need references to these variables for bookmark status updates
        bookmarkIcon = null; // We'll set this dynamically in the menu
        pageBookmarkIcon = null; // We'll set this dynamically in the menu
    }

// Replace your showBookmarkMenu method with this one

    private void showBookmarkMenu(View anchor) {
        IconPopupMenu popup = new IconPopupMenu(requireContext(), anchor);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.bookmark_menu, popup.getMenu());

        // Try to show icons - this works on most devices
        popup.showIcons();

        // Update menu items based on bookmark status
        MenuItem chapterItem = popup.getMenu().findItem(R.id.bookmark_chapter);
        MenuItem pageItem = popup.getMenu().findItem(R.id.bookmark_page);

        if (isChapterBookmarked) {
            chapterItem.setTitle("Remove Chapter Bookmark");
            popup.updateMenuIcon(R.id.bookmark_chapter, R.drawable.ic_bookmarked);
        } else {
            chapterItem.setTitle("Bookmark Chapter");
            popup.updateMenuIcon(R.id.bookmark_chapter, R.drawable.ic_bookmark);
        }

        if (isPageBookmarked) {
            pageItem.setTitle("Remove Page Bookmark");
            popup.updateMenuIcon(R.id.bookmark_page, R.drawable.ic_bookmarked);
        } else {
            pageItem.setTitle("Bookmark Page");
            popup.updateMenuIcon(R.id.bookmark_page, R.drawable.ic_bookmark);
        }

        // Show page bookmark option in all modes since we now have pagination in both
        pageItem.setVisible(true);

        // Set click listener for menu items
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.bookmark_chapter) {
                toggleBookmarkStatus();
                return true;
            } else if (itemId == R.id.bookmark_page) {
                togglePageBookmarkStatus();
                return true;
            }
            return false;
        });

        // Show the popup menu
        popup.show();
    }
    private void toggleBookmarkMenu() {
        if (isBookmarkMenuOpen) {
            // Close the menu with animation
            Animation fadeOut = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_out);
            fadeOut.setDuration(200);
            bookmarkDropdownMenu.startAnimation(fadeOut);
            bookmarkDropdownMenu.setVisibility(View.GONE);
            isBookmarkMenuOpen = false;
        } else {
            // Open the menu with animation
            bookmarkDropdownMenu.setVisibility(View.VISIBLE);
            Animation fadeIn = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in);
            fadeIn.setDuration(200);
            bookmarkDropdownMenu.startAnimation(fadeIn);
            isBookmarkMenuOpen = true;
        }
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
        // Update the menu icon - show filled bookmark if anything is bookmarked
        if (bookmarkMenuIcon != null) {
            boolean anyBookmarkActive = isChapterBookmarked || isPageBookmarked;
            if (anyBookmarkActive) {
                // If something is bookmarked, change the menu icon to show this
                // You could optionally use a different icon to indicate active bookmarks
                bookmarkMenuIcon.setImageResource(R.drawable.ic_vertical_dots_menu);
            } else {
                bookmarkMenuIcon.setImageResource(R.drawable.ic_vertical_dots_menu);
            }
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
    @OptIn(markerClass = UnstableApi.class)
    public void onPageChanged(int newPage) {
        currentPageNumber = newPage;
        int surahNumber = quranMetadata.getSurahNumberForPage(newPage);
        updateSurahHeader(surahNumber);
        checkBookmarkStatus(); // Check bookmark status when page changes

        if ("verseByVerse".equals(layoutType)) {
            Fragment fragment = getChildFragmentManager().findFragmentById(R.id.recitationFragmentContainerView);
            if (fragment instanceof ByAyatRecitationFragment) {
                ByAyatRecitationFragment ayatFragment = (ByAyatRecitationFragment) fragment;
                // The ByAyatRecitationFragment should handle showing the right page of verses
            }
        }
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