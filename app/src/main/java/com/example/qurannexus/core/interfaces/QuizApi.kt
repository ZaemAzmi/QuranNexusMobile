package com.example.qurannexus.core.interfaces

import com.example.qurannexus.features.quiz.models.AnswerResponse
import com.example.qurannexus.features.quiz.models.FinishQuizResponse
import com.example.qurannexus.features.quiz.models.QuizProgressResponse
import com.example.qurannexus.features.quiz.models.QuizResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface QuizApi {
    @POST("api/v1/quiz/start")
    fun startQuiz(@Body surahId: String): Call<QuizResponse>

    @POST("api/v1/quiz/answer")
    fun submitAnswer(
        @Query("surah_id") surahId: String,
        @Query("ayah_key") ayahKey: String,
        @Query("question_id") questionId: Int,
        @Query("selected_answer") selectedAnswer: String,
        @Query("correct_answer") correctAnswer: String
    ): Call<AnswerResponse>

    @GET("api/v1/quiz/progress")
    fun getQuizProgress(@Query("surah_id") surahId: String): Call<QuizProgressResponse>

    @POST("api/v1/quiz/finish")
    fun finishQuiz(@Query("surah_id") surahId: String): Call<FinishQuizResponse>
}