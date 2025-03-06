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
        // Create calendar for the specified week
        val calendar = Calendar.getInstance()
        calendar.clear()
        calendar.set(Calendar.YEAR, 2025)  // Fixed year for consistency
        calendar.set(Calendar.WEEK_OF_YEAR, weekNumber)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        // Get the start date (Monday of the week)
        val startDate = calendar.time

        // Add 6 days to get to Sunday (end of the week)
        calendar.add(Calendar.DAY_OF_YEAR, 6)
        val endDate = calendar.time

        // Log the calculated dates
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        Log.d("WeekRange", "Week $weekNumber: ${dateFormatter.format(startDate)} to ${dateFormatter.format(endDate)}")

        return Pair(startDate, endDate)
    }
    fun getWeekDetails(weekNumber: Int): WeekDetails {
        val dateRange = getWeekDateRange(weekNumber)
        val startDate = dateRange.first
        val endDate = dateRange.second

        // Format dates to match database format
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startDateStr = dateFormat.format(startDate)
        val endDateStr = dateFormat.format(endDate)

        // Log the week range
        Log.d("WeekDetails", "Getting details for week $weekNumber: $startDateStr to $endDateStr")

        // Create a calendar to iterate through all days in the week
        val calendar = Calendar.getInstance().apply {
            time = startDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endCalendar = Calendar.getInstance().apply {
            time = endDate
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }

        // Create a map to hold all days in the week with their minutes
        val daysMap = mutableMapOf<String, Int>()

        // Initialize all days in the range with 0 minutes
        while (!calendar.after(endCalendar)) {
            val currentDateStr = dateFormat.format(calendar.time)
            daysMap[currentDateStr] = 0
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Log all initialized days
        Log.d("WeekDetails", "Initialized days: ${daysMap.keys.joinToString()}")

        // Add actual recitation times for days within this exact range
        var totalMinutes = 0
        recitationTimes.forEach { (dateStr, minutes) ->
            // Use strict date comparison
            if (dateStr >= startDateStr && dateStr <= endDateStr) {
                daysMap[dateStr] = minutes
                totalMinutes += minutes
                Log.d("WeekDetails", "Adding $minutes minutes from $dateStr to total")
            }
        }

        Log.d("WeekDetails", "Total minutes calculated: $totalMinutes")

        // Convert to DailyRecitation objects
        val dailyRecitations = daysMap.map { (dateStr, minutes) ->
            val date = dateFormat.parse(dateStr)!!
            DailyRecitation(date, minutes)
        }.sortedBy { it.date }

        // Calculate statistics
        val daysWithRecitation = dailyRecitations.count { it.minutes > 0 }

        // Calculate average minutes (only for days with recitation)
        val averageMinutes = if (daysWithRecitation > 0) {
            totalMinutes.toDouble() / daysWithRecitation
        } else {
            0.0
        }

        return WeekDetails(
            startDate = startDate,
            endDate = endDate,
            totalMinutes = totalMinutes,
            averageMinutes = averageMinutes,
            daysRecited = daysWithRecitation,
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
        // Update recitation times
        val recitationTimes = streakData.recitationTimes ?: emptyMap()
        updateRecitationTimes(recitationTimes)

        // Process daily data
        val dailyData = recitationTimes.toList()
            .sortedBy { it.first }
            .takeLast(30) // Last 30 days
        _dailyRecitationData.postValue(dailyData)

        // Process weekly data with strict week boundaries
        processWeeklyRecitationData(recitationTimes)
    }
    fun processWeeklyRecitationData(recitationTimes: Map<String, Int>) {
        val weeklyData = mutableMapOf<Int, Int>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // First, determine all weeks in the data
        val calendar = Calendar.getInstance()
        val weeks = mutableSetOf<Int>()

        recitationTimes.keys.forEach { dateStr ->
            try {
                val date = dateFormat.parse(dateStr)
                if (date != null) {
                    calendar.time = date
                    weeks.add(calendar.get(Calendar.WEEK_OF_YEAR))
                }
            } catch (e: Exception) {
                Log.e("HomepageStatisticsViewModel", "Error parsing date: $dateStr", e)
            }
        }

        // For each week, calculate the date range and sum minutes
        weeks.forEach { weekNumber ->
            val weekRange = getWeekDateRange(weekNumber)
            val startDate = weekRange.first
            val endDate = weekRange.second

            val startDateStr = dateFormat.format(startDate)
            val endDateStr = dateFormat.format(endDate)

            // Log the date range we're searching for
            Log.d("WeeklyDataCalc", "Processing week $weekNumber: $startDateStr to $endDateStr")

            // Calculate total for this week by filtering and summing
            var weekTotal = 0
            recitationTimes.forEach { (dateStr, minutes) ->
                if (dateStr >= startDateStr && dateStr <= endDateStr) {
                    weekTotal += minutes
                    Log.d("WeeklyDataCalc", "  Adding $minutes minutes from $dateStr to week $weekNumber")
                }
            }

            // Store the total
            weeklyData[weekNumber] = weekTotal
            Log.d("WeeklyDataCalc", "Week $weekNumber total: $weekTotal minutes")
        }

        // Convert to sorted list for the chart
        val chartData = weeklyData.map { (week, total) ->
            Pair(week, total)
        }.sortedBy { it.first }

        // Update LiveData
        _weeklyRecitationData.postValue(chartData)
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