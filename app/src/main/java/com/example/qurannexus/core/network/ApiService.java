package com.example.qurannexus.core.network;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiService {

    private static Retrofit quranRetrofit = null;
    private static Retrofit prayerTimesRetrofit = null;
    private static final String LOCAL_API_URL = "http://10.0.2.2:8000";
    private static final String PRAYER_TIMES_API_URL = "https://api.aladhan.com/";

    public static Retrofit getQuranClient() {
        if (quranRetrofit == null) {
            quranRetrofit = new Retrofit.Builder()
                    .baseUrl(LOCAL_API_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return quranRetrofit;
    }

    public static Retrofit getPrayerTimesClient() {
        if (prayerTimesRetrofit == null) {
            prayerTimesRetrofit = new Retrofit.Builder()
                    .baseUrl(PRAYER_TIMES_API_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return prayerTimesRetrofit;
    }
}
