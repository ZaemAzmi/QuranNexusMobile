package com.example.qurannexus.features.quiz

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.qurannexus.R
import com.example.qurannexus.databinding.FragmentQuizCategoryBinding
import com.example.qurannexus.features.bookmark.models.BookmarkRequest
import com.example.qurannexus.features.home.WordDetailsActivity
import com.example.qurannexus.features.quiz.models.NotesHighlightItem
import com.example.qurannexus.features.quiz.models.QuestionCategoryAdapter
import com.example.qurannexus.features.quiz.models.QuizViewModel
import com.example.qurannexus.features.words.models.DailyWord
import com.example.qurannexus.features.words.models.WordDetails
import com.example.qurannexus.features.words.models.WordManagementViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QuizCategoryFragment : Fragment() {

    private var _binding: FragmentQuizCategoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: QuizViewModel by viewModels()
    private val wordViewModel: WordManagementViewModel by viewModels()
    private var currentDailyWord: WordDetails? = null
    private var isWordBookmarked = false
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
        setupDailyWord()
        setupBookmarkButton()
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
    private fun setupDailyWord() {
        // Get userId from SharedPreferences
        val userId = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("user_id", "") ?: ""

        wordViewModel.getDailyWord(userId)
        wordViewModel.dailyWord.observe(viewLifecycleOwner) { word ->
            currentDailyWord = word
            binding.dailyWordSection.apply {
                arabicWord.text = word.word_text
                wordExplanation.text = "${word.translation} (${word.transliteration})"
            }

            checkBookmarkStatus(word.word_id)
        }
    }
    private fun setupBookmarkButton() {
        binding.dailyWordSection.bookmarkButton.setOnClickListener {
            currentDailyWord?.let { word ->
                if (isWordBookmarked) {
                    removeWordBookmark(word.word_id)
                } else {
                    addWordBookmark(word)
                }
            }
        }

        binding.dailyWordSection.readMore.setOnClickListener {
            currentDailyWord?.let { word ->
                navigateToWordDetails(word)
            }
        }
    }

    private fun addWordBookmark(word: WordDetails) {
        val token = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("token", null)

        if (token == null) {
            Toast.makeText(context, "Please login to bookmark words", Toast.LENGTH_SHORT).show()
            return
        }

        val request = BookmarkRequest(
            type = "word",
            item_id = word.word_id,
            word_text = word.word_text,
            translation = word.translation,
            transliteration = word.transliteration,
            surah_name = word.first_occurrence.surah_name,
            ayah_key = word.first_occurrence.ayah_key
        )

        wordViewModel.addBookmark("Bearer $token", request)
        wordViewModel.bookmarkStatus.observe(viewLifecycleOwner) { response ->
            if (response.status == "success") {
                isWordBookmarked = true
                binding.dailyWordSection.bookmarkButton.setImageResource(R.drawable.ic_heart_bookmarked)
                Toast.makeText(context, "Word bookmarked successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to bookmark word", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun removeWordBookmark(wordId: String) {
        val token = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("token", null)

        if (token == null) {
            Toast.makeText(context, "Please login to remove bookmarks", Toast.LENGTH_SHORT).show()
            return
        }

        wordViewModel.removeBookmark(token, "word", wordId)
        wordViewModel.bookmarkStatus.observe(viewLifecycleOwner) { response ->
            if (response.status == "success") {
                isWordBookmarked = false
                binding.dailyWordSection.bookmarkButton.setImageResource(R.drawable.ic_heart)
                Toast.makeText(context, "Bookmark removed successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to remove bookmark", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkBookmarkStatus(wordId: String) {
        val token = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("token", null)

        if (token != null) {
            wordViewModel.checkBookmarkStatus(token, wordId).observe(viewLifecycleOwner) { bookmarked ->
                isWordBookmarked = bookmarked
                binding.dailyWordSection.bookmarkButton.setImageResource(
                    if (bookmarked) R.drawable.ic_heart_bookmarked
                    else R.drawable.ic_heart
                )
            }
        }
    }

    private fun navigateToWordDetails(word: WordDetails) {
        val intent = Intent(requireContext(), WordDetailsActivity::class.java).apply {
            putExtra("WORD_ID", word.word_id)
            putExtra("WORD_TEXT", word.word_text)
            putExtra("TRANSLATION", word.translation)
            putExtra("TRANSLITERATION", word.transliteration)
            putExtra("SURAH_NAME", word.first_occurrence.surah_name)
            putExtra("AYAH_KEY", word.first_occurrence.ayah_key)
        }
        startActivity(intent)
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