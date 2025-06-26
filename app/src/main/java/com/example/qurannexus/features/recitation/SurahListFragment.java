package com.example.qurannexus.features.recitation;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qurannexus.R;
import com.example.qurannexus.core.interfaces.QuranApi;
import com.example.qurannexus.core.utils.QuranMetadata;
import com.example.qurannexus.core.utils.SurahDetails;
import com.example.qurannexus.features.recitation.models.SurahListResponse;
import com.example.qurannexus.features.recitation.models.SurahModel;
import com.example.qurannexus.features.recitation.models.SurahListAdapter;
import com.example.qurannexus.core.network.ApiService;

import java.io.IOException;
import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
@AndroidEntryPoint
public class SurahListFragment extends Fragment {

    SurahListResponse surahListResponse;
    String layoutType = "verseByVerse";
    SurahListAdapter surahListAdapter;
    SearchView searchView;
    int currentTab = 0;
    private QuranApi quranApi;
    private View view;
    private View errorView;
    private RecyclerView surahRecyclerView;
    private ArrayList<SurahModel> allSurahModels = new ArrayList<>();
    private ArrayList<SurahModel> filteredSurahModels = new ArrayList<>();

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000; // 1 second initial delay
    private int retryCount = 0;
    private Handler retryHandler = new Handler();
    private ProgressBar loadingIndicator;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_surah_list, container, false);

        searchView = view.findViewById(R.id.searchSurahView);
        loadingIndicator = view.findViewById(R.id.loadingIndicator);
        quranApi = ApiService.getQuranClient().create(QuranApi.class);
//        fetchSurahs();
        loadSurahsFromMetadata();
        Button retryButton = view.findViewById(R.id.retryButton);
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                retryFetch();
            }
        });

        return view;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupSearchView();
    }
    private void setupSearchView() {
        searchView.post(() -> {
            EditText searchEditText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
            if (searchEditText != null) {
                float textSize = getResources().getDimension(R.dimen.text_size_medium) /
                        getResources().getDisplayMetrics().scaledDensity;
                searchEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
            } else {
                Log.e("SurahListFragment", "search_src_text not found inside SearchView");
            }

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
        });
    }
    private void loadSurahsFromMetadata() {
        showLoading();
        // Clear previous data
        allSurahModels.clear();
        filteredSurahModels.clear();

        QuranMetadata metadata = QuranMetadata.Companion.getInstance();
        // Loop from Surah 1 to 114
        for (int i = 1; i <= 114; i++) {
            SurahDetails details = metadata.getSurahDetails(i);
            if (details != null) {
                // Convert SurahDetails to the SurahModel that the adapter uses
                SurahModel model = new SurahModel(
                        details.getEnglishName(),
                        details.getArabicName(),
                        String.valueOf(details.getSurahIndex()),
                        details.getTranslationName(),
                        String.valueOf(details.getNumberOfVerses()),
                        false // isBookmarked can be handled later if needed
                );
                allSurahModels.add(model);
            }
        }

        // Initially, the filtered list is the same as the full list
        filteredSurahModels.addAll(allSurahModels);

        // Update the UI with the full list
        updateUI(filteredSurahModels);
        hideLoading();
    }

    private void filterSurahList(String query) {
        // This method now filters the local 'allSurahModels' list
        if (allSurahModels.isEmpty()) return;

        query = query.toLowerCase().trim();
        filteredSurahModels.clear();

        if (query.isEmpty()) {
            filteredSurahModels.addAll(allSurahModels);
        } else {
            for (SurahModel surah : allSurahModels) {
                // Search by English name, Arabic name, or Surah number
                if (surah.getName().toLowerCase().contains(query) ||
                        surah.getArabicSurahName().contains(query) ||
                        surah.getSurahNumber().equals(query)) {
                    filteredSurahModels.add(surah);
                }
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
        showLoading();
        Call<SurahListResponse> call = quranApi.getAllSurahs();
        call.enqueue(new Callback<SurahListResponse>() {
            @Override
            public void onResponse(Call<SurahListResponse> call, Response<SurahListResponse> response) {
                if (!isAdded()) return;
                hideLoading();
                if (response.isSuccessful() && response.body() != null) {
                    resetRetryCount();
                    handleSuccessResponse(response.body());
                } else {
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(Call<SurahListResponse> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                hideLoading();

                handleFailure(t);
            }
        });
    }
    private void showLoading() {
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoading() {
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(View.GONE);
        }
    }
    private void handleSuccessResponse(SurahListResponse response) {
        if (response.getData() != null) {
            surahListResponse = response;
            filteredSurahModels.clear();
            filteredSurahModels.addAll(response.getData());
            updateUI(filteredSurahModels);
        } else {
            showErrorState("No data available");
        }
    }
    private void showErrorState(String errorMessage) {
        if (!isAdded()) {
            return;
        }
        errorView = view.findViewById(R.id.errorView);
        if (errorView != null) {
            errorView.setVisibility(View.VISIBLE);
        }
        RecyclerView surahRecyclerView = view.findViewById(R.id.SurahRecyclerView);
        if (surahRecyclerView != null) {
            surahRecyclerView.setVisibility(View.GONE);
        }
    }

    private void handleErrorResponse(Response<SurahListResponse> response) {
        String errorMessage;
        switch (response.code()) {
            case 404:
                errorMessage = "Surah data not found";
                break;
            case 401:
                errorMessage = "Authentication required";
                break;
            case 503:
                errorMessage = "Service temporarily unavailable";
                break;
            default:
                errorMessage = "Server error: " + response.code();
        }
        showErrorState(errorMessage);
    }

    private void handleFailure(Throwable t) {
        String errorMessage;
        if (t instanceof IOException) {
            errorMessage = "Network error. Please check your connection.";
        } else {
            errorMessage = "An unexpected error occurred";
        }

        if (retryCount < MAX_RETRY_ATTEMPTS) {
            scheduleRetry();
        } else {
            showErrorState(errorMessage);
            Log.e("SurahList", "Failed after " + MAX_RETRY_ATTEMPTS + " attempts", t);
        }
    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    private void scheduleRetry() {
        long delay = RETRY_DELAY_MS * (long) Math.pow(2, retryCount);
        retryHandler.postDelayed(() -> {
            retryCount++;
            fetchSurahs();
        }, delay);
    }

    private void resetRetryCount() {
        retryCount = 0;
        retryHandler.removeCallbacksAndMessages(null);
    }

    private void retryFetch() {
        if (!isNetworkAvailable()) {
            showErrorState("No internet connection");
            return;
        }

        resetRetryCount();
        if (errorView != null) {
            errorView.setVisibility(View.GONE);
        }
        if (surahRecyclerView != null) {
            surahRecyclerView.setVisibility(View.VISIBLE);
        }
        fetchSurahs();
    }


}
