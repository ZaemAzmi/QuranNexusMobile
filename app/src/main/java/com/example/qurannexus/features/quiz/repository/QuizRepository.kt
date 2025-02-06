package com.example.qurannexus.features.quiz.repository

import android.util.Log
import com.example.qurannexus.core.interfaces.AuthApi
import com.example.qurannexus.core.interfaces.QuizApi
import com.example.qurannexus.core.network.ApiService
import com.example.qurannexus.features.home.models.User
import com.example.qurannexus.features.quiz.models.AnswerResponse
import com.example.qurannexus.features.quiz.models.BatchAnswer
import com.example.qurannexus.features.quiz.models.BatchAnswerResponse
import com.example.qurannexus.features.quiz.models.FinishQuizResponse
import com.example.qurannexus.features.quiz.models.QuizProgressResponse
import com.example.qurannexus.features.quiz.models.QuizResponse
import com.example.qurannexus.features.quiz.models.StartQuizRequest
import com.example.qurannexus.features.quiz.models.SubmitAnswerRequest
import com.example.qurannexus.features.quiz.models.SubmitBatchRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class QuizRepository @Inject constructor(private val api: QuizApi, private val authApi: AuthApi) {
    private fun getAuthHeader(): String {
        val token = ApiService.getAuthToken()
        return "Bearer $token"
    }
    suspend fun getUserProfile(token: String): User? {
        return withContext(Dispatchers.IO) {
            try {
                val response = authApi.getUserProfile(token)?.execute()
                if (response?.isSuccessful == true) {
                    response?.body()?.data
                } else {
                    if (response != null) {
                        Log.e("QuizRepository", "Error: ${response.errorBody()?.string()}")
                    }
                    null
                }
            } catch (e: Exception) {
                Log.e("QuizRepository", "Error getting user profile", e)
                null
            }
        }
    }
    suspend fun startQuiz(request: StartQuizRequest): QuizResponse? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("QuizRepository", "Starting quiz with request: $request")
                val response = api.startQuiz(getAuthHeader(), request).execute()

                if (response.isSuccessful) {
                    response.body()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("QuizRepository", "Start quiz failed: $errorBody")
                    Log.d("QuizRepository", "Auth token used: ${getAuthHeader()}")
                    null
                }
            } catch (e: Exception) {
                Log.e("QuizRepository", "Error starting quiz", e)
                null
            }
        }
    }

    suspend fun submitBatchAnswers(surahId: String, answers: List<BatchAnswer>): BatchAnswerResponse? {
        return withContext(Dispatchers.IO) {
            try {
                // First ensure quiz is started
                val startRequest = StartQuizRequest(surahId = surahId)
                val startResponse = api.startQuiz(getAuthHeader(), startRequest).execute()

                if (!startResponse.isSuccessful) {
                    Log.e("QuizRepository", "Failed to start quiz before submitting answers")
                    return@withContext null
                }

                val submitRequests = answers.map { answer ->
                    SubmitAnswerRequest(
                        surah_id = surahId,
                        ayah_key = answer.ayahKey,
                        question_id = answer.questionId,
                        selected_answer = answer.selectedAnswer,
                        correct_answer = answer.correctAnswer
                    )
                }

                val request = SubmitBatchRequest(answers = submitRequests)
                val response = api.submitBatchAnswers(getAuthHeader(), surahId, request).execute()

                if (response.isSuccessful) {
                    response.body()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("QuizRepository", "Submit batch failed: $errorBody")
                    Log.d("QuizRepository", "Auth token used: ${getAuthHeader()}")
                    null
                }
            } catch (e: Exception) {
                Log.e("QuizRepository", "Error submitting batch", e)
                null
            }
        }
    }

    suspend fun finishQuiz(surahId: String): FinishQuizResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.finishQuiz(getAuthHeader(), surahId).execute()
                if (response.isSuccessful) {
                    response.body()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("QuizRepository", "Finish quiz failed: $errorBody")
                    Log.d("QuizRepository", "Auth token used: ${getAuthHeader()}")
                    null
                }
            } catch (e: Exception) {
                Log.e("QuizRepository", "Error finishing quiz", e)
                null
            }
        }
    }
}