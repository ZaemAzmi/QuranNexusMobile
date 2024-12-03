package com.example.qurannexus.features.quiz.models

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
class QuestionCategoryAdapter(
    private val onCategoryClicked: (QuestionCategory) -> Unit
) : RecyclerView.Adapter<QuestionCategoryAdapter.CategoryViewHolder>() {

    private val categories = mutableListOf<QuestionCategory>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_item_quiz_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.bind(category, onCategoryClicked)
    }

    override fun getItemCount() = categories.size

    fun submitList(newCategories: List<QuestionCategory>) {
        categories.clear()
        categories.addAll(newCategories)
        notifyDataSetChanged()
    }

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryName: TextView = itemView.findViewById(R.id.categoryTitle)
        private val categoryImage: ImageView = itemView.findViewById(R.id.categoryImage)
        private val numberOfQuestions : TextView = itemView.findViewById(R.id.numberOfQuestions)

        fun bind(category: QuestionCategory, onCategoryClicked: (QuestionCategory) -> Unit) {
            categoryName.text = category.name
            categoryImage.setImageResource(category.image)
            numberOfQuestions.text = category.numberOfQuestions.toString() + " Questions"
            itemView.setOnClickListener { onCategoryClicked(category) }
        }
    }
}
