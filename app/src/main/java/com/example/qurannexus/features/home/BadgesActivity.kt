package com.example.qurannexus.features.home

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.features.home.models.Badge
import com.example.qurannexus.features.home.models.BadgeAdapter

class BadgesActivity : AppCompatActivity() {

     private lateinit var backButton : ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_badges)

        val recyclerView: RecyclerView = findViewById(R.id.rvBadges)

        // Hardcoded badges
        val badges = listOf(
            Badge("Novices Reward", "Started reading the Quran", R.drawable.badge_1, "Completed"),
            Badge("Daily Reciter", "Read Quran every day for a week", R.drawable.badge_2, "In Progress"),
            Badge("Tajweed Master", "Learned and applied Tajweed rules", R.drawable.badge_3, "Not Achieved"),
            Badge("Surah Al-Fatiha", "Memorized Surah Al-Fatiha", R.drawable.badge_1, "Completed"),
            Badge("Surah Yaseen", "Completed reciting Surah Yaseen", R.drawable.badge_2, "Not Achieved"),
            Badge("Night Worshipper", "Recited Quran during Tahajjud", R.drawable.badge_3, "In Progress"),
            Badge("Ramadan Achiever", "Completed Quran during Ramadan", R.drawable.badge_1, "Completed"),
            Badge("Charity Reward", "Shared Quranic verses with friends", R.drawable.badge_2, "Not Achieved")
        )
        val tabAll = findViewById<TextView>(R.id.tabAll)
        val tabCompleted = findViewById<TextView>(R.id.tabCompleted)
        tabAll.setOnClickListener {
            // Switch active tab
            tabAll.setTextColor(ContextCompat.getColor(this, R.color.bottom_nav_dark_green))
            tabCompleted.setTextColor(ContextCompat.getColor(this, R.color.white))

            // Handle showing "All" badges (for now no backend logic)
            // Update RecyclerView data as per "All" badge selection
        }

        tabCompleted.setOnClickListener {
            // Switch active tab
            tabAll.setTextColor(ContextCompat.getColor(this, R.color.white))
            tabCompleted.setTextColor(ContextCompat.getColor(this, R.color.bottom_nav_dark_green))

            // Handle showing "Completed" badges (for now no backend logic)
            // Update RecyclerView data as per "Completed" badge selection
        }
        backButton = findViewById(R.id.badgeActivityPreviousButton)
        backButton.setOnClickListener {
            finish()
        }
        val adapter = BadgeAdapter(badges) { badge ->
            Log.d("BadgeActivity", "Creating dialog for badge: ${badge.title}")
            Toast.makeText(this, "Clicked: ${badge.title}", Toast.LENGTH_SHORT).show()
            val dialog = BadgeDetailsDialog(this, badge)
            dialog.show()
        }
        recyclerView.layoutManager = GridLayoutManager(this, 2) // 2 cards per row
        recyclerView.adapter = adapter
    }
}


