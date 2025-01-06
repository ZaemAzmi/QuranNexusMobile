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
import com.example.qurannexus.features.bookmark.models.Bookmark;
import com.example.qurannexus.features.bookmark.models.BookmarksResponse;
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
    private int surahNumber;
    private ArrayList<Ayah> ayatModels = new ArrayList<>();
    private SurahRecitationByAyatAdapter byAyatAdapter;
    private RecyclerView byAyatRecyclerView;
    private QuranApi quranApi;
    private ArrayList<String> bookmarkedAyahKeys = new ArrayList<>();

    public ByAyatRecitationFragment() {}

    public static ByAyatRecitationFragment newInstance(int surahNumber) {
        ByAyatRecitationFragment fragment = new ByAyatRecitationFragment();
        Bundle args = new Bundle();
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
            fetchBookmarksAndVerses(surahNumber);
            fetchVersesByAyat(surahNumber);
        }
        return view;
    }

    private void fetchBookmarksAndVerses(int surahIndex) {
        // Fetch bookmarks first
//        quranApi.getBookmarks("verse").enqueue(new Callback<BookmarksResponse>() {
//            @Override
//            public void onResponse(Call<BookmarksResponse> call, Response<BookmarksResponse> response) {
//                if (response.isSuccessful() && response.body() != null) {
//                    List<Bookmark> bookmarks = response.body().getBookmarks();
//                    for (Bookmark bookmark : bookmarks) {
//                        bookmarkedAyahKeys.add(bookmark.getAyahKey());
//                    }
//                    // Now fetch verses
//                    fetchVersesByAyat(surahIndex);
//                }
//            }
//
//            @Override
//            public void onFailure(Call<BookmarksResponse> call, Throwable t) {
//                Log.e("ByAyatRecitationFragment", "Failed to fetch bookmarks", t);
//                // Still fetch verses even if bookmarks fail
//                fetchVersesByAyat(surahIndex);
//            }
//        });
    }

    private void fetchVersesByAyat(int surahIndex) {
        quranApi.getVersesBySurah(surahIndex).enqueue(new Callback<AyahRecitationModel>() {
            @Override
            public void onResponse(Call<AyahRecitationModel> call, Response<AyahRecitationModel> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Ayah> ayahs = response.body().getData();
                    ayatModels.clear();

                    for (Ayah ayah : ayahs) {
                        // Mark as bookmarked if its AyahKey exists in bookmarkedAyahKeys
                        ayah.setBookmarked(bookmarkedAyahKeys.contains(ayah.getAyahKey()));
                        ayatModels.add(ayah);
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
