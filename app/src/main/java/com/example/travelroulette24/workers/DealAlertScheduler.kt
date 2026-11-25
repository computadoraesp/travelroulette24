package com.example.travelroulette24.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.travelroulette24.PreferenceHelper
import com.example.travelroulette24.data.PriceAlertManager
import com.example.travelroulette24.data.byok.ApiCredentialsManager
import java.util.concurrent.TimeUnit

/**
 * Schedules or cancels the periodic WorkManager check for latent ❤️ Wishlist and 🔔 Deal Radar alerts.
 */
object DealAlertScheduler {

    private const val UNIQUE_WORK_NAME = "travel_roulette_background_deal_radar"

    fun syncSchedule(context: Context) {
        val appContext = context.applicationContext
        val workManager = runCatching { WorkManager.getInstance(appContext) }.getOrNull() ?: return

        val hasWishlist = PreferenceHelper.getFavoriteDestinations().isNotEmpty()
        val hasRadar = runCatching {
            PriceAlertManager.getInstance(appContext).configFlow.value.isEnabled
        }.getOrDefault(false)

        if (!hasWishlist && !hasRadar) {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            return
        }

        val isRealMode = runCatching {
            ApiCredentialsManager(appContext).isRealMode
        }.getOrDefault(false)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (isRealMode) NetworkType.CONNECTED else NetworkType.NOT_REQUIRED)
            .build()

        val request = PeriodicWorkRequestBuilder<BackgroundDealAlertWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
