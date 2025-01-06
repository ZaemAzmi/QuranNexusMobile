package com.example.qurannexus.features.quiz

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.qurannexus.databinding.FragmentQuizQuestionBinding
import com.example.qurannexus.features.quiz.models.QuestionData
import com.example.qurannexus.features.quiz.models.QuizState
import com.example.qurannexus.features.quiz.models.QuizViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class QuizQuestionFragment : Fragment() {

    private var _binding: FragmentQuizQuestionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: QuizViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizQuestionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = QuizQuestionFragmentArgs.fromBundle(requireArguments())
        viewModel.loadQuestions(args.chapterNumber)
        setupNavigationButton()
        observeQuizState()
        observeCurrentQuestion()
        setupSubmitButton()
    }

    private fun observeQuizState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.quizState.collectLatest { state ->
                when (state) {
                    is QuizState.Initial -> {
                        // Initial state, nothing to do
                    }
                    is QuizState.Started -> {
                        Toast.makeText(context, "Quiz started!", Toast.LENGTH_SHORT).show()
                    }
                    is QuizState.AnswerSubmitted -> {
                        if (state.isCorrect) {
                            showPopup("Correct Answer!", "Good job!")
                            binding.submitButton.text = "Continue"
                        } else {
                            val currentQuestion = viewModel.currentQuestion.value
                            showPopup("Wrong Answer!", "Hint: ${currentQuestion?.translation}")
                        }
                    }
                    is QuizState.Finished -> {
                        navigateToResults()
                    }
                    is QuizState.Error -> {
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun observeCurrentQuestion() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentQuestion.collectLatest { questionData ->
                if (questionData != null) {
                    displayQuestion(questionData)
                    resetUI()
                }
            }
        }
    }

    private fun setupSubmitButton() {
        binding.submitButton.setOnClickListener {
            if (binding.submitButton.text == "Continue") {
                viewModel.loadNextQuestion()
                resetUI()
            } else {
                handleAnswerSubmission()
            }
        }
    }

    private fun handleAnswerSubmission() {
        val selectedId = binding.optionsGroup.checkedRadioButtonId
        if (selectedId != -1) {
            val selectedOption = binding.root.findViewById<RadioButton>(selectedId).text.toString()
            viewModel.submitAnswer(selectedOption)
        } else {
            Toast.makeText(context, "Please select an answer", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayQuestion(questionData: QuestionData) {
        binding.questionText.text = questionData.question
        binding.optionsGroup.removeAllViews()

        questionData.options.forEach { option ->
            val radioButton = RadioButton(context).apply {
                text = option
                textSize = 18f
                setPadding(12, 12, 12, 12)
            }
            binding.optionsGroup.addView(radioButton)
        }
    }

    private fun showPopup(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun resetUI() {
        binding.submitButton.text = "Submit Answer"
        binding.optionsGroup.clearCheck()
    }

    private fun navigateToResults() {
        val action = QuizQuestionFragmentDirections
            .actionQuizQuestionFragmentToQuizResultFragment()
        findNavController().navigate(action)
    }

    private fun setupNavigationButton() {
        binding.previousButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}