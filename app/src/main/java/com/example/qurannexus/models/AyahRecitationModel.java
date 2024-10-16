package com.example.qurannexus.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AyahRecitationModel {
    private List<Ayah> data;

    public List<Ayah> getData() {
        return data;
    }

    public void setData(List<Ayah> data) {
        this.data = data;
    }


}

