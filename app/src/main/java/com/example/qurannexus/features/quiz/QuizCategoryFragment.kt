package com.example.qurannexus.features.quiz

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.qurannexus.R
import com.example.qurannexus.databinding.FragmentQuizCategoryBinding
import com.example.qurannexus.features.bookmark.models.BookmarkRequest
import com.example.qurannexus.features.words.WordDetailsActivity
import com.example.qurannexus.features.quiz.models.QuestionCategoryAdapter
import com.example.qurannexus.features.quiz.models.QuizProgress
import com.example.qurannexus.features.quiz.models.QuizViewModel
import com.example.qurannexus.features.words.models.WordDetails
import com.example.qurannexus.features.words.models.WordManagementViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

@AndroidEntryPoint
class QuizCategoryFragment : Fragment() {

    private var _binding: FragmentQuizCategoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: QuizViewModel by viewModels()
    private val wordViewModel: WordManagementViewModel by viewModels()
    private var currentDailyWord: WordDetails? = null
    private var isWordBookmarked = false
    private val TAG = "QuizCategoryFragment"
    private var dailyActivityChart: LineChart? = null
    private var surahPerformanceChart: BarChart? = null
    private var chartsAdapter: ChartsAdapter? = null
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
        updateChartData()
        setupCharts()
//        highlightSectionSetup(view)
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

//        binding.seeAllQuizzesText.setOnClickListener{
//            val transaction = parentFragmentManager.beginTransaction()
//            transaction.replace(R.id.quizFragmentContainer, QuizCategoryFragment())
//            transaction.addToBackStack(null)
//            transaction.commit()
//        }
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

            checkBookmarkStatus(word.word_text)
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
        Log.d(TAG, "addWordBookmark: Attempting to add bookmark for '${word.word_text}'")
        val token = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("token", null)

        if (token == null) {
            Log.w(TAG, "addWordBookmark: Failed. Token is null.")
            Toast.makeText(context, "Please login to bookmark words", Toast.LENGTH_SHORT).show()
            return
        }

        val itemProperties = mapOf(
            "word_text" to word.word_text,
            "translation" to word.translation,
            "transliteration" to word.transliteration,
            "surah_name" to word.first_occurrence.surah_name,
            "ayah_key" to word.first_occurrence.ayah_key
        )

        val request = BookmarkRequest(
            type = "word",
            itemProperties = itemProperties,
            notes = ""
        )
        Log.d(TAG, "addWordBookmark: Sending request to ViewModel: $request")

