package com.example.qurannexus.features.quiz.models

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.databinding.CardItemQuizChapterBinding

class QuizChapterOptionsAdapter(
    private val chapters: List<Int>, // List of chapter numbers
    private val onChapterClick: (Int) -> Unit // Callback for click
) : RecyclerView.Adapter<QuizChapterOptionsAdapter.ChapterViewHolder>() {

    class ChapterViewHolder(val binding: CardItemQuizChapterBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterViewHolder {
        val binding = CardItemQuizChapterBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ChapterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChapterViewHolder, position: Int) {
        val chapter = chapters[position]

        // Convert to Arabic numeral
        holder.binding.chapterNumberText.text = convertToArabicNumerals(chapter)

        // Set click listener
        holder.binding.root.setOnClickListener {
            onChapterClick(chapter)
        }
    }

    override fun getItemCount() = chapters.size

    private fun convertToArabicNumerals(number: Int): String {
        // Replace this with your existing conversion method
        val arabicNumbers = mapOf(
            '0' to '٠', '1' to '١', '2' to '٢', '3' to '٣', '4' to '٤',
            '5' to '٥', '6' to '٦', '7' to '٧', '8' to '٨', '9' to '٩'
        )
        return number.toString().map { arabicNumbers[it] ?: it }.joinToString("")
    }
}

