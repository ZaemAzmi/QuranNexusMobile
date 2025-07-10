package com.example.qurannexus.core.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.qurannexus.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiService {

    private static Retrofit quranRetrofit = null;
    private static Retrofit prayerTimesRetrofit = null;
    // Get the base URL from your single source of truth
    private static final String QURAN_API_URL = BuildConfig.BASE_URL;

    private static final String PRAYER_TIMES_API_URL = "https://api.aladhan.com/";
    private static String authToken = null;

    // Create custom Gson instance
    private static Gson createGson() {
        return new GsonBuilder()
                // Don't register the custom SafeStringDeserializer for now - it might be filtering diacritics
                //.registerTypeAdapter(String.class, new SafeStringDeserializer())
                .setLenient()
                .create();
    }

    public static Retrofit getQuranClient() {
        if (quranRetrofit == null) {
            quranRetrofit = createRetrofit(QURAN_API_URL);
        }
        return quranRetrofit;
    }

    public static Retrofit getPrayerTimesClient() {
        if (prayerTimesRetrofit == null) {
            prayerTimesRetrofit = createRetrofit(PRAYER_TIMES_API_URL);
        }
        return prayerTimesRetrofit;
    }

    private static OkHttpClient createHttpClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Custom logger to skip HTML content

        return new OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public static Retrofit createRetrofit(String baseUrl) {
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(createHttpClient())
                .addConverterFactory(GsonConverterFactory.create(createGson()))  // Use custom Gson
                .build();
    }
    public static void setAuthToken(String token) {
        Log.d("ApiService", "Setting auth token: " + token);
        authToken = token;
        // Force recreation of the Retrofit instance with new token
        quranRetrofit = null;
    }

    public static void clearInstance() {
        quranRetrofit = null;
        authToken = null;
    }

    public static String getAuthToken() {
        return authToken;
    }
}
