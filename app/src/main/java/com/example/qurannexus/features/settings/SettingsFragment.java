package com.example.qurannexus.features.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.example.qurannexus.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        // Retrieve the SharedPreferences
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();

        // Recitation Layout Switch Logic
        SwitchPreferenceCompat recitationLayoutSwitch = findPreference("recitation_layout_by_page");
        if (recitationLayoutSwitch != null) {
            recitationLayoutSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean isByPage = (Boolean) newValue;
                // Save preference
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("recitation_layout_by_page", isByPage);
                editor.apply();
                Log.d("SettingsFragment", "Recitation layout preference changed: " + isByPage);
                return true;
            });
        }

        // Audio Background Playback Switch Logic
        SwitchPreferenceCompat audioBackgroundSwitch = findPreference("audio_background_play");
        if (audioBackgroundSwitch != null) {
            audioBackgroundSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean allowBackgroundPlay = (Boolean) newValue;
                // Save preference
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("audio_background_play", allowBackgroundPlay);
                editor.apply();
                Log.d("SettingsFragment", "Audio background playback preference changed: " + allowBackgroundPlay);
                return true;
            });
        }

    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate our custom layout FIRST
        View view = inflater.inflate(R.layout.layout_settings_fragment, container, false);

        backButtonSetup(view);
        ViewGroup preferenceContainer = view.findViewById(android.R.id.list_container);
        // Let the parent class do its work of inflating the preferences, but tell it
        // to use our container instead of creating its own view.
        View preferenceView = super.onCreateView(inflater, preferenceContainer, savedInstanceState);
        // Add the inflated preferences to our container
        if (preferenceContainer != null) {
            preferenceContainer.addView(preferenceView);
        }
        return view;
    }

    private void backButtonSetup(View view) {
        ImageView backButton = view.findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }
}
