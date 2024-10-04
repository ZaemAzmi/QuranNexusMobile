package com.example.qurannexus.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.etebarian.meowbottomnavigation.MeowBottomNavigation;
import com.example.qurannexus.R;
import com.example.qurannexus.enums.BottomMenuItemId;
import com.example.qurannexus.fragments.HomeFragment;
import com.example.qurannexus.fragments.PrayerTimesFragment;
import com.example.qurannexus.fragments.SurahListFragment;
import com.example.qurannexus.fragments.IrabFragment;
import com.example.qurannexus.fragments.SettingsFragment;
import com.example.qurannexus.fragments.TajweedFragment;
import com.google.android.material.navigation.NavigationView;

import io.realm.Realm;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Realm.init(this);
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
            selectedFragment = new SurahListFragment();
        } else if (itemId == R.id.nav_settings) {
            selectedFragment = new SettingsFragment();
        }  else if (itemId == R.id.nav_irab) {
            selectedFragment = new IrabFragment();
        }else if (itemId == R.id.nav_tajweed) {
        }else if (itemId == R.id.nav_test) {
            Intent i = new Intent(new Intent(MainActivity.this, TestActivity.class));
            startActivity(i);
        }

        if (selectedFragment != null) {
          loadFragment(selectedFragment);
        }
    }


    private void setupMeowNavigationBar(){

        MeowBottomNavigation meowBottomNavigation = findViewById(R.id.meowBottomNav);

        meowBottomNavigation.add(new MeowBottomNavigation.Model(BottomMenuItemId.HOME.getId(), R.drawable.ic_home));
        meowBottomNavigation.add(new MeowBottomNavigation.Model(BottomMenuItemId.TAJWEED.getId(), R.drawable.ic_home));
        meowBottomNavigation.add(new MeowBottomNavigation.Model(BottomMenuItemId.IRAB.getId(), R.drawable.ic_home));
        meowBottomNavigation.add(new MeowBottomNavigation.Model(BottomMenuItemId.TEST.getId(), R.drawable.ic_home));

        meowBottomNavigation.setOnClickMenuListener(new MeowBottomNavigation.ClickListener() {
            @Override
            public void onClickItem(MeowBottomNavigation.Model model) {
                Fragment selectedFragment = null;

                switch (BottomMenuItemId.fromId(model.getId())) {
                    case HOME:
                        selectedFragment = new HomeFragment();
                        break;
                    case IRAB:
                        selectedFragment = new SurahListFragment();
                        break;
                    case TAJWEED:
                        selectedFragment = new TajweedFragment();
                        break;
                    case TEST:
                        selectedFragment = new PrayerTimesFragment();
                        break;
                }

                if (selectedFragment != null) {
                    loadFragment(selectedFragment); // Method to load the fragment
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
    }
}
