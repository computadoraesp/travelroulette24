package com.example.travelroulette24.data.byok

import android.graphics.Bitmap
import com.example.travelroulette24.ui.DestinationWeather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray

/**
 * Slim facade coordinating modular BYOK travel services:
 * - [FlightProviderService] (Kiwi Tequila, Amadeus, RapidAPI Skyscanner)
 * - [SurfaceTransportService] (Custom REST APIs, Navitia, Open-HAFAS rail)
 * - [DestinationEnrichmentService] (Open-Meteo, ExchangeRate, OpenTripMap, Unsplash, AviationStack, Gemini)
 *
 * Strictly executes ONLY live network logic (no synthetic/demo fallbacks).
 */
class ByokTravelService(
    val credentials: ApiCredentialsManager,
    val quotaGovernor: ApiQuotaGovernor? = null
) {
    private val flightService = FlightProviderService(credentials, quotaGovernor)
    private val surfaceService = SurfaceTransportService(credentials, quotaGovernor)
    private val enrichmentService = DestinationEnrichmentService(credentials, quotaGovernor)

    data class LiveFlightRadarStatus(
        val flightNumber: String,
        val airline: String?,
        val status: String,
        val terminal: String?,
        val gate: String?,
        val delayMinutes: Int = 0
    )

    suspend fun fetchFlightOffers(
        originHubs: List<String>,
        mode: String,
        stayMinHours: Int,
        stayMaxHours: Int,
        departureWindowHours: Int = 24
    ): JsonArray? = withContext(Dispatchers.IO) {
        val originKey = originHubs.sorted().joinToString("-")
        val cacheKey = "flights_${originKey}_${mode}_${stayMinHours}_${stayMaxHours}_${departureWindowHours}"

        surfaceService.fetchFromCustomProviders(
            customMode = CustomTravelProvider.MODE_FLIGHT,
            quotaPrefix = "custom_flight_",
            cacheKey = cacheKey,
            originHubs = originHubs
        )?.let { return@withContext it }

        flightService.fetchBuiltInFlightOffers(originHubs, cacheKey, departureWindowHours)
    }

    suspend fun fetchTrainOffers(
        originHubs: List<String>,
        mode: String,
        stayMinHours: Int,
        stayMaxHours: Int,
        originCity: String = ""
    ): JsonArray? = withContext(Dispatchers.IO) {
        val originKey = originHubs.sorted().joinToString("-")
        val cacheKey = "trains_${originKey}_${mode}_${stayMinHours}_${stayMaxHours}"

        surfaceService.fetchFromCustomProviders(
            customMode = CustomTravelProvider.MODE_TRAIN,
            quotaPrefix = "custom_train_",
            cacheKey = cacheKey,
            originHubs = originHubs
        )?.let { return@withContext it }

        val navitiaToken = credentials.navitiaToken
        if (navitiaToken.isNotBlank()) {
            val offers = surfaceService.fetchNavitiaTrainOffers(navitiaToken, originHubs)
            if (offers != null && offers.isNotEmpty()) {
                quotaGovernor?.putCachedOffers(cacheKey, offers)
                return@withContext offers
            }
        }

        if (credentials.openHafasEnabled) {
            val hafasOffers = surfaceService.fetchOpenHafasTrainOffers(
                originCity = originCity,
                originHubs = originHubs,
                mode = mode,
                stayMinHours = stayMinHours
            )
            if (hafasOffers != null && hafasOffers.isNotEmpty()) {
                quotaGovernor?.putCachedOffers(cacheKey, hafasOffers)
                return@withContext hafasOffers
            }
        }

        null
    }

    suspend fun fetchBoatOffers(
        originHubs: List<String>,
        mode: String,
        stayMinHours: Int,
        stayMaxHours: Int
    ): JsonArray? = withContext(Dispatchers.IO) {
        val originKey = originHubs.sorted().joinToString("-")
        val cacheKey = "boats_${originKey}_${mode}_${stayMinHours}_${stayMaxHours}"

        surfaceService.fetchFromCustomProviders(
            customMode = CustomTravelProvider.MODE_FERRY,
            quotaPrefix = "custom_boat_",
            cacheKey = cacheKey,
            originHubs = originHubs
        )
    }

    suspend fun testProviderConnection(
        endpointUrl: String,
        authHeader: String,
        apiKey: String,
        mode: String = "flight",
        testOrigin: String = "MAD"
    ): Pair<Boolean, String> = surfaceService.testProviderConnection(
        endpointUrl = endpointUrl,
        authHeader = authHeader,
        apiKey = apiKey,
        mode = mode,
        testOrigin = testOrigin
    )

    suspend fun fetchTopAttractions(cityName: String): List<String> =
        enrichmentService.fetchTopAttractions(cityName)

    suspend fun fetchLiveWeather(latitude: Double, longitude: Double): DestinationWeather? =
        enrichmentService.fetchLiveWeather(latitude, longitude)

    suspend fun fetchCurrencyRate(targetCurrency: String): Double =
        enrichmentService.fetchCurrencyRate(targetCurrency)

    suspend fun fetchGeminiExpressItinerary(
        cityName: String,
        mode: String,
        stayHours: Int,
        budgetFormatted: String
    ): String? = enrichmentService.fetchGeminiExpressItinerary(cityName, mode, stayHours, budgetFormatted)

    suspend fun fetchLiveFlightStatus(
        originHub: String,
        destHub: String
    ): LiveFlightRadarStatus? = enrichmentService.fetchLiveFlightStatus(originHub, destHub)

    suspend fun fetchDestinationPhoto(cityName: String): Bitmap? =
        enrichmentService.fetchDestinationPhoto(cityName)
}
