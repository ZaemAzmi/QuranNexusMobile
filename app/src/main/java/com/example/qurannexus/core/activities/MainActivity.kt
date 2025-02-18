package com.example.qurannexus.core.activities

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import com.etebarian.meowbottomnavigation.MeowBottomNavigation
import com.etebarian.meowbottomnavigation.MeowBottomNavigation.ClickListener
import com.etebarian.meowbottomnavigation.MeowBottomNavigation.ReselectListener
import com.etebarian.meowbottomnavigation.MeowBottomNavigation.ShowListener
import com.example.qurannexus.R
import com.example.qurannexus.core.enums.BottomMenuItemId
import com.example.qurannexus.core.enums.BottomMenuItemId.Companion.fromId
import com.example.qurannexus.core.utils.QuranMetadata
import com.example.qurannexus.features.auth.AuthActivity
import com.example.qurannexus.features.auth.AuthService
import com.example.qurannexus.features.bookmark.BookmarkFragment
import com.example.qurannexus.features.home.HomeFragment
import com.example.qurannexus.features.irab.IrabFragment
import com.example.qurannexus.features.prayerTimes.PrayerTimesFragment
import com.example.qurannexus.features.quiz.QuizActivity
import com.example.qurannexus.features.recitation.ByAyatRecitationFragment
import com.example.qurannexus.features.recitation.RecitationPageFragment
import com.example.qurannexus.features.recitation.SurahListFragment
import com.example.qurannexus.features.recitation.models.SurahModel
import com.example.qurannexus.features.settings.SettingsFragment
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var authService: AuthService
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var meowBottomNavigation: MeowBottomNavigation
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        authService = AuthService()
        setupNavigationDrawer()
        setupMeowNavigationBar()
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainFragmentContainer, HomeFragment())
                .commit()
        }
        // Handle navigation from intent
        if (intent.getBooleanExtra("NAVIGATE_TO_RECITATION", false)) {
            handleRecitationNavigation(intent)
        }
    }

    @OptIn(UnstableApi::class)
    private fun handleRecitationNavigation(intent: Intent) {
        val chapterId = intent.getStringExtra("CHAPTER_ID") ?: return
        val verseNumber = intent.getStringExtra("VERSE_NUMBER") ?: return
        val isByPage = intent.getBooleanExtra("IS_BY_PAGE", false)

        if (isByPage) {
            // Handle page-based navigation (your existing code)
            val quranMetadata = QuranMetadata.getInstance()
            val surahDetails = quranMetadata.getSurahDetails(chapterId.toInt())

            val surahModel = SurahModel(
                surahDetails?.translationName ?: " ",
                surahDetails?.arabicName,
                chapterId,
                surahDetails?.englishName,
                verseNumber,
                false
            )

            // Create and show RecitationPageFragment
            val fragment = RecitationPageFragment.newInstance(
                surahModel,
                "pageByPage",
                chapterId.toInt() - 1
            ).apply {
                arguments = Bundle().apply {
                    putParcelable("surahModel", surahModel)
                    putInt("scrollToVerse", verseNumber.toInt())
                }
            }
            loadFragment(fragment)
        } else {
            // Handle verse-based navigation
            val fragment = ByAyatRecitationFragment.newInstance(
                chapterId.toInt(),
                verseNumber.toInt()
            )
            loadFragment(fragment)
        }
    }

    private fun setupNavigationDrawer() {
        val sideMenuButton = findViewById<ImageView>(R.id.sideMenuButton)
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
            R.id.nav_settings -> SettingsFragment()
            R.id.nav_irab -> IrabFragment()
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


    private fun setupMeowNavigationBar() {
        meowBottomNavigation = findViewById(R.id.meowBottomNav)

        with(meowBottomNavigation) {
            add(MeowBottomNavigation.Model(BottomMenuItemId.HOME.id, R.drawable.ic_home))
            add(MeowBottomNavigation.Model(BottomMenuItemId.SURAHLIST.id, R.drawable.ic_quran))
            add(MeowBottomNavigation.Model(BottomMenuItemId.BOOKMARK.id, R.drawable.ic_bookmark))
            add(MeowBottomNavigation.Model(BottomMenuItemId.QUIZ.id, R.drawable.ic_note))

            setOnClickMenuListener { model ->
                when (BottomMenuItemId.fromId(model.id)) {
                    BottomMenuItemId.HOME -> loadFragment(HomeFragment())
                    BottomMenuItemId.SURAHLIST -> loadFragment(SurahListFragment())
                    BottomMenuItemId.BOOKMARK -> loadFragment(BookmarkFragment())
                    BottomMenuItemId.QUIZ -> {
                        startActivity(Intent(this@MainActivity, QuizActivity::class.java))
                    }

                    null -> {
                        // Handle unknown menu item ID
                    }
                }
            }
            setOnShowListener { }

            setOnReselectListener { }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.mainFragmentContainer, fragment)
            .addToBackStack(null)
            .commit()

        when (fragment) {
            is HomeFragment -> meowBottomNavigation.show(BottomMenuItemId.HOME.id, true)
            is SurahListFragment -> meowBottomNavigation.show(BottomMenuItemId.SURAHLIST.id, true)
            is BookmarkFragment -> meowBottomNavigation.show(BottomMenuItemId.BOOKMARK.id, true)
            is PrayerTimesFragment -> meowBottomNavigation.show(BottomMenuItemId.QUIZ.id, true)
        }
    }

    private fun handleLogout() {
        // Show loading dialog
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Logging out...")
            show()
        }

        authService.logout(this) {
            progressDialog.dismiss()
            // Redirect to auth activity regardless of server response
            Intent(this@MainActivity, AuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(this)
            }
            finish()
            null // Required for Java lambda compatibility
        }
    }
}
