package com.example.qurannexus.interfaces
import com.example.qurannexus.models.LoginRequest
import com.example.qurannexus.models.LoginResponse
import com.example.qurannexus.models.RegisterRequest
import com.example.qurannexus.models.RegisterResponse
import com.example.qurannexus.models.User
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

    @GET("api/v1/profile")
    fun getUserProfile(@Header("Authorization") token: String): Call<User?>?
}