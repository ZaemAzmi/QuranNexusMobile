package com.example.qurannexus.features.quiz.models

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.databinding.ItemQuizBatchBinding

data class QuizBatch(
    val batchNumber: Int,
    val startQuestion: Int,
    val endQuestion: Int,
    val score: Score? = null
)
data class Score(
    val correctAnswers: Int,
    val totalQuestions: Int
) {
    val percentage: Int
        get() = ((correctAnswers.toFloat() / totalQuestions.toFloat()) * 100).toInt()
}
class QuizBatchAdapter(
    // This is the lambda function that will be executed when a user clicks an item.
    // It takes the batch's QuizBatch object as input and returns nothing (Unit).
    private val onBatchClicked: (batch: QuizBatch) -> Unit
) : ListAdapter<QuizBatch, QuizBatchAdapter.BatchViewHolder>(BatchDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BatchViewHolder {
        val binding = ItemQuizBatchBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        // Pass the click listener lambda to the ViewHolder
        return BatchViewHolder(binding, onBatchClicked)
    }

    override fun onBindViewHolder(holder: BatchViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // The ViewHolder now takes the listener in its constructor
    class BatchViewHolder(
        private val binding: ItemQuizBatchBinding,
        private val clickListener: (batch: QuizBatch) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        // The bind method now only needs the batch object
        fun bind(batch: QuizBatch) {
            binding.questionRangeText.text = "Questions ${batch.startQuestion}-${batch.endQuestion}"

            if (batch.score != null) {
                // --- BATCH IS COMPLETED ---
                binding.scoreText.visibility = View.VISIBLE
                binding.statusText.visibility = View.VISIBLE

                // Use the 'percentage' computed property from the Score data class
                binding.scoreText.text = "Score: ${batch.score.percentage}%"
                binding.statusText.text = "Completed (Tap to retry)"

                binding.quizBatchCard.setCardBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.accent_dark_teal_cyan)
                )
            } else {
                // --- BATCH IS NOT COMPLETED ---
                binding.scoreText.visibility = View.GONE
                binding.statusText.text = "Start Quiz"

                binding.quizBatchCard.setCardBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.green_blue_100)
                )
            }

            // Set the click listener on the entire item view
            binding.root.setOnClickListener {
                clickListener(batch) // Pass the entire batch object back to the fragment
            }
        }
    }

    // DiffUtil remains the same
    private class BatchDiffCallback : DiffUtil.ItemCallback<QuizBatch>() {
        override fun areItemsTheSame(oldItem: QuizBatch, newItem: QuizBatch): Boolean {
            return oldItem.batchNumber == newItem.batchNumber
        }

        override fun areContentsTheSame(oldItem: QuizBatch, newItem: QuizBatch): Boolean {
            return oldItem == newItem
        }
    }
}