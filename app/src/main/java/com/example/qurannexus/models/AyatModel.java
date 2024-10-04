package com.example.qurannexus.models;

public class AyatModel {
    public String arabicScript;
    public String englishTranslation;
    public String ayatNumber;
    int ayatIndex;
    int surahIndex;
    boolean isBookmarked;
    public AyatModel(String arabicScript,
                     String englishTranslation,
                     String ayatNumber,
                     int surahIndex,
                     int ayatIndex,
                     boolean isBookmarked) {
        this.arabicScript = arabicScript;
        this.englishTranslation = englishTranslation;
        this.ayatNumber = ayatNumber;
        this.surahIndex = surahIndex;
        this.ayatIndex = ayatIndex;
        this.isBookmarked = isBookmarked;
    }
    public int getAyatIndex() {
        return ayatIndex;
    }

    public int getSurahIndex() {
        return surahIndex;
    }
    public boolean isBookmarked() {
        return isBookmarked;
    }

    public void setBookmarked(boolean bookmarked) {
        isBookmarked = bookmarked;
    }
}
