package com.example.qurannexus.features.quiz.repository

import com.example.qurannexus.core.interfaces.QuizApi
import com.example.qurannexus.features.quiz.models.AnswerResponse
import com.example.qurannexus.features.quiz.models.FinishQuizResponse
import com.example.qurannexus.features.quiz.models.QuizProgressResponse
import com.example.qurannexus.features.quiz.models.QuizResponse
import com.example.qurannexus.features.quiz.models.StartQuizRequest
import com.example.qurannexus.features.quiz.models.SubmitAnswerRequest
import javax.inject.Inject

class QuizRepository @Inject constructor(private val api: QuizApi) {

    fun startQuiz(request: StartQuizRequest): QuizResponse? {
        return try {
            val response = api.startQuiz(request.surah_id).execute()
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun submitAnswer(request: SubmitAnswerRequest): AnswerResponse? {
        return try {
            val response = api.submitAnswer(
                request.surah_id,
                request.ayah_key,
                request.question_id,
                request.selected_answer,
                request.correct_answer
            ).execute()
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getQuizProgress(surahId: String): QuizProgressResponse? {
        return try {
            val response = api.getQuizProgress(surahId).execute()
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun finishQuiz(surahId: String): FinishQuizResponse? {
        return try {
            val response = api.finishQuiz(surahId).execute()
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}