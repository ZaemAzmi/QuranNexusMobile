package com.example.qurannexus.features.prayerTimes

import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.features.prayerTimes.models.PrayerTimesAdapter
import com.example.qurannexus.features.prayerTimes.ui.LocationSelectionDialog
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


@AndroidEntryPoint
class PrayerTimesFragment : Fragment() {

    private val viewModel: PrayerTimesViewModel by activityViewModels()

    private lateinit var dateTextView: TextView
    private lateinit var locationTextView: TextView
    private lateinit var weekdayTextView: TextView
    private lateinit var nextPrayerTextView: TextView
    private lateinit var timerTextView: TextView
    private lateinit var prayerTimesRecycler: RecyclerView
    private lateinit var currentTimeTextView: TextView
    private var timeUpdateHandler: Handler? = null
    private var timeUpdateRunnable: Runnable? = null
    private var countDownTimer: CountDownTimer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            updateCurrentTime()
            handler.postDelayed(this, 1000)
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Just inflate and return the view
        return inflater.inflate(R.layout.fragment_prayer_times, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize all views and set up observers
        initViews(view)
        setupLocationSelection(view)
        observeViewModel()
        startTimeUpdates()

        // Load default data for KL
        val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        viewModel.fetchPrayerTimes(currentDate, "Kuala Lumpur", "MY")
    }
    private fun initViews(view: View) {
        dateTextView = view.findViewById(R.id.dateTextView)
        locationTextView = view.findViewById(R.id.locationTextView)
        weekdayTextView = view.findViewById(R.id.weekdayTextView)
        currentTimeTextView = view.findViewById(R.id.currentTimeTextView)
        nextPrayerTextView = view.findViewById(R.id.nextPrayerTextView)
        timerTextView = view.findViewById(R.id.timerTextView)
        prayerTimesRecycler = view.findViewById(R.id.prayerTimesRecycler)


        prayerTimesRecycler.layoutManager = LinearLayoutManager(context)
        prayerTimesRecycler.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            // Add ItemDecoration if needed
        }
        val locations = listOf(
            "Kuala Lumpur, MY" to Pair("Kuala Lumpur", "MY"),
            "London, GB" to Pair("London", "GB"),
            "Dubai, AE" to Pair("Dubai", "AE")
            // Add more locations as needed
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item,
            locations.map { it.first })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

    }

    private fun setupLocationSelection(view: View) {
        val locationIcon = view.findViewById<ImageView>(R.id.locationIcon)
        locationIcon.setOnClickListener { showLocationDialog() }
        locationTextView.setOnClickListener { showLocationDialog() }
    }

    private fun showLocationDialog() {
        LocationSelectionDialog(requireContext()) { city, country ->
            viewModel.fetchPrayerTimes(
                SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date()),
                city,
                country
            )
            locationTextView.text = "$city, $country"
        }.show()
    }


    private fun startTimeUpdates() {
        val handler = Handler(Looper.getMainLooper())
        val updateTimeRunnable = object : Runnable {
            override fun run() {
                // Only show hours and minutes for current time
                val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                currentTimeTextView.text = "Current Time: $currentTime"
                handler.postDelayed(this, 60000) // Update every minute instead of every second
            }
        }
        handler.post(updateTimeRunnable)
    }
    private fun updateCurrentTime() {
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        currentTimeTextView.text = "Current Time: $currentTime"
    }
    private fun startCountdownTimer(timeInMinutes: Int) {
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(timeInMinutes * 60 * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = millisUntilFinished / (1000 * 60 * 60)
                val minutes = (millisUntilFinished % (1000 * 60 * 60)) / (1000 * 60)
                val seconds = (millisUntilFinished % (1000 * 60)) / 1000

                timerTextView.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            }

            override fun onFinish() {
                // Recalculate next prayer when timer finishes
                viewModel.calculateNextPrayer()
            }
        }.start()
    }

    private fun observeViewModel() {
        viewModel.prayerTimesLiveData.observe(viewLifecycleOwner) { prayerTimes ->
            prayerTimesRecycler.adapter = PrayerTimesAdapter(prayerTimes)
        }

        viewModel.dateLiveData.observe(viewLifecycleOwner) { date ->
            dateTextView.text = date ?: "-"
        }
        viewModel.weekdayLiveData.observe(viewLifecycleOwner) { weekday ->
            weekdayTextView.text = weekday ?: "-"
        }
        viewModel.nextPrayerLiveData.observe(viewLifecycleOwner) { nextPrayer ->
            nextPrayerTextView.text = "Next Prayer: ${nextPrayer?.name ?: "-"}"
        }

        viewModel.timerLiveData.observe(viewLifecycleOwner) { timerText ->
            timerTextView.text = timerText ?: "-"
        }

        viewModel.errorLiveData.observe(viewLifecycleOwner) { error ->
            // Handle error - maybe show a Toast or Snackbar
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
        loadInitialData()
        // Start updating current time
        startTimeUpdates()
    }
    private fun loadInitialData() {
        val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        viewModel.fetchPrayerTimes(currentDate, "Kuala Lumpur", "MY")
    }
    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up handlers and timers
        timeUpdateHandler?.removeCallbacks(timeUpdateRunnable ?: return)
        timeUpdateHandler = null
        timeUpdateRunnable = null
    }
}

