package com.example.qurannexus.features.prayerTimes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qurannexus.R
import com.example.qurannexus.features.prayerTimes.models.PrayerTimesAdapter
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class PrayerTimesFragment : Fragment() {

    private val viewModel: PrayerTimesViewModel by viewModels()

    private lateinit var dateTextView: TextView
    private lateinit var locationTextView: TextView
    private lateinit var weekdayTextView: TextView
    private lateinit var nextPrayerTextView: TextView
    private lateinit var timerTextView: TextView
    private lateinit var prayerTimesRecycler: RecyclerView
    private lateinit var currentTimeTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_prayer_times, container, false)
        initViews(view)
        observeViewModel()
        return view
    }

    private fun initViews(view: View) {
        dateTextView = view.findViewById(R.id.dateTextView)
        locationTextView = view.findViewById(R.id.locationTextView)
        weekdayTextView = view.findViewById(R.id.weekdayTextView)
        currentTimeTextView = view.findViewById(R.id.currentTimeTextView)
        nextPrayerTextView = view.findViewById(R.id.nextPrayerTextView)
        timerTextView = view.findViewById(R.id.timerTextView)
        prayerTimesRecycler = view.findViewById(R.id.prayerTimesRecycler)

        viewModel.fetchPrayerTimes("04-10-2024", "Kuala Lumpur", "MY")
    }

    private fun observeViewModel() {
        viewModel.prayerTimesLiveData.observe(viewLifecycleOwner) { prayerTimes ->
            prayerTimesRecycler.layoutManager = LinearLayoutManager(context)
            prayerTimesRecycler.adapter = PrayerTimesAdapter(prayerTimes)
        }

        viewModel.dateLiveData.observe(viewLifecycleOwner) { date ->
            dateTextView.text = date
            locationTextView.text = "Kuala Lumpur, Malaysia"
        }

        viewModel.weekdayLiveData.observe(viewLifecycleOwner) { weekday ->
            weekdayTextView.text = weekday
        }

        viewModel.nextPrayerLiveData.observe(viewLifecycleOwner) { nextPrayer ->
            nextPrayer?.let {
                nextPrayerTextView.text = "Next Prayer: ${it.name}"
                viewModel.updateCountdown(it.time)
            }
        }

        viewModel.timerLiveData.observe(viewLifecycleOwner) { timerText ->
            timerTextView.text = timerText
        }
    }
}

