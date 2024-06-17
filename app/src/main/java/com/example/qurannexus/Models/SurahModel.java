package com.example.qurannexus.Models;

import io.realm.RealmObject;

public class SurahModel {
    public SurahModel(String surahName, String arabicSurahName, String surahNumber, String surahMeaning, int ayatNumber) {
        this.surahName = surahName;
        this.arabicSurahName = arabicSurahName;
        this.surahNumber = surahNumber;
        this.surahMeaning = surahMeaning;
        this.ayatNumber = ayatNumber;
    }
    String surahName;
    String arabicSurahName;
    String surahNumber;
    String surahMeaning;
    int ayatNumber;

    public String getSurahName() {
        return surahName;
    }

    public void setSurahName(String surahName) {
        this.surahName = surahName;
    }

    public String getArabicSurahName() {
        return arabicSurahName;
    }

    public void setArabicSurahName(String arabicSurahName) {
        this.arabicSurahName = arabicSurahName;
    }

    public String getSurahNumber() {
        return surahNumber;
    }

    public void setSurahNumber(String surahNumber) {
        this.surahNumber = surahNumber;
    }

    public String getSurahMeaning() {
        return surahMeaning;
    }

    public void setSurahMeaning(String surahMeaning) {
        this.surahMeaning = surahMeaning;
    }

    public int getAyatNumber() {
        return ayatNumber;
    }

    public void setAyatNumber(int ayatNumber) {
        this.ayatNumber = ayatNumber;
    }
}
