package com.example.qurannexus.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SurahListResponse {
    @SerializedName("data")
    private List<SurahModel> data;

    public List<SurahModel> getData() {
        return data;
    }

    public void setData(List<SurahModel> data) {
        this.data = data;
    }
}
