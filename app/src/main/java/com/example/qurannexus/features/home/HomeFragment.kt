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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.qurannexus.R
import com.example.qurannexus.features.bookmark.BookmarkFragment
import com.example.qurannexus.features.prayerTimes.PrayerTimesFragment
import com.example.qurannexus.core.interfaces.HighlightClickListener
import com.example.qurannexus.core.interfaces.QuranApi
import com.example.qurannexus.features.analysis.QuranAnalysisFragment
import com.example.qurannexus.features.home.models.Badge
import com.example.qurannexus.features.home.models.HighlightItem
import com.example.qurannexus.features.prayerTimes.models.PrayerTimesResponse
import com.example.qurannexus.features.home.models.DailyInspirationAdapter
import com.example.qurannexus.features.home.models.HighlightsRecyclerAdapter
import com.example.qurannexus.features.auth.AuthService
import com.example.qurannexus.features.bookmark.models.BookmarksResponse
import com.example.qurannexus.features.home.achievement.AchievementService
import com.example.qurannexus.features.home.dailyQuote.DailyQuotesService
import com.example.qurannexus.features.home.dailyQuote.QuoteBookmarkService
import com.example.qurannexus.features.prayerTimes.PrayerTimesViewModel
import com.example.qurannexus.features.recitation.SurahListFragment
import com.example.qurannexus.features.statistics.HomepageStatisticsFragment
import com.example.qurannexus.features.words.models.DailyQuote
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

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

    private lateinit var recitationChart: LineChart
    private lateinit var weeklyRecitationChart: BarChart
    private lateinit var currentStreakValue: TextView
    private lateinit var longestStreakValue: TextView
    private lateinit var consistencyScoreValue: TextView
    @Inject
    lateinit var dailyQuotesService: DailyQuotesService
    @Inject
    lateinit var quoteBookmarkService: QuoteBookmarkService
    @Inject
    lateinit var quranApi: QuranApi

    private val bookmarkedQuoteIds = mutableSetOf<String>()
    private var userToken: String? = null

    private val viewModel: PrayerTimesViewModel by activityViewModels()

    // Replace the existing quotes list with this
    private var inspirationQuotes = listOf(
        DailyQuote(
            Id = "default1",
            Title = "Daily Inspiration",
            Description = "Verily, with hardship comes ease.",
            Source = "Quran 94:5"
        ),
        DailyQuote(
            Id = "default2",
            Title = "Daily Inspiration",
            Description = "The best among you are those who have the best character.",
            Source = "Hadith"
        ),
        DailyQuote(
            Id = "default3",
            Title = "Daily Inspiration",
            Description = "Whoever is kind, Allah will be kind to him; therefore be kind to people on earth.",
            Source = "Hadith"
        )
    )


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        authService = AuthService()
        getUserToken()
        // Initialize UI views
        greetingsText = view.findViewById(R.id.homepageGreetingsText)
        prayerTrailerCard = view.findViewById(R.id.prayerTrailerCard)
        nextPrayerTextView = prayerTrailerCard.findViewById(R.id.nextPrayerTextView)
        timerTextView = prayerTrailerCard.findViewById(R.id.timerTextView)
        dateTextView = prayerTrailerCard.findViewById(R.id.dateTextView)
        seeAllBadgeText = view.findViewById(R.id.seeAllBadgeText)
        llScrollableBadges = view.findViewById(R.id.llScrollableBadges)

        // Set default values immediately
        nextPrayerTextView.text = "Next Prayer: -"
        timerTextView.text = "-"
        dateTextView.text = "-"
        greetingsText.text = "Salaam"

        // Initialize services (but defer loading achievements)
        achievementService = AchievementService(requireContext())

        // Setup basic UI elements
        setupObservers()
        prayerTrailerCard.setOnClickListener {
            loadFragment(PrayerTimesFragment())
        }

        // Initialize UI views
        viewPager = view.findViewById(R.id.viewPager)
        tabLayout = view.findViewById(R.id.tabLayout)

        // Make sure we initialize default quotes
        inspirationQuotes = listOf(
            DailyQuote(
                Id = "default1",
                Title = "Daily Inspiration",
                Description = "Verily, with hardship comes ease.",
                Source = "Quran 94:5"
            ),
            DailyQuote(
                Id = "default2",
                Title = "Daily Inspiration",
                Description = "The best among you are those who have the best character.",
                Source = "Hadith"
            )
        )

        // Setup quotes after view initialization
        setupDailyQuotes()
        // Setup UI that doesn't involve heavy disk operations
        highlightSectionSetup(view)
        setupNavigation()

        // Setup default badges first
        setupAchievements(AchievementService.PREDEFINED_BADGES)

        // Defer heavy operations to after the view is created
        view.post {
            loadInitialData()
            loadUserGreeting()
            loadAchievements()

            if (savedInstanceState == null) {
                childFragmentManager.beginTransaction()
                    .replace(R.id.statisticsContainer, HomepageStatisticsFragment())
                    .commit()
            }
        }

        return view
    }
    private fun setupDailyQuotes() {
        // First, set up UI with default quotes
        setupQuoteViewPager()

        // Then fetch from API
        dailyQuotesService.fetchDailyQuotes(object : DailyQuotesService.DailyQuotesCallback {
            override fun onQuotesReceived(quotes: List<DailyQuote>) {
                if (quotes.isNotEmpty()) {
                    inspirationQuotes = quotes
                    // Update UI on main thread
                    activity?.runOnUiThread {
                        Log.d("HomeFragment", "Received ${quotes.size} quotes from API")
                        setupQuoteViewPager()
                    }
                }
            }

            override fun onError(message: String) {
                Log.e("HomeFragment", "Error fetching quotes: $message")
                // We'll keep using the default quotes that were already set
            }
        })
    }

    private fun setupQuoteViewPager() {
        if (!::viewPager.isInitialized || !::tabLayout.isInitialized) {
            Log.e("HomeFragment", "ViewPager or TabLayout not initialized!")
            return
        }

        try {
            // Create and set the adapter with proper parameters
            val adapter = DailyInspirationAdapter(
                inspirationQuotes,
                requireContext(),
                quoteBookmarkService,  // Pass the service
                userToken,             // Pass the token
                bookmarkedQuoteIds     // Pass the bookmarked IDs set
            )
            viewPager.adapter = adapter
            Log.d("HomeFragment", "New adapter set with ${inspirationQuotes.size} quotes")

            // Connect TabLayout with ViewPager2
            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                // No customization needed
            }.attach()
            Log.d("HomeFragment", "TabLayoutMediator attached")
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error setting up ViewPager: ${e.message}", e)
        }
    }


    private fun getUserToken() {
        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        userToken = sharedPreferences.getString("token", null)
    }

    private fun fetchBookmarkedQuotes() {
        val call = quranApi.getBookmarks("Bearer $userToken")
        call.enqueue(object : Callback<BookmarksResponse> {
            override fun onResponse(call: Call<BookmarksResponse>, response: Response<BookmarksResponse>) {
                if (response.isSuccessful) {
                    val bookmarksResponse = response.body()
                    if (bookmarksResponse != null && bookmarksResponse.status == "success") {
                        // Extract quote IDs from the bookmarks
                        bookmarkedQuoteIds.clear()
                        bookmarksResponse.bookmarks.quotes.forEach { quote ->
                            bookmarkedQuoteIds.add(quote.itemProperties.quoteId)
                        }

                        // Update the UI if ViewPager is already set up
                        activity?.runOnUiThread {
                            setupQuoteViewPager()
                        }
                    }
                } else {
                    Log.e("HomeFragment", "Failed to fetch bookmarks: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<BookmarksResponse>, t: Throwable) {
                Log.e("HomeFragment", "Network error fetching bookmarks", t)
            }
        })
    }

    fun highlightSectionSetup(rootView : View){
        val highlightsRecyclerView: RecyclerView = rootView.findViewById(R.id.highlightsRecyclerView)

        // Create the list of highlights
        val highlightsList = listOf(
            HighlightItem(R.drawable.ic_mosque, "Prayer Times"),
            HighlightItem(R.drawable.ic_analysis, "Quran Analysis"),
            HighlightItem(R.drawable.ic_bookmark_black, "Bookmarks"),
            HighlightItem(R.drawable.ic_quran, "Quran"),
            HighlightItem(R.drawable.ic_note, "Quizzes")
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
            1 -> QuranAnalysisFragment()
            2 -> BookmarkFragment()
            3 -> SurahListFragment()
//            4 -> Quiz()
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
        // Set default greeting immediately so UI is responsive
        greetingsText.text = "Salaam, User"

        // Use a coroutine for disk I/O
        lifecycleScope.launch(Dispatchers.IO) {
            val sharedPreferences = requireContext().applicationContext
                .getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            val username = sharedPreferences.getString("username", null)
            val token = sharedPreferences.getString("token", null)

            withContext(Dispatchers.Main) {
                if (username != null) {
                    // Update UI with username from preferences
                    greetingsText.text = "Salaam, $username"
                } else if (token != null) {
                    // If token exists but no username, try API call (which is already async)
                    authService.getUserProfile(token) { user ->
                        if (user?.name != null) {
                            // Save the username for future reference
                            lifecycleScope.launch(Dispatchers.IO) {
                                sharedPreferences.edit().putString("username", user.name).apply()
                            }
                            greetingsText.text = "Salaam, ${user.name}"
                        }
                    }
                }
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

    override fun onDestroyView() {
        super.onDestroyView()
        // Set adapter to null to allow proper garbage collection
        if (::viewPager.isInitialized) {
            viewPager.adapter = null
        }
    }
}


