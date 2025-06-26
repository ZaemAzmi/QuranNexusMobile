package com.example.qurannexus.features.auth

import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.example.qurannexus.R
import com.example.qurannexus.features.onboard.WelcomeFragment

class AuthActivity : AppCompatActivity() {

    // companion object for constants - Best Practice!
    companion object {
        const val EXTRA_ACTION = "com.example.qurannexus.auth.ACTION"
        const val ACTION_NAVIGATE_TO_LOGIN = "NAVIGATE_TO_LOGIN"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_auth)

        // Make UI go edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.navigationBarColor = Color.TRANSPARENT

        // Apply insets for the status bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Only apply top padding for status bar, bottom is handled by fragments
            view.setPadding(view.paddingLeft, systemBars.top, view.paddingRight, view.paddingBottom)
            insets
        }

        if (savedInstanceState == null) {
            handleIntentNavigation()
        }
    }

    private fun handleIntentNavigation() {
        val action = intent.getStringExtra(EXTRA_ACTION)

        // Decide which fragment to show based on the intent action
        val initialFragment = if (action == ACTION_NAVIGATE_TO_LOGIN) {
            LoginFragment()
        } else {
            WelcomeFragment()
        }

        showFragment(initialFragment)
    }

    // Helper function to show a fragment
    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.authFragmentContainer, fragment)
            .commit()
    }
}