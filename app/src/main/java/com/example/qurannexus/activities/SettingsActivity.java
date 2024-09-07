package com.example.qurannexus.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.qurannexus.R;
import com.example.qurannexus.fragments.SettingsFragment;

public class SettingsActivity extends AppCompatActivity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Check if we're restoring the state or if it's a new creation
        if (savedInstanceState == null) {
            // Load the settings fragment
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.settings_fragment_container, new SettingsFragment())
                    .commit();
        }
    }
}