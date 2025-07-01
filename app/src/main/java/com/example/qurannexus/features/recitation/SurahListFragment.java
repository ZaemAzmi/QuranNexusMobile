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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qurannexus.R;
import com.example.qurannexus.core.activities.MainActivity;
import com.example.qurannexus.core.interfaces.QuranApi;
import com.example.qurannexus.core.utils.QuranMetadata;
import com.example.qurannexus.core.utils.SurahDetails;
import com.example.qurannexus.features.recitation.models.SurahListResponse;
import com.example.qurannexus.features.recitation.models.SurahModel;
import com.example.qurannexus.features.recitation.models.SurahListAdapter;
import com.example.qurannexus.core.network.ApiService;
import com.example.qurannexus.features.recitation.viewModels.SurahListViewModel;

import java.io.IOException;
import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
@AndroidEntryPoint
public class SurahListFragment extends Fragment {

    SurahListAdapter surahListAdapter;
    SearchView searchView;
    private View view;
    private RecyclerView surahRecyclerView;
    private ProgressBar loadingIndicator;
    private SurahListViewModel viewModel;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_surah_list, container, false);

        searchView = view.findViewById(R.id.searchSurahView);
        loadingIndicator = view.findViewById(R.id.loadingIndicator);
        surahRecyclerView = view.findViewById(R.id.SurahRecyclerView);

        return view;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SurahListViewModel.class);

        setupSearchView();
        setupRecyclerView();
        observeViewModel();
    }
    private void setupRecyclerView() {
        surahListAdapter = new SurahListAdapter(requireActivity(), new ArrayList<>(), "verseByVerse");
        surahRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        surahRecyclerView.setAdapter(surahListAdapter);

        surahRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (getActivity() instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) getActivity();
                    if (dy > 10) { // Scrolling down
                        mainActivity.setBottomNavigationVisibility(false);
                    } else if (dy < -10) { // Scrolling up
                        mainActivity.setBottomNavigationVisibility(true);
                    }
                }
            }
        });
    }
    private void observeViewModel() {
        loadingIndicator.setVisibility(View.VISIBLE);
        // The viewModel is now guaranteed to be non-null here.
        viewModel.getSurahList().observe(getViewLifecycleOwner(), surahs -> {
            if (surahs != null) {
                surahListAdapter.updateData(surahs);
                loadingIndicator.setVisibility(View.GONE);
            }
        });
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
                    viewModel.filterSurahList(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    viewModel.filterSurahList(newText);
                    return true;
                }
            });
        });
    }
    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(true);
        }
    }
}
