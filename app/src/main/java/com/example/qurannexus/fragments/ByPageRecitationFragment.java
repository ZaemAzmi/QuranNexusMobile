package com.example.qurannexus.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.qurannexus.models.SurahModel;
import com.example.qurannexus.models.SurahNameModel;
import com.example.qurannexus.R;
import com.example.qurannexus.services.DatabaseService;

public class ByPageRecitationFragment extends Fragment {
    private static final String ARG_SURAH = "surah";
    private DatabaseService databaseService;
    private SurahModel surahModel;
    private SurahNameModel surahNameModel;
    private TextView pageTextView;

    public static ByPageRecitationFragment newInstance(SurahModel surahModel) {
        ByPageRecitationFragment fragment = new ByPageRecitationFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_SURAH, surahModel);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            surahModel = getArguments().getParcelable(ARG_SURAH);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_by_page_recitation, container, false);

        pageTextView = view.findViewById(R.id.byPageTextView);
        databaseService = new DatabaseService("application-0-plbqdoy", new DatabaseService.DatabaseInitCallback() {
            @Override
            public void onInitSuccess() {
                if (surahModel != null) {
                    fetchCombinedVerses(Integer.parseInt(surahModel.getSurahNumber()), pageTextView);
                }
            }

            @Override
            public void onInitFailure(Throwable error) {
                Log.e("ByPageRecitationFragment", "Database initialization failed", error);
            }
        });
        return view;
    }
    private void fetchCombinedVerses(int surahIndex, TextView pageTextView) {
        databaseService.getCombinedVerses(surahIndex, combinedVerses -> {
            pageTextView.setText(combinedVerses);
        });
    }
    public void updateSurahContent(SurahNameModel newSurahNameModel) {
        surahNameModel = newSurahNameModel;
        pageTextView = getView().findViewById(R.id.byPageTextView);
        fetchCombinedVerses(newSurahNameModel.getSurahIndex(), pageTextView);
    }
}