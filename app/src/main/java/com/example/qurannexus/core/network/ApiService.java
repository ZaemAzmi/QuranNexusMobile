package com.example.qurannexus.core.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

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
    private static final String LOCAL_API_URL = "http://10.0.2.2:8000/api/v1/mobile/";
//    private static final String LOCAL_API_URL = "http://192.168.0.27:8000";
    private static final String PRAYER_TIMES_API_URL = "https://api.aladhan.com/";
    private static String authToken = null;

    private static class SafeStringDeserializer implements JsonDeserializer<String> {
        @Override
        public String deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            if (json.isJsonNull()) return null;

            try {
                String text = json.getAsString();
                // Don't filter out ANY Arabic characters or diacritics
                // Only remove truly problematic control characters that break rendering
                return text;
            } catch (Exception e) {
                return "";
            }
        }
    }

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
            quranRetrofit = createRetrofit(LOCAL_API_URL);
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
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

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

    private static OkHttpClient createAuthenticatedClient() {
        // Add logging interceptor
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        return new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request originalRequest = chain.request();

                        // If we have an auth token, add it to the request
                        if (authToken != null && !authToken.isEmpty()) {
                            Request newRequest = originalRequest.newBuilder()
                                    .header("Authorization", "Bearer " + authToken)
                                    .build();
                            Log.d("ApiService", "Adding auth header: Bearer " + authToken);
                            return chain.proceed(newRequest);
                        }

                        Log.d("ApiService", "No auth token available");
                        return chain.proceed(originalRequest);
                    }
                })
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public static void clearInstance() {
        quranRetrofit = null;
        authToken = null;
    }

    public static String getAuthToken() {
        return authToken;
    }
}
