package com.example.qurannexus.features.quiz.models

import android.app.Application
import android.util.Log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qurannexus.features.quiz.repository.QuizRepository
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val context: Application,
    private val quizRepository: QuizRepository
) : ViewModel() {

    private val _quizState = MutableStateFlow<QuizState>(QuizState.Initial)
    val quizState: StateFlow<QuizState> = _quizState

    private var questionList: List<QuestionData> = emptyList()
    private var currentQuestionIndex = 0
    private var currentSurahId: String? = null
    private var currentAyahKey: String? = null

    private val _currentQuestion = MutableStateFlow<QuestionData?>(null)
    val currentQuestion: StateFlow<QuestionData?> = _currentQuestion

    fun loadQuestions(surahNumber: Int) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val fileName = "surah_$surahNumber.json"
                    val json = loadJsonFromAssets(fileName)
                    val gson = Gson()
                    val quizData = gson.fromJson(json, JsonObject::class.java)
                    currentSurahId = quizData["surah_id"].asString

                    val ayahs = quizData["ayahs"]?.asJsonArray
                    if (ayahs != null) {
                        questionList = ayahs.flatMap { ayah ->
                            val ayahKey = ayah.asJsonObject["ayah_key"].asString
                            ayah.asJsonObject["questions"].asJsonArray.map { question ->
                                val questionData = gson.fromJson(question, QuestionData::class.java)
                                currentAyahKey = ayahKey
                                questionData
                            }
                        }
                        currentQuestionIndex = 0
                        _currentQuestion.value = questionList.firstOrNull()

                        // Start the quiz in the backend
                        currentSurahId?.let { startQuiz(it) }
                    }
                }
            } catch (e: Exception) {
                _quizState.value = QuizState.Error("Error loading questions: ${e.message}")
            }
        }
    }

    private fun startQuiz(surahId: String) {
        viewModelScope.launch {
            try {
                Log.d("QuizViewModel", "Starting quiz for surah: $surahId")
                val request = StartQuizRequest(surah_id = surahId)
                Log.d("QuizViewModel", "Request body: $request")

                val response = withContext(Dispatchers.IO) {
                    quizRepository.startQuiz(request)
                }

                Log.d("QuizViewModel", "Quiz start response: $response")

                if (response != null) {
                    _quizState.value = QuizState.Started(response.message)
                } else {
                    _quizState.value = QuizState.Error("Failed to start quiz")
                }
            } catch (e: Exception) {
                Log.e("QuizViewModel", "Error starting quiz", e)
                _quizState.value = QuizState.Error("Error starting quiz: ${e.message}")
            }
        }
    }

    fun submitAnswer(selectedAnswer: String) {
        viewModelScope.launch {
            try {
                Log.d("QuizViewModel", "Submitting answer: $selectedAnswer")
                Log.d("QuizViewModel", "Current surahId: $currentSurahId")
                Log.d("QuizViewModel", "Current ayahKey: $currentAyahKey")
                Log.d("QuizViewModel", "Current questionIndex: $currentQuestionIndex")

                val currentQ = _currentQuestion.value
                if (currentQ != null && currentSurahId != null && currentAyahKey != null) {
                    val request = SubmitAnswerRequest(
                        surah_id = currentSurahId!!,
                        ayah_key = currentAyahKey!!,
                        question_id = currentQuestionIndex,
                        selected_answer = selectedAnswer,
                        correct_answer = currentQ.answer
                    )

                    Log.d("QuizViewModel", "Submit answer request: $request")

                    val response = withContext(Dispatchers.IO) {
                        quizRepository.submitAnswer(request)
                    }

                    Log.d("QuizViewModel", "Submit answer response: $response")

                    if (response != null) {
                        _quizState.value = QuizState.AnswerSubmitted(
                            isCorrect = response.isCorrect,
                            message = response.message
                        )
                        if (currentQuestionIndex < questionList.size - 1) {
                            loadNextQuestion()
                        } else {
                            finishQuiz()
                        }
                    } else {
                        _quizState.value = QuizState.Error("Failed to submit answer")
                    }
                }
            } catch (e: Exception) {
                Log.e("QuizViewModel", "Error submitting answer", e)
                _quizState.value = QuizState.Error("Error submitting answer: ${e.message}")
            }
        }
    }

    fun loadNextQuestion() {
        currentQuestionIndex++
        if (currentQuestionIndex < questionList.size) {
            _currentQuestion.value = questionList[currentQuestionIndex]
        }
    }

    private fun finishQuiz() {
        viewModelScope.launch {
            try {
                currentSurahId?.let { surahId ->
                    val response = withContext(Dispatchers.IO) {
                        quizRepository.finishQuiz(surahId)
                    }
                    if (response != null) {
                        _quizState.value = QuizState.Finished(response.message)
                    } else {
                        _quizState.value = QuizState.Error("Failed to finish quiz")
                    }
                }
            } catch (e: Exception) {
                _quizState.value = QuizState.Error("Error finishing quiz: ${e.message}")
            }
        }
    }

    private fun loadJsonFromAssets(filename: String): String {
        return context.assets.open(filename).bufferedReader().use { it.readText() }
    }
}

sealed class QuizState {
    object Initial : QuizState()
    data class Started(val message: String) : QuizState()
    data class AnswerSubmitted(val isCorrect: Boolean, val message: String) : QuizState()
    data class Finished(val message: String) : QuizState()
    data class Error(val message: String) : QuizState()
}