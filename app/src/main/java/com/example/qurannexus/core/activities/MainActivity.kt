package com.example.qurannexus.core.activities

// Removed unused MeowBottomNavigation Listeners for brevity, add back if used
// import com.etebarian.meowbottomnavigation.MeowBottomNavigation.ClickListener
// import com.etebarian.meowbottomnavigation.MeowBottomNavigation.ReselectListener
// import com.etebarian.meowbottomnavigation.MeowBottomNavigation.ShowListener
// import com.example.qurannexus.core.utils.QuranMetadata // Not directly used in this revised method
// import com.example.qurannexus.features.recitation.ByAyatRecitationFragment // RecitationPageFragment handles this
// import com.example.qurannexus.features.recitation.models.SurahModel // Not needed for this navigation
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import com.etebarian.meowbottomnavigation.MeowBottomNavigation
import com.example.qurannexus.R
import com.example.qurannexus.core.enums.BottomMenuItemId
import com.example.qurannexus.core.utils.TokenManager
import com.example.qurannexus.core.utils.UtilityService
import com.example.qurannexus.features.analysis.QuranAnalysisFragment
import com.example.qurannexus.features.auth.AuthActivity
import com.example.qurannexus.features.auth.AuthService
import com.example.qurannexus.features.bookmark.BookmarkFragment
import com.example.qurannexus.features.home.HomeFragment
import com.example.qurannexus.features.prayerTimes.PrayerTimesFragment
import com.example.qurannexus.features.quiz.QuizActivity
import com.example.qurannexus.features.recitation.RecitationPageFragment
import com.example.qurannexus.features.recitation.SurahListFragment
import com.example.qurannexus.features.settings.SettingsFragment
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity (
) : AppCompatActivity() {
    private lateinit var authService: AuthService
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var meowBottomNavigation: MeowBottomNavigation
    private lateinit var sideMenuButton : ImageView
    @Inject
    lateinit var tokenManager: TokenManager
    @Inject
    lateinit var utilityService: UtilityService
    companion object { // Added for Logcat tag
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(TAG, "onCreate called")

        authService = AuthService()
        setupNavigationDrawer()
        setupMeowNavigationBar()

        // Handle navigation from intent FIRST, before loading default HomeFragment if savedInstanceState is null
        if (intent.getBooleanExtra("NAVIGATE_TO_RECITATION", false)) {
            Log.d(TAG, "Intent has NAVIGATE_TO_RECITATION = true")
            handleRecitationNavigation(intent)
            // It's important that after handling specific navigation, we don't then
            // immediately replace it with HomeFragment if savedInstanceState is null.
            // So, we can return or use an else block for the default HomeFragment loading.
        } else if (savedInstanceState == null) {
            Log.d(TAG, "No specific navigation, savedInstanceState is null, loading HomeFragment.")
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainFragmentContainer, HomeFragment())
                .commit()
            meowBottomNavigation.show(BottomMenuItemId.HOME.id, true) // Ensure correct tab is shown
        }
    }

    // This method is called if MainActivity is already running and receives a new intent
    // (e.g., from WordDetailsActivity with FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP)

    override fun onNewIntent(intent: Intent) { // intent should be nullable Intent?
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent called with intent: $intent")
        if (intent != null && intent.getBooleanExtra("NAVIGATE_TO_RECITATION", false)) {
            Log.d(TAG, "onNewIntent: Intent has NAVIGATE_TO_RECITATION = true")
            setIntent(intent) // This is important to update the activity's current intent
            handleRecitationNavigation(intent) // Pass the non-null intent
        }
    }
    @OptIn(UnstableApi::class) // Keep if RecitationPageFragment or children use UnstableApi
    private fun handleRecitationNavigation(intent: Intent) {
        Log.d(TAG, "handleRecitationNavigation called")
        val chapterId = intent.getStringExtra("CHAPTER_ID") // 1-based from WordDetailsActivity
        val verseNumber = intent.getStringExtra("VERSE_NUMBER") // 1-based from WordDetailsActivity
        val isByPage = intent.getBooleanExtra("IS_BY_PAGE", false)

        if (chapterId == null || verseNumber == null) {
            Log.e(TAG, "Chapter ID or Verse Number is null in recitation navigation intent. Aborting.")
            Toast.makeText(this, "Error: Missing navigation details.", Toast.LENGTH_SHORT).show()
            // Fallback to home or show error state
            if (supportFragmentManager.findFragmentById(R.id.mainFragmentContainer) == null) {
                loadFragment(HomeFragment()) // Ensure some fragment is loaded
            }
            return
        }

        val targetPageNumber: Int? = if (isByPage) intent.getIntExtra("TARGET_PAGE_NUMBER", -1).takeIf { it != -1 } else null
        val scrollToVerseOnPage: Int? = if (isByPage) intent.getIntExtra("SCROLL_TO_VERSE_ON_PAGE", -1).takeIf { it != -1 } else null
        // HIGHLIGHT_CHAPTER_ID for page mode will be the same as chapterId

        val currentSurahIndexForVerseMode: Int? = if (!isByPage) intent.getIntExtra("CURRENT_SURAH_INDEX", -1).takeIf { it != -1 } else null

        Log.d(TAG, "handleRecitationNavigation Params: isByPage=$isByPage, chapterId=$chapterId, verseNumber=$verseNumber, targetPage=$targetPageNumber, scrollToVerseOnPage=$scrollToVerseOnPage, surahIndexVerseMode=$currentSurahIndexForVerseMode")

        if (isByPage && targetPageNumber == null) {
            Log.e(TAG, "Page navigation selected, but TARGET_PAGE_NUMBER is missing or invalid. chapterId: $chapterId, verseNumber: $verseNumber")
            Toast.makeText(this, "Error: Could not determine target page.", Toast.LENGTH_SHORT).show()
            // Attempt to fallback or show error
            val derivedPage = chapterId.toIntOrNull()?.let { com.example.qurannexus.core.utils.QuranMetadata.getInstance().getStartingPage(it) }
            if (derivedPage != null && derivedPage != -1) {
                Log.w(TAG, "Fallback: Navigating to start page of Surah $chapterId (Page $derivedPage)")
                val fragment = RecitationPageFragment.newInstanceForNavigation(
                    true,                         // isByPage
                    chapterId,                    // chapterId
                    verseNumber,                  // verseNumber
                    derivedPage,                  // targetPageNumber
                    verseNumber.toIntOrNull(),    // scrollToVerseOnPage
                    null                          // currentSurahIndexForVerseMode
                )
                loadFragment(fragment)
            } else {
                if (supportFragmentManager.findFragmentById(R.id.mainFragmentContainer) == null) {
                    loadFragment(HomeFragment())
                }
            }
            return
        }

        val actualSurahIndexForVerseMode = if (!isByPage) {
            currentSurahIndexForVerseMode ?: chapterId.toIntOrNull()?.minus(1)
        } else {
            null
        }

        if (!isByPage && actualSurahIndexForVerseMode == null) {
            Log.e(TAG, "Verse navigation selected, but could not determine 0-based surah index. chapterId: $chapterId")
            Toast.makeText(this, "Error: Could not determine Surah for navigation.", Toast.LENGTH_SHORT).show()
            if (supportFragmentManager.findFragmentById(R.id.mainFragmentContainer) == null) {
                loadFragment(HomeFragment())
            }
            return
        }

        val fragment = RecitationPageFragment.newInstanceForNavigation(
            isByPage,                       // isByPage
            chapterId,                      // chapterId
            verseNumber,                    // verseNumber
            targetPageNumber,               // targetPageNumber
            scrollToVerseOnPage,            // scrollToVerseOnPage
            actualSurahIndexForVerseMode    // currentSurahIndexForVerseMode
        )
        loadFragment(fragment)
        // Optionally, ensure the correct bottom navigation tab is selected for recitation
        meowBottomNavigation.show(BottomMenuItemId.SURAHLIST.id, true) // Or whichever ID represents your recitation section
    }

    private fun setupNavigationDrawer() {
        sideMenuButton = findViewById(R.id.sideMenuButton)
        drawerLayout = findViewById(R.id.main)
        navigationView = findViewById(R.id.side_navigation_view)

        sideMenuButton.setOnClickListener { drawerLayout.openDrawer(navigationView) }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            handleSideNavigationItemSelected(menuItem)
            drawerLayout.closeDrawers()
            true
        }
    }

    private fun handleSideNavigationItemSelected(menuItem: MenuItem) {
        val selectedFragment: Fragment? =
            when (menuItem.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_bookmark -> {
                    if (tokenManager.getToken() != null) {
                        // User is logged in, so load the BookmarkFragment directly.
                        loadFragment(BookmarkFragment())
                    } else {
                        // User is NOT logged in, show the login required dialog.
                        utilityService.showLoginRequiredDialog(this) {
                            // This lambda block runs when the "Log In & Continue" button
                            // on the dialog is clicked.
                            val intent = Intent(this, AuthActivity::class.java).apply {
                                putExtra(AuthActivity.EXTRA_ACTION, AuthActivity.ACTION_NAVIGATE_TO_LOGIN)
                            }
                            startActivity(intent)
                        }
                    }
                    null
                }
                R.id.nav_prayer_times -> PrayerTimesFragment()
                R.id.nav_analysis -> QuranAnalysisFragment()
                R.id.nav_settings -> SettingsFragment()
                R.id.nav_test -> {
                    startActivity(Intent(this, TestActivity::class.java))
                    null
                }
                R.id.nav_logout -> {
                    handleLogout()
                    null
                }
                else -> null
            }
        selectedFragment?.let { loadFragment(it) }
    }

    fun setBottomNavigationVisibility(isVisible: Boolean) {
        if (::meowBottomNavigation.isInitialized) {
            if (isVisible) {
                showBottomNavigation()
            } else {
                hideBottomNavigation()
            }
        }
    }

    private fun hideBottomNavigation() {
        if (meowBottomNavigation.visibility == View.VISIBLE) {
            meowBottomNavigation.animate()
                .translationY(meowBottomNavigation.height.toFloat())
                .alpha(0f)
                .setDuration(300)
                .withEndAction { meowBottomNavigation.visibility = View.GONE }
                .start()
        }
    }

    private fun showBottomNavigation() {
        if (meowBottomNavigation.visibility != View.VISIBLE) {
            meowBottomNavigation.visibility = View.VISIBLE
            meowBottomNavigation.translationY = meowBottomNavigation.height.toFloat()
            meowBottomNavigation.alpha = 0f
            meowBottomNavigation.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(300)
                .start()
        }
    }

    private fun setupMeowNavigationBar() {
        meowBottomNavigation = findViewById(R.id.meowBottomNav)
        // ... (rest of your Meow setup code remains the same)
        with(meowBottomNavigation) {
            add(MeowBottomNavigation.Model(BottomMenuItemId.HOME.id, R.drawable.ic_home))
            add(MeowBottomNavigation.Model(BottomMenuItemId.SURAHLIST.id, R.drawable.ic_quran))
            add(MeowBottomNavigation.Model(BottomMenuItemId.ANALYSIS.id, R.drawable.ic_analysis))
            add(MeowBottomNavigation.Model(BottomMenuItemId.BOOKMARK.id, R.drawable.ic_bookmark))
            add(MeowBottomNavigation.Model(BottomMenuItemId.QUIZ.id, R.drawable.ic_note))

            setOnClickMenuListener { model ->
                when (BottomMenuItemId.fromId(model.id)) {
                    BottomMenuItemId.HOME -> loadFragment(HomeFragment())
                    BottomMenuItemId.SURAHLIST -> loadFragment(SurahListFragment()) // This typically leads to RecitationPageFragment after selection
                    BottomMenuItemId.ANALYSIS -> loadFragment(QuranAnalysisFragment())
                    BottomMenuItemId.BOOKMARK -> {
                        // You can protect any item this way
                        if (tokenManager.getToken() != null) {
                            // User is logged in, show the bookmark fragment
                            loadFragment(BookmarkFragment())
                        } else {
                            // User is NOT logged in, show the dialog
                            utilityService.showLoginRequiredDialog(this@MainActivity) {
                                // This lambda runs when the "Login" button in the dialog is clicked
                                val intent = Intent(this@MainActivity, AuthActivity::class.java).apply {
                                    putExtra(AuthActivity.EXTRA_ACTION, AuthActivity.ACTION_NAVIGATE_TO_LOGIN)
                                }
                                startActivity(intent)
                            }
                        }
                    }
                    BottomMenuItemId.QUIZ -> {
                        // 1. Check if a token exists. A non-null token means the user is logged in.
                        if (tokenManager.getToken() == null) {

                            // 2. If NO token, call your utility function to show the dialog.
                            utilityService.showLoginRequiredDialog(this@MainActivity) {
                                // 3. This is the 'onLoginClicked' lambda. It defines what happens
                                //    when the user taps "Log In & Continue" on the popup.

                                // 4. Create an Intent to start AuthActivity.
                                val intent = Intent(this@MainActivity, AuthActivity::class.java)

                                // 5. Add the special "action" extra to tell AuthActivity
                                //    to show the LoginFragment directly.
                                intent.putExtra(AuthActivity.EXTRA_ACTION, AuthActivity.ACTION_NAVIGATE_TO_LOGIN)

                                // 6. Start the activity.
                                startActivity(intent)
                            }
                        } else {
                            // 7. If a token EXISTS, the user is logged in, so proceed to the QuizActivity.
                            startActivity(Intent(this@MainActivity, QuizActivity::class.java))
                        }
                    }
                    null -> {
                        // Handle unknown menu item ID
                    }
                }
            }
            setOnShowListener { } // No-op listener if not used
            setOnReselectListener { } // No-op listener if not used
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.mainFragmentContainer, fragment)
            .addToBackStack(null)
            .commit()

        // Update Meow Bottom Navigation selected item based on the loaded fragment
        // You might need a more robust way to map fragments to menu IDs if not direct
        val menuIdToShow = when (fragment) {
            is HomeFragment -> BottomMenuItemId.HOME.id
            is SurahListFragment, is RecitationPageFragment -> BottomMenuItemId.SURAHLIST.id // Group recitation under SurahList tab
            is QuranAnalysisFragment -> BottomMenuItemId.ANALYSIS.id
            is BookmarkFragment -> BottomMenuItemId.BOOKMARK.id
            // is PrayerTimesFragment -> BottomMenuItemId.QUIZ.id // Assuming PrayerTimesFragment maps to Quiz icon
            else -> -1 // Or some default / no-change
        }
        if (menuIdToShow != -1 && ::meowBottomNavigation.isInitialized) {
            try {
                meowBottomNavigation.show(menuIdToShow, true)
            } catch (e: Exception) {
                Log.e(TAG, "Error showing MeowBottomNavigation tab: $menuIdToShow", e)
            }
        }
    }

    private fun handleLogout() {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Logging out...")
            setCancelable(false)
            show()
        }
        authService.logout(this) {
            progressDialog.dismiss()
            Intent(this@MainActivity, AuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(this)
            }
            finish()
            null
        }
    }
    fun getSideMenuButton(): ImageView {
        return sideMenuButton
    }
}