package com.example.qurannexus.features.onboard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.qurannexus.R
import com.example.qurannexus.core.activities.MainActivity
import com.example.qurannexus.core.network.ApiService
import com.example.qurannexus.features.auth.AuthActivity
import com.example.qurannexus.features.auth.AuthService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var authService: AuthService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        authService = AuthService()

        // We use lifecycleScope to launch a coroutine that is automatically
        // cancelled when the Activity is destroyed. This prevents memory leaks.
        lifecycleScope.launch {
            // Optional: A small delay to ensure the splash screen is visible
            // for at least a moment, improving perceived performance.
            delay(1500)

            val token = authService.getStoredToken(this@SplashActivity)

            // If there's no token, go directly to the authentication flow.
            if (token.isNullOrEmpty()) {
                navigateToAuthActivity()
                return@launch
            }

            // If a token exists, we must verify it's still valid.
            try {
                // IMPORTANT: Set the token in our ApiService so the /profile call is authenticated
                ApiService.setAuthToken(token)

                val user = authService.getUserProfileAsync(token)

                if (user != null) {
                    // Token is valid, user profile was fetched successfully.
                    navigateToMainActivity()
                } else {
                    // Token is invalid or expired (API call failed or returned null).
                    // Clear the bad token and navigate to the login screen.
                    authService.logout(this@SplashActivity) {} // The callback can be empty
                    navigateToAuthActivity()
                }
            } catch (e: Exception) {
                // Handle cases like no internet connection.
                // In this case, we also send the user to the login screen.
                navigateToAuthActivity()
            }
        }
    }

    private fun navigateToMainActivity() {
        // Create an intent to go to the main part of the app
        val intent = Intent(this, MainActivity::class.java).apply {
            // Flags to clear the back stack, so the user can't go back to the splash screen.
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish() // Call finish to remove SplashActivity from the back stack
    }

    private fun navigateToAuthActivity() {
        // Create an intent to go to the login/register flow
        val intent = Intent(this, AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish() // Call finish to remove SplashActivity from the back stack
    }
}