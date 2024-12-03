package com.example.qurannexus.features.quiz

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.qurannexus.R
import com.example.qurannexus.databinding.FragmentQuizQuestionBinding
import dagger.hilt.android.AndroidEntryPoint

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

        val categoryName = arguments?.getString("category") ?: return
        viewModel.loadQuestions(categoryName)

        showQuestion()

        binding.submitButton.setOnClickListener {
            val selectedAnswer = binding.optionsGroup.checkedRadioButtonId
            if (selectedAnswer != -1) {
                val answerText = binding.optionsGroup.findViewById<RadioButton>(selectedAnswer).text.toString()
                viewModel.checkAnswer(answerText)

                val nextQuestion = viewModel.getNextQuestion()
                if (nextQuestion != null) {
                    showQuestion()
                } else {
                    val action = QuizQuestionFragmentDirections.actionQuizQuestionFragmentToQuizResultFragment()
                    findNavController().navigate(action)
//                    val navController = findNavController()
//                    navController.navigate(R.id.action_quizQuestionFragment_to_quizResultFragment)
                }
            }
        }
    }

    private fun showQuestion() {
        val question = viewModel.getNextQuestion()
        binding.questionText.text = question?.text
        binding.optionsGroup.removeAllViews()

        question?.options?.forEach { option ->
            val radioButton = RadioButton(context).apply { text = option }
            binding.optionsGroup.addView(radioButton)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}