package com.example.qurannexus.models;

import com.google.gson.annotations.SerializedName;

public class AyatModel {
    private String arabicScript; // This will now be the complete verse
    private String englishTranslation;
    private String ayatNumber; // This should be in the "chapter:verse" format
    private boolean isBookmarked;

    // Constructor
    public AyatModel(String arabicScript, String englishTranslation, String ayatNumber) {
        this.arabicScript = arabicScript;
        this.englishTranslation = englishTranslation;
        this.ayatNumber = ayatNumber;
        this.isBookmarked = false; // Default value
    }

    // Getters and Setters
    public String getArabicScript() {
        return arabicScript;
    }

    public String getEnglishTranslation() {
        return englishTranslation;
    }

    public String getAyatNumber() {
        return ayatNumber;
    }

    public boolean isBookmarked() {
        return isBookmarked;
    }

    public void setBookmarked(boolean bookmarked) {
        isBookmarked = bookmarked;
    }
}

