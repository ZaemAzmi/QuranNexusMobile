package com.example.qurannexus.features.recitation;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qurannexus.R;
import com.example.qurannexus.core.interfaces.QuranApi;
import com.example.qurannexus.features.recitation.models.Ayah;
import com.example.qurannexus.features.recitation.models.AyahRecitationModel;
import com.example.qurannexus.features.recitation.models.SurahModel;
import com.example.qurannexus.features.recitation.models.SurahRecitationByAyatAdapter;
import com.example.qurannexus.core.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ByAyatRecitationFragment extends Fragment {
    private static final String ARG_SURAH_NUMBER = "surah_number";
    private SurahModel surahModel;
    private int surahNumber;
    private ArrayList<Ayah> ayatModels = new ArrayList<>();
    SurahRecitationByAyatAdapter byAyatAdapter;
    RecyclerView byAyatRecyclerView;
    private QuranApi quranApi;
    public ByAyatRecitationFragment() {}
    public static ByAyatRecitationFragment newInstance(int surahNumber) {
        ByAyatRecitationFragment fragment = new ByAyatRecitationFragment();
        Bundle args = new Bundle();
//        Log.e("inside byayat fragment","surahNumber;"+surahNumber);
        args.putInt(ARG_SURAH_NUMBER, surahNumber);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            surahNumber = getArguments().getInt(ARG_SURAH_NUMBER);
        }
        quranApi = ApiService.getQuranClient().create(QuranApi.class);
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_by_ayat_recitation, container, false);

        byAyatRecyclerView = view.findViewById(R.id.byAyatRecyclerView);
        byAyatRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        byAyatAdapter = new SurahRecitationByAyatAdapter(getContext(), ayatModels);
        byAyatRecyclerView.setAdapter(byAyatAdapter);

        if (surahNumber != 0) {
            fetchVersesByAyat(surahNumber);
        }
        return view;
    }

    private void fetchVersesByAyat(int surahIndex) {
        quranApi.getVersesBySurah(surahIndex).enqueue(new Callback<AyahRecitationModel>() {
            @Override
            public void onResponse(Call<AyahRecitationModel> call, Response<AyahRecitationModel> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Ayah> ayahs = response.body().getData();
                    ayatModels.clear();
                    // Loop through each Ayah in the response and create AyatModel instances
                    for (Ayah ayah : ayahs) {
                        // Create the AyatModel instance
                        Ayah ayatModel = new Ayah(
                                ayah.getId(),
                                ayah.getSurahId(),
                                ayah.getAyahIndex(),
                                ayah.getAyahKey(),
                                ayah.getPageId(),
                                ayah.getJuzId(),
                                ayah.getBismillah(),
                                ayah.getArabicText(),
                                ayah.getWords(),
                                ayah.getTranslations()
                        );
                        // Add to the list
                        ayatModels.add(ayatModel);
                    }

                    // Notify the adapter of data changes
                    byAyatAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<AyahRecitationModel> call, Throwable t) {
                Log.e("ByAyatRecitationFragment", "Failed to fetch verses", t);
            }
        });
    }
}