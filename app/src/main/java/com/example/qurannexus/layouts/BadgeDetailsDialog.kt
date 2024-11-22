package com.example.qurannexus.layouts

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.qurannexus.R
import com.example.qurannexus.models.Badge

class BadgeDetailsDialog(context: Context, private val badge: Badge) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_badge_achievement)

        val ivPopupBadgeIcon: ImageView = findViewById(R.id.ivPopupBadgeIcon)
        val tvPopupBadgeTitle: TextView = findViewById(R.id.tvPopupBadgeTitle)
        val tvPopupBadgeDescription: TextView = findViewById(R.id.tvPopupBadgeDescription)
        val btnShareBadge: Button = findViewById(R.id.btnShareBadge)

        ivPopupBadgeIcon.setImageResource(badge.iconRes)
        tvPopupBadgeTitle.text = badge.title
        tvPopupBadgeDescription.text = "You earned this badge for an amazing accomplishment!"

        btnShareBadge.setOnClickListener {
            // Share badge logic
            Toast.makeText(context, "Share feature coming soon!", Toast.LENGTH_SHORT).show()
        }
    }
}
