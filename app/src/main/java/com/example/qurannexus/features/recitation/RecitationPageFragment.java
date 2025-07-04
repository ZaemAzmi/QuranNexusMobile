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
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.FlowLiveDataConversions;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.UnstableApi;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.lifecycle.LifecycleKt;

import com.example.qurannexus.R;
import com.example.qurannexus.core.activities.MainActivity;
import com.example.qurannexus.core.database.entities.QuranAyahDetailEntity;
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
import com.example.qurannexus.features.recitation.viewModels.PageDataState;
import com.example.qurannexus.features.recitation.viewModels.RecitationViewModel;
import com.example.qurannexus.features.statistics.interfaces.StatisticsApi;
import com.example.qurannexus.features.statistics.models.UpdateRecitationTimesRequest;
import com.google.gson.Gson;

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
    private ProgressBar mainLoadingIndicator;
    // New variables for handling navigation intent parameters
    private boolean isNavigatingWithExtras = false; // Flag to indicate if launched with specific nav extras
    private boolean navIsByPage = false;
    private String navChapterId = null; // 1-based
    private String navVerseNumber = null; // 1-based
    private int navTargetPageNumber = -1;
    private int navScrollToVerseOnPage = -1;
    private int navCurrentSurahIndex = -1; // 0-based, for verse mode

    private String fragmentTypeFromNav = null; // To store "pageByPage" or "verseByVerse" if coming from nav
    // NEW: ViewModel
    private RecitationViewModel viewModel;
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
        Log.d("RPF_onCreate", "onCreate called.");
        // Obtain the ViewModel
        Bundle args = getArguments();
        if (args != null) {
            isNavigatingWithExtras = args.getBoolean("IS_NAVIGATING_WITH_EXTRAS", false);
            Log.d("RPF_onCreate", "isNavigatingWithExtras: " + isNavigatingWithExtras);

            if (isNavigatingWithExtras) {
                navIsByPage = args.getBoolean("NAV_IS_BY_PAGE", false);
                navChapterId = args.getString("NAV_CHAPTER_ID");
                navVerseNumber = args.getString("NAV_VERSE_NUMBER");
                fragmentTypeFromNav = args.getString("FRAGMENT_TYPE_FROM_NAV");

                layoutType = fragmentTypeFromNav; // Set layoutType based on navigation preference

                Log.d("RPF_onCreate", "NAV - isByPage: " + navIsByPage + ", layoutType: " + layoutType);
                Log.d("RPF_onCreate", "NAV - chapterId: " + navChapterId + ", verseNumber: " + navVerseNumber);


                if (navIsByPage) {
                    navTargetPageNumber = args.getInt("NAV_TARGET_PAGE_NUMBER", -1);
                    navScrollToVerseOnPage = args.getInt("NAV_SCROLL_TO_VERSE_ON_PAGE", -1);
                    currentPageNumber = (navTargetPageNumber != -1) ? navTargetPageNumber : 1; // Use target page
                    // For page mode, currentSurahIndex is less critical initially, header updates later
                    Log.d("RPF_onCreate", "NAV_PAGE_MODE - targetPage: " + navTargetPageNumber + ", scrollOnPage: " + navScrollToVerseOnPage);

                } else { // Verse mode navigation from WordDetailsActivity
                    navCurrentSurahIndex = args.getInt("NAV_CURRENT_SURAH_INDEX", -1); // 0-based
                    if (navCurrentSurahIndex != -1) {
                        currentSurahIndex = navCurrentSurahIndex;
                    } else if (navChapterId != null) { // Fallback if NAV_CURRENT_SURAH_INDEX wasn't passed correctly
                        try {
                            currentSurahIndex = Integer.parseInt(navChapterId) -1;
                        } catch (NumberFormatException e) {
                            currentSurahIndex = 0; // Absolute fallback
                        }
                    }
                    // scrollToVerse will be taken from NAV_VERSE_NUMBER for ByAyatRecitationFragment
                    if (navVerseNumber != null) {
                        try {
                            scrollToVerse = Integer.parseInt(navVerseNumber);
                        } catch (NumberFormatException e) {
                            scrollToVerse = -1;
                        }
                    }
                    Log.d("RPF_onCreate", "NAV_VERSE_MODE - currentSurahIndex (0-based): " + currentSurahIndex + ", scrollToVerse: " + scrollToVerse);
                }
                surahModel = null; // Don't use surahModel if navigating with specific extras

            } else { // Existing argument handling (e.g., from SurahListFragment)
                Log.d("RPF_onCreate", "Not navigating with specific extras, using old arg parsing.");
                surahModel = args.getParcelable("surah_model");
                startPosition = args.getInt("start_position", 0); // Provide default
                mode = args.getString("mode");
                layoutType = args.getString("fragment_type");
                currentSurahIndex = args.getInt("current_surah_index", -1); // Default to -1 if not present

                if (args.containsKey("initial_page")) {
                    currentPageNumber = args.getInt("initial_page");
                }
                if (args.containsKey("scroll_to_verse")) { // This is the generic scroll_to_verse
                    scrollToVerse = args.getInt("scroll_to_verse", -1);
                }
            }
        }

        // Default layoutType if not set by any means
        if (layoutType == null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
            layoutType = prefs.getBoolean(KEY_LAYOUT_TYPE, false) ? "pageByPage" : "verseByVerse";
            Log.d("RPF_onCreate", "LayoutType not set, defaulting from prefs: " + layoutType);
        }

        quranApi = ApiService.getQuranClient().create(QuranApi.class);
        bookmarkApi = ApiService.getQuranClient().create(BookmarkApi.class);
        statisticsApi = ApiService.getQuranClient().create(StatisticsApi.class);
        authToken = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                .getString("token", null);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupScrollListeners();
    }
    public static RecitationPageFragment newInstanceForNavigation(
            boolean isByPage,
            String chapterId, // 1-based
            String verseNumber, // 1-based
            Integer targetPageNumber, // Nullable, only for isByPage = true
            Integer scrollToVerseOnPage, // Nullable, only for isByPage = true
            Integer currentSurahIndexForVerseMode // Nullable, 0-based, only for isByPage = false
    ) {
        RecitationPageFragment fragment = new RecitationPageFragment();
        Bundle args = new Bundle();
        Log.d("RPF_newInstanceNav", "isByPage: " + isByPage + ", chap: " + chapterId + ", verse: " + verseNumber + ", targetPage: " + targetPageNumber + ", scrollOnPage: " + scrollToVerseOnPage + ", surahIdxVerseMode: " + currentSurahIndexForVerseMode);

        args.putBoolean("IS_NAVIGATING_WITH_EXTRAS", true); // Special flag
        args.putBoolean("NAV_IS_BY_PAGE", isByPage);
        args.putString("NAV_CHAPTER_ID", chapterId);
        args.putString("NAV_VERSE_NUMBER", verseNumber);
        args.putString("FRAGMENT_TYPE_FROM_NAV", isByPage ? "pageByPage" : "verseByVerse");


        if (isByPage) {
            if (targetPageNumber != null) {
                args.putInt("NAV_TARGET_PAGE_NUMBER", targetPageNumber);
            }
            if (scrollToVerseOnPage != null) {
                args.putInt("NAV_SCROLL_TO_VERSE_ON_PAGE", scrollToVerseOnPage);
            }
        } else { // VerseByVerse navigation
            if (currentSurahIndexForVerseMode != null) {
                args.putInt("NAV_CURRENT_SURAH_INDEX", currentSurahIndexForVerseMode); // 0-based
            }
            // NAV_VERSE_NUMBER (String) will be used by ByAyatRecitationFragment as its scrollToVerse (int)
            if (verseNumber != null) {
                args.putString("SCROLL_TO_VERSE", verseNumber); // For ByAyatRecitationFragment
            }
        }
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_recitation_page, container, false);
        viewModel = new ViewModelProvider(this).get(RecitationViewModel.class);
        mainLoadingIndicator = rootView.findViewById(R.id.mainLoadingIndicator);
        surahNameTextView = rootView.findViewById(R.id.surahNameTextView);
        surahNameEnglishTextView = rootView.findViewById(R.id.englishSurahNameTextView);
        quranMetadata = QuranMetadata.Companion.getInstance();
