package com.example.travelroulette24.data

import android.content.Context
import android.content.SharedPreferences
import com.example.travelroulette24.PreferenceHelper
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import com.example.travelroulette24.ui.HubCoordinates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages Bargain Price Alerts (Radar de Alertas de Chollo) for TravelRoulette24.
 * Focused exclusively on open deal hunting via 3 radar strategies:
 * - [AlertMode.LOWEST_PRICE]: Lowest ticket price (<= maxPriceEur)
 * - [AlertMode.BEST_KM_PER_PRICE]: Best distance-to-price ratio (>= minKmPerEur)
 * - [AlertMode.HISTORICAL_LOW]: Historical lowest price (engine 30d/12m opportunity or local record low)
 *
 * Latent ❤️ Wishlist alerts (budget & historical tolerance) are handled independently
 * by [com.example.travelroulette24.utils.DealNotificationDispatcher].
 */
class PriceAlertManager(context: Context) {

    @Serializable
    enum class AlertMode {
        LOWEST_PRICE,
        BEST_KM_PER_PRICE,
        HISTORICAL_LOW
    }

    @Serializable
    data class AlertConfig(
        val isEnabled: Boolean = true,
        val alertMode: AlertMode = AlertMode.LOWEST_PRICE,
        val maxPriceEur: Float = 35.0f,
        val minKmPerEur: Float = 15.0f,
        val enforceMaxPriceInValueMode: Boolean = false,
        val targetDestination: String? = null // null = Any, TARGET_ALL_FAVORITES = All ❤️, or specific favorite code/city
    )

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val _configFlow = MutableStateFlow(loadConfig())
    val configFlow: StateFlow<AlertConfig> = _configFlow.asStateFlow()

    private fun loadConfig(): AlertConfig {
        val serialized = prefs.getString(KEY_ALERT_CONFIG, null) ?: return AlertConfig()
        return try {
            json.decodeFromString<AlertConfig>(serialized)
        } catch (_: Exception) {
            AlertConfig()
        }
    }

    fun updateConfig(newConfig: AlertConfig) {
        try {
            val serialized = json.encodeToString(newConfig)
            prefs.edit().putString(KEY_ALERT_CONFIG, serialized).apply()
        } catch (_: Exception) {
            // Gracefully ignore
        }
        _configFlow.value = newConfig
    }

    /**
     * Checks if an individual item matches the active Deal Radar criteria.
     */
    fun matchesAlert(
        item: TravelIntelligenceEngine.ResultItem,
        config: AlertConfig = _configFlow.value,
        favoriteDestinations: List<String> = PreferenceHelper.getFavoriteDestinations(),
        historicalLowByHub: Map<String, Double> = emptyMap()
    ): Boolean {
        if (!config.isEnabled) return false

        val target = config.targetDestination?.trim()?.takeIf { it.isNotBlank() }
        if (target != null) {
            val targetMatches = if (target == TARGET_ALL_FAVORITES) {
                isDestinationInFavorites(item.destinationHub, favoriteDestinations)
            } else {
                destinationMatchesTarget(item.destinationHub, target)
            }
            if (!targetMatches) return false
        }

        return when (config.alertMode) {
            AlertMode.LOWEST_PRICE -> {
                item.totalPrice <= config.maxPriceEur
            }
            AlertMode.BEST_KM_PER_PRICE -> {
                val distKm = HubCoordinates.calculateDistanceKm(item.originHub, item.destinationHub)
                val valueIndex = TravelIntelligenceEngine.computeTravelValueIndex(distKm, item.price)
                val meetsRatio = valueIndex >= config.minKmPerEur
                val meetsBudget = !config.enforceMaxPriceInValueMode || item.totalPrice <= config.maxPriceEur
                meetsRatio && meetsBudget
            }
            AlertMode.HISTORICAL_LOW -> {
                val destKey = item.destinationHub.uppercase()
                val cityKey = HubCoordinates.getCity(item.destinationHub).uppercase()
                val recordedLow = historicalLowByHub[destKey] ?: historicalLowByHub[cityKey]
                val beatsRecordedLow = recordedLow != null && recordedLow > 0.0 && item.price <= recordedLow
                val isHistoricalLow = item.isOpportunity || beatsRecordedLow
                val meetsBudget = !config.enforceMaxPriceInValueMode || item.totalPrice <= config.maxPriceEur
                isHistoricalLow && meetsBudget
            }
        }
    }

    /**
     * Filters a list of results returning only those that trigger the Deal Radar.
     */
    fun getMatchingDeals(
        results: List<TravelIntelligenceEngine.ResultItem>,
        config: AlertConfig = _configFlow.value,
        favoriteDestinations: List<String> = PreferenceHelper.getFavoriteDestinations(),
        historicalLowByHub: Map<String, Double> = emptyMap()
    ): List<TravelIntelligenceEngine.ResultItem> {
        if (!config.isEnabled) return emptyList()
        return results.filter {
            matchesAlert(
                item = it,
                config = config,
                favoriteDestinations = favoriteDestinations,
                historicalLowByHub = historicalLowByHub
            )
        }
    }

    private fun isDestinationInFavorites(
        destinationHub: String,
        favoriteDestinations: List<String>
    ): Boolean {
        if (favoriteDestinations.isEmpty()) return false
        return favoriteDestinations.any { fav -> destinationMatchesTarget(destinationHub, fav) }
    }

    private fun destinationMatchesTarget(destinationHub: String, target: String): Boolean {
        if (destinationHub.equals(target, ignoreCase = true)) return true
        val itemCity = HubCoordinates.getCity(destinationHub)
        val targetCity = HubCoordinates.getCity(target)
        return itemCity.equals(target, ignoreCase = true) ||
            (targetCity.isNotBlank() && itemCity.equals(targetCity, ignoreCase = true))
    }

    companion object {
        const val TARGET_ALL_FAVORITES = "__FAVORITES__"
        private const val PREFS_NAME = "travel_roulette_price_alerts"
        private const val KEY_ALERT_CONFIG = "key_alert_config_json"

        @Volatile
        private var INSTANCE: PriceAlertManager? = null

        fun getInstance(context: Context): PriceAlertManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PriceAlertManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
