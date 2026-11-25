package com.example.travelroulette24.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.travelroulette24.MainActivity
import com.example.travelroulette24.PreferenceHelper
import com.example.travelroulette24.R
import com.example.travelroulette24.budget.BudgetGuardian
import com.example.travelroulette24.data.FavoritePriceHistoryDb
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import com.example.travelroulette24.ui.HubCoordinates

/**
 * Evaluates latent ❤️ Wishlist destination offers & active Deal Radar matches,
 * dispatching high-priority local notifications + tactile vibration.
 */
object DealNotificationDispatcher {

    private const val CHANNEL_ID = "travel_roulette_wishlist_alerts"
    private const val NOTIFICATION_ID_WISHLIST = 2401
    private const val NOTIFICATION_ID_RADAR = 2402

    @Volatile
    private var lastNotifiedSignature: String? = null

    fun matchesWishlistDestination(
        destinationHub: String,
        favoriteDestinations: List<String>
    ): Boolean = PreferenceHelper.isDestinationDesired(destinationHub, favoriteDestinations)

    /**
     * Returns live offers that match the user's latent ❤️ Wishlist destinations
     * whenever an offer fits the active budget, beats/matches historical low acceptance,
     * or is marked as a historical opportunity when no budget cap rejects it.
     */
    fun findLatentWishlistDeals(
        results: List<TravelIntelligenceEngine.ResultItem>,
        favoriteDestinations: List<String>,
        budget: BudgetGuardian.Budget? = null,
        historicalLowByHub: Map<String, Double> = emptyMap(),
        tolerancePercent: Int = 15
    ): List<TravelIntelligenceEngine.ResultItem> {
        if (favoriteDestinations.isEmpty() || results.isEmpty()) return emptyList()
        val hasBudgetCap = budget?.enabled == true && budget.maxPerTrip != null
        val maxPerTrip = budget?.maxPerTrip ?: Double.MAX_VALUE

        return results.filter { item ->
            if (!matchesWishlistDestination(item.destinationHub, favoriteDestinations)) return@filter false

            val destKey = item.destinationHub.uppercase()
            val cityKey = HubCoordinates.getCity(item.destinationHub).uppercase()
            val histLow = historicalLowByHub[destKey] ?: historicalLowByHub[cityKey]

            val withinHistory = histLow != null && histLow > 0.0 &&
                FavoritePriceHistoryDb.evaluateAcceptance(
                    currentPrice = item.price,
                    lowestHistoricalPrice = histLow,
                    tolerancePercent = tolerancePercent
                ).isWithinAcceptanceRange

            if (hasBudgetCap) {
                item.totalPrice <= maxPerTrip || withinHistory
            } else {
                item.isOpportunity || withinHistory || histLow == null
            }
        }
    }

    /**
     * Triggers tactile vibration + system notification once per distinct deal signature.
     */
    fun notifyIfNeeded(
        context: Context,
        wishlistDeals: List<TravelIntelligenceEngine.ResultItem>,
        radarDeals: List<TravelIntelligenceEngine.ResultItem>,
        currencyCode: String,
        currencyRate: Double
    ) {
        if (wishlistDeals.isEmpty() && radarDeals.isEmpty()) return

        val primaryDeal = wishlistDeals.minByOrNull { it.price } ?: radarDeals.minByOrNull { it.price } ?: return
        val signature = buildString {
            append("w:")
            append(wishlistDeals.joinToString(",") { "${it.destinationHub}:${it.price}" })
            append("|r:")
            append(radarDeals.take(3).joinToString(",") { "${it.destinationHub}:${it.price}" })
        }

        if (signature == lastNotifiedSignature) return
        lastNotifiedSignature = signature

        vibrateAlert(context)

        val destCity = HubCoordinates.getCity(primaryDeal.destinationHub)
        val formattedPrice = CurrencyHelper.formatPrice(primaryDeal.price, currencyCode, currencyRate)

        val (title, body, notificationId) = if (wishlistDeals.isNotEmpty()) {
            Triple(
                context.getString(R.string.wishlist_notification_title, destCity),
                context.getString(R.string.wishlist_notification_body, wishlistDeals.size, destCity, formattedPrice),
                NOTIFICATION_ID_WISHLIST
            )
        } else {
            Triple(
                context.getString(R.string.deal_notification_title),
                context.getString(R.string.deal_notification_body, radarDeals.size, destCity, formattedPrice),
                NOTIFICATION_ID_RADAR
            )
        }

        postNotification(context, notificationId, title, body)
    }

    @Suppress("DEPRECATION")
    private fun vibrateAlert(context: Context) {
        try {
            val timings = longArrayOf(0L, 120L, 80L, 180L)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = manager?.defaultVibrator
                if (vibrator?.hasVibrator() == true) {
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
                }
            } else {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator?.hasVibrator() == true) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
                    } else {
                        vibrator.vibrate(timings, -1)
                    }
                }
            }
        } catch (_: Exception) {
            // Gracefully ignore devices/emulators without vibrator support
        }
    }

    private fun postNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_wishlist_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notification_channel_wishlist_desc)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0L, 120L, 80L, 180L)
                }
                manager.createNotificationChannel(channel)
            }

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            manager.notify(notificationId, notification)
        } catch (_: Exception) {
            // Gracefully ignore notification errors
        }
    }
}
