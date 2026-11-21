package com.example.travelroulette24

import android.content.Context
import android.content.SharedPreferences
import com.example.travelroulette24.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

object PreferenceHelper {
    private const val PREFS_NAME = "travel_roulette_prefs"
    private const val KEY_ORGANIZER_ID = "organizer_id"
    private const val KEY_LUCKY_ICON = "lucky_icon"
    private const val KEY_SORT_OPTION = "sort_option"
    private const val KEY_VALUE_COMPARISON = "value_comparison_enabled"
    private const val KEY_THEME_MODE = "app_theme_mode"
    private lateinit var prefs: SharedPreferences

    private val _themeModeFlow = MutableStateFlow(AppThemeMode.SYSTEM)
    val themeModeFlow: StateFlow<AppThemeMode> = _themeModeFlow.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Ensure an organizer ID exists
        if (!prefs.contains(KEY_ORGANIZER_ID)) {
            prefs.edit().putString(KEY_ORGANIZER_ID, UUID.randomUUID().toString()).apply()
        }
        _themeModeFlow.value = getThemeMode()
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

    fun getLuckyIcon(): String =
        if (::prefs.isInitialized) prefs.getString(KEY_LUCKY_ICON, "🍀") ?: "🍀" else "🍀"

    fun setLuckyIcon(icon: String) {
        if (::prefs.isInitialized) prefs.edit().putString(KEY_LUCKY_ICON, icon).apply()
    }

    fun getOrganizerId(): String =
        if (::prefs.isInitialized) prefs.getString(KEY_ORGANIZER_ID, "") ?: "" else ""

    fun getSortOptionName(defaultName: String = "BEST_VALUE"): String =
        if (::prefs.isInitialized) prefs.getString(KEY_SORT_OPTION, defaultName) ?: defaultName else defaultName

    fun setSortOptionName(name: String) {
        if (::prefs.isInitialized) prefs.edit().putString(KEY_SORT_OPTION, name).apply()
    }

    fun isValueComparisonEnabled(): Boolean =
        if (::prefs.isInitialized) prefs.getBoolean(KEY_VALUE_COMPARISON, false) else false

    fun setValueComparisonEnabled(enabled: Boolean) {
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_VALUE_COMPARISON, enabled).apply()
    }

    /** Records a local-only booking click under the active ranking mode to validate the km/€ hypothesis. */
    fun recordBookingClick(sortOptionName: String) {
        if (!::prefs.isInitialized) return
        val key = "booking_clicks_$sortOptionName"
        val current = prefs.getInt(key, 0)
        prefs.edit().putInt(key, current + 1).apply()
    }

    /** Returns local booking click counts grouped by ranking mode name. */
    fun getBookingClicksByMode(): Map<String, Int> {
        if (!::prefs.isInitialized) return emptyMap()
        val modes = listOf("BEST_VALUE", "CHEAPEST", "FURTHEST_DISTANCE", "EXTREME_ROULETTE", "EARLIEST_DEPARTURE")
        return modes.associateWith { prefs.getInt("booking_clicks_$it", 0) }
    }

    fun getFavoriteDestinations(): List<String> {
        if (!::prefs.isInitialized) return emptyList()
        val raw = prefs.getString("favorite_destinations_csv", "").orEmpty()
        return raw.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(3)
    }

    fun setFavoriteDestinations(hubs: List<String>) {
        if (!::prefs.isInitialized) return
        val csv = hubs.map { it.trim().uppercase() }.filter { it.isNotBlank() }.distinct().take(3).joinToString(",")
        prefs.edit().putString("favorite_destinations_csv", csv).apply()
    }

    fun getFavoriteTolerancePercent(): Int =
        if (::prefs.isInitialized) prefs.getInt("favorite_tolerance_pct", 15).coerceIn(5, 40) else 15

    fun setFavoriteTolerancePercent(percent: Int) {
        if (!::prefs.isInitialized) return
        prefs.edit().putInt("favorite_tolerance_pct", percent.coerceIn(5, 40)).apply()
    }
}
