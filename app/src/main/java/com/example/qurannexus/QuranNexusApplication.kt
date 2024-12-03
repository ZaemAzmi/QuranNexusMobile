package com.example.qurannexus

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class QuranNexusApplication : Application(){
    override fun onCreate() {
        super.onCreate()
        // Any custom initialization logic
    }
}