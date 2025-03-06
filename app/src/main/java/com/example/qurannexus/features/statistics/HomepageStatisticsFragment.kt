package com.example.qurannexus.features.statistics

import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.qurannexus.R
import com.example.qurannexus.features.statistics.interfaces.RecitationDataReceiver
import com.example.qurannexus.features.statistics.models.RecitationStreakData
import com.example.qurannexus.features.statistics.viewmodels.HomepageStatisticsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class HomepageStatisticsFragment : Fragment() {

    private val viewModel: HomepageStatisticsViewModel by viewModels()

    private lateinit var chartViewPager: ViewPager2
    private lateinit var chartTabLayout: TabLayout
    private lateinit var currentStreakValue: TextView
    private lateinit var longestStreakValue: TextView
    private lateinit var consistencyScoreValue: TextView
    private lateinit var consistencyLabel: TextView

    // Chart fragments
    private val fragments = listOf<Fragment>(
        DailyChartFragment(),
        WeeklyChartFragment()
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_homepage_recitation_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupViewPager()
        setupInfoButton()
        observeViewModel()
        fetchData()
    }

    private fun initializeViews(view: View) {
        currentStreakValue = view.findViewById(R.id.currentStreakValue)
        longestStreakValue = view.findViewById(R.id.longestStreakValue)
        consistencyScoreValue = view.findViewById(R.id.consistencyScoreValue)
        consistencyLabel = view.findViewById(R.id.consistencyLabel)
        chartViewPager = view.findViewById(R.id.chartViewPager)
        chartTabLayout = view.findViewById(R.id.chartTabLayout)
    }
    interface ChartVisibilityHandler {
        fun onBecameVisible()
        fun onBecameHidden()
    }
    private fun setupViewPager() {
        // Set up the adapter for the ViewPager
        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = fragments.size
            override fun createFragment(position: Int): Fragment = fragments[position]
        }

        chartViewPager.adapter = adapter
        chartViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // Ensure the correct fragment's chart is visible
                fragments.forEachIndexed { index, fragment ->
                    if (fragment is RecitationDataReceiver) {
                        // Tell the fragment whether it's selected or not
                        if (index == position) {
                            (fragment as? ChartVisibilityHandler)?.onBecameVisible()
                        } else {
                            (fragment as? ChartVisibilityHandler)?.onBecameHidden()
                        }
                    }
                }
            }
        })
        // Connect the TabLayout with the ViewPager
        TabLayoutMediator(chartTabLayout, chartViewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Daily"
                1 -> "Weekly"
                else -> ""
            }
        }.attach()
    }

    private fun setupInfoButton() {
        view?.findViewById<View>(R.id.btnInfoRecitation)?.setOnClickListener {
            showInfoDialog()
        }
    }

    private fun showInfoDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("About Recitation Stats")
            .setMessage(
                "• Current Streak: Days in a row you've recited\n" +
                        "• Longest Streak: Your record for consecutive days\n" +
                        "• Consistency: Average days per week you recite\n\n" +
                        "Keep reciting daily to build your streak and improve consistency!"
            )
            .setPositiveButton("Got it") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun updateStats(streakData: RecitationStreakData) {
        // Apply animations for value changes
        animateTextChange(currentStreakValue, currentStreakValue.text.toString(),
            streakData.currentStreak.toString())
        animateTextChange(longestStreakValue, longestStreakValue.text.toString(),
            streakData.longestStreak.toString())

        // Update consistency score with improved display
        updateConsistencyDisplay(streakData)

        // Notify chart fragments about data update
        fragments.forEach {
            if (it is RecitationDataReceiver) {
                it.onRecitationDataReceived(streakData)
            }
        }
    }

    private fun animateTextChange(textView: TextView, oldValue: String, newValue: String) {
        // Skip animation if the values are the same or if oldValue is not a number
        if (oldValue == newValue || oldValue.toIntOrNull() == null) {
            textView.text = newValue
            return
        }

        val oldNum = oldValue.toInt()
        val newNum = newValue.toInt()

        // Simple counting animation
        val animator = ValueAnimator.ofInt(oldNum, newNum)
        animator.duration = 1000
        animator.addUpdateListener { animation ->
            textView.text = animation.animatedValue.toString()
        }
        animator.start()
    }

    private fun animateFloatTextChange(textView: TextView, oldValue: Float, newValue: Float) {
        // Skip animation if the values are the same
        if (oldValue == newValue) {
            textView.text = String.format("%.1f", newValue)
            return
        }

        val animator = ValueAnimator.ofFloat(oldValue, newValue)
        animator.duration = 1000
        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Float
            textView.text = String.format("%.1f", animatedValue)
        }
        animator.start()
    }

    private fun showError(message: String) {
        // Show a more user-friendly error message
        val errorSnackbar = Snackbar.make(
            requireView(),
            "Couldn't load your stats: $message",
            Snackbar.LENGTH_LONG
        )
        errorSnackbar.setAction("Retry") {
            fetchData()
        }
        errorSnackbar.show()
    }

    private fun updateConsistencyDisplay(streakData: RecitationStreakData) {
        // Get consistency metrics if available
        val consistencyMetrics = streakData.consistencyMetrics

        if (consistencyMetrics != null) {
            // Use the days per week metric
            val daysPerWeek = consistencyMetrics["days_per_week"] as? Float
            if (daysPerWeek != null) {
                // Display as "3.7 days/week"
                consistencyScoreValue.text = String.format("%.1f", daysPerWeek)

                // Update the label to say "days/week"
                consistencyLabel.text = "days/week"
                return
            }
        }

        // Fallback to traditional percentage if new metrics aren't available
        consistencyScoreValue.text = "${streakData.consistencyScore}%"
        consistencyLabel.text = "Consistency"
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is HomepageStatisticsViewModel.UiState.Loading -> {
                    // Show loading state if needed
                }
                is HomepageStatisticsViewModel.UiState.Success -> {
                    // Always update basic stats
                    updateStats(state.streakData)
                }
                is HomepageStatisticsViewModel.UiState.Error -> {
                    showError(state.message)
                }
                HomepageStatisticsViewModel.UiState.Empty -> {
                    // Update UI with empty/default values
                    setupEmptyStates()
                }
            }
        }
    }

    private fun setupEmptyStates() {
        currentStreakValue.text = "0"
        longestStreakValue.text = "0"
        consistencyScoreValue.text = "0"

        // Update label for consistency if using days/week
        consistencyLabel.text = "days/week"

        // Notify fragments about empty state
        fragments.forEach {
            if (it is RecitationDataReceiver) {
                it.onEmptyState()
            }
        }
    }

    private fun fetchData() {
        val token = requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE)
            .getString("token", null)

        if (token != null) {
            viewModel.fetchStatistics(token)
        } else {
            showError("Please log in again")
        }
    }
}