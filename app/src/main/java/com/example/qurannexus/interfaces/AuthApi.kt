package com.example.qurannexus.interfaces
import com.example.qurannexus.models.LoginRequest
import com.example.qurannexus.models.LoginResponse
import com.example.qurannexus.models.RegisterRequest
import com.example.qurannexus.models.RegisterResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST


interface AuthApi {

    @POST("api/register")
    fun register(@Body request: RegisterRequest?): Call<RegisterResponse?>?

    @POST("api/login")
    fun login(@Body request: LoginRequest?): Call<LoginResponse?>?
}