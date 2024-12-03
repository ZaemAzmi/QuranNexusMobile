package com.example.qurannexus.features.words

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.databinding.ItemWordAccordionSectionBinding

class AccordionAdapter : RecyclerView.Adapter<AccordionAdapter.AccordionViewHolder>() {

    private val sections = mutableListOf<AccordionSection>()

    fun submitList(newSections: List<AccordionSection>) {
        sections.clear()
        sections.addAll(newSections)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccordionViewHolder {
        val binding = ItemWordAccordionSectionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AccordionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AccordionViewHolder, position: Int) {
        holder.bind(sections[position])
    }

    override fun getItemCount(): Int = sections.size

    inner class AccordionViewHolder(private val binding: ItemWordAccordionSectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var isExpanded = false

        fun bind(section: AccordionSection) {
            binding.textViewSectionTitle.text = section.title
            binding.textViewItemCount.text = "(${section.words.size})"

            // Setup child RecyclerView
            val childAdapter = ChildItemAdapter()
            childAdapter.submitList(section.words)
            binding.recyclerViewWordAccordionChildItems.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = childAdapter
            }

            // Expand/collapse functionality
            binding.accordionHeader.setOnClickListener {
                isExpanded = !isExpanded
                binding.recyclerViewWordAccordionChildItems.visibility =
                    if (isExpanded) View.VISIBLE else View.GONE

                // Update expand/collapse icon
                binding.imageViewExpandCollapse.setImageResource(
                    if (isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
                )
            }
        }
    }
}