package com.example.qurannexus.features.words.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R

class MorphFormsAdapter(private val morphForms: List<String>) :
    RecyclerView.Adapter<MorphFormsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val morphFormTextView: TextView = view.findViewById(R.id.tvMorphFormText) // ID from item_morph_form_bottom_sheet.xml
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_morph_form_bottom_sheet, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.morphFormTextView.text = morphForms[position]
        // Add click listener here if needed in the future
    }

    override fun getItemCount() = morphForms.size
}