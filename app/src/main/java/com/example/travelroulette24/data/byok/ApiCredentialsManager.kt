package com.example.travelroulette24.data.byok

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages Bring-Your-Own-Key (BYOK) credentials on-device.
 *
 * All keys remain private and stored exclusively on the user's phone
 * (no cloud storage, no registration, no intermediate server).
 */
class ApiCredentialsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var amadeusApiKey: String
        get() = prefs.getString(KEY_AMADEUS_KEY, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_AMADEUS_KEY, value.trim()).apply()

    var amadeusApiSecret: String
        get() = prefs.getString(KEY_AMADEUS_SECRET, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_AMADEUS_SECRET, value.trim()).apply()

    var kiwiApiKey: String
        get() = prefs.getString(KEY_KIWI_KEY, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_KIWI_KEY, value.trim()).apply()

    var rapidApiKey: String
        get() = prefs.getString(KEY_RAPIDAPI_KEY, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_RAPIDAPI_KEY, value.trim()).apply()

    var aviationStackKey: String
        get() = prefs.getString(KEY_AVIATION_KEY, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_AVIATION_KEY, value.trim()).apply()

    var navitiaToken: String
        get() = prefs.getString(KEY_NAVITIA_TOKEN, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_NAVITIA_TOKEN, value.trim()).apply()

    var openHafasEnabled: Boolean
        get() = prefs.getBoolean(KEY_OPEN_HAFAS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_OPEN_HAFAS_ENABLED, value).apply()

    var openTripMapKey: String
        get() = prefs.getString(KEY_OPENTRIPMAP_KEY, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_OPENTRIPMAP_KEY, value.trim()).apply()

    var unsplashKey: String
        get() = prefs.getString(KEY_UNSPLASH_KEY, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_UNSPLASH_KEY, value.trim()).apply()

    var amadeusIsPaidTier: Boolean
        get() = prefs.getBoolean(KEY_AMADEUS_IS_PAID, false)
        set(value) = prefs.edit().putBoolean(KEY_AMADEUS_IS_PAID, value).apply()

    var kiwiIsPaidTier: Boolean
        get() = prefs.getBoolean(KEY_KIWI_IS_PAID, false)
        set(value) = prefs.edit().putBoolean(KEY_KIWI_IS_PAID, value).apply()

    // Custom API / Proprietary Endpoint Support
    var customApiUrl: String
        get() = prefs.getString(KEY_CUSTOM_API_URL, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_CUSTOM_API_URL, value.trim()).apply()

    var customApiKey: String
        get() = prefs.getString(KEY_CUSTOM_API_KEY, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_CUSTOM_API_KEY, value.trim()).apply()

    var customApiAuthHeader: String
        get() = prefs.getString(KEY_CUSTOM_API_HEADER, "Authorization").orEmpty()
        set(value) = prefs.edit().putString(KEY_CUSTOM_API_HEADER, value.trim()).apply()

    var customApiIsPaidTier: Boolean
        get() = prefs.getBoolean(KEY_CUSTOM_API_IS_PAID, false)
        set(value) = prefs.edit().putBoolean(KEY_CUSTOM_API_IS_PAID, value).apply()

    var customApiDailyLimit: Int
        get() = prefs.getInt(KEY_CUSTOM_API_DAILY_LIMIT, 50)
        set(value) = prefs.edit().putInt(KEY_CUSTOM_API_DAILY_LIMIT, value).apply()

    // Google Gemini AI for 24h itineraries & travel intelligence
    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI_KEY, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_GEMINI_KEY, value.trim()).apply()

    // ExchangeRate API for real-time currency conversion
    var exchangeRateKey: String
        get() = prefs.getString(KEY_EXCHANGE_RATE_KEY, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_EXCHANGE_RATE_KEY, value.trim()).apply()

    var currency: String
        get() = prefs.getString(KEY_CURRENCY, "EUR").orEmpty()
        set(value) = prefs.edit().putString(KEY_CURRENCY, value).apply()

    var searchRadiusKm: Int
        get() = prefs.getInt(KEY_RADIUS_KM, 100)
        set(value) = prefs.edit().putInt(KEY_RADIUS_KM, value).apply()

    fun clearAllKeys() {
        prefs.edit()
            .remove(KEY_AMADEUS_KEY)
            .remove(KEY_AMADEUS_SECRET)
            .remove(KEY_AMADEUS_IS_PAID)
            .remove(KEY_KIWI_KEY)
            .remove(KEY_KIWI_IS_PAID)
            .remove(KEY_RAPIDAPI_KEY)
            .remove(KEY_AVIATION_KEY)
            .remove(KEY_NAVITIA_TOKEN)
            .remove(KEY_OPENTRIPMAP_KEY)
            .remove(KEY_UNSPLASH_KEY)
            .remove(KEY_CUSTOM_API_URL)
            .remove(KEY_CUSTOM_API_KEY)
            .remove(KEY_CUSTOM_API_HEADER)
            .remove(KEY_CUSTOM_API_IS_PAID)
            .remove(KEY_CUSTOM_API_DAILY_LIMIT)
            .remove(KEY_GEMINI_KEY)
            .remove(KEY_EXCHANGE_RATE_KEY)
            .apply()
    }

    fun hasAnyFlightKey(): Boolean =
        (amadeusApiKey.isNotBlank() && amadeusApiSecret.isNotBlank()) ||
                kiwiApiKey.isNotBlank() ||
                rapidApiKey.isNotBlank() ||
                customApiUrl.isNotBlank()

    fun hasCustomApi(): Boolean = customApiUrl.isNotBlank()

    fun hasGemini(): Boolean = geminiApiKey.isNotBlank()

    fun hasNavitia(): Boolean = navitiaToken.isNotBlank()

    fun hasOpenTripMap(): Boolean = openTripMapKey.isNotBlank()

    companion object {
        private const val PREFS_NAME = "byok_credentials_prefs"
        private const val KEY_AMADEUS_KEY = "amadeus_api_key"
        private const val KEY_AMADEUS_SECRET = "amadeus_api_secret"
        private const val KEY_AMADEUS_IS_PAID = "amadeus_is_paid"
        private const val KEY_KIWI_KEY = "kiwi_api_key"
        private const val KEY_KIWI_IS_PAID = "kiwi_is_paid"
        private const val KEY_RAPIDAPI_KEY = "rapidapi_key"
        private const val KEY_AVIATION_KEY = "aviation_stack_key"
        private const val KEY_NAVITIA_TOKEN = "navitia_token"
        private const val KEY_OPEN_HAFAS_ENABLED = "open_hafas_enabled"
        private const val KEY_OPENTRIPMAP_KEY = "opentripmap_key"
        private const val KEY_UNSPLASH_KEY = "unsplash_key"
        private const val KEY_CUSTOM_API_URL = "custom_api_url"
        private const val KEY_CUSTOM_API_KEY = "custom_api_key"
        private const val KEY_CUSTOM_API_HEADER = "custom_api_header"
        private const val KEY_CUSTOM_API_IS_PAID = "custom_api_is_paid"
        private const val KEY_CUSTOM_API_DAILY_LIMIT = "custom_api_daily_limit"
        private const val KEY_GEMINI_KEY = "gemini_api_key"
        private const val KEY_EXCHANGE_RATE_KEY = "exchange_rate_key"
        private const val KEY_CURRENCY = "preferred_currency"
        private const val KEY_RADIUS_KM = "search_radius_km"
    }
}
