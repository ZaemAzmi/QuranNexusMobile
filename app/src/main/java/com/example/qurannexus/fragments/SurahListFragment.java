package com.example.qurannexus.fragments;

import android.os.Bundle;

import android.util.Log;
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
import com.example.qurannexus.interfaces.QuranApi;
import com.example.qurannexus.models.adapters.SurahListAdapter;
import com.example.qurannexus.models.SurahModel;
import com.example.qurannexus.services.retrofit.ApiService;
import com.google.android.material.tabs.TabLayout;
import org.bson.Document;
import java.util.ArrayList;
import java.util.List;

import io.realm.mongodb.App;
import io.realm.mongodb.AppConfiguration;
import io.realm.mongodb.Credentials;
import io.realm.mongodb.RealmResultTask;
import io.realm.mongodb.User;
import io.realm.mongodb.mongo.MongoClient;
import io.realm.mongodb.mongo.MongoCollection;
import io.realm.mongodb.mongo.MongoDatabase;
import io.realm.mongodb.mongo.iterable.MongoCursor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SurahListFragment extends Fragment {

    String appID = "application-0-plbqdoy";
    MongoClient mongoClient;
    MongoDatabase mongoDatabase;
    ArrayList<SurahModel> surahModels = new ArrayList<>();
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
        query = query.toLowerCase();
        filteredSurahModels.clear();
        ArrayList<SurahModel> currentList = new ArrayList<>();

        if (currentTab == 0) {
            currentList = surahModels;
        } else if (currentTab == 2) {
            for (SurahModel surah : surahModels) {
                if (surah.isBookmarked()) {
                    currentList.add(surah);
                }
            }
        }

        for (SurahModel surah : currentList) {
            if (surah.getName().toLowerCase().contains(query)) {
                filteredSurahModels.add(surah);
            }
        }
        updateUI(filteredSurahModels);
    }

    private void updateUI(ArrayList<SurahModel> filteredSurahs) {
        RecyclerView surahRecyclerView = getView().findViewById(R.id.SurahRecyclerView);
        surahListAdapter = new SurahListAdapter(requireActivity(), filteredSurahs, layoutType);
        surahRecyclerView.setAdapter(surahListAdapter);
        surahRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void fetchSurahs() {
        Call<List<SurahModel>> call = quranApi.getAllSurahs();
        call.enqueue(new Callback<List<SurahModel>>() {
            @Override
            public void onResponse(Call<List<SurahModel>> call, Response<List<SurahModel>> response) {
                if (response.isSuccessful()) {
                    List<SurahModel> surahList = response.body();

                    Log.d("API_RESPONSE", "Response: " + surahList.toString());
                    if (surahList != null) {
                        surahModels.clear();
                        surahModels.addAll(surahList);

                        // Check if adapter is already set
                        if (surahListAdapter == null) {
                            // Initialize the adapter for the first time
                            RecyclerView surahRecyclerView = getView().findViewById(R.id.SurahRecyclerView);
                            surahListAdapter = new SurahListAdapter(requireActivity(), surahModels, layoutType);
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
            public void onFailure(Call<List<SurahModel>> call, Throwable t) {
                // Handle API call failure
            }
        });
    }

}