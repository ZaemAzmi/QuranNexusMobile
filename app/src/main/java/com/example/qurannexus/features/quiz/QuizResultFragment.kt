package com.example.qurannexus.features.quiz

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.example.qurannexus.R
import dagger.hilt.android.AndroidEntryPoint
import com.example.qurannexus.databinding.FragmentQuizResultBinding
@AndroidEntryPoint
class QuizResultFragment : Fragment() {

    private var _binding: FragmentQuizResultBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuizViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val score = viewModel.score.value ?: 0
        binding.scoreText.text = "Your Score: $score"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}