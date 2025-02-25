package com.example.qurannexus.features.statistics.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.qurannexus.core.network.ApiService
import com.example.qurannexus.features.statistics.interfaces.StatisticsApi
import com.example.qurannexus.features.statistics.models.DailyRecitation
import com.example.qurannexus.features.statistics.models.RecitationStreakData
import com.example.qurannexus.features.statistics.models.RecitationStreakResponse
import com.example.qurannexus.features.statistics.models.WeekDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
@HiltViewModel
class HomepageStatisticsViewModel @Inject constructor() : ViewModel() {

    private val statisticsApi = ApiService.getQuranClient().create(StatisticsApi::class.java)

    // UI State
    sealed class UiState {
        object Loading : UiState()
        data class Success(
            val streakData: RecitationStreakData,
            val recitationTimes: Map<String, Int>
        ) : UiState()
        data class Error(val message: String) : UiState()
        object Empty : UiState()
    }
    private var recitationTimes: Map<String, Int> = emptyMap()
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    // Chart data
    private val _dailyRecitationData = MutableLiveData<List<Pair<String, Int>>>()
    val dailyRecitationData: LiveData<List<Pair<String, Int>>> = _dailyRecitationData

    private val _weeklyRecitationData = MutableLiveData<List<Pair<Int, Int>>>()
    val weeklyRecitationData: LiveData<List<Pair<Int, Int>>> = _weeklyRecitationData

    private val _currentStreak = MutableLiveData<Int>()
    val currentStreak : LiveData<Int> = _currentStreak

    private val _longestStreak = MutableLiveData<Int>()
    val longestStreak : LiveData<Int> = _longestStreak

    private val _consistencyScore = MutableLiveData<Int>()
    val consistencyScore : LiveData<Int> = _consistencyScore

    fun getWeekDateRange(weekNumber: Int): Pair<Date, Date> {
        // Calculate week's start and end dates
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.WEEK_OF_YEAR, weekNumber)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val startDate = calendar.time

        calendar.add(Calendar.DAY_OF_YEAR, 6)
        val endDate = calendar.time

        return Pair(startDate, endDate)
    }
    fun getWeekDetails(weekNumber: Int): WeekDetails {
        val dateRange = getWeekDateRange(weekNumber)
        val startDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dateRange.first)
        val endDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dateRange.second)

        val dailyRecitations = recitationTimes
            .filter { entry ->
                val date = entry.key
                date >= startDateStr && date <= endDateStr
            }
            .map { entry ->
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(entry.key)!!
                DailyRecitation(date, entry.value)
            }
            .sortedBy { it.date }

        return WeekDetails(
            startDate = dateRange.first,
            endDate = dateRange.second,
            totalMinutes = dailyRecitations.sumOf { it.minutes },
            averageMinutes = if (dailyRecitations.isNotEmpty())
                dailyRecitations.map { it.minutes }.average()
            else 0.0,
            daysRecited = dailyRecitations.size,
            dailyRecitations = dailyRecitations
        )
    }

    fun updateRecitationTimes(times: Map<String, Int>) {
        recitationTimes = times
    }
    fun fetchStatistics(token: String) {
        _uiState.value = UiState.Loading

        statisticsApi.getRecitationStreak("Bearer $token")
            .enqueue(object : retrofit2.Callback<RecitationStreakResponse> {
                override fun onResponse(
                    call: retrofit2.Call<RecitationStreakResponse>,
                    response: retrofit2.Response<RecitationStreakResponse>
                ) {
                    Log.e("home view model", response.body().toString())

                    if (response.isSuccessful && response.body() != null) {
                        val streakData = response.body()!!.streakData
                        processRecitationData(streakData)

                        // Handle potentially null recitationTimes
                        _uiState.value = when {
                            streakData.recitationTimes != null -> UiState.Success(
                                streakData = streakData,
                                recitationTimes = streakData.recitationTimes
                            )
                            else -> UiState.Empty
                        }
                    } else {
                        _uiState.value = UiState.Error("Failed to fetch statistics")
                    }
                }

                override fun onFailure(call: retrofit2.Call<RecitationStreakResponse>, t: Throwable) {
                    _uiState.value = UiState.Error(t.message ?: "Unknown error occurred")
                }
            })
    }

    fun processRecitationData(streakData: RecitationStreakData) {
        val recitationTimes = streakData.recitationTimes ?: emptyMap()

        // Update stored recitation times
        updateRecitationTimes(recitationTimes)

        // Process daily data
        val dailyData = recitationTimes.toList()
            .sortedBy { it.first }
            .takeLast(30) // Last 30 days
        _dailyRecitationData.value = dailyData

        // Process weekly data
        val weeklyData = recitationTimes.entries
            .groupBy {
                getWeekNumber(it.key)
            }
            .mapValues { entry ->
                entry.value.sumOf { it.value }
            }
            .toList()
            .sortedBy { it.first }
        _weeklyRecitationData.value = weeklyData
    }

    private fun getWeekNumber(dateStr: String): Int {
        return try {
            val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .parse(dateStr)
            val cal = java.util.Calendar.getInstance()
            cal.time = date
            cal.get(java.util.Calendar.WEEK_OF_YEAR)
        } catch (e: Exception) {
            0
        }
    }
}