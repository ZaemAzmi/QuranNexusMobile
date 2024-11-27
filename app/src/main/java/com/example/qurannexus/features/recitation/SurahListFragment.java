package com.example.qurannexus.features.recitation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qurannexus.R;
import com.example.qurannexus.core.interfaces.QuranApi;
import com.example.qurannexus.features.recitation.models.SurahListResponse;
import com.example.qurannexus.features.recitation.models.SurahModel;
import com.example.qurannexus.features.recitation.models.SurahListAdapter;
import com.example.qurannexus.core.network.ApiService;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SurahListFragment extends Fragment {

    SurahListResponse surahListResponse;
    ArrayList<SurahModel> filteredSurahModels = new ArrayList<>();
    String layoutType = "verseByVerse";
    SurahListAdapter surahListAdapter;
    SearchView searchView;
    int currentTab = 0;

    private QuranApi quranApi;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_surah_list, container, false);

        searchView = view.findViewById(R.id.searchSurahView);

        setupSearchView();
//        setUpSurahListModels();
        quranApi = ApiService.getQuranClient().create(QuranApi.class);
        fetchSurahs();
        return view;
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterSurahList(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterSurahList(newText);
                return true;
            }
        });
    }

    private void filterSurahList(String query) {
//        query = query.toLowerCase();
//        filteredSurahModels.clear();
//        ArrayList<SurahModel> currentList = new ArrayList<>();
//
//        if (currentTab == 0) {
//            currentList = surahListResponse;
//        } else if (currentTab == 2) {
//            for (SurahModel surah : surahListResponse) {
//                if (surah.isBookmarked()) {
//                    currentList.add(surah);
//                }
//            }
//        }
//
//        for (SurahModel surah : currentList) {
//            if (surah.getName().toLowerCase().contains(query)) {
//                filteredSurahModels.add(surah);
//            }
//        }
//        updateUI(filteredSurahModels);
    }

    private void updateUI(ArrayList<SurahModel> filteredSurahs) {
        RecyclerView surahRecyclerView = getView().findViewById(R.id.SurahRecyclerView);
        surahListAdapter = new SurahListAdapter(requireActivity(), filteredSurahs, layoutType);
        surahRecyclerView.setAdapter(surahListAdapter);
        surahRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void fetchSurahs() {
        Call<SurahListResponse> call = quranApi.getAllSurahs();
        call.enqueue(new Callback<SurahListResponse>() {
            @Override
            public void onResponse(Call<SurahListResponse> call, Response<SurahListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().getData() != null) {
                        surahListResponse = response.body();
                        // Check if adapter is already set
                        if (surahListAdapter == null) {
                            // Initialize the adapter for the first time
                            RecyclerView surahRecyclerView = getView().findViewById(R.id.SurahRecyclerView);
                            surahListAdapter = new SurahListAdapter(requireActivity(), surahListResponse.getData(), layoutType);
                            surahRecyclerView.setAdapter(surahListAdapter);
                            surahRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                        } else {
                            // Adapter exists, just update the data
                            surahListAdapter.notifyDataSetChanged();
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<SurahListResponse> call, Throwable t) {
                // Handle API call failure
            }
        });
    }

}