package com.example.qurannexus.features.recitation;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.Toast;

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
    private View view;
    private View errorView;
    private RecyclerView surahRecyclerView;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_surah_list, container, false);

        searchView = view.findViewById(R.id.searchSurahView);

        setupSearchView();
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
        if (surahListResponse == null || surahListResponse.getData() == null) return;

        query = query.toLowerCase();
        filteredSurahModels.clear();

        for (SurahModel surah : surahListResponse.getData()) {
            if (surah.getName().toLowerCase().contains(query)) {
                filteredSurahModels.add(surah);
            }
        }

        updateUI(filteredSurahModels);
    }

    private void updateUI(ArrayList<SurahModel> filteredSurahs) {
        if (!isAdded()) {
            return;
        }
        surahRecyclerView = view.findViewById(R.id.SurahRecyclerView);
        if (surahRecyclerView != null) {
            Context context = getContext();
            if(context!= null){
                if (surahListAdapter == null) {
                    surahListAdapter = new SurahListAdapter(requireActivity(), filteredSurahs, layoutType);
                    surahRecyclerView.setAdapter(surahListAdapter);
                    surahRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                } else {
                    surahListAdapter.updateData(filteredSurahs);
                    surahListAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    private void fetchSurahs() {
        Call<SurahListResponse> call = quranApi.getAllSurahs();
        call.enqueue(new Callback<SurahListResponse>() {
            @Override
            public void onResponse(Call<SurahListResponse> call, Response<SurahListResponse> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().getData() != null) {
                        surahListResponse = response.body();
                        filteredSurahModels.clear();
                        filteredSurahModels.addAll(surahListResponse.getData());
                        updateUI(filteredSurahModels);
                    }
                }
            }

            @Override
            public void onFailure(Call<SurahListResponse> call, Throwable t) {
                // Check if fragment is still attached to activity
                if (!isAdded()) {
                    return;
                }

                // Get context safely
                Context context = getContext();
                if (context != null) {
                    Toast.makeText(context, "Failed to fetch Surahs", Toast.LENGTH_SHORT).show();
                }

                // Optionally, update UI to show error state
                showErrorState(t.getMessage());
            }
        });
    }

    private void showErrorState(String errorMessage) {
        if (!isAdded()) {
            return;
        }

        // Find your error view
        errorView = view.findViewById(R.id.errorView); // Add this to your layout
        if (errorView != null) {
            errorView.setVisibility(View.VISIBLE);
        }

        // You might want to hide the RecyclerView
        RecyclerView surahRecyclerView = view.findViewById(R.id.SurahRecyclerView);
        if (surahRecyclerView != null) {
            surahRecyclerView.setVisibility(View.GONE);
        }
    }

    public void retryFetch() {
        if (errorView != null) {
            errorView.setVisibility(View.GONE);
        }
        if (surahRecyclerView != null) {
            surahRecyclerView.setVisibility(View.VISIBLE);
        }
        fetchSurahs();
    }
}
