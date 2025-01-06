package com.example.qurannexus.features.quiz

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.qurannexus.databinding.FragmentQuizChapterOptionsBinding
import com.example.qurannexus.features.quiz.customViews.ZigZagLayoutManager
import com.example.qurannexus.features.quiz.models.QuizChapterOptionsAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QuizChapterOptionsFragment : Fragment() {

    private var _binding: FragmentQuizChapterOptionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizChapterOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chapters = (1..114).toList() // List of chapters (1 to 114)

        val chapterAdapter = QuizChapterOptionsAdapter(chapters) { chapter ->
            // Navigate to the question fragment with the selected chapter
            val action = QuizChapterOptionsFragmentDirections
                .actionQuizChapterOptionsFragmentToQuizQuestionFragment(chapter)
            findNavController().navigate(action)
        }
        binding.recyclerViewChapters.apply {
            layoutManager = ZigZagLayoutManager()
            adapter = chapterAdapter
        }
        binding.backArrowImageView.setOnClickListener{
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
