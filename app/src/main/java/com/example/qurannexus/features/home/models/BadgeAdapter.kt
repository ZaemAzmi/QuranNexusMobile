package com.example.qurannexus.features.home.models

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R

class BadgeAdapter(
    private val badgeList: List<Badge>,
    private val onBadgeClick: (Badge) -> Unit
) : RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder>() {

    inner class BadgeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val badgeIcon: ImageView = itemView.findViewById(R.id.ivBadgeIcon)
        val badgeTitle: TextView = itemView.findViewById(R.id.tvBadgeTitle)
        val badgeStatus: TextView = itemView.findViewById(R.id.tvBadgeStatus)
        fun bind(badge: Badge) {
            badgeIcon.setImageResource(badge.iconRes)
            badgeTitle.text = badge.title
            badgeStatus.text = badge.status
            Log.d("BadgeAdapter", "Setting click listener for: ${badge.title}")

            // Handle card click
            itemView.setOnClickListener {
                onBadgeClick(badge)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_badge_achievement_with_status, parent, false)
        return BadgeViewHolder(view)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        val badge = badgeList[position]
        Log.d("BadgeAdapter", "Binding badge: ${badge.title}")
        holder.bind(badge)
    }

    override fun getItemCount(): Int = badgeList.size
}

