package com.example.qurannexus.features.quiz

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import com.example.qurannexus.databinding.FragmentQuizResultBinding
import com.example.qurannexus.features.quiz.models.QuizState
import com.example.qurannexus.features.quiz.models.QuizViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class QuizResultFragment : Fragment() {
    private var _binding: FragmentQuizResultBinding? = null
    private val binding get() = _binding!!
    private val viewModel: QuizViewModel by viewModels()
    private val args: QuizResultFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        showLoading(true)  // Start with loading state
        observeViewModel()
        viewModel.loadLastResult()  // Load the last quiz result
    }

    private fun setupUI() {
        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.nextBatchButton.setOnClickListener {
            val nextBatch = args.currentBatch + 1
            if (nextBatch <= args.totalBatches) {
                findNavController().navigate(
                    QuizResultFragmentDirections.actionQuizResultFragmentToQuizQuestionFragment(
                        args.chapterNumber,
                        nextBatch
                    )
                )
            } else {
                Toast.makeText(context, "This is the last batch", Toast.LENGTH_SHORT).show()
            }
        }

        binding.previousBatchButton.setOnClickListener {
            val previousBatch = args.currentBatch - 1
            if (previousBatch > 0) {
                findNavController().navigate(
                    QuizResultFragmentDirections.actionQuizResultFragmentToQuizQuestionFragment(
                        args.chapterNumber,
                        previousBatch
                    )
                )
            } else {
                Toast.makeText(context, "This is the first batch", Toast.LENGTH_SHORT).show()
            }
        }

        binding.returnToChaptersButton.setOnClickListener {
            findNavController().navigate(
                QuizResultFragmentDirections.actionQuizResultFragmentToQuizChapterOptionsFragment()
            )
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.quizState.collectLatest { state ->
                Log.d("QuizResultFragment", "Received state: $state")
                when (state) {
                    is QuizState.Finished -> {
                        Log.d("QuizResultFragment", "Finished state: correct=${state.correctAnswers}, total=${state.totalQuestions}")
                        showLoading(false)
                        updateResultsDisplay(state.correctAnswers, state.totalQuestions)
                    }
                    is QuizState.Error -> {
                        showLoading(false)
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                    }
                    is QuizState.SubmittingAnswers -> {
                        showLoading(true)
                    }
                    else -> {
                        Log.d("QuizResultFragment", "Other state received: $state")
                    }
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.apply {
            loadingGroup.isVisible = show
            resultsGroup.isVisible = !show
        }
    }

    private fun updateResultsDisplay(correctAnswers: Int, totalQuestions: Int) {
        val score = (correctAnswers.toFloat() / totalQuestions.toFloat() * 100).toInt()

        binding.apply {
            scoreText.text = "Score: $score%"
            correctAnswersText.text = "Correct Answers: $correctAnswers"
            totalQuestionsText.text = "Total Questions: $totalQuestions"

            // Always show the buttons
            nextBatchButton.isVisible = true
            previousBatchButton.isVisible = true

            // Handle button clicks with proper validation
            nextBatchButton.setOnClickListener {
                val nextBatch = args.currentBatch + 1
                if (nextBatch <= args.totalBatches) {
                    findNavController().navigate(
                        QuizResultFragmentDirections.actionQuizResultFragmentToQuizQuestionFragment(
                            args.chapterNumber,
                            nextBatch
                        )
                    )
                } else {
                    Toast.makeText(context, "This is the last batch", Toast.LENGTH_SHORT).show()
                }
            }

            previousBatchButton.setOnClickListener {
                val previousBatch = args.currentBatch - 1
                if (previousBatch > 0) {
                    findNavController().navigate(
                        QuizResultFragmentDirections.actionQuizResultFragmentToQuizQuestionFragment(
                            args.chapterNumber,
                            previousBatch
                        )
                    )
                } else {
                    Toast.makeText(context, "This is the first batch", Toast.LENGTH_SHORT).show()
                }
            }

            motivationText.text = when {
                score >= 90 -> "Excellent! Keep up the great work!"
                score >= 70 -> "Good job! You're doing well!"
                score >= 50 -> "Nice effort! Keep practicing!"
                else -> "Don't give up! Practice makes perfect!"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}