//        swipeRefreshLayout = rootView.findViewById(R.id.swipeRefreshLayout);
//        swipeRefreshLayout.setOnRefreshListener(this::refreshCurrentContent);
        observeViewModel();
        fetchDataForCurrentState();
        setupHeaderPadding();
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
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(true);
        }

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
    private void observeViewModel() {
        // This LiveData conversion is fine
        LiveData<PageDataState> pageDataLiveData = FlowLiveDataConversions.asLiveData(viewModel.getPageData());

        pageDataLiveData.observe(getViewLifecycleOwner(), state -> {
//            swipeRefreshLayout.setRefreshing(false);

            if (state instanceof PageDataState.Success) {
                mainLoadingIndicator.setVisibility(View.GONE);
                rootView.findViewById(R.id.recitationFragmentContainerView).setVisibility(View.VISIBLE);

                PageDataState.Success successState = (PageDataState.Success) state;
                if (successState.getAyahs().isEmpty()) {
                    Toast.makeText(getContext(), "No data for this page.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Update the header. This is a primary responsibility.
//                updateSurahHeader(successState.getAyahs().get(0).getSurahId());

                // The ONLY time this fragment should create a child is on the very first load
                // or after a configuration change (like switching layout type).
                Fragment currentChild = getChildFragmentManager().findFragmentById(R.id.recitationFragmentContainerView);
                if (currentChild == null) {
                    displayContent(successState.getAyahs());
                }
                // *** IMPORTANT: We REMOVE the logic that tries to update the child from here. ***
                // The child will now be responsible for updating itself by observing this same ViewModel.

            } else if (state instanceof PageDataState.Error) {
                mainLoadingIndicator.setVisibility(View.GONE);
                rootView.findViewById(R.id.recitationFragmentContainerView).setVisibility(View.VISIBLE);
                // ... (error handling)
            } else if (state instanceof PageDataState.Loading) {
                mainLoadingIndicator.setVisibility(View.VISIBLE);
                rootView.findViewById(R.id.recitationFragmentContainerView).setVisibility(View.INVISIBLE);
            }
        });
    }

    // NEW: Replaces the old fetchVerses() logic
    private void fetchDataForCurrentState() {
        // This logic determines which page to load initially.
        int pageToLoad = 1;

        if (isNavigatingWithExtras && navIsByPage) {
            pageToLoad = (navTargetPageNumber != -1) ? navTargetPageNumber : 1;
        } else if (surahModel != null) {
            pageToLoad = QuranMetadata.Companion.getInstance().getStartingPage(Integer.parseInt(surahModel.getSurahNumber()));
        } else {
            pageToLoad = (currentPageNumber > 1) ? currentPageNumber :
                    (currentSurahIndex != -1 ? QuranMetadata.Companion.getInstance().getStartingPage(currentSurahIndex + 1) : 1);
        }
        currentPageNumber = pageToLoad;
        viewModel.loadPageData(currentPageNumber);
    }

    // NEW: Called when data is successfully loaded from ViewModel
    private void displayContent(List<QuranAyahDetailEntity> ayahs) {
        if (ayahs.isEmpty() || !isAdded()) return;

        // Update header from the first ayah's data
        updateSurahHeader(ayahs.get(0).getSurahId());

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        boolean isByPage = sharedPreferences.getBoolean(KEY_LAYOUT_TYPE, false);
        layoutType = isByPage ? "pageByPage" : "verseByVerse";

        // Serialize the list to pass via bundle
        String ayahsJson = new Gson().toJson(ayahs);

        if ("pageByPage".equals(layoutType)) {
            int verseToScrollOnPage = isNavigatingWithExtras ? navScrollToVerseOnPage : -1;
            String chapterToHighlight = isNavigatingWithExtras ? navChapterId : null;
            displayByPageRecitationFragment(currentPageNumber, ayahsJson, verseToScrollOnPage, chapterToHighlight);
        } else {
            displayByAyatRecitationFragment(currentPageNumber, ayahsJson);
        }
    }
    public void onPageChanged(int newPage) {
        if (currentPageNumber != newPage) {
            currentPageNumber = newPage;

            // 1. Tell the ViewModel to load data for the new page.
            // If the data is already in memory, the ViewModel might not emit a new state,
            // which is fine.
            viewModel.loadPageData(newPage);

            // 2. Proactively update the header using our metadata utility.
            // This ensures the header updates instantly on swipe, even before the
            // ViewModel responds.
            int surahForNewPage = quranMetadata.getSurahNumberForPage(newPage);
            updateSurahHeader(surahForNewPage);

            // 3. Check bookmark status for the new page
            checkBookmarkStatus();
        }
    }
//    @OptIn(markerClass = UnstableApi.class)
//    private void refreshCurrentContent() {
//        // Start refresh animation
//        swipeRefreshLayout.setRefreshing(true);
//        Log.d("RecitationPage", "Refreshing content. Layout type: " + layoutType);
//
//        if ("verseByVerse".equals(layoutType)) {
//            // If verse by verse mode
//            int surahNumber = currentSurahIndex + 1;
//            Log.d("RecitationPage", "Refreshing surah: " + surahNumber);
//
//            // Re-create the fragment with the SAME surah number
//            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
////            ByAyatRecitationFragment newFragment = ByAyatRecitationFragment.newInstance(surahNumber, -1);
////            transaction.replace(R.id.recitationFragmentContainerView, newFragment);
////            transaction.commit();
//
//            // Update the header to show the correct surah
//            updateSurahHeader(surahNumber);
//
//            // Stop refreshing after a delay to allow fragment to load
//            new Handler().postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1000);
//        }
//        else if ("pageByPage".equals(layoutType)) {
//            // If page by page mode - maintain the current page number
//            Log.d("RecitationPage", "Refreshing page: " + currentPageNumber);
//
//            // Re-create the fragment with the SAME page number
//            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
//            ByPageRecitationFragment newFragment = ByPageRecitationFragment.newInstance(currentPageNumber);
//            transaction.replace(R.id.recitationFragmentContainerView, newFragment);
//            transaction.commit();
//
//            // Stop refreshing after a delay to allow fragment to load
//            new Handler().postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1000);
//        }
//        Log.d("RecitationPage", "Created new fragment and committed transaction");
//    }
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
    private void setupHeaderPadding() {
        final View headerLayout = rootView.findViewById(R.id.headerLayout);

        headerLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (getActivity() instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) getActivity();
                    ImageView sideMenuButton = mainActivity.getSideMenuButton();

                    // Also get a reference to the bookmark icon
                    ImageView bookmarkMenuIcon = headerLayout.findViewById(R.id.bookmarkMenuIcon);

                    Log.d("RecitationPageFragment", "OnGlobalLayout fired. Side menu button: " + sideMenuButton);

                    // Ensure both buttons have been measured
                    if (sideMenuButton != null && sideMenuButton.getWidth() > 0 && bookmarkMenuIcon != null && bookmarkMenuIcon.getWidth() > 0) {

                        // --- LEFT SIDE CALCULATION ---
                        // Get the width of the button and its left margin from MainActivity's layout
                        int sideMenuButtonWidth = sideMenuButton.getWidth();
                        int sideMenuButtonMargin = ((ViewGroup.MarginLayoutParams) sideMenuButton.getLayoutParams()).leftMargin;
                        int totalLeftPadding = sideMenuButtonWidth + sideMenuButtonMargin;

                        // --- RIGHT SIDE CALCULATION (THE NEW PART) ---
                        // Get the width of the bookmark icon and its margin within the header
                        int bookmarkIconWidth = bookmarkMenuIcon.getWidth();
                        // The padding of the parent is effectively the margin for the icon
                        int totalRightPadding =  headerLayout.getPaddingRight();


                        // Get the original top and bottom padding to preserve it.
                        int originalTop = headerLayout.getPaddingTop();
                        int originalBottom = headerLayout.getPaddingBottom();

                        // Apply the new SYMMETRIC padding.
                        headerLayout.setPadding(totalLeftPadding, originalTop, totalRightPadding, originalBottom);

                        // IMPORTANT: Remove the listener to prevent it from running again.
                        headerLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                }
            }
        });
    }

    private void setupBookmarkMenu() {
        // Initialize the menu icon
        bookmarkMenuIcon = rootView.findViewById(R.id.bookmarkMenuIcon);

        // Set click listener to show popup menu
        bookmarkMenuIcon.setOnClickListener(this::showBookmarkMenu);

        // We still need references to these variables for bookmark status updates
        bookmarkIcon = null; // We'll set this dynamically in the menu
        pageBookmarkIcon = null; // We'll set this dynamically in the menu
    }

