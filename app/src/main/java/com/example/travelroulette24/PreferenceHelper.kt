package com.example.travelroulette24

import android.content.Context
import android.content.SharedPreferences
import com.example.travelroulette24.ui.HubCoordinates
import com.example.travelroulette24.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

object PreferenceHelper {
    private const val PREFS_NAME = "travel_roulette_prefs"
    private const val KEY_ORGANIZER_ID = "organizer_id"
    private const val KEY_SELECTED_TRANSPORT_MODES = "selected_transport_modes"
    private const val KEY_SORT_OPTION = "sort_option"
    private const val KEY_THEME_MODE = "app_theme_mode"
    private const val KEY_ORIGIN_CITY = "last_origin_city"
    private const val KEY_ORIGIN_LAT = "last_origin_lat"
    private const val KEY_ORIGIN_LON = "last_origin_lon"
    private lateinit var prefs: SharedPreferences

    private val _themeModeFlow = MutableStateFlow(AppThemeMode.SYSTEM)
    val themeModeFlow: StateFlow<AppThemeMode> = _themeModeFlow.asStateFlow()

    private val _favoriteDestinationsFlow = MutableStateFlow<List<String>>(emptyList())
    val favoriteDestinationsFlow: StateFlow<List<String>> = _favoriteDestinationsFlow.asStateFlow()

