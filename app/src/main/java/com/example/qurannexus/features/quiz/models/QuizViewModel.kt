package com.example.qurannexus.features.quiz.models

import android.app.Application
import android.content.Context
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

    companion object {
        private var lastQuizResult: QuizState.Finished? = null
        const val QUESTIONS_PER_BATCH = 10
    }
    private var totalQuestionsInSurah: Int = 0
    private val _quizState = MutableStateFlow<QuizState>(QuizState.Initial)
    val quizState: StateFlow<QuizState> = _quizState

    private val _currentBatch = MutableStateFlow<BatchData?>(null)
    val currentBatch: StateFlow<BatchData?> = _currentBatch

    private val _currentQuestion = MutableStateFlow<QuestionData?>(null)
    val currentQuestion: StateFlow<QuestionData?> = _currentQuestion

    private var questionList: List<QuestionData> = emptyList()
    private val currentBatchAnswers = mutableListOf<BatchAnswer>()
    private var currentSurahId: String? = null
    private val batchScores = mutableMapOf<Int, Score>()
    private val _surahProgress = MutableStateFlow<Map<Int, Score>>(emptyMap())
    val surahProgress: StateFlow<Map<Int, Score>> = _surahProgress
    private val _quizProgress = MutableStateFlow<List<QuizProgress>>(emptyList())
    val quizProgress: StateFlow<List<QuizProgress>> = _quizProgress

    init {
        loadUserQuizProgress()
    }

    private fun loadUserQuizProgress() {
        viewModelScope.launch {
            try {
                // Get token from SharedPreferences
                val token = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                    .getString("token", null)

                if (token != null) {
                    val userProfile = quizRepository.getUserProfile("Bearer $token")
                    userProfile?.quiz_progress?.let { progress ->
                        _quizProgress.value = progress
                    }
                }
            } catch (e: Exception) {
                Log.e("QuizViewModel", "Error loading quiz progress", e)
            }
        }
    }
    fun loadSurahProgress(surahId: String) {
        viewModelScope.launch {
            // You'll need a new repository function and API endpoint for this.
            // Let's assume you have getQuizProgress(surahId) in your repo.
            val progress = quizRepository.getQuizProgress(surahId)
            progress?.batchScores?.let { scoresMap ->
                // Assuming batch_scores is Map<String, BatchScoreDto> from backend
                val scores = scoresMap.mapKeys { it.key.toInt() }
                    .mapValues { Score(it.value.correct, it.value.total) }
                _surahProgress.value = scores
            }
        }
    }
    fun getUserQuizProgress(): StateFlow<List<QuizProgress>> {
        // Reload data when requested
        loadUserQuizProgress()
        return _quizProgress
    }
    fun loadSurah(surahNumber: Int) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val fileName = "surah_$surahNumber.json"
                    val json = loadJsonFromAssets(fileName)

                    val gson = Gson()
                    val quizData = gson.fromJson(json, JsonObject::class.java)
                    currentSurahId = surahNumber.toString()

                    val ayahs = quizData["ayahs"]?.asJsonArray
                    if (ayahs != null) {
                        questionList = ayahs.flatMap { ayah ->
                            val ayahKey = ayah.asJsonObject["ayah_key"].asString
                            ayah.asJsonObject["questions"].asJsonArray.map { question ->
                                val questionObj = question.asJsonObject
                                QuestionData(
                                    question = questionObj["question"].asString,
                                    answer = questionObj["answer"].asString,
                                    options = gson.fromJson(questionObj["options"], Array<String>::class.java).toList(),
                                    translation = questionObj["translation"].asString,
                                    ayahKey = ayahKey
                                )
                            }
                        }
                        totalQuestionsInSurah = questionList.size
                        Log.d("QuizViewModel", "Loaded ${questionList.size} questions")
                        _quizState.value = QuizState.SurahLoaded(
                            chapterNumber = surahNumber,
                            totalQuestions = questionList.size
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("QuizViewModel", "Error loading surah", e)
                _quizState.value = QuizState.Error("Error loading questions: ${e.message}")
            }
        }
    }
    fun getTotalQuestionsInSurah(): Int = totalQuestionsInSurah
    fun startBatch(batchNumber: Int) {
        viewModelScope.launch {
            try {
                val startIndex = (batchNumber - 1) * QUESTIONS_PER_BATCH
                val endIndex = minOf(startIndex + QUESTIONS_PER_BATCH, questionList.size)
                val batchQuestions = questionList.subList(startIndex, endIndex)

                // Clear previous batch data
                currentBatchAnswers.clear()

                // Initialize new batch
                _currentBatch.value = BatchData(
                    batchNumber = batchNumber,
                    currentQuestionNumber = 1,
                    questions = batchQuestions,
                    currentQuestion = batchQuestions.firstOrNull()
                )

                _currentQuestion.value = batchQuestions.firstOrNull()

                // Start quiz in API
                currentSurahId?.let {
                    try {
                        val request = StartQuizRequest(surahId = it)
                        withContext(Dispatchers.IO) {
                            quizRepository.startQuiz(request)
                        }
                    } catch (e: Exception) {
                        Log.e("QuizViewModel", "Error starting quiz on API", e)
                    }
                }

                _quizState.value = QuizState.BatchStarted(batchNumber)
            } catch (e: Exception) {
                _quizState.value = QuizState.Error("Error starting batch: ${e.message}")
            }
        }
    }

    fun submitAnswer(selectedAnswer: String) {
        val currentQ = _currentQuestion.value
        val batch = _currentBatch.value

        if (currentQ != null && batch != null) {
            // Store answer locally
            val answer = BatchAnswer(
                questionId = batch.currentQuestionNumber,
                ayahKey = currentQ.ayahKey ?: "",
                selectedAnswer = selectedAnswer,
                correctAnswer = currentQ.answer,
                isCorrect = selectedAnswer == currentQ.answer
            )
            currentBatchAnswers.add(answer)

            val isLastQuestion = batch.currentQuestionNumber >= batch.questions.size

            _quizState.value = QuizState.AnswerSubmitted(
                isCorrect = answer.isCorrect,
                nextQuestionAvailable = !isLastQuestion
            )

            if (!isLastQuestion) {
                loadNextQuestion()
            } else {
                submitBatchAnswers()
            }
        }
    }

    private fun submitBatchAnswers() {
        viewModelScope.launch {
            try {
                _quizState.value = QuizState.SubmittingAnswers()

                val batchAnswerResponse = quizRepository.submitBatchAnswers(
                    surahId = currentSurahId ?: throw IllegalStateException("Surah ID is null"),
                    batchNumber = currentBatch.value?.batchNumber ?: 0,
                    answers = currentBatchAnswers
                )

                if (batchAnswerResponse != null) {

                    currentBatch.value?.let { batch ->
                        batchScores[batch.batchNumber] = Score(
                            correctAnswers = batchAnswerResponse.correct_answers,
                            totalQuestions = batchAnswerResponse.total_questions
                        )
                    }
                    val totalBatches = (getTotalQuestionsInSurah() + QUESTIONS_PER_BATCH - 1) / QUESTIONS_PER_BATCH
                    // Check if the number of completed batches now equals the total number of batches
                    if (batchScores.size == totalBatches) {
                        val finishResponse = quizRepository.finishQuiz(currentSurahId!!)
                        if (finishResponse == null) {
                            // Handle error, maybe log it. The quiz can proceed, but the chart might not update.
                            Log.w("QuizViewModel", "Failed to mark Surah as completed.")
                        }
                    }
                    val finishedState = QuizState.Finished(
                        message = "Batch completed!", // Generic message
                        correctAnswers = batchAnswerResponse.correct_answers,
                        totalQuestions = batchAnswerResponse.total_questions
                    )
                    lastQuizResult = finishedState
                    _quizState.value = finishedState

                } else {
                    _quizState.value = QuizState.Error("Failed to submit answers")
                }
            } catch (e: Exception) {
                Log.e("QuizViewModel", "Error in submitBatchAnswers", e)
                _quizState.value = QuizState.Error("Error: ${e.message}")
            }
        }
    }
    fun generateBatchList(totalBatches: Int, totalQuestions: Int): List<QuizBatch> {
        val existingScores = _surahProgress.value
        return (1..totalBatches).map { batchNumber ->
            val startQuestion = ((batchNumber - 1) * QUESTIONS_PER_BATCH) + 1
            val endQuestion = minOf(batchNumber * QUESTIONS_PER_BATCH, totalQuestions)
            val score = existingScores[batchNumber]
            QuizBatch(
                batchNumber = batchNumber,
                startQuestion = startQuestion,
                endQuestion = endQuestion,
                score = score // <-- Populate the score here!
            )
        }
    }
    fun loadLastResult() {
        lastQuizResult?.let { result ->
            _quizState.value = result
        }
    }
    private fun loadNextQuestion() {
        _currentBatch.value?.let { batch ->
            val newQuestionNumber = batch.currentQuestionNumber + 1
            val nextQuestion = batch.questions.getOrNull(newQuestionNumber - 1)

            _currentBatch.value = batch.copy(
                currentQuestionNumber = newQuestionNumber,
                currentQuestion = nextQuestion
            )
            _currentQuestion.value = nextQuestion
        }
    }

    private fun loadJsonFromAssets(filename: String): String {
        return context.assets.open(filename).bufferedReader().use { it.readText() }
    }
    override fun onCleared() {
        super.onCleared()
        clearCurrentBatch()
        lastQuizResult = null
    }
    fun clearCurrentBatch() {
        currentBatchAnswers.clear()
        _currentBatch.value = null
        _currentQuestion.value = null
    }
}

sealed class QuizState {
    object Initial : QuizState()
    data class SurahLoaded(
        val chapterNumber: Int,
        val totalQuestions: Int
    ) : QuizState()
    data class BatchStarted(val batchNumber: Int) : QuizState()
    data class SubmittingAnswers(val progress: Float = 0f) : QuizState()
    data class AnswerSubmitted(
        val isCorrect: Boolean,
        val nextQuestionAvailable: Boolean
    ) : QuizState()
    data class Finished(
        val message: String,
        val correctAnswers: Int,
        val totalQuestions: Int
    ) : QuizState()
    data class Error(val message: String) : QuizState()
}

data class QuestionData(
    val question: String,
    val answer: String,
    val options: List<String>,
    val translation: String,
    val ayahKey: String? = null
)