// Replace your showBookmarkMenu method with this one

    private void showBookmarkMenu(View anchor) {

        Context wrapper = new ContextThemeWrapper(requireContext(), R.style.Theme_App_PopupMenu_Dark);

        // The rest of your code remains exactly the same!
        IconPopupMenu popup = new IconPopupMenu(wrapper, anchor);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.bookmark_menu, popup.getMenu());
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
//    @OptIn(markerClass = UnstableApi.class)
//    private void fetchVerses() {
//        Log.d("RPF_fetchVerses", "LayoutType: " + layoutType + ", isNavigatingWithExtras: " + isNavigatingWithExtras);
//
//        if ("pageByPage".equals(layoutType)) {
//            int pageToDisplay;
//            int verseToScrollOnPage = -1; // Default, no scroll
//            String chapterToHighlight = null;
//
//            if (isNavigatingWithExtras && navIsByPage) {
//                pageToDisplay = (navTargetPageNumber != -1) ? navTargetPageNumber : 1;
//                verseToScrollOnPage = navScrollToVerseOnPage; // Can be -1
//                chapterToHighlight = navChapterId; // For ByPage to know which chapter's verse to highlight
//                Log.d("RPF_fetchVerses", "PAGE_MODE (Nav): page=" + pageToDisplay + ", scrollVerse=" + verseToScrollOnPage + ", chapHighlight=" + chapterToHighlight);
//            } else if (surahModel != null) { // Coming from SurahList, user prefers page mode
//                pageToDisplay = QuranMetadata.Companion.getInstance().getStartingPage(Integer.parseInt(surahModel.getSurahNumber()));
//                if (getArguments() != null && getArguments().containsKey("SCROLL_TO_VERSE")) {
//                    verseToScrollOnPage = getArguments().getInt("SCROLL_TO_VERSE", -1);
//                    chapterToHighlight = surahModel.getSurahNumber();
//                }
//                Log.d("RPF_fetchVerses", "PAGE_MODE (SurahModel): page=" + pageToDisplay);
//            } else { // Fallback or direct page mode selection
//                // Use currentPageNumber if it was set (e.g., by swiping in ByPageRecitationFragment)
//                // or derive from currentSurahIndex if that's all we have
//                pageToDisplay = (currentPageNumber > 1) ? currentPageNumber :
//                        (currentSurahIndex != -1 ? QuranMetadata.Companion.getInstance().getStartingPage(currentSurahIndex + 1) : 1);
//                if (getArguments() != null && getArguments().containsKey("SCROLL_TO_VERSE")) {
//                    verseToScrollOnPage = getArguments().getInt("SCROLL_TO_VERSE", -1);
//                    if(currentSurahIndex != -1) chapterToHighlight = String.valueOf(currentSurahIndex + 1);
//                }
//                Log.d("RPF_fetchVerses", "PAGE_MODE (Fallback): page=" + pageToDisplay);
//            }
//            currentPageNumber = pageToDisplay; // IMPORTANT: Update the fragment's currentPageNumber
//            displayByPageRecitationFragment(pageToDisplay, verseToScrollOnPage, chapterToHighlight);
//            setPageContentCallback(pageToDisplay); // To update header
//
//        } else if ("verseByVerse".equals(layoutType)) {
//            int surahNumberToLoad;
//            int verseToScrollInSurah = -1; // Default to -1 (no specific scroll)
//
//            if (isNavigatingWithExtras && !navIsByPage) {
//                // Use navChapterId (1-based) and navVerseNumber (1-based)
//                surahNumberToLoad = (navChapterId != null) ? Integer.parseInt(navChapterId) : (currentSurahIndex != -1 ? currentSurahIndex + 1 : 1);
//                verseToScrollInSurah = (navVerseNumber != null) ? Integer.parseInt(navVerseNumber) : -1;
//                Log.d("RPF_fetchVerses", "VERSE_MODE (Nav): surah=" + surahNumberToLoad + ", verseScroll=" + verseToScrollInSurah);
//            } else if (surahModel != null) { // Coming from SurahList, user prefers verse mode
//                surahNumberToLoad = Integer.parseInt(surahModel.getSurahNumber());
//                verseToScrollInSurah = scrollToVerse; // Use the general scrollToVerse from args
//                Log.d("RPF_fetchVerses", "VERSE_MODE (SurahModel): surah=" + surahNumberToLoad + ", verseScroll=" + verseToScrollInSurah);
//            } else { // Fallback or direct verse mode selection
//                surahNumberToLoad = (currentSurahIndex != -1) ? currentSurahIndex + 1 : 1; // Ensure valid surah number
//                verseToScrollInSurah = scrollToVerse;
//                Log.d("RPF_fetchVerses", "VERSE_MODE (Fallback): surah=" + surahNumberToLoad + ", verseScroll=" + verseToScrollInSurah);
//            }
//            currentSurahIndex = surahNumberToLoad - 1; // Keep currentSurahIndex (0-based) consistent
//            displayByAyatRecitationFragment(surahNumberToLoad, verseToScrollInSurah);
//            updateSurahHeader(surahNumberToLoad);
//        } else {
//            Log.e("RPF_fetchVerses", "Unknown layoutType: " + layoutType + ". Defaulting to Surah 1, verseByVerse.");
//            displayByAyatRecitationFragment(1, -1);
//            updateSurahHeader(1);
//        }
//    }

