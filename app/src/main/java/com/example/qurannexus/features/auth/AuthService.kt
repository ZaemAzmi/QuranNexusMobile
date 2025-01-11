package com.example.qurannexus.features.auth

import android.content.Context
import android.util.Log
import com.example.qurannexus.core.interfaces.AuthApi
import com.example.qurannexus.core.interfaces.AuthCallback
import com.example.qurannexus.core.network.ApiService
import com.example.qurannexus.features.auth.models.LoginRequest
import com.example.qurannexus.features.auth.models.LoginResponse
import com.example.qurannexus.features.auth.models.RegisterRequest
import com.example.qurannexus.features.auth.models.RegisterResponse
import com.example.qurannexus.features.home.models.User
import com.example.qurannexus.features.home.models.UserResponse
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AuthService {
    companion object {
        private const val PREF_NAME = "UserPrefs"
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
    }

    private val authApi: AuthApi
//    private val gson = GsonBuilder()
//        .setLenient()
//        .create()

    init {
        // Reuse ApiService to create Retrofit instance for authentication
//        val retrofit = ApiService.createRetrofit("http://192.168.1.10:8000/")
        val retrofit = ApiService.createRetrofit("http://10.0.2.2:8000")
        authApi = retrofit.create(AuthApi::class.java)
    }

    fun register(context: Context, request: RegisterRequest, callback: AuthCallback) {
        authApi.register(request)?.enqueue(object : Callback<RegisterResponse?> {
            override fun onResponse(call: Call<RegisterResponse?>, response: Response<RegisterResponse?>) {
                if (response.isSuccessful && response.body() != null) {
                    callback.onSuccess(response.body()!!.message)
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("AuthService", "Registration failed: $errorBody")
                    callback.onError("Registration failed: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<RegisterResponse?>, t: Throwable) {
                Log.e("AuthService", "Registration error", t)
                callback.onError("Error: ${t.message}")
            }
        })
    }

    fun login(context: Context, request: LoginRequest, callback: AuthCallback) {
        Log.d("AuthService", "Attempting login with email: ${request.email}")

        authApi.login(request)?.enqueue(object : Callback<LoginResponse?> {
            override fun onResponse(call: Call<LoginResponse?>, response: Response<LoginResponse?>) {
                if (response.isSuccessful && response.body() != null) {
                    val token = response.body()!!.token
                    Log.d("AuthService", "Received token format: $token")
                    Log.d("AuthService", "Token parts: ${token.split('|')}")
                    // Save token to SharedPreferences
                    context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                        .edit()
                        .putString("token", token)
                        .apply()

                    // Set token in ApiService
                    ApiService.setAuthToken(token)

                    Log.d("AuthService", "Login successful, token received")
                    Log.d("AuthService", "Login successful,${token.toString()}")

                    callback.onSuccess(token)
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("AuthService", "Login failed: $errorBody")
                    callback.onError("Login failed: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<LoginResponse?>, t: Throwable) {
                Log.e("AuthService", "Login network error", t)
                callback.onError("Network error: ${t.message}")
            }
        })
    }

    fun getUserProfile(token: String, callback: (User?) -> Unit) {
        Log.d("AuthService", "Fetching user profile with token: $token")

        authApi.getUserProfile("Bearer $token")?.enqueue(object : Callback<UserResponse?> {
            override fun onResponse(call: Call<UserResponse?>, response: Response<UserResponse?>) {
                // Log the raw response for debugging
                val rawResponse = response.raw().toString()
                Log.d("AuthService", "Raw response: $rawResponse")

                if (response.isSuccessful) {
                    val responseBody = response.body()
                    Log.d("AuthService", "Response body: $responseBody")
                    if (responseBody?.data != null) {
                        Log.d("AuthService", "Profile fetched successfully: ${responseBody.data}")
                        callback(responseBody.data)
                    } else {
                        Log.e("AuthService", "Response successful but data is null")
                        callback(null)
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("AuthService", "Profile fetch failed: $errorBody")
                    Log.e("AuthService", "Response code: ${response.code()}")
                    callback(null)
                }
            }

            override fun onFailure(call: Call<UserResponse?>, t: Throwable) {
                Log.e("AuthService", "Failed to get user profile", t)
                callback(null)
            }
        })
    }

    fun logout(context: Context, callback: (Boolean) -> Unit) {
        // Get stored token
        val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)

        if (token == null) {
            // If no token exists, consider it already logged out
            clearUserData(context)
            callback(true)
            return
        }

        // Make the logout API call
        authApi.logout("Bearer $token")?.enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                if (response.isSuccessful) {
                    // Clear all stored user data
                    clearUserData(context)
                    callback(true)
                } else {
                    // If server call fails, still clear local data
                    clearUserData(context)
                    callback(false)
                }
            }

            override fun onFailure(call: Call<Unit>, t: Throwable) {
                // On network failure, still clear local data
                clearUserData(context)
                callback(false)
            }
        })
    }

    private fun clearUserData(context: Context) {
        // Clear SharedPreferences
        context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

        // Clear API service token
        ApiService.clearInstance()
    }

    private fun saveAuthToken(context: Context, token: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TOKEN, token)
            .apply()
    }

    fun getStoredToken(context: Context): String? {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, null)
    }
}