        wordViewModel.addBookmark("Bearer $token", request)
        wordViewModel.bookmarkStatus.observe(viewLifecycleOwner) { response ->
            Log.d(TAG, "addWordBookmark: Observed response from ViewModel: $response")
            if (response.status == "success") {
                isWordBookmarked = true
                binding.dailyWordSection.bookmarkButton.setImageResource(R.drawable.ic_heart_bookmarked)
                Toast.makeText(context, "Word bookmarked successfully", Toast.LENGTH_SHORT).show()
            } else {
                Log.e(TAG, "addWordBookmark: API reported failure: ${response.message}")
                Toast.makeText(context, "Failed to bookmark word: ${response.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun removeWordBookmark(wordText: String) {
        Log.d(TAG, "removeWordBookmark: Attempting to remove bookmark for '$wordText'")
        val token = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("token", null)

        if (token == null) {
            Log.w(TAG, "removeWordBookmark: Failed. Token is null.")
            Toast.makeText(context, "Please login to remove bookmarks", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "removeWordBookmark: Calling ViewModel to remove word.")
        wordViewModel.removeBookmark(token, "word", wordText)
        wordViewModel.bookmarkStatus.observe(viewLifecycleOwner) { response ->
            Log.d(TAG, "removeWordBookmark: Observed response from ViewModel: $response")
            if (response.status == "success") {
                isWordBookmarked = false
                binding.dailyWordSection.bookmarkButton.setImageResource(R.drawable.ic_heart)
                Toast.makeText(context, "Bookmark removed successfully", Toast.LENGTH_SHORT).show()
            } else {
                Log.e(TAG, "removeWordBookmark: API reported failure: ${response.message}")
                Toast.makeText(context, "Failed to remove bookmark: ${response.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkBookmarkStatus(wordText: String) {
        val token = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("token", null)

        if (token != null) {
            wordViewModel.checkBookmarkStatus("Bearer $token", wordText).observe(viewLifecycleOwner) { bookmarked ->
                isWordBookmarked = bookmarked
                binding.dailyWordSection.bookmarkButton.setImageResource(
                    if (bookmarked) R.drawable.ic_heart_bookmarked
                    else R.drawable.ic_heart
                )
            }
        }
    }

    private fun navigateToWordDetails(word: WordDetails) {
        // FIX 2: Corrected the Intent to pass the data WordDetailsActivity expects.
        val intent = Intent(requireContext(), WordDetailsActivity::class.java).apply {
            putExtra(WordDetailsActivity.EXTRA_WORD_TEXT_FOR_PRESELECTION, word.word_text)
        }
        startActivity(intent)
    }



    override fun onDestroyView() {
        super.onDestroyView()
        dailyActivityChart = null
        surahPerformanceChart = null
        chartsAdapter = null
        _binding = null
    }

    private fun onHighlightClick(position: Int) {
        // Handle fragment navigation or other actions based on position

    }
    private fun setupCharts() {
        chartsAdapter = ChartsAdapter(requireContext()) { lineChart, barChart ->
            dailyActivityChart = lineChart
            surahPerformanceChart = barChart
            // Now that charts are initialized, start observing data
            observeChartData()
        }

        binding.chartsViewPager.adapter = chartsAdapter

        // Set up dots indicator
        TabLayoutMediator(binding.dotsIndicator, binding.chartsViewPager) { _, _ -> }.attach()

        binding.chartsViewPager.setPageTransformer { page, position ->
            page.alpha = 1 - 0.25f * abs(position)
            page.scaleY = 0.85f + 0.15f * (1 - abs(position))
        }
    }
    private fun observeChartData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getUserQuizProgress().collect { quizProgress ->
                dailyActivityChart?.let { updateDailyActivityChart(it, quizProgress) }
                surahPerformanceChart?.let { updateSurahPerformanceChart(it, quizProgress) }
            }
        }
    }
    private fun updateChartData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getUserQuizProgress().collect { quizProgress ->
                dailyActivityChart?.let { chart ->
                    updateDailyActivityChart(chart, quizProgress)
                }
                surahPerformanceChart?.let { chart ->
                    updateSurahPerformanceChart(chart, quizProgress)
                }
            }
        }
    }

    private fun updateDailyActivityChart(chart: LineChart, quizProgress: List<QuizProgress>) {
        val dailyActivity = quizProgress
            .filter { !it.start_time.isNullOrEmpty() }
            .groupBy { it.start_time!!.substringBefore(" ") }
            .mapValues { entry ->
                entry.value.sumOf { it.correct_answers + it.wrong_answers }
            }
            .toSortedMap()
        val entries = dailyActivity.map { (date, count) ->
            Entry(date.toMillis(), count.toFloat())
        }

        val dataSet = LineDataSet(entries, "Questions Answered").apply {
            color = ContextCompat.getColor(requireContext(), R.color.primaryColor)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.primaryColor))
            lineWidth = 2f
            circleRadius = 4f
            setDrawValues(false)
        }
        chart.setData(LineData(dataSet))
        chart.invalidate()
    }
    private fun updateSurahPerformanceChart(chart: BarChart, quizProgress: List<QuizProgress>) {
        val surahPerformance = quizProgress
            .filter { it.surah_id != null }
            .map { progress ->
                val total = progress.correct_answers + progress.wrong_answers
                val correctPercentage = if (total > 0) {
                    (progress.correct_answers * 100f) / total
                } else 0f
                BarEntry(progress.surah_id!!.toFloat(), correctPercentage)
            }
            .sortedBy { it.x }

        val dataSet = BarDataSet(surahPerformance, "Accuracy %").apply {
            color = ContextCompat.getColor(requireContext(), R.color.primaryColor)
            valueTextSize = 10f
        }

        chart.setData(BarData(dataSet))
        chart.invalidate()
    }




    private fun String.toMillis(): Float {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .parse(this)?.time?.toFloat() ?: 0f
    }
}