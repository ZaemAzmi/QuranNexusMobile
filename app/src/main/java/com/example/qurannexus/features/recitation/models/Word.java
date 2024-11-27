package com.example.qurannexus.features.recitation.models;

import com.google.gson.annotations.SerializedName;

public class Word {
    @SerializedName("Id")
    private String Id;
    @SerializedName("Surah Id")
    private String SurahId;
    @SerializedName("Ayah Index")
    private String AyahIndex;
    @SerializedName("Word Index")
    private String WordIndex;
    @SerializedName("Ayah Key")
    private String AyahKey;
    @SerializedName("Word Key")
    private String WordKey;
    @SerializedName("Page Id")
    private String PageId;
    @SerializedName("Line Number")
    private int LineNumber;
    @SerializedName("Text")
    private String Text;
    @SerializedName("Translation")
    private String Translation;

    public String getTranslation() {
        return Translation;
    }

    public void setTranslation(String translation) {
        Translation = translation;
    }

    public String getTransliteration() {
        return Transliteration;
    }

    public void setTransliteration(String transliteration) {
        Transliteration = transliteration;
    }

    @SerializedName("Transliteration")
    private String Transliteration;
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

    public String getWordIndex() {
        return WordIndex;
    }

    public void setWordIndex(String wordIndex) {
        WordIndex = wordIndex;
    }

    public String getAyahKey() {
        return AyahKey;
    }

    public void setAyahKey(String ayahKey) {
        AyahKey = ayahKey;
    }

    public String getWordKey() {
        return WordKey;
    }

    public void setWordKey(String wordKey) {
        WordKey = wordKey;
    }

    public String getPageId() {
        return PageId;
    }

    public void setPageId(String pageId) {
        PageId = pageId;
    }

    public int getLineNumber() {
        return LineNumber;
    }

    public void setLineNumber(int lineNumber) {
        LineNumber = lineNumber;
    }

    public String getText() {
        return Text;
    }

    public void setText(String text) {
        Text = text;
    }
}
