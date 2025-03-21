package com.example.qurannexus.core.interfaces
import com.example.qurannexus.features.auth.ForgotPasswordRequest
import com.example.qurannexus.features.auth.ForgotPasswordResponse
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
    @POST("register")
    fun register(@Body request: RegisterRequest?): Call<RegisterResponse?>?
    @POST("login")
    fun login(@Body request: LoginRequest?): Call<LoginResponse?>?
    @POST("logout")
    fun logout(@Header("Authorization") token: String): Call<Unit>?
    @GET("profile")
    fun getUserProfile(@Header("Authorization") token: String): Call<UserResponse?>?
    @POST("forgot-password")
    fun forgotPassword(@Body request: ForgotPasswordRequest): Call<ForgotPasswordResponse>

}