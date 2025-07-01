package com.example.qurannexus.features.recitation;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.FlowLiveDataConversions;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qurannexus.core.activities.MainActivity;
import com.example.qurannexus.core.database.entities.QuranAyahDetailEntity;
import com.example.qurannexus.features.recitation.models.SurahRecitationByAyatAdapter;
import com.example.qurannexus.features.recitation.viewModels.PageDataState;
import com.example.qurannexus.features.recitation.viewModels.RecitationViewModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.example.qurannexus.R;
import java.lang.reflect.Type;
import java.util.ArrayList;

@UnstableApi
public class SingleAyahPageFragment extends Fragment {

    private static final String ARG_PAGE_NUMBER = "arg_page_number"; // Changed argument
    private int pageNumber;
    private RecitationViewModel sharedViewModel;
    private RecyclerView recyclerView;
    private SurahRecitationByAyatAdapter adapter;
    // MODIFIED: newInstance now only needs the page number
    public static SingleAyahPageFragment newInstance(int pageNumber) {
        SingleAyahPageFragment fragment = new SingleAyahPageFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE_NUMBER, pageNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get the shared ViewModel from the grandparent (RecitationPageFragment)
        sharedViewModel = new ViewModelProvider(requireParentFragment().requireParentFragment()).get(RecitationViewModel.class);

        if (getArguments() != null) {
            pageNumber = getArguments().getInt(ARG_PAGE_NUMBER);
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_verses_page, container, false);
        recyclerView = view.findViewById(R.id.versesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Setup adapter with an empty list initially
        adapter = new SurahRecitationByAyatAdapter(requireContext(), this,  new ArrayList<>());
        recyclerView.setAdapter(adapter);

        observeViewModel(); // Start observing
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // The grandparent fragment is RecitationPageFragment, its parent is MainActivity
                if (requireParentFragment().requireParentFragment().getActivity() instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) requireParentFragment().requireParentFragment().getActivity();
                    if(mainActivity != null){
                        if (dy > 10) { // Scrolling down
                            mainActivity.setBottomNavigationVisibility(false);
                        } else if (dy < -10) { // Scrolling up
                            mainActivity.setBottomNavigationVisibility(true);
                        }
                    }
                }
            }
        });
        return view;
    }

    // NEW: The core logic for self-updating
    private void observeViewModel() {
        LiveData<PageDataState> pageDataLiveData = FlowLiveDataConversions.asLiveData(sharedViewModel.getPageData());
        pageDataLiveData.observe(getViewLifecycleOwner(), state -> {
            if (state instanceof PageDataState.Success) {
                PageDataState.Success successState = (PageDataState.Success) state;
                if (!successState.getAyahs().isEmpty()) {
                    // This fragment only cares about the data if it's for its own page
                    if (successState.getAyahs().get(0).getPageId() == this.pageNumber) {
                        adapter.updateData(successState.getAyahs());
                    }
                }
            }
        });
    }

}