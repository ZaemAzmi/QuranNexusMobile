package com.example.qurannexus.features.recitation.models;

import com.google.gson.annotations.SerializedName;

public class Translation {
    @SerializedName("Id")
    private String Id;
    @SerializedName("Translation Info Id")
    private String TranslationInfoId;
    @SerializedName("Surah Id")
    private String SurahId;
    @SerializedName("Ayah Index")
    private String AyahIndex;
    @SerializedName("Ayah Key")
    private String AyahKey;
    @SerializedName("Text")
    private String Text;

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    public String getTranslationInfoId() {
        return TranslationInfoId;
    }

    public void setTranslationInfoId(String translationInfoId) {
        TranslationInfoId = translationInfoId;
    }

    public String getSurahId() {
        return SurahId;
    }

    public void setSurahId(String surahId) {
        SurahId = surahId;
    }

    public String getAyahIndex() {
        return AyahIndex;
    }

    public void setAyahIndex(String ayahIndex) {
        AyahIndex = ayahIndex;
    }

    public String getAyahKey() {
        return AyahKey;
    }

    public void setAyahKey(String ayahKey) {
        AyahKey = ayahKey;
    }

    public String getText() {
        return Text;
    }

    public void setText(String text) {
        Text = text;
    }
}
