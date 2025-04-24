package com.example.qurannexus

import android.app.Application
import android.os.StrictMode
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class QuranNexusApplication : Application() {
    override fun onCreate() {
        // Set strict mode before doing anything else
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    // Uncomment for more immediate feedback during development
                    //.penaltyDialog()
                    .build()
            )

            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }

        // Pre-initialize common objects on background thread
        Thread {
            try {
                // Pre-warm any expensive initializations here
                Log.d("QuranNexusApp", "Background initialization started")
                // Add your initialization code here
            } catch (e: Exception) {
                // Log the exception
                Log.e("QuranNexusApp", "Error during background initialization", e)
            }
        }.start()

        super.onCreate()
    }
}