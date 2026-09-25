package com.example.travelroulette24

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

object PreferenceHelper {
    private const val PREFS_NAME = "travel_roulette_prefs"
    private const val KEY_BUDGET = "budget_filter"
    private const val KEY_ORGANIZER_ID = "organizer_id"
    private const val KEY_LUCKY_ICON = "lucky_icon"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Ensure an organizer ID exists
        if (!prefs.contains(KEY_ORGANIZER_ID)) {
            prefs.edit().putString(KEY_ORGANIZER_ID, UUID.randomUUID().toString()).apply()
        }
    }

    fun getLuckyIcon(): String = prefs.getString(KEY_LUCKY_ICON, "🍀") ?: "🍀"

    fun setLuckyIcon(icon: String) {
        prefs.edit().putString(KEY_LUCKY_ICON, icon).apply()
    }

    fun getBudget(): Int? = if (prefs.contains(KEY_BUDGET)) prefs.getInt(KEY_BUDGET, 0) else null

    fun setBudget(value: Int?) {
        prefs.edit().apply {
            if (value == null) remove(KEY_BUDGET) else putInt(KEY_BUDGET, value)
        }.apply()
    }

    fun getOrganizerId(): String = prefs.getString(KEY_ORGANIZER_ID, "") ?: ""
}