    enum class WishlistToggleResult {
        ADDED,
        REMOVED,
        LIMIT_REACHED
    }

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Ensure an organizer ID exists
        if (!prefs.contains(KEY_ORGANIZER_ID)) {
            prefs.edit().putString(KEY_ORGANIZER_ID, UUID.randomUUID().toString()).apply()
        }
        _themeModeFlow.value = getThemeMode()
        _favoriteDestinationsFlow.value = getFavoriteDestinations()
    }

    fun getThemeMode(): AppThemeMode {
        if (!::prefs.isInitialized) return AppThemeMode.SYSTEM
        val saved = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
        return runCatching { AppThemeMode.valueOf(saved ?: AppThemeMode.SYSTEM.name) }
            .getOrDefault(AppThemeMode.SYSTEM)
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeModeFlow.value = mode
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        }
    }

    fun getSelectedTransportModes(): Set<String> {
        val defaultModes = setOf("flight", "train", "ferry")
        if (!::prefs.isInitialized) return defaultModes
        val raw = prefs.getString(KEY_SELECTED_TRANSPORT_MODES, "flight,train,ferry").orEmpty()
        val parsed = raw.split(",")
            .map { it.trim().lowercase() }
            .map { if (it == "boat") "ferry" else it }
            .filter { it in defaultModes }
            .toSet()
        return parsed.ifEmpty { defaultModes }
    }

    fun setSelectedTransportModes(modes: Set<String>) {
        val validModes = setOf("flight", "train", "ferry")
        val normalized = modes
            .map { it.trim().lowercase() }
            .map { if (it == "boat") "ferry" else it }
            .filter { it in validModes }
            .toSet()
            .ifEmpty { validModes }
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_SELECTED_TRANSPORT_MODES, normalized.joinToString(",")).apply()
        }
    }

    fun getOrganizerId(): String =
        if (::prefs.isInitialized) prefs.getString(KEY_ORGANIZER_ID, "") ?: "" else ""

    fun getSortOptionName(defaultName: String = "BEST_VALUE"): String =
        if (::prefs.isInitialized) prefs.getString(KEY_SORT_OPTION, defaultName) ?: defaultName else defaultName

    fun setSortOptionName(name: String) {
        if (::prefs.isInitialized) prefs.edit().putString(KEY_SORT_OPTION, name).apply()
    }

    fun saveLastKnownOrigin(city: String, lat: Double, lon: Double) {
        if (!::prefs.isInitialized || city.isBlank()) return
        prefs.edit()
            .putString(KEY_ORIGIN_CITY, city)
            .putString(KEY_ORIGIN_LAT, lat.toString())
            .putString(KEY_ORIGIN_LON, lon.toString())
            .apply()
    }

    fun getLastKnownOrigin(): Triple<String, Double, Double>? {
        if (!::prefs.isInitialized) return null
        val city = prefs.getString(KEY_ORIGIN_CITY, null) ?: return null
        val lat = prefs.getString(KEY_ORIGIN_LAT, null)?.toDoubleOrNull() ?: return null
        val lon = prefs.getString(KEY_ORIGIN_LON, null)?.toDoubleOrNull() ?: return null
        return Triple(city, lat, lon)
    }

    fun getFavoriteDestinations(): List<String> {
        if (!::prefs.isInitialized) return emptyList()
        val raw = prefs.getString("favorite_destinations_csv", "").orEmpty()
        return raw.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(3)
    }

    fun setFavoriteDestinations(hubs: List<String>) {
        val normalized = hubs.map { it.trim().uppercase() }.filter { it.isNotBlank() }.distinct().take(3)
        _favoriteDestinationsFlow.value = normalized
        if (!::prefs.isInitialized) return
        prefs.edit().putString("favorite_destinations_csv", normalized.joinToString(",")).apply()
    }

    fun isDestinationDesired(destinationHub: String, favorites: List<String> = _favoriteDestinationsFlow.value): Boolean {
        val hubCity = HubCoordinates.getCity(destinationHub)
        return favorites.any { fav ->
            fav.equals(destinationHub, ignoreCase = true) ||
                HubCoordinates.getCity(fav).equals(hubCity, ignoreCase = true) ||
                hubCity.equals(fav, ignoreCase = true)
        }
    }

    fun toggleDesiredDestination(destinationHub: String): WishlistToggleResult {
        val current = _favoriteDestinationsFlow.value.ifEmpty { getFavoriteDestinations() }
        val hubCity = HubCoordinates.getCity(destinationHub)
        val existing = current.firstOrNull { fav ->
            fav.equals(destinationHub, ignoreCase = true) ||
                HubCoordinates.getCity(fav).equals(hubCity, ignoreCase = true) ||
                hubCity.equals(fav, ignoreCase = true)
        }
        return if (existing != null) {
            setFavoriteDestinations(current - existing)
            WishlistToggleResult.REMOVED
        } else if (current.size < 3) {
            setFavoriteDestinations(current + destinationHub.trim().uppercase())
            WishlistToggleResult.ADDED
        } else {
            WishlistToggleResult.LIMIT_REACHED
        }
    }

    fun getFavoriteTolerancePercent(): Int =
        if (::prefs.isInitialized) prefs.getInt("favorite_tolerance_pct", 15).coerceIn(5, 40) else 15

    fun setFavoriteTolerancePercent(percent: Int) {
        if (!::prefs.isInitialized) return
        prefs.edit().putInt("favorite_tolerance_pct", percent.coerceIn(5, 40)).apply()
    }

    fun getPassengers(): Int =
        if (::prefs.isInitialized) prefs.getInt("travel_passengers", 1).coerceIn(1, 10) else 1

    fun setPassengers(count: Int) {
        if (!::prefs.isInitialized) return
        prefs.edit().putInt("travel_passengers", count.coerceIn(1, 10)).apply()
    }

    fun getTravelMode(): String =
        if (::prefs.isInitialized) prefs.getString("travel_mode", "one_way") ?: "one_way" else "one_way"

    fun setTravelMode(mode: String) {
        if (!::prefs.isInitialized) return
        prefs.edit().putString("travel_mode", mode).apply()
    }

    fun getDepartureWindowHours(): Int {
        if (!::prefs.isInitialized) return 24
        val saved = prefs.getInt("departure_window_hours", 24)
        return if (saved in setOf(24, 48, 72)) saved else 24
    }

    fun setDepartureWindowHours(hours: Int) {
        if (!::prefs.isInitialized) return
        val normalized = if (hours in setOf(24, 48, 72)) hours else 24
        prefs.edit().putInt("departure_window_hours", normalized).apply()
    }
}
