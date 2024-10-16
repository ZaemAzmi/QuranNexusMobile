package com.example.qurannexus.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Ayah {
    @SerializedName("Id")
    private String Id;
    @SerializedName("Surah Id")
    private String SurahId;
    @SerializedName("Ayah Index")
    private String AyahIndex;
    @SerializedName("Ayah Key")
    private String AyahKey;
    @SerializedName("Page Id")
    private String PageId;
    @SerializedName("Juz Id")
    private String JuzId;
    @SerializedName("Bismillah")
    private String Bismillah;
    @SerializedName("Arabic Text")
    private String ArabicText;
    @SerializedName("Words")
    private List<Word> Words;
    @SerializedName("Translations")
    private List<Translation> Translations;

    public Ayah(String id,
                String surahId,
                String ayahIndex,
                String ayahKey,
                String pageId,
                String juzId,
                String bismillah,
                String arabicText,
                List<Word> words,
                List<Translation> translations) {
        Id = id;
        SurahId = surahId;
        AyahIndex = ayahIndex;
        AyahKey = ayahKey;
        PageId = pageId;
        JuzId = juzId;
        Bismillah = bismillah;
        ArabicText = arabicText;
        Words = words;
        Translations = translations;
    }

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
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

    public String getPageId() {
        return PageId;
    }

    public void setPageId(String pageId) {
        PageId = pageId;
    }

    public String getJuzId() {
        return JuzId;
    }

    public void setJuzId(String juzId) {
        JuzId = juzId;
    }

    public String getBismillah() {
        return Bismillah;
    }

    public void setBismillah(String bismillah) {
        Bismillah = bismillah;
    }

    public String getArabicText() {
        return ArabicText;
    }

    public void setArabicText(String arabicText) {
        ArabicText = arabicText;
    }

    public List<Word> getWords() {
        return Words;
    }

    public void setWords(List<Word> words) {
        Words = words;
    }

    public List<Translation> getTranslations() {
        return Translations;
    }

    public void setTranslations(List<Translation> translations) {
        Translations = translations;
    }
}
