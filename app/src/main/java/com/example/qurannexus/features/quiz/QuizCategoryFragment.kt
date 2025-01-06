package com.example.qurannexus.features.quiz

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.qurannexus.R
import com.example.qurannexus.databinding.FragmentQuizCategoryBinding
import com.example.qurannexus.features.quiz.models.NotesHighlightItem
import com.example.qurannexus.features.quiz.models.QuestionCategoryAdapter
import com.example.qurannexus.features.quiz.models.QuizViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QuizCategoryFragment : Fragment() {

    private var _binding: FragmentQuizCategoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: QuizViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        val navController = Navigation.findNavController(requireActivity(), R.id.quizFragmentContainer)
        val adapter = QuestionCategoryAdapter { category ->
            val action = QuizCategoryFragmentDirections.actionQuizCategoryFragmentToQuizChapterOptionsFragment()
            findNavController().navigate(action)
        }

        highlightSectionSetup(view)

        // Handle navigation when the quiz button is clicked
        binding.answerQuizButton.setOnClickListener {
            val action = QuizCategoryFragmentDirections
                .actionQuizCategoryFragmentToQuizChapterOptionsFragment()
            findNavController().navigate(action)
        }

        binding.previousButton.setOnClickListener{
            activity?.finish()
        }


        var isBookmarked = false
        val heartBookmarkIcon = binding.dailyWordSection.bookmarkButton
        heartBookmarkIcon.setOnClickListener{
            if (isBookmarked) {
                heartBookmarkIcon.setImageResource(R.drawable.ic_heart)
            } else {
                heartBookmarkIcon.setImageResource(R.drawable.ic_heart_bookmarked)
            }
            isBookmarked = !isBookmarked
        }

        binding.seeAllQuizzesText.setOnClickListener{
            val transaction = parentFragmentManager.beginTransaction()
            transaction.replace(R.id.quizFragmentContainer, QuizCategoryFragment())
            transaction.addToBackStack(null)
            transaction.commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun highlightSectionSetup(rootView: View) {
        val llScrollableHighlights: LinearLayout = rootView.findViewById(R.id.llScrollableNotes)

        // Create the list of highlights manually (no loop needed here)
        val highlightsList = listOf(
            NotesHighlightItem("Tajweed", R.drawable.badge_note),
            NotesHighlightItem("Arabic", R.drawable.badge_note1),
            NotesHighlightItem("I'rab", R.drawable.badge_note),
            NotesHighlightItem("Hifz", R.drawable.badge_note1)
        )

        // Iterate through the list and dynamically create cards
        highlightsList.forEach { highlight ->
            // Inflate the card layout
            val highlightView = LayoutInflater.from(context).inflate(R.layout.card_item_squared_notes_highlights, llScrollableHighlights, false)

            // Find views inside the card
            val highlightImage: ImageView = highlightView.findViewById(R.id.highlightImage)
            val highlightTitle: TextView = highlightView.findViewById(R.id.highlightText)

            // Set image and title
            highlightImage.setImageResource(highlight.imageResId)
            highlightTitle.text = highlight.title

            // Make the card clickable
            highlightView.isClickable = true
            highlightView.isFocusable = true
            highlightView.setOnClickListener {
                onHighlightClick(highlightsList.indexOf(highlight)) // Trigger action based on the index
            }

            // Add the card to the container
            llScrollableHighlights.addView(highlightView)
        }
    }

    private fun onHighlightClick(position: Int) {
        // Handle fragment navigation or other actions based on position

    }

}