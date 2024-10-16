package com.example.qurannexus.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class SurahModel implements Parcelable {
    public SurahModel(String surahName,
                      String arabicSurahName,
                      String surahNumber,
                      String surahMeaning,
                      String numberOfAyahs,
                      boolean isBookmarked) {
        this.surahName = surahName;
        this.arabicSurahName = arabicSurahName;
        this.surahNumber = surahNumber;
        this.surahMeaning = surahMeaning;
        this.ayatNumber = numberOfAyahs;
        this.isBookmarked = isBookmarked;
    }
    @SerializedName("Name")
    String surahName;
    @SerializedName("Arabic Name")
    String arabicSurahName;
    @SerializedName("Id")
    String surahNumber;
    @SerializedName("Name Meaning")
    String surahMeaning;
    @SerializedName("Number of ayahs")
    String ayatNumber;
    private boolean isBookmarked;
    public String getName() {
        return surahName;
    }

    public void setName(String name) {
        this.surahName = name;
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

    @Override
    public String toString(){
       return "Surah " + surahNumber + ": " + surahName;
    }
}
