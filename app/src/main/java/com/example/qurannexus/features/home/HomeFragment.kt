package com.example.qurannexus.features.home

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.qurannexus.R
import com.example.qurannexus.features.bookmark.BookmarkFragment
import com.example.qurannexus.features.tajweed.TajweedFragment
import com.example.qurannexus.features.prayerTimes.PrayerTimesFragment
import com.example.qurannexus.core.interfaces.HighlightClickListener
import com.example.qurannexus.features.home.models.Badge
import com.example.qurannexus.features.home.models.HighlightItem
import com.example.qurannexus.features.prayerTimes.models.PrayerTimesResponse
import com.example.qurannexus.features.home.models.DailyInspirationAdapter
import com.example.qurannexus.features.home.models.HighlightsRecyclerAdapter
import com.example.qurannexus.features.auth.AuthService
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class HomeFragment : Fragment(), HighlightClickListener {

    private lateinit var prayerTrailerCard: View
    private lateinit var nextPrayerTextView: TextView
    private lateinit var timerTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var greetingsText: TextView
    private lateinit var authService: AuthService
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    private lateinit var seeAllBadgeText : TextView
    private lateinit var llScrollableBadges : LinearLayout
    private val quotes = listOf(
        "And those who strive for Us- We will surely guide them to Our ways.",
        "Indeed, Allah is with those who fear Him and those who are doers of good.",
        "So remember Me; I will remember you."
    )

    private val topBadges = listOf(
        Badge(
            title = "Early Riser",
            description = "Awarded for checking the app during Fajr time for 7 consecutive days.",
            iconRes = R.drawable.badge_1
        ),
        Badge(
            title = "Quran Explorer",
            description = "Granted for reading verses from 10 different surahs.",
            iconRes = R.drawable.badge_2
        ),
        Badge(
            title = "Daily Devotion",
            description = "Earned for completing at least one verse every day for a month.",
            iconRes = R.drawable.badge_3
        ),
        Badge(
            title = "Consistency King",
            description = "Unlocked for using the app daily for 100 days in a row.",
            iconRes = R.drawable.badge_2
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        authService = AuthService()
        greetingsText = view.findViewById(R.id.homepageGreetingsText)
        prayerTrailerCard = view.findViewById(R.id.prayerTrailerCard)
        nextPrayerTextView = prayerTrailerCard.findViewById(R.id.nextPrayerTextView)
        timerTextView = prayerTrailerCard.findViewById(R.id.timerTextView)
        dateTextView = prayerTrailerCard.findViewById(R.id.dateTextView)

        seeAllBadgeText = view.findViewById(R.id.seeAllBadgeText)
        llScrollableBadges = view.findViewById(R.id.llScrollableBadges)

        loadUserGreeting()
        prayerTrailerCard.setOnClickListener {
           loadFragment(PrayerTimesFragment())
        }
        fetchPrayerTimes()


        viewPager = view.findViewById(R.id.viewPager)
        tabLayout = view.findViewById(R.id.tabLayout)
        val adapter = DailyInspirationAdapter(quotes, requireContext())
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            // Optionally, you can customize the tab, but this will show dots by default
        }.attach()
        highlightSectionSetup(view)
        setupAchievements()
        setupNavigation()
        seeAllBadgeText.setOnClickListener {
            val intent = Intent(context, BadgesActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    fun highlightSectionSetup(rootView : View){
        val highlightsRecyclerView: RecyclerView = rootView.findViewById(R.id.highlightsRecyclerView)

        // Create the list of highlights
        val highlightsList = listOf(
            HighlightItem(R.drawable.ic_mosque, "Prayer Times"),
            HighlightItem(R.drawable.ic_duas, "Duas"),
            HighlightItem(R.drawable.ic_quran, "Bookmarks"),
            HighlightItem(R.drawable.ic_chatbot, "Tajweed"),
            HighlightItem(R.drawable.ic_mosque, "NexusAI")
        )

        val layoutManager = object : LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) {
            override fun checkLayoutParams(lp: RecyclerView.LayoutParams?): Boolean {
                // Adjust item width to show approximately 2.5 items
                lp?.width = (width / 4.5).toInt()
                return true
            }
        }
        highlightsRecyclerView.layoutManager = layoutManager

        val adapter = HighlightsRecyclerAdapter(highlightsList, this)
        highlightsRecyclerView.adapter = adapter

        // Add a small space between items
        val itemSpacing = resources.getDimensionPixelSize(R.dimen.highlight_item_spacing)
        highlightsRecyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                outRect.right = itemSpacing
                outRect.left = itemSpacing
            }
        })
        // Enable snapping to each item
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(highlightsRecyclerView)
    }

    override fun onHighlightClick(position: Int) {
        val selectedFragment : Fragment = when(position){
            0 -> PrayerTimesFragment()
//            1 -> DuasFragment()
            2 -> BookmarkFragment()
            3 -> TajweedFragment()
//            4 -> NexusAIFragment()
            else -> return
        }
        loadFragment(selectedFragment)
    }
    private fun loadFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.mainFragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
    private fun fetchPrayerTimes() {
//        val apiService = ApiService.getPrayerTimesClient().create(PrayerTimesApi::class.java)
//        val call = apiService.getPrayerTimes("04-10-2024","Kuala Lumpur", "MY")
//
//        call.enqueue(object : Callback<PrayerTimesResponse> {
//            override fun onResponse(
//                call: Call<PrayerTimesResponse>,
//                response: Response<PrayerTimesResponse>
//            ) {
//                if (response.isSuccessful) {
//                    val prayerTimesResponse = response.body()
//                    prayerTimesResponse?.let { data ->
//                        val timings = data.data?.timings
//                        val date = data.data?.date
//
//                        // Set date and location
//                        dateTextView.text = date?.readable
//                        locationTextView.text = "Kuala Lumpur, Malaysia"
//                        weekdayTextView.text = date?.gregorian?.weekday?.en
//
//                        // Set prayer times in RecyclerView
//                        val prayerTimesList = listOf(
//                            PrayerTime("Fajr", timings?.Fajr ?: ""),
//                            PrayerTime("Sunrise", timings?.Sunrise ?: ""),
//                            PrayerTime("Dhuhr", timings?.Dhuhr ?: ""),
//                            PrayerTime("Asr", timings?.Asr ?: ""),
//                            PrayerTime("Maghrib", timings?.Maghrib ?: ""),
//                            PrayerTime("Isha", timings?.Isha ?: "")
//                        )
//
//                        prayerTimesRecycler.layoutManager = LinearLayoutManager(context)
//                        prayerTimesRecycler.adapter = PrayerTimesAdapter(prayerTimesList)
//
//                        // Calculate next prayer and start countdown timer
//                        calculateNextPrayer(timings)
//                        startCountdownTimer(timings)
//                    }
//                } else {
//                    handleApiFailure("Failed to fetch prayer times")
//                }
//            }
//
//            override fun onFailure(call: Call<PrayerTimesResponse>, t: Throwable) {
//                handleApiFailure("Network error: Unable to retrieve prayer times")
//            }
//        })
    }

    private fun handleApiFailure(message: String) {
        // Display a message to the user in case of failure
        timerTextView.text = message
        nextPrayerTextView.text = ""
    }

    private fun calculateNextPrayer(timings: PrayerTimesResponse.Timings?) {
//        val currentTime = getCurrentTimeIn24H()
//
//        // Convert prayer times to minutes and find the next prayer
//        val prayerTimesList = listOf(
//            timings?.Fajr,
//            timings?.Sunrise,
//            timings?.Dhuhr,
//            timings?.Asr,
//            timings?.Maghrib,
//            timings?.Isha
//        ).mapIndexed { index, time ->
//            Pair(index, convertTimeToMinutes(time))
//        }
//
//        // Get the next prayer time
//        val nextPrayer = prayerTimesList.find { currentTime < it.second }
//            ?: prayerTimesList.first() // Wrap to the first prayer if all times have passed
//
//        currentPrayerIndex = nextPrayer.first
//        nextPrayerTextView.text = "Next Prayer: ${getPrayerName(currentPrayerIndex)}"
    }

    private fun startCountdownTimer(timings: PrayerTimesResponse.Timings?) {
        var currentTimer: CountDownTimer? = null

        // Prepare the schedule and start timer for the next prayer
//        val prayers = getPrayerSchedule(timings)
//        val nextPrayer = findNextPrayer(prayers)
//
//        nextPrayer?.let {
//            startNewTimer(it)
//        }
    }
    private fun loadUserGreeting() {
        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        var username = sharedPreferences.getString("username", null)
        val token = sharedPreferences.getString("token", null) // Get the stored token
        if (username != null) {
            // If username is available, display it
            greetingsText.text = "Salaam, $username"
        } else {
            // If no username, try to fetch it using the token
            if (token != null) {
                // If token is available, make the network request
                authService.getUserProfile(token) { user ->
                    if (user?.name != null) {
                        // Save the username in SharedPreferences
                        sharedPreferences.edit().putString("username", user.name).apply()
                        greetingsText.text = "Salaam, ${user.name}"
                    } else {
                        greetingsText.text = "Salaam, Guest"
                    }
                }
            } else {
                greetingsText.text = "Salaam, Guest"
            }
        }
    }
    private fun setupAchievements() {
        topBadges.forEach { badge ->
            val badgeView = LayoutInflater.from(context).inflate(R.layout.card_badge_achievement, llScrollableBadges, false)
            val badgeIcon: ImageView = badgeView.findViewById(R.id.ivBadgeIcon)
            val badgeTitle: TextView = badgeView.findViewById(R.id.tvBadgeTitle)

            badgeIcon.setImageResource(badge.iconRes)
            badgeTitle.text = badge.title
            badgeView.isClickable = true
            badgeView.isFocusable = true
            badgeView.setOnClickListener {
                Log.d("BadgeClick", "Clicked on: ${badge.title}")
                showBadgePopup(badge)
            }

            llScrollableBadges.addView(badgeView)
        }
    }

    private fun setupNavigation() {
        seeAllBadgeText.setOnClickListener {
            // Navigate to All Badges Page
            val intent = Intent(context, BadgesActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showBadgePopup(badge: Badge) {
        context?.let {
            Log.d("HomeFragment", "Opening dialog for badge: ${badge.title}")
            val dialog = BadgeDetailsDialog(it, badge)
            dialog.show()
        } ?: Log.e("HomeFragment", "Context is null. Cannot show dialog.")
    }


}


