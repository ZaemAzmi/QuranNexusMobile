package com.example.qurannexus.features.home

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.features.home.achievement.AchievementService
import com.example.qurannexus.features.home.models.Badge
import com.example.qurannexus.features.home.models.BadgeAdapter

class BadgesActivity : AppCompatActivity() {
    private lateinit var achievementService: AchievementService
    private lateinit var recyclerView: RecyclerView
    private lateinit var backButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_badges)

        achievementService = AchievementService(context = this)
        recyclerView = findViewById(R.id.rvBadges)
        backButton = findViewById(R.id.badgeActivityPreviousButton)

        setupTabActions()
        loadAchievements()
        setupBackButton()
    }

    private fun loadAchievements() {
        achievementService.getAchievementStatus { statusMap ->
            if (statusMap != null) {
                // Convert predefined badges to list with status
                val badgesList = AchievementService.PREDEFINED_BADGES.map { badge ->
                    val status = statusMap[badge.id]
                    badge.copy(
                        status = status?.status ?: "Not Achieved"
                    )
                }

                runOnUiThread {
                    setupRecyclerView(badgesList)
                }
            } else {
                // If failed to get status, show predefined badges with default status
                setupRecyclerView(AchievementService.PREDEFINED_BADGES)
            }
        }
    }

    private fun setupRecyclerView(badges: List<Badge>) {

        achievementService.getAchievementStatus { statusMap ->
            // Handle null statusMap gracefully and update badges with statuses
            val updatedBadges = badges.map { badge ->
                // Retrieve the status object from the map using badge id
                val status = statusMap?.get(badge.id)
                // Check if the status is null or not and then compare it to "Completed"
                badge.copy(
                    status = if (status?.status == "Completed") {  // Safe call and comparison
                        "Achieved"
                    } else {
                        "Not Achieved"
                    }
                )
            }
        }
        val adapter = BadgeAdapter(badges) { badge ->
            showBadgePopup(badge)
        }
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter
    }

    private fun setupTabActions() {
        val tabAll = findViewById<TextView>(R.id.tabAll)
        val tabCompleted = findViewById<TextView>(R.id.tabCompleted)

        tabAll.setOnClickListener {
            tabAll.setTextColor(ContextCompat.getColor(this, R.color.bottom_nav_dark_green))
            tabCompleted.setTextColor(ContextCompat.getColor(this, R.color.white))
            loadAchievements() // Reload all achievements
        }

        tabCompleted.setOnClickListener {
            tabAll.setTextColor(ContextCompat.getColor(this, R.color.white))
            tabCompleted.setTextColor(ContextCompat.getColor(this, R.color.bottom_nav_dark_green))
            // Filter only completed achievements
            achievementService.getAchievementStatus { statusMap ->
                if (statusMap != null) {
                    val completedBadges = AchievementService.PREDEFINED_BADGES.filter { badge ->
                        statusMap[badge.id]?.status == "Completed"
                    }.map{ badge ->
                        // Make sure the status is explicitly set to "Achieved" for completed badges
                        badge.copy(status = "Achieved")
                    }
                    runOnUiThread {
                        setupRecyclerView(completedBadges)
                    }
                }
            }
        }
    }

    private fun setupBackButton() {
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun showBadgePopup(badge: Badge) {
        val dialog = BadgeDetailsDialog(this, badge)
        dialog.show()
    }
}
