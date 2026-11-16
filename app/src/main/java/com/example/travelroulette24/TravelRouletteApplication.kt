package com.example.travelroulette24

import android.app.Application

class TravelRouletteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PreferenceHelper.init(this)
    }
}
