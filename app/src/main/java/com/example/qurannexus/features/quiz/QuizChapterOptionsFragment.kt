package com.example.qurannexus.features.quiz

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.qurannexus.core.network.ApiService
import com.example.qurannexus.databinding.FragmentQuizChapterOptionsBinding
import com.example.qurannexus.features.quiz.customViews.ZigZagLayoutManager
import com.example.qurannexus.features.quiz.models.QuizChapterOptionsAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QuizChapterOptionsFragment : Fragment() {
    private var _binding: FragmentQuizChapterOptionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizChapterOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chapters = (1..114).toList()
        verifyToken()
        val chapterAdapter = QuizChapterOptionsAdapter(chapters) { chapter ->
            // Navigate to QuizQuestionRangeFragment instead of QuizQuestionFragment
            try {
                val action = QuizChapterOptionsFragmentDirections
                    .actionQuizChapterOptionsFragmentToQuizQuestionRangeFragment(chapter)
                findNavController().navigate(action)
            } catch (e: Exception) {
                Log.e("QuizChapterOptions", "Navigation failed", e)
            }
        }

        binding.recyclerViewChapters.apply {
            layoutManager = ZigZagLayoutManager()
            adapter = chapterAdapter
        }

        binding.backArrowImageView.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun verifyToken(){
        context?.let { ctx ->
            val sharedPrefs = ctx.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            val token = sharedPrefs.getString("token", null)
            if (token != null) {
                ApiService.setAuthToken(token)
                Log.d("QuizChapterOptions", "Token set: $token")
            } else {
                Log.e("QuizChapterOptions", "No token found!")
                // Handle no token case - maybe navigate to login
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
