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
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.example.qurannexus.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        // Retrieve the SharedPreferences
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        SwitchPreferenceCompat recitationLayoutSwitch = findPreference("recitation_layout_by_page");

        // Set up change listener
        if (recitationLayoutSwitch != null) {
            recitationLayoutSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean isByPage = (Boolean) newValue;
                // Save preference
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("recitation_layout_by_page", isByPage);
                editor.apply();
                Log.d("SettingsFragment", "Preference changed: " + isByPage);
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