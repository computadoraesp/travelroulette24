package com.example.travelroulette24.data.byok

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages Bring-Your-Own-Key (BYOK) credentials on-device, including
 * specialized custom travel provider endpoints per mode:
 * - ✈️ Avión (Flight)
 * - 🚆 Tren (Train)
 * - 🚢 Barco / Ferry (Boat)
 *
 * All keys remain private and stored exclusively on the user's phone.
 */
class ApiCredentialsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // -------------------------------------------------------------
    // 🎯 MODO DE OPERACIÓN: REAL (EN VIVO) vs DEMO (SIMULADO)
    // -------------------------------------------------------------
    var isRealMode: Boolean
        get() = prefs.getBoolean(KEY_IS_REAL_MODE, true) // Por defecto en Modo Real
        set(value) = prefs.edit().putBoolean(KEY_IS_REAL_MODE, value).apply()

    fun getOverallStatusTitle(): String {
        return if (isRealMode) {
            if (hasCustomApi()) "🟢 Modo Real (APIs Personalizadas activas)"
            else if (hasAnyFlightKey() || hasAnyTrainKey()) "🟢 Modo Real (BYOK en vivo)"
            else "🟢 Modo Real (En Vivo - Motores y Reserva Oficial)"
        } else {
            "⚪ Modo Demo / Simulación"
        }
    }

    fun getFlightStatusLabel(): String {
        return if (!isRealMode) "⚪ Demo"
        else if (hasCustomFlight()) "Custom (${customFlightProviderName.ifBlank { "API" }})"
        else if (hasAnyFlightKey()) "BYOK"
        else "🟢 Real"
    }

    fun getTrainStatusLabel(): String {
        return if (!isRealMode) "⚪ Demo"
        else if (hasCustomTrain()) "Custom (${customTrainProviderName.ifBlank { "API" }})"
        else if (hasAnyTrainKey()) "BYOK"
        else "🟢 Real"
    }

    fun getBoatStatusLabel(): String {
        return if (!isRealMode) "⚪ Demo"
        else if (hasCustomBoat()) "Custom (${customBoatProviderName.ifBlank { "API" }})"
        else "🟢 Real"
    }

    fun activatePublicLiveProviders() {
        isRealMode = true
        openHafasEnabled = true
        if (customFlightApiUrl.isBlank()) {
            customFlightProviderName = "Google Flights & Live Aggregator"
            customFlightApiUrl = "https://www.google.com/travel/flights"
            customFlightApiEnabled = true
        }
        if (customTrainApiUrl.isBlank()) {
            customTrainProviderName = "Open-HAFAS & European Rail"
            customTrainApiUrl = "https://v6.db.transport.rest"
            customTrainApiEnabled = true
        }
        if (customBoatApiUrl.isBlank()) {
            customBoatProviderName = "FerryHopper & DirectFerries Live"
            customBoatApiUrl = "https://www.ferryhopper.com"
            customBoatApiEnabled = true
        }
    }

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

    // -------------------------------------------------------------
    // ✈️ 1. CUSTOM API - AVIÓN (FLIGHT)
    // -------------------------------------------------------------
    var customFlightProviderName: String
        get() = prefs.getString(KEY_CUSTOM_FLIGHT_NAME, "Mi Proveedor Vuelos").orEmpty()
        set(value) = prefs.edit().putString(KEY_CUSTOM_FLIGHT_NAME, value.trim()).apply()

    var customFlightApiUrl: String
        get() = prefs.getString(KEY_CUSTOM_API_URL, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_CUSTOM_API_URL, value.trim()).apply()

    var customFlightApiKey: String
        get() = prefs.getString(KEY_CUSTOM_API_KEY, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_CUSTOM_API_KEY, value.trim()).apply()

    var customFlightApiHeader: String
        get() = prefs.getString(KEY_CUSTOM_API_HEADER, "Authorization").orEmpty()
        set(value) = prefs.edit().putString(KEY_CUSTOM_API_HEADER, value.trim()).apply()

    var customFlightApiEnabled: Boolean
        get() = prefs.getBoolean(KEY_CUSTOM_FLIGHT_ENABLED, customFlightApiUrl.isNotBlank())
        set(value) = prefs.edit().putBoolean(KEY_CUSTOM_FLIGHT_ENABLED, value).apply()

    var customFlightIsPaidTier: Boolean
        get() = prefs.getBoolean(KEY_CUSTOM_API_IS_PAID, false)
        set(value) = prefs.edit().putBoolean(KEY_CUSTOM_API_IS_PAID, value).apply()

    var customFlightDailyLimit: Int
        get() = prefs.getInt(KEY_CUSTOM_API_DAILY_LIMIT, 50)
        set(value) = prefs.edit().putInt(KEY_CUSTOM_API_DAILY_LIMIT, value).apply()

    // Backward compatibility aliases
    var customApiUrl: String
        get() = customFlightApiUrl
        set(value) { customFlightApiUrl = value }

    var customApiKey: String
        get() = customFlightApiKey
        set(value) { customFlightApiKey = value }

    var customApiAuthHeader: String
        get() = customFlightApiHeader
        set(value) { customFlightApiHeader = value }

    var customApiIsPaidTier: Boolean
        get() = customFlightIsPaidTier
        set(value) { customFlightIsPaidTier = value }

    var customApiDailyLimit: Int
        get() = customFlightDailyLimit
        set(value) { customFlightDailyLimit = value }

    // -------------------------------------------------------------
    // 🚆 2. CUSTOM API - TREN (TRAIN)
    // -------------------------------------------------------------
    var customTrainProviderName: String
        get() = prefs.getString(KEY_CUSTOM_TRAIN_NAME, "Mi Proveedor Trenes").orEmpty()
        set(value) = prefs.edit().putString(KEY_CUSTOM_TRAIN_NAME, value.trim()).apply()

    var customTrainApiUrl: String
        get() = prefs.getString(KEY_CUSTOM_TRAIN_URL, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_CUSTOM_TRAIN_URL, value.trim()).apply()

    var customTrainApiKey: String
        get() = prefs.getString(KEY_CUSTOM_TRAIN_KEY, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_CUSTOM_TRAIN_KEY, value.trim()).apply()

    var customTrainApiHeader: String
        get() = prefs.getString(KEY_CUSTOM_TRAIN_HEADER, "X-Api-Key").orEmpty()
        set(value) = prefs.edit().putString(KEY_CUSTOM_TRAIN_HEADER, value.trim()).apply()

    var customTrainApiEnabled: Boolean
        get() = prefs.getBoolean(KEY_CUSTOM_TRAIN_ENABLED, customTrainApiUrl.isNotBlank())
        set(value) = prefs.edit().putBoolean(KEY_CUSTOM_TRAIN_ENABLED, value).apply()

    var customTrainIsPaidTier: Boolean
        get() = prefs.getBoolean(KEY_CUSTOM_TRAIN_IS_PAID, false)
        set(value) = prefs.edit().putBoolean(KEY_CUSTOM_TRAIN_IS_PAID, value).apply()

    var customTrainDailyLimit: Int
        get() = prefs.getInt(KEY_CUSTOM_TRAIN_DAILY_LIMIT, 50)
        set(value) = prefs.edit().putInt(KEY_CUSTOM_TRAIN_DAILY_LIMIT, value).apply()

    // -------------------------------------------------------------
    // 🚢 3. CUSTOM API - BARCO / FERRY (BOAT)
    // -------------------------------------------------------------
    var customBoatProviderName: String
        get() = prefs.getString(KEY_CUSTOM_BOAT_NAME, "Mi Proveedor Barcos").orEmpty()
        set(value) = prefs.edit().putString(KEY_CUSTOM_BOAT_NAME, value.trim()).apply()

    var customBoatApiUrl: String
        get() = prefs.getString(KEY_CUSTOM_BOAT_URL, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_CUSTOM_BOAT_URL, value.trim()).apply()

    var customBoatApiKey: String
        get() = prefs.getString(KEY_CUSTOM_BOAT_KEY, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_CUSTOM_BOAT_KEY, value.trim()).apply()

    var customBoatApiHeader: String
        get() = prefs.getString(KEY_CUSTOM_BOAT_HEADER, "Authorization").orEmpty()
        set(value) = prefs.edit().putString(KEY_CUSTOM_BOAT_HEADER, value.trim()).apply()

    var customBoatApiEnabled: Boolean
        get() = prefs.getBoolean(KEY_CUSTOM_BOAT_ENABLED, customBoatApiUrl.isNotBlank())
        set(value) = prefs.edit().putBoolean(KEY_CUSTOM_BOAT_ENABLED, value).apply()

    var customBoatIsPaidTier: Boolean
        get() = prefs.getBoolean(KEY_CUSTOM_BOAT_IS_PAID, false)
        set(value) = prefs.edit().putBoolean(KEY_CUSTOM_BOAT_IS_PAID, value).apply()

    var customBoatDailyLimit: Int
        get() = prefs.getInt(KEY_CUSTOM_BOAT_DAILY_LIMIT, 50)
        set(value) = prefs.edit().putInt(KEY_CUSTOM_BOAT_DAILY_LIMIT, value).apply()

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

    // -------------------------------------------------------------
    // Advanced Multi-Provider Store & Helpers
    // -------------------------------------------------------------
    fun getAllCustomProviders(): List<CustomTravelProvider> {
        val rawJson = prefs.getString(KEY_CUSTOM_PROVIDERS_JSON, "").orEmpty()
        val list = if (rawJson.isNotBlank()) {
            try {
                json.decodeFromString<List<CustomTravelProvider>>(rawJson)
            } catch (_: Exception) {
                emptyList()
            }
        } else emptyList()

        val mutableList = list.toMutableList()

        // Ensure default mode providers are reflected if configured
        if (customFlightApiUrl.isNotBlank() && mutableList.none { it.id == "flight_primary" }) {
            mutableList.add(
                CustomTravelProvider(
                    id = "flight_primary",
                    name = customFlightProviderName.ifBlank { "Mi Proveedor Vuelos" },
                    mode = CustomTravelProvider.MODE_FLIGHT,
                    endpointUrl = customFlightApiUrl,
                    authHeader = customFlightApiHeader,
                    apiKey = customFlightApiKey,
                    isEnabled = customFlightApiEnabled,
                    isPaidTier = customFlightIsPaidTier,
                    dailyLimit = customFlightDailyLimit
                )
            )
        }

        if (customTrainApiUrl.isNotBlank() && mutableList.none { it.id == "train_primary" }) {
            mutableList.add(
                CustomTravelProvider(
                    id = "train_primary",
                    name = customTrainProviderName.ifBlank { "Mi Proveedor Trenes" },
                    mode = CustomTravelProvider.MODE_TRAIN,
                    endpointUrl = customTrainApiUrl,
                    authHeader = customTrainApiHeader,
                    apiKey = customTrainApiKey,
                    isEnabled = customTrainApiEnabled,
                    isPaidTier = customTrainIsPaidTier,
                    dailyLimit = customTrainDailyLimit
                )
            )
        }

        if (customBoatApiUrl.isNotBlank() && mutableList.none { it.id == "boat_primary" }) {
            mutableList.add(
                CustomTravelProvider(
                    id = "boat_primary",
                    name = customBoatProviderName.ifBlank { "Mi Proveedor Barcos" },
                    mode = CustomTravelProvider.MODE_FERRY,
                    endpointUrl = customBoatApiUrl,
                    authHeader = customBoatApiHeader,
                    apiKey = customBoatApiKey,
                    isEnabled = customBoatApiEnabled,
                    isPaidTier = customBoatIsPaidTier,
                    dailyLimit = customBoatDailyLimit
                )
            )
        }

        return mutableList
    }

    fun saveCustomProvider(provider: CustomTravelProvider) {
        // Sync primary mode fields if matching ID or mode
        when (provider.mode) {
            CustomTravelProvider.MODE_FLIGHT -> {
                customFlightProviderName = provider.name
                customFlightApiUrl = provider.endpointUrl
                customFlightApiKey = provider.apiKey
                customFlightApiHeader = provider.authHeader
                customFlightApiEnabled = provider.isEnabled
                customFlightIsPaidTier = provider.isPaidTier
                customFlightDailyLimit = provider.dailyLimit
            }
            CustomTravelProvider.MODE_TRAIN -> {
                customTrainProviderName = provider.name
                customTrainApiUrl = provider.endpointUrl
                customTrainApiKey = provider.apiKey
                customTrainApiHeader = provider.authHeader
                customTrainApiEnabled = provider.isEnabled
                customTrainIsPaidTier = provider.isPaidTier
                customTrainDailyLimit = provider.dailyLimit
            }
            CustomTravelProvider.MODE_FERRY -> {
                customBoatProviderName = provider.name
                customBoatApiUrl = provider.endpointUrl
                customBoatApiKey = provider.apiKey
                customBoatApiHeader = provider.authHeader
                customBoatApiEnabled = provider.isEnabled
                customBoatIsPaidTier = provider.isPaidTier
                customBoatDailyLimit = provider.dailyLimit
            }
        }

        val current = getAllCustomProviders().filterNot { it.id == provider.id }.toMutableList()
        current.add(provider)
        try {
            prefs.edit().putString(KEY_CUSTOM_PROVIDERS_JSON, json.encodeToString(current)).apply()
        } catch (_: Exception) {}
    }

    fun deleteCustomProvider(id: String) {
        val current = getAllCustomProviders().filterNot { it.id == id }
        try {
            prefs.edit().putString(KEY_CUSTOM_PROVIDERS_JSON, json.encodeToString(current)).apply()
        } catch (_: Exception) {}

        if (id == "flight_primary") customFlightApiUrl = ""
        if (id == "train_primary") customTrainApiUrl = ""
        if (id == "boat_primary") customBoatApiUrl = ""
    }

    fun getCustomProvidersForMode(mode: String): List<CustomTravelProvider> {
        val targetMode = when (mode.lowercase()) {
            "flight", "vuelo", "avion", "avión" -> CustomTravelProvider.MODE_FLIGHT
            "train", "tren" -> CustomTravelProvider.MODE_TRAIN
            "ferry", "barco", "boat" -> CustomTravelProvider.MODE_FERRY
            else -> mode
        }
        return getAllCustomProviders().filter { it.mode.equals(targetMode, ignoreCase = true) && it.isEnabled }
    }

    fun hasCustomFlight(): Boolean = customFlightApiUrl.isNotBlank() && customFlightApiEnabled
    fun hasCustomTrain(): Boolean = customTrainApiUrl.isNotBlank() && customTrainApiEnabled
    fun hasCustomBoat(): Boolean = customBoatApiUrl.isNotBlank() && customBoatApiEnabled

    fun hasCustomProviderForMode(mode: String): Boolean = when (mode.lowercase()) {
        "flight", "vuelo", "avion", "avión" -> hasCustomFlight()
        "train", "tren" -> hasCustomTrain()
        "ferry", "barco", "boat" -> hasCustomBoat()
        else -> false
    }

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
            .remove(KEY_CUSTOM_FLIGHT_NAME)
            .remove(KEY_CUSTOM_FLIGHT_ENABLED)
            .remove(KEY_CUSTOM_TRAIN_NAME)
            .remove(KEY_CUSTOM_TRAIN_URL)
            .remove(KEY_CUSTOM_TRAIN_KEY)
            .remove(KEY_CUSTOM_TRAIN_HEADER)
            .remove(KEY_CUSTOM_TRAIN_ENABLED)
            .remove(KEY_CUSTOM_TRAIN_IS_PAID)
            .remove(KEY_CUSTOM_TRAIN_DAILY_LIMIT)
            .remove(KEY_CUSTOM_BOAT_NAME)
            .remove(KEY_CUSTOM_BOAT_URL)
            .remove(KEY_CUSTOM_BOAT_KEY)
            .remove(KEY_CUSTOM_BOAT_HEADER)
            .remove(KEY_CUSTOM_BOAT_ENABLED)
            .remove(KEY_CUSTOM_BOAT_IS_PAID)
            .remove(KEY_CUSTOM_BOAT_DAILY_LIMIT)
            .remove(KEY_CUSTOM_PROVIDERS_JSON)
            .remove(KEY_GEMINI_KEY)
            .remove(KEY_EXCHANGE_RATE_KEY)
            .apply()
    }

    fun hasAnyFlightKey(): Boolean =
        (amadeusApiKey.isNotBlank() && amadeusApiSecret.isNotBlank()) ||
                kiwiApiKey.isNotBlank() ||
                rapidApiKey.isNotBlank() ||
                hasCustomFlight()

    fun hasAnyTrainKey(): Boolean =
        navitiaToken.isNotBlank() || openHafasEnabled || hasCustomTrain()

    fun hasAnyBoatKey(): Boolean =
        hasCustomBoat()

    fun hasCustomApi(): Boolean =
        hasCustomFlight() || hasCustomTrain() || hasCustomBoat()

    fun hasGemini(): Boolean = geminiApiKey.isNotBlank()

    fun hasNavitia(): Boolean = navitiaToken.isNotBlank()

    fun hasOpenTripMap(): Boolean = openTripMapKey.isNotBlank()

    companion object {
        private const val PREFS_NAME = "byok_credentials_prefs"
        private const val KEY_IS_REAL_MODE = "is_real_mode"
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

        // Flight Custom
        private const val KEY_CUSTOM_API_URL = "custom_api_url"
        private const val KEY_CUSTOM_API_KEY = "custom_api_key"
        private const val KEY_CUSTOM_API_HEADER = "custom_api_header"
        private const val KEY_CUSTOM_API_IS_PAID = "custom_api_is_paid"
        private const val KEY_CUSTOM_API_DAILY_LIMIT = "custom_api_daily_limit"
        private const val KEY_CUSTOM_FLIGHT_NAME = "custom_flight_name"
        private const val KEY_CUSTOM_FLIGHT_ENABLED = "custom_flight_enabled"

        // Train Custom
        private const val KEY_CUSTOM_TRAIN_NAME = "custom_train_name"
        private const val KEY_CUSTOM_TRAIN_URL = "custom_train_url"
        private const val KEY_CUSTOM_TRAIN_KEY = "custom_train_key"
        private const val KEY_CUSTOM_TRAIN_HEADER = "custom_train_header"
        private const val KEY_CUSTOM_TRAIN_ENABLED = "custom_train_enabled"
        private const val KEY_CUSTOM_TRAIN_IS_PAID = "custom_train_is_paid"
        private const val KEY_CUSTOM_TRAIN_DAILY_LIMIT = "custom_train_daily_limit"

        // Boat Custom
        private const val KEY_CUSTOM_BOAT_NAME = "custom_boat_name"
        private const val KEY_CUSTOM_BOAT_URL = "custom_boat_url"
        private const val KEY_CUSTOM_BOAT_KEY = "custom_boat_key"
        private const val KEY_CUSTOM_BOAT_HEADER = "custom_boat_header"
        private const val KEY_CUSTOM_BOAT_ENABLED = "custom_boat_enabled"
        private const val KEY_CUSTOM_BOAT_IS_PAID = "custom_boat_is_paid"
        private const val KEY_CUSTOM_BOAT_DAILY_LIMIT = "custom_boat_daily_limit"

        private const val KEY_CUSTOM_PROVIDERS_JSON = "custom_providers_json"

        private const val KEY_GEMINI_KEY = "gemini_api_key"
        private const val KEY_EXCHANGE_RATE_KEY = "exchange_rate_key"
        private const val KEY_CURRENCY = "preferred_currency"
        private const val KEY_RADIUS_KM = "search_radius_km"
    }
}
