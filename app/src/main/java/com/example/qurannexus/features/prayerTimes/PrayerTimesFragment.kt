package com.example.qurannexus.features.prayerTimes

import PrayerTimesApi
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.features.prayerTimes.models.PrayerTime
import com.example.qurannexus.features.prayerTimes.models.PrayerTimesResponse
import com.example.qurannexus.features.prayerTimes.models.PrayerTimesAdapter
import com.example.qurannexus.core.network.ApiService
// Ensure you import the correct Retrofit callback
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*


class PrayerTimesFragment : Fragment() {

    private lateinit var dateTextView: TextView
    private lateinit var locationTextView: TextView
    private lateinit var weekdayTextView: TextView
    private lateinit var nextPrayerTextView: TextView
    private lateinit var timerTextView: TextView
    private lateinit var prayerTimesRecycler: RecyclerView
    private lateinit var currentTimeTextView: TextView
    private val prayerTimes: List<PrayerTime> = listOf() // Initialize with prayer times

    private var currentPrayerIndex = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_prayer_times, container, false)

        // Initialize views
        dateTextView = view.findViewById(R.id.dateTextView)
        locationTextView = view.findViewById(R.id.locationTextView)
        weekdayTextView = view.findViewById(R.id.weekdayTextView)
        currentTimeTextView = view.findViewById(R.id.tv_current_time)
        nextPrayerTextView = view.findViewById(R.id.nextPrayerTextView)
        timerTextView = view.findViewById(R.id.timerTextView)
        prayerTimesRecycler = view.findViewById(R.id.prayerTimesRecycler)

        fetchPrayerTimes()
        startUpdatingCurrentTime()
        return view
    }

    private fun fetchPrayerTimes() {
        val apiService = ApiService.getPrayerTimesClient().create(PrayerTimesApi::class.java)
        val call = apiService.getPrayerTimes("04-10-2024","Kuala Lumpur", "MY")

        call.enqueue(object : Callback<PrayerTimesResponse> {
            override fun onResponse(
                call: Call<PrayerTimesResponse>,
                response: Response<PrayerTimesResponse>
            ) {
                if (response.isSuccessful) {
                    val prayerTimesResponse = response.body()
                    prayerTimesResponse?.let { data ->
                        val timings = data.data?.timings
                        val date = data.data?.date

                        // Set date and location
                        dateTextView.text = date?.readable
                        locationTextView.text = "Kuala Lumpur, Malaysia"
                        weekdayTextView.text = date?.gregorian?.weekday?.en

                        // Set prayer times in RecyclerView
                        val prayerTimesList = listOf(
                            PrayerTime("Fajr", timings?.Fajr ?: ""),
                            PrayerTime("Sunrise", timings?.Sunrise ?: ""),
                            PrayerTime("Dhuhr", timings?.Dhuhr ?: ""),
                            PrayerTime("Asr", timings?.Asr ?: ""),
                            PrayerTime("Maghrib", timings?.Maghrib ?: ""),
                            PrayerTime("Isha", timings?.Isha ?: ""),
                            PrayerTime("Imsak", timings?.Imsak ?: "")
                        )

                        prayerTimesRecycler.layoutManager = LinearLayoutManager(context)
                        prayerTimesRecycler.adapter = PrayerTimesAdapter(prayerTimesList)

                        calculateNextPrayer(timings)
                        startCountdownTimer(timings)
                    }
                } else {
                    // Handle failure
                }
            }

            override fun onFailure(call: Call<PrayerTimesResponse>, t: Throwable) {
                // Handle failure (e.g., no network connection)
            }
        })
    }

    private fun calculateNextPrayer(timings: PrayerTimesResponse.Timings?) {
        val currentTime = getCurrentTimeIn24H()

        // List of prayer times in 24-hour format
        val prayerTimesList = listOf(
            timings?.Fajr,
            timings?.Sunrise,
            timings?.Dhuhr,
            timings?.Asr,
            timings?.Maghrib,
            timings?.Isha,
            timings?.Imsak
        ).map { time -> convertTimeToMinutes(time) }

        // Find the next prayer
        var nextPrayerTime: String? = null
        for (i in prayerTimesList.indices) {
            if (currentTime < prayerTimesList[i]) {
                nextPrayerTime = prayerTimesList[i].toString()
                currentPrayerIndex = i
                break
            }
        }
        nextPrayerTextView.text = "Next Prayer: ${getPrayerName(currentPrayerIndex)}"
    }

    private fun convertTimeToMinutes(time: String?): Int {
        time?.let {
            val timeParts = it.split(":")
            if (timeParts.size == 2) {
                val hours = timeParts[0].toInt()
                val minutes = timeParts[1].toInt()
                return hours * 60 + minutes
            }
        }
        return 0
    }

    private fun getCurrentTimeIn24H(): Int {
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val timeParts = currentTime.split(":")
        val hours = timeParts[0].toInt()
        val minutes = timeParts[1].toInt()
        return hours * 60 + minutes
    }
    private fun getPrayerName(index: Int): String {
        return when (index) {
            0 -> "Fajr"
            1 -> "Sunrise"
            2 -> "Dhuhr"
            3 -> "Asr"
            4 -> "Maghrib"
            5 -> "Isha"
            6 -> "Imsak"
            else -> "Unknown"
        }
    }
    private fun startUpdatingCurrentTime() {
        val handler = Handler(Looper.getMainLooper())
        val updateTimeRunnable = object : Runnable {
            override fun run() {
                updateCurrentTime()
                handler.postDelayed(this, 1000) // Update every second
            }
        }
        handler.post(updateTimeRunnable) // Start the handler
    }
    // Method to update current time in TextView
    private fun updateCurrentTime() {
        val currentTime = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date()) // 12-hour format
        currentTimeTextView.text = currentTime
    }
    private data class PrayerSchedule(
        val name: String,
        val time: String
    )
    private fun getPrayerSchedule(timings: PrayerTimesResponse.Timings?): List<PrayerSchedule> {
        return listOf(
            PrayerSchedule("Fajr", timings?.Fajr ?: ""),
            PrayerSchedule("Sunrise", timings?.Sunrise ?: ""),
            PrayerSchedule("Dhuhr", timings?.Dhuhr ?: ""),
            PrayerSchedule("Asr", timings?.Asr ?: ""),
            PrayerSchedule("Maghrib", timings?.Maghrib ?: ""),
            PrayerSchedule("Isha", timings?.Isha ?: "")
        )
    }

    private fun findNextPrayer(prayers: List<PrayerSchedule>): PrayerSchedule? {
        val currentTimeInMinutes = getCurrentTimeIn24H()

        return prayers.firstOrNull { prayer ->
            convertTimeToMinutes(prayer.time) > currentTimeInMinutes
        } ?: prayers.firstOrNull() // If no next prayer today, return first prayer of next day
    }

    private fun startCountdownTimer(timings: PrayerTimesResponse.Timings?) {
        var currentTimer: CountDownTimer? = null

        fun startNewTimer(prayerSchedule: PrayerSchedule) {
            val prayerTimeInMinutes = convertTimeToMinutes(prayerSchedule.time)
            val currentTimeInMinutes = getCurrentTimeIn24H()

            var timeLeft = prayerTimeInMinutes - currentTimeInMinutes

            // If the prayer is tomorrow (negative time difference)
            if (timeLeft < 0) {
                timeLeft += 24 * 60 // Add 24 hours in minutes
            }

            currentTimer?.cancel() // Cancel any existing timer

            currentTimer = object : CountDownTimer(timeLeft * 60 * 1000L, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val secondsLeft = millisUntilFinished / 1000
                    val minutesLeft = secondsLeft / 60
                    val hoursLeft = minutesLeft / 60

                    timerTextView.text = String.format(
                        "Next Prayer: %02d:%02d:%02d",
                        hoursLeft,
                        minutesLeft % 60,
                        secondsLeft % 60
                    )
                }

                override fun onFinish() {
                    // Find and start timer for the next prayer
                    val prayers = getPrayerSchedule(timings)
                    val nextPrayer = findNextPrayer(prayers)

                    nextPrayer?.let {
                        startNewTimer(it)
                    }
                }
            }.start()
        }

        // Start the initial timer
        val prayers = getPrayerSchedule(timings)
        val nextPrayer = findNextPrayer(prayers)

        nextPrayer?.let {
            startNewTimer(it)
        }
    }
}

//    private fun startCountdownTimer(timings: PrayerTimesResponse.Timings?) {
//        val nextPrayerTimeInMinutes = convertTimeToMinutes(timings?.Fajr) // You can modify based on next prayer
//        val currentTimeInMinutes = getCurrentTimeIn24H()
//
//        val timeLeft = nextPrayerTimeInMinutes - currentTimeInMinutes
//
//        val countDownTimer = object : CountDownTimer(timeLeft * 60 * 1000L, 1000) {
//            override fun onTick(millisUntilFinished: Long) {
//                val secondsLeft = millisUntilFinished / 1000
//                val minutesLeft = secondsLeft / 60
//                val hoursLeft = minutesLeft / 60
//
//                timerTextView.text = String.format("%02d:%02d:%02d", hoursLeft, minutesLeft % 60, secondsLeft % 60)
//            }
//
//            override fun onFinish() {
//                timerTextView.text = "Time for prayer!"
//
//            }
//        }
//        countDownTimer.start()
//    }