package com.example.qurannexus.core.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.etebarian.meowbottomnavigation.MeowBottomNavigation;
import com.example.qurannexus.R;
import com.example.qurannexus.core.enums.BottomMenuItemId;
import com.example.qurannexus.features.auth.AuthActivity;
import com.example.qurannexus.features.auth.AuthService;
import com.example.qurannexus.features.bookmark.BookmarkFragment;
import com.example.qurannexus.features.home.HomeFragment;
import com.example.qurannexus.features.prayerTimes.PrayerTimesFragment;
import com.example.qurannexus.features.quiz.QuizActivity;
import com.example.qurannexus.features.recitation.SurahListFragment;
import com.example.qurannexus.features.irab.IrabFragment;
import com.example.qurannexus.features.settings.SettingsFragment;
import com.google.android.material.navigation.NavigationView;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {
    private AuthService authService;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    MeowBottomNavigation meowBottomNavigation;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        authService = new AuthService();
        setupNavigationDrawer();
        setupMeowNavigationBar();
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.mainFragmentContainer, new HomeFragment())
                    .commit();
        }
    }

    private void setupNavigationDrawer() {
        ImageView sideMenuButton = findViewById(R.id.sideMenuButton);
        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.side_navigation_view);

        sideMenuButton.setOnClickListener(v -> drawerLayout.openDrawer(navigationView));

        navigationView.setNavigationItemSelectedListener(menuItem -> {
            handleSideNavigationItemSelected(menuItem);
            drawerLayout.closeDrawers();
            return true;
        });
    }

    private void handleSideNavigationItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        Fragment selectedFragment = null;

        if (itemId == R.id.nav_home) {
            selectedFragment = new HomeFragment();
        } else if (itemId == R.id.nav_settings) {
            selectedFragment = new SettingsFragment();
        }  else if (itemId == R.id.nav_irab) {
            selectedFragment = new IrabFragment();
        }else if (itemId == R.id.nav_tajweed) {
        }else if (itemId == R.id.nav_test) {
            Intent i = new Intent(new Intent(MainActivity.this, TestActivity.class));
            startActivity(i);
        }else if (itemId == R.id.nav_logout) {
            handleLogout();
        }

        if (selectedFragment != null) {
          loadFragment(selectedFragment);
        }
    }


    private void setupMeowNavigationBar(){

        meowBottomNavigation = findViewById(R.id.meowBottomNav);

        meowBottomNavigation.add(new MeowBottomNavigation.Model(BottomMenuItemId.HOME.getId(), R.drawable.ic_home));
        meowBottomNavigation.add(new MeowBottomNavigation.Model(BottomMenuItemId.SURAHLIST.getId(), R.drawable.ic_quran));
        meowBottomNavigation.add(new MeowBottomNavigation.Model(BottomMenuItemId.BOOKMARK.getId(), R.drawable.ic_bookmark));
        meowBottomNavigation.add(new MeowBottomNavigation.Model(BottomMenuItemId.QUIZ.getId(), R.drawable.ic_note));
        meowBottomNavigation.setOnClickMenuListener(new MeowBottomNavigation.ClickListener() {
            @Override
            public void onClickItem(MeowBottomNavigation.Model model) {
                Fragment selectedFragment = null;

                switch (BottomMenuItemId.fromId(model.getId())) {
                    case HOME:
                        selectedFragment = new HomeFragment();
                        break;
                    case SURAHLIST:
                        selectedFragment = new SurahListFragment();
                        break;
                    case BOOKMARK:
                        selectedFragment = new BookmarkFragment();
                        break;
                    case QUIZ:
                        Intent i = new Intent(new Intent(MainActivity.this, QuizActivity.class));
                        startActivity(i);
                        break;
                }

                if (selectedFragment != null) {
                    loadFragment(selectedFragment);
                }
            }
        });
        meowBottomNavigation.setOnReselectListener(new MeowBottomNavigation.ReselectListener() {
            @Override
            public void onReselectItem(MeowBottomNavigation.Model model) {
                // Optionally handle reselection (e.g., scroll to the top of the fragment)
                // If you don't need special behavior, leave this empty.
            }
        });
        meowBottomNavigation.setOnShowListener(new MeowBottomNavigation.ShowListener() {
            @Override
            public void onShowItem(MeowBottomNavigation.Model model) {

            }
        });
    }
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.mainFragmentContainer, fragment)
                .commit();

        if(meowBottomNavigation != null){
            if (fragment instanceof HomeFragment) {
                meowBottomNavigation.show(BottomMenuItemId.HOME.getId(), true);
            } else if (fragment instanceof SurahListFragment) {
                meowBottomNavigation.show(BottomMenuItemId.SURAHLIST.getId(), true);
            } else if (fragment instanceof BookmarkFragment) {
                meowBottomNavigation.show(BottomMenuItemId.BOOKMARK.getId(), true);
            } else if (fragment instanceof PrayerTimesFragment) {
                meowBottomNavigation.show(BottomMenuItemId.QUIZ.getId(), true);
            }
        }
    }

    private void handleLogout() {
        // Show loading dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Logging out...");
        progressDialog.show();

        authService.logout(this, success -> {
            progressDialog.dismiss();

            // Redirect to auth activity regardless of server response
            Intent intent = new Intent(MainActivity.this, AuthActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return null;
        });
    }

}
