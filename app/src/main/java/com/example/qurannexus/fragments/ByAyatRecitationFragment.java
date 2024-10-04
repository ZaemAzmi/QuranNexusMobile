package com.example.qurannexus.fragments;

import android.os.Bundle;

import com.example.qurannexus.models.SurahNameModel;
import com.example.qurannexus.services.DatabaseService;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.qurannexus.models.AyatModel;
import com.example.qurannexus.models.SurahModel;
import com.example.qurannexus.models.adapters.SurahRecitationByAyatAdapter;
import com.example.qurannexus.R;

import java.util.ArrayList;

public class ByAyatRecitationFragment extends Fragment {
    private static final String ARG_SURAH = "surah";
    private SurahModel surahModel;
    private SurahNameModel surahNameModel;
    private DatabaseService databaseService;
    private ArrayList<AyatModel> ayatModels = new ArrayList<>();
    SurahRecitationByAyatAdapter byAyatAdapter;
    public ByAyatRecitationFragment() {}
    public static ByAyatRecitationFragment newInstance(SurahModel surahModel) {
        ByAyatRecitationFragment fragment = new ByAyatRecitationFragment();
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
        View view = inflater.inflate(R.layout.fragment_by_ayat_recitation, container, false);

        RecyclerView byAyatRecyclerView = view.findViewById(R.id.byAyatRecyclerView);
        byAyatRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        databaseService = new DatabaseService("application-0-plbqdoy", new DatabaseService.DatabaseInitCallback() {
            @Override
            public void onInitSuccess() {
                byAyatAdapter = new SurahRecitationByAyatAdapter(getContext(), ayatModels, databaseService);
                byAyatRecyclerView.setAdapter(byAyatAdapter);
                if (surahModel != null) {
                    fetchVersesByAyat(Integer.parseInt(surahModel.getSurahNumber()));
                }
            }
            @Override
            public void onInitFailure(Throwable error) {
                Log.e("ByAyatRecitationFragment", "Database initialization failed", error);
            }
        });
        return view;
    }

    private void fetchVersesByAyat(int surahIndex){
        databaseService.getVersesByAyat(surahIndex, ayatList-> {
            ayatModels.clear();
            ayatModels.addAll(ayatList);
            byAyatAdapter.notifyDataSetChanged();
        });
    }

    public void updateSurahContent(SurahNameModel newSurahNameModel) {
        surahNameModel = newSurahNameModel;
        fetchVersesByAyat(surahNameModel.getSurahIndex());
    }
}