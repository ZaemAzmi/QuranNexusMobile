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
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
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
import com.example.qurannexus.features.home.achievement.AchievementService
import com.example.qurannexus.features.prayerTimes.PrayerTimesViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
@AndroidEntryPoint
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

    private lateinit var achievementService: AchievementService

    private val viewModel: PrayerTimesViewModel by activityViewModels()

    private val quotes = listOf(
        "And those who strive for Us- We will surely guide them to Our ways.",
        "Indeed, Allah is with those who fear Him and those who are doers of good.",
        "So remember Me; I will remember you."
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


        achievementService = AchievementService(requireContext())
        setupAchievements(AchievementService.PREDEFINED_BADGES)

        loadAchievements()

        loadUserGreeting()
        prayerTrailerCard.setOnClickListener {
           loadFragment(PrayerTimesFragment())
        }
        // Set default values
        nextPrayerTextView.text = "Next Prayer: -"
        timerTextView.text = "-"
        dateTextView.text = "-"

        setupObservers()
        loadInitialData()

        viewPager = view.findViewById(R.id.viewPager)
        tabLayout = view.findViewById(R.id.tabLayout)
        val adapter = DailyInspirationAdapter(quotes, requireContext())
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            // Optionally, you can customize the tab, but this will show dots by default
        }.attach()
        highlightSectionSetup(view)
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
    private fun setupObservers() {
        viewModel.apply {
            dateLiveData.observe(viewLifecycleOwner) { date ->
                dateTextView.text = date ?: "-"
            }

            nextPrayerLiveData.observe(viewLifecycleOwner) { nextPrayer ->
                nextPrayerTextView.text = "Next Prayer: ${nextPrayer?.name ?: "-"}"
            }

            timerLiveData.observe(viewLifecycleOwner) { timerText ->
                if (timerText != null) {
                    timerTextView.text = "Time Remaining: $timerText"
                } else {
                    timerTextView.text = "-"
                }
            }
        }
    }
    private fun loadInitialData() {
        val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        viewModel.fetchPrayerTimes(currentDate, "Kuala Lumpur", "MY")
    }
    private fun handleApiFailure(message: String) {
        // Display a message to the user in case of failure
        timerTextView.text = message
        nextPrayerTextView.text = ""
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

                    if (user != null) {
                        Log.d("AuthDebugHme", "User fetched: ${user.name}")
                    } else {
                        Log.e("AuthDebugHome", "Failed to fetch user profile.")
                    }
                    if (user?.name != null) {
                        // Save the username in SharedPreferences
                        sharedPreferences.edit().putString("username", user.name).apply()
                        greetingsText.text = "Salaam, ${user.name}"
                    } else {
                        greetingsText.text = "Salaam, User"
                    }
                }
            } else {
                greetingsText.text = "Salaam, User"
            }
        }
    }

    private fun loadAchievements() {
        achievementService.getAchievementStatus { statusMap ->
            if (statusMap != null) {
                // Convert predefined badges with status
                val updatedBadges = AchievementService.PREDEFINED_BADGES.map { badge ->
                    val status = statusMap[badge.id]
                    badge.copy(
                        status = status?.status ?: "Not Achieved"
                    )
                }
                setupAchievements(updatedBadges)
            } else {
                // If failed to get status, show predefined badges with default status
                setupAchievements(AchievementService.PREDEFINED_BADGES)
            }
        }
    }

    fun setupAchievements(badges: List<Badge>) {
        llScrollableBadges.removeAllViews() // Clear existing badges

        badges.forEach { badge ->
            val badgeView = LayoutInflater.from(context).inflate(R.layout.card_badge_achievement, llScrollableBadges, false)
            val badgeIcon: ImageView = badgeView.findViewById(R.id.ivBadgeIcon)
            val badgeTitle: TextView = badgeView.findViewById(R.id.tvBadgeTitle)

            badgeIcon.setImageResource(badge.iconRes)
            badgeTitle.text = badge.title

            badgeView.setOnClickListener {
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