//    @OptIn(markerClass = UnstableApi.class)
//    private void setPageContentCallback(int pageNumber) {
//        // Create callback for page content
//        PageAdapter.PageContentCallback callback = new PageAdapter.PageContentCallback() {
//            @Override
//            public void onPageContentFetched(SpannableStringBuilder content) {
//                // Get first surah in the page from QuranMetadata
//                int surahNumber = quranMetadata.getSurahNumberForPage(pageNumber);
//                updateSurahHeader(surahNumber);
//            }
//
//            @Override
//            public void onPageContentFetchFailed(SpannableStringBuilder error) {
//                // Handle error
//            }
//        };
//
//        // Pass callback to ByPageRecitationFragment
//        Fragment currentFragment = getChildFragmentManager()
//                .findFragmentById(R.id.recitationFragmentContainerView);
//        if (currentFragment instanceof ByPageRecitationFragment) {
//            ((ByPageRecitationFragment) currentFragment).setPageContentCallback(callback);
//        }
//    }

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

//    @OptIn(markerClass = UnstableApi.class)
//    public void onPageChanged(int newPage) {
//        currentPageNumber = newPage;
//        int surahNumber = quranMetadata.getSurahNumberForPage(newPage);
//        updateSurahHeader(surahNumber);
//        checkBookmarkStatus(); // Check bookmark status when page changes
//
//        if ("verseByVerse".equals(layoutType)) {
//            Fragment fragment = getChildFragmentManager().findFragmentById(R.id.recitationFragmentContainerView);
//            if (fragment instanceof ByAyatRecitationFragment) {
//                ByAyatRecitationFragment ayatFragment = (ByAyatRecitationFragment) fragment;
//                // The ByAyatRecitationFragment should handle showing the right page of verses
//            }
//        }
//    }

