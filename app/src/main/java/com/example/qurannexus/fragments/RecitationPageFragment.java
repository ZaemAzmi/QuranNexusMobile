package com.example.qurannexus.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.qurannexus.R;
import com.example.qurannexus.models.SurahModel;
import com.example.qurannexus.models.SurahNameModel;
import com.example.qurannexus.services.DatabaseService;

public class RecitationPageFragment extends Fragment {

    private final String APP_ID = "application-0-plbqdoy";
    SurahModel surahModel;
    String layoutType = "verseByVerse";
    DatabaseService databaseService;
    private static final String KEY_LAYOUT_TYPE = "recitation_layout_by_page";
    private int currentSurahIndex;
    private View rootView;
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
        databaseService = new DatabaseService(APP_ID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_recitation_page, container, false);
        setupUI();
        return rootView;
    }

    private void setupUI() {
        // Retrieve the user's layout preference
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean isByPage = sharedPreferences.getBoolean(KEY_LAYOUT_TYPE, false);
        layoutType = isByPage ? "pageByPage" : "verseByVerse";
        displayFragment(layoutType);

        // Setup navigation buttons
        ImageButton previousSurahButton = rootView.findViewById(R.id.previousSurahButton);
        ImageButton nextSurahButton = rootView.findViewById(R.id.nextSurahButton);

        previousSurahButton.setOnClickListener(v -> navigateToSurah(currentSurahIndex - 1));
        nextSurahButton.setOnClickListener(v -> navigateToSurah(currentSurahIndex + 1));

        // Setup bookmark functionality
        ImageView bookmarkIcon = rootView.findViewById(R.id.bookmarkIcon);
        bookmarkIcon.setOnClickListener(v -> toggleBookmarkStatus());
    }

    private void displayFragment(String fragmentType) {
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();

        if ("verseByVerse".equals(fragmentType)) {
            Log.d("RecitationPageFragment", "Displaying2 fragment: " + fragmentType);
            ByAyatRecitationFragment fragment = ByAyatRecitationFragment.newInstance(surahModel);
            transaction.replace(R.id.recitationFragmentContainerView, fragment);
        } else if ("pageByPage".equals(fragmentType)) {
            Log.d("RecitationPageFragment", "Displaying1 fragment: " + fragmentType);
            ByPageRecitationFragment fragment = ByPageRecitationFragment.newInstance(surahModel);
            transaction.replace(R.id.recitationFragmentContainerView, fragment);
        }
        transaction.commit();
    }

    private void toggleBookmarkStatus() {
        surahModel.setBookmarked(!surahModel.isBookmarked());
        updateBookmarkStatusInDatabase(surahModel.getSurahNumber(), surahModel.isBookmarked());
    }

    private void updateBookmarkStatusInDatabase(String surahNumber, boolean isBookmarked) {
        databaseService.updateBookmarkStatus(surahNumber, isBookmarked, new DatabaseService.UpdateCallback() {
            @Override
            public void onUpdateComplete(boolean success) {
                if (success) {
                    Toast.makeText(getContext(), "Bookmark updated successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Failed to update bookmark", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void navigateToSurah(int newIndex) {
        if (newIndex >= 0 && newIndex <= 114) {
            currentSurahIndex = newIndex;
            TextView surahNameTextView = rootView.findViewById(R.id.surahNameTextView);
            TextView surahNameEnglishTextView = rootView.findViewById(R.id.englishSurahNameTextView);
            databaseService.getSurahModelByIndex(currentSurahIndex, new DatabaseService.SurahModelCallback() {
                @Override
                public void onSurahModelLoaded(SurahNameModel surahModel) {
                    if (surahModel != null) {
                        surahNameTextView.setText(surahModel.getSurahName());
                        surahNameEnglishTextView.setText(surahModel.getEnglishName());

                        Fragment currentFragment = getChildFragmentManager().findFragmentById(R.id.recitationFragmentContainerView);
                        if (currentFragment instanceof ByAyatRecitationFragment) {
                            ((ByAyatRecitationFragment) currentFragment).updateSurahContent(surahModel);
                        } else if (currentFragment instanceof ByPageRecitationFragment) {
                            ((ByPageRecitationFragment) currentFragment).updateSurahContent(surahModel);
                        }
                    }
                }
            });
        }
    }
}