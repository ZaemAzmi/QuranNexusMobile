package com.example.qurannexus.features.home.achievement

import android.content.Context
import android.util.Log
import com.example.qurannexus.R
import com.example.qurannexus.core.interfaces.QuranApi
import com.example.qurannexus.core.network.ApiService
import com.example.qurannexus.features.home.models.Badge
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.atomic.AtomicReference

class AchievementService(private val context: Context) {
    private val quranApi = ApiService.getQuranClient().create(QuranApi::class.java)
    private val tokenCache = AtomicReference<String?>(null)
    // Use lazy initialization to defer SharedPreferences access
    init {
        // Load token on a background thread during initialization
        Thread {
            try {
                val token = context.applicationContext.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                    .getString("token", null)
                tokenCache.set(token)
                Log.d("AchievementService", "Token loaded in background")
            } catch (e: Exception) {
                Log.e("AchievementService", "Error loading token", e)
            }
        }.start()
    }
    // Safe method to get token
    private fun getAuthToken(): String? {
        return tokenCache.get() ?: run {
            // If not cached yet, load it (should be rare after initialization)
            val token = context.applicationContext.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                .getString("token", null)
            tokenCache.set(token)
            token
        }
    }

    companion object {
        // Predefined achievements
        @JvmField
        var PREDEFINED_BADGES = listOf(
            Badge(
                id = "longest_chapter",
                title = "Al-Baqarah Explorer",
                description = "Read the longest chapter of the Quran",
                iconRes = R.drawable.badge_12,
                status = "Not Achieved"
            ),
            Badge(
                id = "shortest_chapter",
                title = "Al-Kawthar Reader",
                description = "Read the shortest chapter of the Quran",
                iconRes = R.drawable.badge_11,
                status = "Not Achieved"
            ),
            Badge(
                id = "weekly_streak",
                title = "Weekly Devotion",
                description = "Maintain a 7-day reading streak",
                iconRes = R.drawable.badge_2,
                status = "Not Achieved"
            )
        )
    }

    fun getAchievementStatus(callback: (Map<String, AchievementStatus>?) -> Unit) {
        val token = getAuthToken()
        if (token != null) {
            quranApi.getAchievementStatus("Bearer $token").enqueue(object : Callback<AchievementStatusResponse> {
                override fun onResponse(
                    call: Call<AchievementStatusResponse>,
                    response: Response<AchievementStatusResponse>
                ) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        callback(response.body()?.achievement_status)
                    } else {
                        callback(null)
                    }
                }

                override fun onFailure(call: Call<AchievementStatusResponse>, t: Throwable) {
                    callback(null)
                }
            })
        }
    }

    fun unlockAchievement(achievementId: String, callback: (Boolean) -> Unit) {
        val token = getAuthToken()
        if(token != null){
            val request = UnlockAchievementRequest(achievementId)
            quranApi.unlockAchievement("Bearer $token", request).enqueue(object : Callback<BaseResponse> {
                override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                    callback(response.isSuccessful && response.body()?.status == "success")
                }
                override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                    callback(false)
                }
            })
        } else{
            callback(false)
        }
    }
    @JvmOverloads
    fun checkStreakEligibility(callback: StreakCheckCallback) {
        // Get token on current thread from cache (fast)
        val token = getAuthToken()

        if (token != null) {
            quranApi.checkStreakAchievement("Bearer $token").enqueue(object : Callback<StreakResponse> {
                override fun onResponse(call: Call<StreakResponse>, response: Response<StreakResponse>) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        val isEligible = response.body()?.is_eligible ?: false
                        val currentStreak = response.body()?.current_streak ?: 0
                        callback.onStreakChecked(isEligible, currentStreak)
                    } else {
                        callback.onStreakChecked(false, 0)
                    }
                }

                override fun onFailure(call: Call<StreakResponse>, t: Throwable) {
                    callback.onStreakChecked(false, 0)
                }
            })
        } else {
            callback.onStreakChecked(false, 0)
        }
    }

}

interface StreakCheckCallback {
    fun onStreakChecked(isEligible: Boolean, currentStreak: Int)
}