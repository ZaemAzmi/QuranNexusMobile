package com.example.qurannexus.features.auth

import android.content.Context
import com.example.qurannexus.core.interfaces.AuthApi
import com.example.qurannexus.core.interfaces.AuthCallback
import com.example.qurannexus.features.auth.models.LoginRequest
import com.example.qurannexus.features.auth.models.LoginResponse
import com.example.qurannexus.features.auth.models.RegisterRequest
import com.example.qurannexus.features.auth.models.RegisterResponse
import com.example.qurannexus.features.home.models.User
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AuthService {

    private val authApi: AuthApi

    init {
        // Create a logging interceptor
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY // Log the request and response body

        // Add the logging interceptor to the OkHttpClient
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        // Create Retrofit instance with logging interceptor
        authApi = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8000") // Replace with your base URL
            .client(client) // Add the OkHttpClient with logging interceptor
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }
    fun register(context: Context, request: RegisterRequest, callback: AuthCallback) {
        val call: Call<RegisterResponse?>? = authApi.register(request)
        call?.enqueue(object : Callback<RegisterResponse?> {
            override fun onResponse(call: Call<RegisterResponse?>, response: Response<RegisterResponse?>) {
                if (response.isSuccessful && response.body() != null) {
                    callback.onSuccess(response.body()!!.message)
                } else {
                    // Log the response error (you can log more details if needed)
                    callback.onError("Registration failed: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<RegisterResponse?>, t: Throwable) {
                callback.onError("Error: " + t.message)
            }
        })
    }

    fun login(context: Context, request: LoginRequest, callback: AuthCallback) {
        val call: Call<LoginResponse?>? = authApi.login(request)
        call?.enqueue(object : Callback<LoginResponse?> {
            override fun onResponse(call: Call<LoginResponse?>, response: Response<LoginResponse?>) {
                if (response.isSuccessful && response.body() != null) {
                    callback.onSuccess(response.body()!!.token)
                } else {
                    callback.onError("Login failed")
                }
            }

            override fun onFailure(call: Call<LoginResponse?>, t: Throwable) {
                callback.onError("Error: " + t.message)
            }
        })
    }

    fun getUserProfile(token: String, callback: (User?) -> Unit) {
        val call: Call<User?>? = authApi.getUserProfile("Bearer $token")
        call?.enqueue(object : Callback<User?> {
            override fun onResponse(call: Call<User?>, response: Response<User?>) {
                if (response.isSuccessful && response.body() != null) {
                    callback(response.body())
                } else {
                    callback(null) // Handle failure case
                }
            }

            override fun onFailure(call: Call<User?>, t: Throwable) {
                callback(null) // Handle network or other failure
            }
        })
    }

}