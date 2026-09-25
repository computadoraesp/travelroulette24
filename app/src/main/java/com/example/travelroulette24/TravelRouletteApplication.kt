package com.example.travelroulette24

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TravelRouletteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PreferenceHelper.init(this)
    }
}
