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
import com.example.qurannexus.services.ApiService;
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
    TabLayout tabLayout;
    SearchView searchView;
    int currentTab = 0;

    private QuranApi quranApi;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_surah_list, container, false);

        tabLayout = view.findViewById(R.id.tabLayout);
        searchView = view.findViewById(R.id.searchSurahView);

        setupTabLayout();
        setupSearchView();
//        setUpSurahListModels();
        quranApi = ApiService.getQuranClient().create(QuranApi.class);
        fetchSurahs();
        return view;
    }

    private void setupTabLayout() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                if (currentTab == 0) {
                    updateUI(surahModels);
                } else if (currentTab == 2) {
                    ArrayList<SurahModel> savedSurahs = new ArrayList<>();
                    for (SurahModel surah : surahModels) {
                        if (surah.isBookmarked()) {
                            savedSurahs.add(surah);
                        }
                    }
                    updateUI(savedSurahs);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
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

    private void setUpSurahListModels() {
        App app = new App(new AppConfiguration.Builder(appID).build());
        app.loginAsync(Credentials.anonymous(), new App.Callback<User>() {
            @Override
            public void onResult(App.Result<User> result) {
                if (result.isSuccess()) {
                    User user = app.currentUser();
                    mongoClient = user.getMongoClient("mongodb-atlas");
                    mongoDatabase = mongoClient.getDatabase("quran_database");
                    MongoCollection<Document> mongoCollection = mongoDatabase.getCollection("surahs");

                    RealmResultTask<MongoCursor<Document>> findTask = mongoCollection.find().iterator();
                    findTask.getAsync(task -> {
                        if (task.isSuccess()) {
                            MongoCursor<Document> results = task.get();
                            while (results.hasNext()) {
                                Document currentDoc = results.next();
                                String surahNumber = String.valueOf(currentDoc.getInteger("index", 0));
                                String arabicSurahName = currentDoc.getString("name");
                                String surahName = currentDoc.getString("tname");
                                String surahMeaning = currentDoc.getString("ename");
                                String ayatNumber = String.valueOf(currentDoc.getInteger("ayas", 0));
                                boolean isBookmarked = currentDoc.getBoolean("bookmarked", false);

                                surahModels.add(new SurahModel(
                                        surahName,
                                        arabicSurahName,
                                        surahNumber,
                                        surahMeaning,
                                        ayatNumber,
                                        isBookmarked
                                ));
                            }
                            updateUI(surahModels);
                        }
                    });
                }
            }
        });
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