package com.example.qurannexus.features.quiz.models

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.databinding.ItemQuizBatchBinding

data class QuizBatch(
    val batchNumber: Int,
    val startQuestion: Int,
    val endQuestion: Int,
    val score: Int? = null
)

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
                scoreText.text = batch.score?.let { "$it/10" } ?: "-/10"
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