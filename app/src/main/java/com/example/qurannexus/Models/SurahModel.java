package com.example.qurannexus.Models;

import android.os.Parcel;
import android.os.Parcelable;

public class SurahModel implements Parcelable {
    public SurahModel(String surahName,
                      String arabicSurahName,
                      String surahNumber,
                      String surahMeaning,
                      String ayatNumber,
                      boolean isBookmarked) {
        this.surahName = surahName;
        this.arabicSurahName = arabicSurahName;
        this.surahNumber = surahNumber;
        this.surahMeaning = surahMeaning;
        this.ayatNumber = ayatNumber;
        this.isBookmarked = isBookmarked;
    }

    String surahName;
    String arabicSurahName;
    String surahNumber;
    String surahMeaning;
    String ayatNumber;
    private boolean isBookmarked;
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

    public String getAyatNumber() {
        return ayatNumber;
    }

    public void setAyatNumber(String ayatNumber) {
        this.ayatNumber = ayatNumber;
    }

    public boolean isBookmarked() {
        return isBookmarked;
    }

    public void setBookmarked(boolean bookmarked) {
        isBookmarked = bookmarked;
    }
    protected SurahModel(Parcel in) {
        surahName = in.readString();
        surahMeaning = in.readString();
        arabicSurahName = in.readString();
        ayatNumber = in.readString();
        surahNumber = in.readString();
    }

    public static final Creator<SurahModel> CREATOR = new Creator<SurahModel>() {
        @Override
        public SurahModel createFromParcel(Parcel in) {
            return new SurahModel(in);
        }

        @Override
        public SurahModel[] newArray(int size) {
            return new SurahModel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(surahName);
        dest.writeString(surahMeaning);
        dest.writeString(arabicSurahName);
        dest.writeString(ayatNumber);
        dest.writeString(surahNumber);
    }
}
