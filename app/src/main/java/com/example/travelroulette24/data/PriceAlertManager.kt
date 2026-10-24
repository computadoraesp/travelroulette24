package com.example.travelroulette24.data

import android.content.Context
import android.content.SharedPreferences
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages Bargain Price Alerts (Radar de Alertas de Chollo) for TravelRoulette24.
 */
class PriceAlertManager(context: Context) {

    @Serializable
    data class AlertConfig(
        val isEnabled: Boolean = true,
        val maxPriceEur: Float = 35.0f,
        val targetDestination: String? = null // null means "Cualquier destino"
    )

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    private val _configFlow = MutableStateFlow(loadConfig())
    val configFlow: StateFlow<AlertConfig> = _configFlow.asStateFlow()

    private fun loadConfig(): AlertConfig {
        val serialized = prefs.getString(KEY_ALERT_CONFIG, null) ?: return AlertConfig()
        return try {
            json.decodeFromString<AlertConfig>(serialized)
        } catch (e: Exception) {
            AlertConfig()
        }
    }

    fun updateConfig(newConfig: AlertConfig) {
        try {
            val serialized = json.encodeToString(newConfig)
            prefs.edit().putString(KEY_ALERT_CONFIG, serialized).apply()
        } catch (e: Exception) {
            // Gracefully ignore
        }
        _configFlow.value = newConfig
    }

    /**
     * Checks if an individual item matches the active price alert criteria.
     */
    fun matchesAlert(item: TravelIntelligenceEngine.ResultItem, config: AlertConfig = _configFlow.value): Boolean {
        if (!config.isEnabled) return false
        if (item.price > config.maxPriceEur) return false
        if (!config.targetDestination.isNullOrBlank() &&
            !item.destinationHub.equals(config.targetDestination, ignoreCase = true)
        ) {
            return false
        }
        return true
    }

    /**
     * Filters a list of results returning only those that trigger the price alert.
     */
    fun getMatchingDeals(
        results: List<TravelIntelligenceEngine.ResultItem>,
        config: AlertConfig = _configFlow.value
    ): List<TravelIntelligenceEngine.ResultItem> {
        if (!config.isEnabled) return emptyList()
        return results.filter { matchesAlert(it, config) }
    }

    companion object {
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
