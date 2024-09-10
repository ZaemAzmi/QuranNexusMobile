package com.example.qurannexus.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.qurannexus.R;
import com.example.qurannexus.fragments.HomeFragment;
import com.example.qurannexus.fragments.SettingsFragment;
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

        // Initially load HomeFragment
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
            handleNavigationItemSelected(menuItem);
            drawerLayout.closeDrawers();
            return true;
        });
    }

    private void handleNavigationItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        Fragment selectedFragment = null;

        if (itemId == R.id.nav_home) {
            selectedFragment = new HomeFragment();
        } else if (itemId == R.id.nav_settings) {
            selectedFragment = new SettingsFragment();
        } else if (itemId == R.id.nav_tajweed) {
        }

        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.mainFragmentContainer, selectedFragment)
                    .commit();
        }
    }
}
