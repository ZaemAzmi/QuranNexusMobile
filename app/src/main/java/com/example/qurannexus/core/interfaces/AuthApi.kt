package com.example.qurannexus.core.interfaces
import com.example.qurannexus.features.auth.models.LoginRequest
import com.example.qurannexus.features.auth.models.LoginResponse
import com.example.qurannexus.features.auth.models.RegisterRequest
import com.example.qurannexus.features.auth.models.RegisterResponse
import com.example.qurannexus.features.home.models.User
import com.example.qurannexus.features.home.models.UserResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST


interface AuthApi {
    @POST("api/v1/register")
    fun register(@Body request: RegisterRequest?): Call<RegisterResponse?>?

    @POST("api/v1/login")
    fun login(@Body request: LoginRequest?): Call<LoginResponse?>?
    @POST("api/v1/logout")
    fun logout(@Header("Authorization") token: String): Call<Unit>?
    @GET("api/v1/profile")
    fun getUserProfile(@Header("Authorization") token: String): Call<UserResponse?>?


}