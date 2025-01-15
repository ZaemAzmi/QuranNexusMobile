package com.example.qurannexus.features.recitation;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.media3.common.util.UnstableApi;
import androidx.preference.PreferenceManager;

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
import com.example.qurannexus.core.utils.SurahDetails;
import com.example.qurannexus.features.bookmark.models.BookmarkRequest;
import com.example.qurannexus.features.bookmark.models.BookmarkResponse;
import com.example.qurannexus.features.bookmark.models.BookmarksResponse;
import com.example.qurannexus.features.bookmark.models.RemoveBookmarkResponse;
import com.example.qurannexus.features.recitation.models.PageAdapter;
import com.example.qurannexus.features.recitation.models.SurahModel;
import com.example.qurannexus.core.utils.QuranMetadata;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecitationPageFragment extends Fragment {
    SurahModel surahModel;
    String layoutType = "verseByVerse";
    private static final String KEY_LAYOUT_TYPE = "recitation_layout_by_page";
    private int currentSurahIndex;
    private int currentPageNumber;
    private View rootView;
    private QuranApi quranApi;
    private boolean isChapterBookmarked = false;
    private ImageView bookmarkIcon;
    private String authToken;
    private static final String ARG_SCROLL_TO_VERSE = "scrollToVerse";
    private TextView surahNameTextView;
    private TextView surahNameEnglishTextView;
    private QuranMetadata quranMetadata;
    public RecitationPageFragment() {
    }

    public static RecitationPageFragment newInstance(SurahModel surahModel, String fragmentType, int currentSurahIndex) {
        RecitationPageFragment fragment = new RecitationPageFragment();
        Bundle args = new Bundle();
        args.putParcelable("surahModel", surahModel);
        args.putString("fragmentType", fragmentType);
        args.putInt("currentSurahIndex", currentSurahIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            surahModel = getArguments().getParcelable("surahModel");
            layoutType = getArguments().getString("fragmentType");
            currentSurahIndex = getArguments().getInt("currentSurahIndex");
        }

        quranApi = ApiService.getQuranClient().create(QuranApi.class);
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
                        List<String> chapterBookmarks = bookmarksResponse.getBookmarks().getChapters();
                        isChapterBookmarked = chapterBookmarks.contains(String.valueOf(currentSurahIndex + 1));
                        updateBookmarkIcon();
                    }
                }

                @Override
                public void onFailure(Call<BookmarksResponse> call, Throwable t) {
                    Log.e("RecitationPageFragment", "Failed to check bookmark status", t);
                }
            });
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
                                    updateBookmarkIcon();
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
                BookmarkRequest request = new BookmarkRequest(
                        "chapter",
                        surahNumber,  // Using the adjusted surah number
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null

                );

                quranApi.addBookmark("Bearer " + authToken, request)
                        .enqueue(new Callback<BookmarkResponse>() {
                    @Override
                    public void onResponse(Call<BookmarkResponse> call, Response<BookmarkResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                        isChapterBookmarked = true;
                        updateBookmarkIcon();
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

    private void updateBookmarkIcon() {
        if (bookmarkIcon != null) {
            bookmarkIcon.setImageResource(isChapterBookmarked ?
                    R.drawable.ic_bookmarked : R.drawable.ic_bookmark);
        }
    }
    private void fetchVerses() {
        int surahNumber = Integer.parseInt(surahModel.getSurahNumber());
        final int scrollToVerse = getArguments() != null ? getArguments().getInt("scrollToVerse", -1) : -1;

        if ("verseByVerse".equals(layoutType)) {
            displayByAyatRecitationFragment(surahNumber, scrollToVerse);
            updateSurahHeader(surahNumber);
        } else if ("pageByPage".equals(layoutType)) {
            int startingPage = QuranMetadata.Companion.getInstance().getStartingPage(surahNumber);
            displayByPageRecitationFragment(startingPage);
            setPageContentCallback(startingPage);
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

    // Add method to be called from ByPageRecitationFragment when page changes
    public void onPageChanged(int newPage) {
        currentPageNumber = newPage;
        int surahNumber = quranMetadata.getSurahNumberForPage(newPage);
        updateSurahHeader(surahNumber);
    }

    // Add method to be called from ByAyatRecitationFragment when verse changes
    public void onVerseChanged(int surahNumber) {
        updateSurahHeader(surahNumber);
    }

    @OptIn(markerClass = UnstableApi.class)
    private void displayByAyatRecitationFragment(int surahNumber, int scrollToVerse) {
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        ByAyatRecitationFragment fragment = ByAyatRecitationFragment.newInstance(surahNumber, scrollToVerse);
        transaction.replace(R.id.recitationFragmentContainerView, fragment);
        transaction.commit();
    }

    @OptIn(markerClass = UnstableApi.class)
    private void displayByPageRecitationFragment(int surahNumber) {
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        ByPageRecitationFragment fragment = ByPageRecitationFragment.newInstance(surahNumber);
        transaction.replace(R.id.recitationFragmentContainerView, fragment);
        transaction.commit();
    }

    private void navigateToSurah(int newIndex) {
        if (newIndex >= 0 && newIndex <= 114) {
            currentSurahIndex = newIndex;


        }
    }

}