package com.example.qurannexus.activities;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import com.example.qurannexus.R;
import com.example.qurannexus.fragments.ByAyatRecitationFragment;
import com.example.qurannexus.fragments.ByPageRecitationFragment;
import com.example.qurannexus.models.SurahModel;
import com.example.qurannexus.models.SurahNameModel;
import com.example.qurannexus.services.DatabaseService;
import com.google.android.material.navigation.NavigationView;

public class RecitationPageActivity extends AppCompatActivity {
    private final String APP_ID = "application-0-plbqdoy";
    SurahModel surahModel;
    String layoutType = "verseByVerse";
    DatabaseService databaseService;
    private static final String KEY_LAYOUT_TYPE = "recitation_layout_by_page";
    private int currentSurahIndex;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recitation_page);

        setupNavigationDrawer();

        databaseService = new DatabaseService(APP_ID);
        surahModel = getIntent().getParcelableExtra("surahModel");
        layoutType = getIntent().getStringExtra("fragmentType");

        if (surahModel != null) {
            // Retrieve the user's layout preference
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            boolean isByPage = sharedPreferences.getBoolean(KEY_LAYOUT_TYPE, false);
            String layoutType = isByPage ? "pageByPage" : "verseByVerse";
            displayFragment(layoutType);
        }
        currentSurahIndex = getIntent().getIntExtra("currentSurahIndex", 0);

        ImageButton previousSurahButton = findViewById(R.id.previousSurahButton);
        ImageButton nextSurahButton = findViewById(R.id.nextSurahButton);

        previousSurahButton.setOnClickListener(v -> navigateToSurah(currentSurahIndex - 1));
        nextSurahButton.setOnClickListener(v -> navigateToSurah(currentSurahIndex + 1));

        ImageView bookmarkIcon = findViewById(R.id.bookmarkIcon);
        bookmarkIcon.setOnClickListener(v -> {
            // Toggle the bookmark status
            toggleBookmarkStatus();
        });
    }
    private void displayFragment(String fragmentType) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        Log.d("RecitationPageActivity", "Displaying fragment: " + fragmentType);
        if ("verseByVerse".equals(fragmentType)) {
            ByAyatRecitationFragment fragment = ByAyatRecitationFragment.newInstance(surahModel);
            transaction.replace(R.id.recitationFragmentContainerView, fragment);
        } else if ("pageByPage".equals(fragmentType)) {
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
                    // Optionally, update UI or provide feedback to user
                    Toast.makeText(RecitationPageActivity.this, "Bookmark updated successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(RecitationPageActivity.this, "Failed to update bookmark", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void setupNavigationDrawer() {
        // Set up the side menu button to open the drawer
        ImageView sideMenuButton = findViewById(R.id.recitationSideMenuButton);
        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.recitation_side_navigation_view);
        sideMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(navigationView);
            }
        });
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                handleNavigationItemSelected(menuItem);
                drawerLayout.closeDrawers();
                return true;
            }
        });
    }
    private void handleNavigationItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.nav_home) {
            Intent intent = new Intent(RecitationPageActivity.this, MainActivity.class);
            startActivity(intent);
        } else if (itemId == R.id.nav_settings) {
            Intent intent = new Intent(RecitationPageActivity.this, SettingsActivity.class);
            startActivity(intent);
        } else if (itemId == R.id.nav_tajweed) {
            // Handle the tajweed action
        }else if(itemId == R.id.nav_test) {
            Intent intent = new Intent(RecitationPageActivity.this, TestActivity.class);
            startActivity(intent);
        } else {
            // Handle default action
        }
    }
    private void navigateToSurah(int newIndex) {
        if (newIndex >= 0 && newIndex <= 114) {
            currentSurahIndex = newIndex;
            TextView surahNameTextView = findViewById(R.id.surahNameTextView);
            TextView surahNameEnglishTextView = findViewById(R.id.englishSurahNameTextView);
            databaseService.getSurahModelByIndex(currentSurahIndex, new DatabaseService.SurahModelCallback() {
                @Override
                public void onSurahModelLoaded(SurahNameModel surahModel) {
                    if (surahModel != null) {
                        surahNameTextView.setText(surahModel.getSurahName());
                        surahNameEnglishTextView.setText(surahModel.getEnglishName());
                        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.recitationFragmentContainerView);
                        if (currentFragment instanceof ByAyatRecitationFragment) {
                            ((ByAyatRecitationFragment) currentFragment).updateSurahContent(surahModel);
                        } else if (currentFragment instanceof ByPageRecitationFragment) {
                            ((ByPageRecitationFragment) currentFragment).updateSurahContent(surahModel);
                        }
                    } else {
                        Log.e(TAG, "SurahModel not found for index: " + currentSurahIndex);
                    }
                }
            });
        }
    }

}