package com.example.qurannexus.features.quiz

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.qurannexus.R
import com.example.qurannexus.features.quiz.models.Question
import com.example.qurannexus.features.quiz.models.QuestionCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class QuizViewModel @Inject constructor() : ViewModel() {

    private val _categories = MutableLiveData<List<QuestionCategory>>()
    val categories: LiveData<List<QuestionCategory>> = _categories

    private val _questions = MutableLiveData<List<Question>>()
    val questions: LiveData<List<Question>> = _questions

    private val _score = MutableLiveData(0)
    val score: LiveData<Int> = _score

    private var currentQuestionIndex = 0

    init {
        loadCategories()
    }

    // Load hardcoded categories
    private fun loadCategories() {
        _categories.value = listOf(
            QuestionCategory("Tajweed", R.drawable.badge_11, 5),
            QuestionCategory("I‘rab", R.drawable.badge_11, 13),
            QuestionCategory("Arabic Words", R.drawable.badge_11, 7),
            QuestionCategory("Complete the Quranic Sentences", R.drawable.badge_11, 3),
        )
    }

    // Load hardcoded questions for the selected category
    fun loadQuestions(categoryName: String) {
        _questions.value = when (categoryName) {
            "Tajweed" -> listOf(
                Question("What is the correct pronunciation of 'غ'?", listOf("Ghayn", "Ayn", "Ra"), "Ghayn"),
                Question("Which symbol indicates a prolonged sound?", listOf("Madd", "Shaddah", "Sukun"), "Madd")
            )
            else -> emptyList()
        }
        currentQuestionIndex = 0
    }

    // Check if the answer is correct
    fun checkAnswer(answer: String): Boolean {
        val currentQuestion = _questions.value?.get(currentQuestionIndex) ?: return false
        val isCorrect = currentQuestion.answer == answer
        if (isCorrect) {
            _score.value = _score.value?.plus(1)
        }
        currentQuestionIndex++
        return isCorrect
    }

    // Get the next question or null if finished
    fun getNextQuestion(): Question? {
        return _questions.value?.getOrNull(currentQuestionIndex)
    }
}