//    @OptIn(markerClass = UnstableApi.class)
//    private void displayByAyatRecitationFragment(int surahNumber, int scrollToVerse) {
//        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
//        ByAyatRecitationFragment fragment = ByAyatRecitationFragment.newInstance(surahNumber, scrollToVerse);
//        transaction.replace(R.id.recitationFragmentContainerView, fragment);
//        transaction.commit();
//    }
//
//    // In RecitationPageFragment.java
//    @OptIn(markerClass = UnstableApi.class)
//    private void displayByPageRecitationFragment(int pageNumber, int scrollToVerseOnPage, String highlightChapterId) {
//        Log.d("RPF_displayByPage", "Page: " + pageNumber + ", ScrollToVerse: " + scrollToVerseOnPage + ", HighlightChap: " + highlightChapterId);
//        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
//        // Ensure ByPageRecitationFragment.newInstance is updated to accept these
//        ByPageRecitationFragment fragment = ByPageRecitationFragment.newInstance(pageNumber, scrollToVerseOnPage, highlightChapterId);
//        transaction.replace(R.id.recitationFragmentContainerView, fragment);
//        transaction.commit();
//    }
    // MODIFIED: Child fragment instantiation methods
    @OptIn(markerClass = UnstableApi.class)
    private void displayByAyatRecitationFragment(int pageNumber, String ayahsJson) {
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        ByAyatRecitationFragment fragment = ByAyatRecitationFragment.newInstance(pageNumber);
        transaction.replace(R.id.recitationFragmentContainerView, fragment);
        transaction.commit();
    }

    @OptIn(markerClass = UnstableApi.class)
    private void displayByPageRecitationFragment(int pageNumber, String ayahsJson, int scrollToVerse, String highlightChapter) {
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        ByPageRecitationFragment fragment = ByPageRecitationFragment.newInstance(pageNumber, ayahsJson, scrollToVerse, highlightChapter);
        transaction.replace(R.id.recitationFragmentContainerView, fragment);
        transaction.commit();
    }
//    @OptIn(markerClass = UnstableApi.class)
//    private void displayByPageRecitationFragment(int pageNumber) {
//        displayByPageRecitationFragment(pageNumber, -1, null);
//    }

}