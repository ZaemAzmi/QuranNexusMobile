package com.example.qurannexus.features.quiz

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.qurannexus.databinding.FragmentQuizQuestionRangeBinding
import com.example.qurannexus.features.quiz.models.QuizBatch
import com.example.qurannexus.features.quiz.models.QuizBatchAdapter
import com.example.qurannexus.features.quiz.models.QuizState
import com.example.qurannexus.features.quiz.models.QuizViewModel
import com.example.qurannexus.features.quiz.models.QuizViewModel.Companion.QUESTIONS_PER_BATCH
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class QuizQuestionRangeFragment : Fragment() {
    private var _binding: FragmentQuizQuestionRangeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: QuizViewModel by viewModels()
    private val args: QuizQuestionRangeFragmentArgs by navArgs()
    private lateinit var batchAdapter: QuizBatchAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizQuestionRangeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        viewModel.loadSurah(args.chapterNumber)

        binding.backArrowImageView.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupRecyclerView() {
        batchAdapter = QuizBatchAdapter { batchNumber ->
            navigateToQuizQuestions(batchNumber)
        }
        binding.recyclerViewBatches.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = batchAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.quizState.collectLatest { state ->
                when (state) {
                    is QuizState.SurahLoaded -> {
                        val totalBatches = (state.totalQuestions + QUESTIONS_PER_BATCH - 1) / QUESTIONS_PER_BATCH
                        val batchList = viewModel.generateBatchList(totalBatches, state.totalQuestions)
                        Log.d("QuizRange", "Generating batch list with scores: $batchList")
                        batchAdapter.submitList(batchList)

                        binding.totalQuestionsText.text = "Total Questions: ${state.totalQuestions}"
                        binding.chapterNumberText.text = "Chapter ${args.chapterNumber}"
                    }
                    is QuizState.Error -> {
                        // Handle error state
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                    }
                    else -> {} // Handle other states if needed
                }
            }
        }
    }

    private fun generateBatchList(totalBatches: Int, totalQuestions: Int): List<QuizBatch> {
        return (1..totalBatches).map { batchNumber ->
            val startQuestion = ((batchNumber - 1) * QUESTIONS_PER_BATCH) + 1
            val endQuestion = minOf(batchNumber * QUESTIONS_PER_BATCH, totalQuestions)
            QuizBatch(
                batchNumber = batchNumber,
                startQuestion = startQuestion,
                endQuestion = endQuestion,
                score = null
            )
        }
    }

    private fun navigateToQuizQuestions(batchNumber: Int) {
        val action = QuizQuestionRangeFragmentDirections
            .actionQuizQuestionRangeFragmentToQuizQuestionFragment(
                args.chapterNumber,
                batchNumber
            )
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}