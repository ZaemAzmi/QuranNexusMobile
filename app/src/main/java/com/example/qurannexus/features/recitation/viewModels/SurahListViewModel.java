// Create new file: SurahListViewModel.java
package com.example.qurannexus.features.recitation.viewModels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.qurannexus.core.utils.QuranMetadata;
import com.example.qurannexus.core.utils.SurahDetails;
import com.example.qurannexus.features.recitation.models.SurahModel;

import java.util.ArrayList;
import java.util.List;

public class SurahListViewModel extends ViewModel {

    // This holds the original, complete list of all Surahs. It's loaded only once.
    private final List<SurahModel> allSurahs = new ArrayList<>();

    // This is the LiveData that the UI will observe. It holds the list to be displayed.
    private final MutableLiveData<List<SurahModel>> _surahList = new MutableLiveData<>();
    public LiveData<List<SurahModel>> getSurahList() {
        return _surahList;
    }

    // This holds the current search query, so it's not lost on rotation etc.
    private String currentQuery = "";

    public SurahListViewModel() {
        loadSurahs();
    }

    private void loadSurahs() {
        // This logic is moved from the fragment to the ViewModel.
        QuranMetadata metadata = QuranMetadata.Companion.getInstance();
        for (int i = 1; i <= 114; i++) {
            SurahDetails details = metadata.getSurahDetails(i);
            if (details != null) {
                SurahModel model = new SurahModel(
                        details.getEnglishName(),
                        details.getArabicName(),
                        String.valueOf(details.getSurahIndex()),
                        details.getTranslationName(),
                        String.valueOf(details.getNumberOfVerses()),
                        false
                );
                allSurahs.add(model);
            }
        }
        // Post the initial full list to the LiveData
        _surahList.setValue(new ArrayList<>(allSurahs));
    }

    public void filterSurahList(String query) {
        currentQuery = query.toLowerCase().trim();
        if (currentQuery.isEmpty()) {
            _surahList.setValue(new ArrayList<>(allSurahs));
            return;
        }

        ArrayList<SurahModel> filteredList = new ArrayList<>();
        for (SurahModel surah : allSurahs) {
            if (surah.getName().toLowerCase().contains(currentQuery) ||
                    surah.getArabicSurahName().contains(currentQuery) ||
                    surah.getSurahNumber().equals(currentQuery)) {
                filteredList.add(surah);
            }
        }
        _surahList.setValue(filteredList);
    }
}