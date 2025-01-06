package com.example.qurannexus.core.network;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiService {

    private static Retrofit quranRetrofit = null;
    private static Retrofit prayerTimesRetrofit = null;
    private static final String LOCAL_API_URL = "http://10.0.2.2:8000";
    private static final String PRAYER_TIMES_API_URL = "https://api.aladhan.com/";
    private static String authToken = null;

    public static Retrofit getQuranClient() {
        if (quranRetrofit == null) {
            quranRetrofit = new Retrofit.Builder()
                    .baseUrl(LOCAL_API_URL)
                    .client(createAuthenticatedClient())
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

    public static void setAuthToken(String token) {
        authToken = token;
    }

    private static OkHttpClient createAuthenticatedClient() {
        return new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request originalRequest = chain.request();

                        // If we have an auth token, add it to the request
                        if (authToken != null && !authToken.isEmpty()) {
                            Request newRequest = originalRequest.newBuilder()
                                    .header("Authorization", "Bearer " + authToken)
                                    .build();
                            return chain.proceed(newRequest);
                        }

                        return chain.proceed(originalRequest);
                    }
                })
                .build();
    }

    public static void clearInstance() {
        quranRetrofit = null;
        authToken = null;
    }
}
