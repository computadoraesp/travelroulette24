package com.example.travelroulette24.data

import android.content.Context
import android.content.SharedPreferences
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages user-saved favorite travel escapes using local persistent storage.
 */
class SavedTripsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    private val _savedTripsFlow = MutableStateFlow<List<TravelIntelligenceEngine.ResultItem>>(loadTrips())
    val savedTripsFlow: StateFlow<List<TravelIntelligenceEngine.ResultItem>> = _savedTripsFlow.asStateFlow()

    private fun loadTrips(): List<TravelIntelligenceEngine.ResultItem> {
        val serialized = prefs.getString(KEY_SAVED_TRIPS, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<TravelIntelligenceEngine.ResultItem>>(serialized)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun persistTrips(list: List<TravelIntelligenceEngine.ResultItem>) {
        try {
            val serialized = json.encodeToString(list)
            prefs.edit().putString(KEY_SAVED_TRIPS, serialized).apply()
        } catch (e: Exception) {
            // Log or ignore gracefully
        }
        _savedTripsFlow.value = list
    }

    fun isSaved(item: TravelIntelligenceEngine.ResultItem): Boolean {
        return _savedTripsFlow.value.any { isSameTrip(it, item) }
    }

    fun toggleSave(item: TravelIntelligenceEngine.ResultItem): Boolean {
        val current = _savedTripsFlow.value.toMutableList()
        val existingIndex = current.indexOfFirst { isSameTrip(it, item) }
        val nowSaved: Boolean
        if (existingIndex >= 0) {
            current.removeAt(existingIndex)
            nowSaved = false
        } else {
            current.add(0, item) // Add newest at top
            nowSaved = true
        }
        persistTrips(current)
        return nowSaved
    }

    fun removeTrip(item: TravelIntelligenceEngine.ResultItem) {
        val current = _savedTripsFlow.value.toMutableList()
        val index = current.indexOfFirst { isSameTrip(it, item) }
        if (index >= 0) {
            current.removeAt(index)
            persistTrips(current)
        }
    }

    fun clearAll() {
        persistTrips(emptyList())
    }

    private fun isSameTrip(
        a: TravelIntelligenceEngine.ResultItem,
        b: TravelIntelligenceEngine.ResultItem
    ): Boolean {
        return a.originHub == b.originHub &&
                a.destinationHub == b.destinationHub &&
                a.departureTime == b.departureTime &&
                a.price == b.price
    }

    companion object {
        private const val PREFS_NAME = "travel_roulette_saved_trips"
        private const val KEY_SAVED_TRIPS = "key_saved_trips_json"

        @Volatile
        private var INSTANCE: SavedTripsManager? = null

        fun getInstance(context: Context): SavedTripsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SavedTripsManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
