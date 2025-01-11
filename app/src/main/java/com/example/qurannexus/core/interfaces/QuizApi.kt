package com.example.qurannexus.core.interfaces

import com.example.qurannexus.features.quiz.models.AnswerResponse
import com.example.qurannexus.features.quiz.models.BatchAnswerResponse
import com.example.qurannexus.features.quiz.models.FinishQuizResponse
import com.example.qurannexus.features.quiz.models.QuizProgressResponse
import com.example.qurannexus.features.quiz.models.QuizResponse
import com.example.qurannexus.features.quiz.models.StartQuizRequest
import com.example.qurannexus.features.quiz.models.SubmitBatchRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface QuizApi {
    @POST("api/v1/quiz/start")
    fun startQuiz(
        @Header("Authorization") auth: String,
        @Body request: StartQuizRequest  // Changed to use proper request object
    ): Call<QuizResponse>

    @POST("api/v1/quiz/submit-batch")
    fun submitBatchAnswers(
        @Header("Authorization") auth: String,
        @Query("surah_id") surahId: String,
        @Body request: SubmitBatchRequest
    ): Call<BatchAnswerResponse>

    @POST("api/v1/quiz/finish")
    fun finishQuiz(
        @Header("Authorization") auth: String,
        @Query("surah_id") surahId: String
    ): Call<FinishQuizResponse>
}