package com.example.travelroulette24

import android.app.Application
import com.example.travelroulette24.workers.DealAlertScheduler

class TravelRouletteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PreferenceHelper.init(this)
        DealAlertScheduler.syncSchedule(this)
    }
}
