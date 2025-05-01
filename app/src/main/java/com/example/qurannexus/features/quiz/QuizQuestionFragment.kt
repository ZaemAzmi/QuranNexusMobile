package com.example.qurannexus.features.quiz

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.activity.OnBackPressedCallback
import com.example.qurannexus.databinding.FragmentQuizQuestionBinding
import com.example.qurannexus.features.quiz.models.QuestionData
import com.example.qurannexus.features.quiz.models.QuizState
import com.example.qurannexus.features.quiz.models.QuizViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.navigation.fragment.navArgs
import com.example.qurannexus.R
import com.example.qurannexus.features.quiz.models.QuizViewModel.Companion.QUESTIONS_PER_BATCH
import com.google.android.material.dialog.MaterialAlertDialogBuilder


@AndroidEntryPoint
class QuizQuestionFragment : Fragment() {
    private var _binding: FragmentQuizQuestionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: QuizViewModel by viewModels()
    private val args: QuizQuestionFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizQuestionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load surah first
        viewModel.loadSurah(args.chapterNumber)
        setupUI()
        observeViewModel()

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showExitConfirmationDialog()
                }
            }
        )
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.quizState.collect { state ->
                if (state is QuizState.SurahLoaded) {
                    viewModel.startBatch(args.batchNumber)
                }
            }
        }
    }

    private fun setupUI() {
        binding.submitButton.setOnClickListener {
            handleAnswerSubmission()
        }

        binding.showHintButton.setOnClickListener {
            viewModel.currentQuestion.value?.let { question ->
                binding.translationText.visibility = View.VISIBLE
                binding.translationText.text = question.translation
            }
        }

        binding.previousButton.setOnClickListener {
            showExitConfirmationDialog()
        }
    }
    private fun showExitConfirmationDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Exit Quiz")
            .setMessage("Are you sure you want to exit? You will lose all progress.")
            .setPositiveButton("Exit") { _, _ ->
                viewModel.clearCurrentBatch()
                findNavController().popBackStack()
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val titleView = dialog.findViewById<TextView>(androidx.appcompat.R.id.alertTitle)
            val messageView = dialog.findViewById<TextView>(android.R.id.message)
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            val titleSize = resources.getDimension(R.dimen.text_size_xxlarge) / resources.displayMetrics.scaledDensity
            val messageSize = resources.getDimension(R.dimen.text_size_large) / resources.displayMetrics.scaledDensity
            val buttonSize = resources.getDimension(R.dimen.text_size_xlarge) / resources.displayMetrics.scaledDensity

            titleView?.setTextSize(TypedValue.COMPLEX_UNIT_SP, titleSize)
            messageView?.setTextSize(TypedValue.COMPLEX_UNIT_SP, messageSize)
            positiveButton?.setTextSize(TypedValue.COMPLEX_UNIT_SP, buttonSize)
            negativeButton?.setTextSize(TypedValue.COMPLEX_UNIT_SP, buttonSize)
        }

        dialog.show()
    }

    private fun observeViewModel() {
        // Observe quiz state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.quizState.collect { state ->
                when (state) {
                    is QuizState.BatchStarted -> {
                        showLoading(false)
                    }
                    is QuizState.AnswerSubmitted -> {
                        if (state.isCorrect) {
                            Toast.makeText(context, "Correct!", Toast.LENGTH_SHORT).show()
                        }
                        binding.optionsGroup.clearCheck()
                        binding.translationText.visibility = View.GONE
                    }
                    is QuizState.Finished -> {
                        navigateToResults()
                    }
                    is QuizState.Error -> {
                        showLoading(false)
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                    }
                    else -> {}
                }
            }
        }

        // Observe current batch
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentBatch.collect { batch ->
                batch?.let {
                    binding.progressText.text = "Question ${it.currentQuestionNumber} of ${it.questions.size}"
                    updateQuestionDisplay(it.currentQuestion)
                }
            }
        }
    }
    private fun showLoading(show: Boolean) {
        if (show) {
            binding.progressBar.visibility = View.VISIBLE
            binding.submitButton.isEnabled = false
        } else {
            binding.progressBar.visibility = View.GONE
            binding.submitButton.isEnabled = true
        }
    }
    private fun updateQuestionDisplay(question: QuestionData?) {
        question?.let {
            binding.questionText.text = it.question
            binding.optionsGroup.removeAllViews()
            binding.translationText.visibility = View.GONE
            binding.translationText.text = it.translation

            it.options.forEach { option ->
                val radioButton = RadioButton(context).apply {
                    text = option
                    textSize = resources.getDimension(R.dimen.text_size_large) / resources.displayMetrics.scaledDensity

                    // Set padding from dimensions resource
                    val padding = resources.getDimensionPixelSize(R.dimen.padding_intermediate)
                    setPadding(padding, padding, padding, padding)

                    // Apply the same background as in XML
                    setBackgroundResource(R.drawable.rounded_recyclerview_background_white)

                    // Set text color
                    setTextColor(resources.getColor(android.R.color.black, null))

                    // Apply button tint
                    buttonTintList = resources.getColorStateList(R.color.light_green_button, null)
                }

                // Create layout params with margins
                val layoutParams = RadioGroup.LayoutParams(
                    RadioGroup.LayoutParams.MATCH_PARENT,
                    RadioGroup.LayoutParams.WRAP_CONTENT
                )
                val verticalMargin = resources.getDimensionPixelSize(R.dimen.margin_medium)
                layoutParams.setMargins(0, verticalMargin, 0, verticalMargin)

                // Apply layout params
                radioButton.layoutParams = layoutParams

                binding.optionsGroup.addView(radioButton)
            }
        }
    }

    private fun handleAnswerSubmission() {
        val selectedId = binding.optionsGroup.checkedRadioButtonId
        if (selectedId != View.NO_ID) {
            val radioButton = binding.root.findViewById<RadioButton>(selectedId)
            if (radioButton != null) {
                val selectedOption = radioButton.text.toString()
                viewModel.submitAnswer(selectedOption)
            } else {
                Toast.makeText(context, "Please select an answer", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Please select an answer", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToResults() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.quizState.collectLatest { state ->
                when (state) {
                    is QuizState.Finished -> {

                        val totalQuestions = viewModel.getTotalQuestionsInSurah()
                        val totalBatches = (totalQuestions + QuizViewModel.QUESTIONS_PER_BATCH - 1) / QuizViewModel.QUESTIONS_PER_BATCH


                        Log.d("QuizQuestion", "Current batch: ${args.batchNumber}")
                        Log.d("QuizQuestion", "Total batches: $totalBatches")
                        Log.d("QuizQuestion", "Total questions: ${state.totalQuestions}")
                        val action = QuizQuestionFragmentDirections
                            .actionQuizQuestionFragmentToQuizResultFragment(
                                args.chapterNumber,
                                args.batchNumber,
                                totalBatches
                            )
                        findNavController().navigate(action)
                    }
                    else -> {} // Handle other states if needed
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}