package com.example.qurannexus.core.utils

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_PREFS = "UserPrefs"
    }

    // Cache the token to prevent repeated disk reads
    @Volatile
    private var cachedToken: String? = null

    // Initialize cache in a background thread
    init {
        Thread {
            try {
                cachedToken = sharedPreferences.getString(KEY_TOKEN, null)
            } catch (e: Exception) {
                // Log error if needed
            }
        }.start()
    }

    // For immediate use, may block if cache isn't initialized yet
    fun getToken(): String? {
        return cachedToken ?: sharedPreferences.getString(KEY_TOKEN, null).also {
            cachedToken = it
        }
    }

    // Non-blocking suspend function
    suspend fun getTokenAsync(): String? = withContext(Dispatchers.IO) {
        cachedToken ?: sharedPreferences.getString(KEY_TOKEN, null).also {
            cachedToken = it
        }
    }

    fun saveToken(token: String) {
        // Update cache immediately
        cachedToken = token

        // Perform disk write in a background thread
        Thread {
            sharedPreferences.edit().putString(KEY_TOKEN, token).apply()
        }.start()
    }

    // Suspend version for use in coroutines
    suspend fun saveTokenAsync(token: String) = withContext(Dispatchers.IO) {
        cachedToken = token
        sharedPreferences.edit().putString(KEY_TOKEN, token).apply()
    }

    fun clearToken() {
        // Clear cache immediately
        cachedToken = null

        // Perform disk write in a background thread
        Thread {
            sharedPreferences.edit().remove(KEY_TOKEN).apply()
        }.start()
    }

    // Suspend version for use in coroutines
    suspend fun clearTokenAsync() = withContext(Dispatchers.IO) {
        cachedToken = null
        sharedPreferences.edit().remove(KEY_TOKEN).apply()
    }

    fun isLoggedIn(): Boolean = getToken() != null
}