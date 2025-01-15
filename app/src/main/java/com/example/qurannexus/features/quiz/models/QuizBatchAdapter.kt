package com.example.qurannexus.features.quiz.models

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
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
    private val onBatchSelected: (Int) -> Unit
) : ListAdapter<QuizBatch, QuizBatchAdapter.BatchViewHolder>(BatchDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BatchViewHolder {
        val binding = ItemQuizBatchBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BatchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BatchViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BatchViewHolder(
        private val binding: ItemQuizBatchBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onBatchSelected(getItem(position).batchNumber)
                }
            }
        }

        fun bind(batch: QuizBatch) {
            binding.apply {
                questionRangeText.text = "Questions ${batch.startQuestion}-${batch.endQuestion}"

                batch.score?.let { score ->
                    scoreText.text = "${score.correctAnswers}/${score.totalQuestions}"
                    statusText.text = "${score.percentage}%"
                    scoreContainer.visibility = View.VISIBLE

                    // Set status text color based on score
                    val color = when {
                        score.percentage >= 90 -> Color.parseColor("#4CAF50") // Green
                        score.percentage >= 70 -> Color.parseColor("#8BC34A") // Light Green
                        score.percentage >= 50 -> Color.parseColor("#FFC107") // Yellow
                        else -> Color.parseColor("#F44336") // Red
                    }
                    statusText.setTextColor(color)
                } ?: run {
                    scoreContainer.visibility = View.GONE
                }
            }
        }
    }

    private class BatchDiffCallback : DiffUtil.ItemCallback<QuizBatch>() {
        override fun areItemsTheSame(oldItem: QuizBatch, newItem: QuizBatch): Boolean {
            return oldItem.batchNumber == newItem.batchNumber
        }

        override fun areContentsTheSame(oldItem: QuizBatch, newItem: QuizBatch): Boolean {
            return oldItem == newItem
        }
    }
}