package com.example.qurannexus.features.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

        // Reciter Selection Logic
        ListPreference reciterPreference = findPreference("selected_reciter");
        if (reciterPreference != null) {
            reciterPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                String selectedReciter = (String) newValue;
                // Save preference
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("selected_reciter", selectedReciter);
                editor.apply();
                Log.d("SettingsFragment", "Selected reciter changed: " + selectedReciter);
                return true;
            });
        }
    }

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        view.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        ListView listView = view.findViewById(android.R.id.list);
        if (listView != null) {
            listView.setPadding(0,
                    getResources().getDimensionPixelSize(R.dimen.preference_margin_top_16dp),
                    0,
                    0);
        }

        return view;
    }
}
