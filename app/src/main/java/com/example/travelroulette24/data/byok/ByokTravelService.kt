package com.example.travelroulette24.data.byok

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Executes real API requests against external travel providers
 * using user-provided BYOK keys.
 */
class ByokTravelService(
    private val credentials: ApiCredentialsManager,
    val quotaGovernor: ApiQuotaGovernor? = null
) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    private var cachedAmadeusToken: String? = null
    private var tokenExpiryEpoch: Long = 0L

    /**
     * Attempts to fetch real flight offers from active BYOK providers or custom API.
     * Uses cache to preserve tokens and respects daily quota budget.
     * Returns null when quotas are exhausted, cleanly falling back to token libres/open sources.
     */
    suspend fun fetchFlightOffers(
        originHubs: List<String>,
        mode: String,
        stayMinHours: Int,
        stayMaxHours: Int
    ): JsonArray? = withContext(Dispatchers.IO) {
        val originKey = originHubs.sorted().joinToString("-")
        val cacheKey = "flights_${originKey}_${mode}_${stayMinHours}_${stayMaxHours}"

        // 1. Check TTL cache first to avoid burning tokens on repeated roulette spins
        val cached = quotaGovernor?.getCachedOffers(cacheKey)
        if (cached != null && cached.isNotEmpty()) {
            return@withContext cached
        }

        val customUrl = credentials.customApiUrl
        val amadeusKey = credentials.amadeusApiKey
        val amadeusSecret = credentials.amadeusApiSecret
        val kiwiKey = credentials.kiwiApiKey

        val offers: JsonArray? = when {
            // Prioritize Custom API if configured
            customUrl.isNotBlank() -> {
                val canExecute = quotaGovernor?.canExecuteRequest(
                    provider = "custom",
                    isPaidTier = credentials.customApiIsPaidTier,
                    customDailyLimit = credentials.customApiDailyLimit
                ) ?: true

                if (canExecute) {
                    val result = fetchCustomApiOffers(customUrl, credentials.customApiKey, credentials.customApiAuthHeader, originHubs)
                    if (result != null && result.isNotEmpty()) {
                        quotaGovernor?.recordRequest("custom")
                        result
                    } else null
                } else null
            }
            // Next Kiwi Tequila
            kiwiKey.isNotBlank() -> {
                val canExecute = quotaGovernor?.canExecuteRequest(
                    provider = "kiwi",
                    isPaidTier = credentials.kiwiIsPaidTier,
                    monthlyQuota = 2000
                ) ?: true

                if (canExecute) {
                    val result = fetchKiwiOffers(kiwiKey, originHubs)
                    if (result != null && result.isNotEmpty()) {
                        quotaGovernor?.recordRequest("kiwi")
                        result
                    } else null
                } else null
            }
            // Next Amadeus
            amadeusKey.isNotBlank() && amadeusSecret.isNotBlank() -> {
                val canExecute = quotaGovernor?.canExecuteRequest(
                    provider = "amadeus",
                    isPaidTier = credentials.amadeusIsPaidTier,
                    monthlyQuota = 2000
                ) ?: true

                if (canExecute) {
                    val result = fetchAmadeusInspirations(amadeusKey, amadeusSecret, originHubs)
                    if (result != null && result.isNotEmpty()) {
                        quotaGovernor?.recordRequest("amadeus")
                        result
                    } else null
                } else null
            }
            else -> null
        }

        if (offers != null && offers.isNotEmpty()) {
            quotaGovernor?.putCachedOffers(cacheKey, offers)
        }
        offers
    }

    private fun fetchCustomApiOffers(
        url: String,
        apiKey: String,
        authHeader: String,
        originHubs: List<String>
    ): JsonArray? {
        val origin = originHubs.firstOrNull { it.length == 3 } ?: "PAR"
        val fullUrl = if (url.contains("?")) "$url&origin=$origin" else "$url?origin=$origin"

        val reqBuilder = Request.Builder().url(fullUrl)
        if (apiKey.isNotBlank()) {
            val headerVal = if (authHeader.equals("Authorization", ignoreCase = true) && !apiKey.startsWith("Bearer ", ignoreCase = true)) {
                "Bearer $apiKey"
            } else {
                apiKey
            }
            reqBuilder.addHeader(authHeader, headerVal)
        }

        return try {
            val response = httpClient.newCall(reqBuilder.build()).execute()
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val element = json.parseToJsonElement(body)
            when (element) {
                is JsonArray -> element
                is JsonObject -> {
                    // Check standard wrappers like "data", "results", "options", "offers"
                    element["data"]?.jsonArray
                        ?: element["results"]?.jsonArray
                        ?: element["offers"]?.jsonArray
                        ?: element["options"]?.jsonArray
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchAmadeusInspirations(
        apiKey: String,
        apiSecret: String,
        originHubs: List<String>
    ): JsonArray? {
        val token = getOrRefreshAmadeusToken(apiKey, apiSecret) ?: return null
        val origin = originHubs.firstOrNull { it.length == 3 } ?: "PAR"

        val url = "https://test.api.amadeus.com/v1/shopping/flight-destinations?origin=$origin"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()

        return try {
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val root = json.parseToJsonElement(body).jsonObject
            val data = root["data"]?.jsonArray ?: return null

            val results = mutableListOf<JsonObject>()
            val now = Instant.now()

            data.forEachIndexed { idx, item ->
                val obj = item.jsonObject
                val dest = obj["destination"]?.jsonPrimitive?.content ?: return@forEachIndexed
                val priceObj = obj["price"]?.jsonObject
                val price = priceObj?.get("total")?.jsonPrimitive?.content?.toDoubleOrNull() ?: 35.0

                val depDate = obj["departureDate"]?.jsonPrimitive?.content
                val departureTime = if (depDate != null) {
                    try {
                        val parsed = LocalDate.parse(depDate, DateTimeFormatter.ISO_LOCAL_DATE)
                        parsed.atTime(10 + (idx % 10), 30).atZone(java.time.ZoneOffset.UTC).toInstant().toString()
                    } catch (_: Exception) {
                        now.plusSeconds(3600L * (2 + idx * 2)).toString()
                    }
                } else {
                    now.plusSeconds(3600L * (2 + idx * 2)).toString()
                }

                results.add(
                    buildJsonObject {
                        put("originHub", kotlinx.serialization.json.JsonPrimitive(origin))
                        put("destinationHub", kotlinx.serialization.json.JsonPrimitive(dest))
                        put("price", kotlinx.serialization.json.JsonPrimitive(price))
                        put("departureTime", kotlinx.serialization.json.JsonPrimitive(departureTime))
                        put("returnTime", kotlinx.serialization.json.JsonNull)
                        put("checkInOnline", kotlinx.serialization.json.JsonPrimitive(true))
                        put("deepLink", kotlinx.serialization.json.JsonPrimitive("https://www.google.com/travel/flights?q=flights+from+$origin+to+$dest"))
                        put("mode", kotlinx.serialization.json.JsonPrimitive("flight"))
                    }
                )
            }
            JsonArray(results)
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchKiwiOffers(apiKey: String, originHubs: List<String>): JsonArray? {
        val origin = originHubs.firstOrNull { it.length == 3 } ?: "CDG"
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        val tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

        val url = "https://api.tequila.kiwi.com/v2/search?fly_from=$origin&date_from=$today&date_to=$tomorrow&limit=20&sort=price"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", apiKey)
            .build()

        return try {
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val root = json.parseToJsonElement(body).jsonObject
            val data = root["data"]?.jsonArray ?: return null

            val results = mutableListOf<JsonObject>()
            data.forEach { item ->
                val obj = item.jsonObject
                val from = obj["flyFrom"]?.jsonPrimitive?.content ?: origin
                val to = obj["flyTo"]?.jsonPrimitive?.content ?: return@forEach
                val price = obj["price"]?.jsonPrimitive?.doubleOrNull ?: 40.0
                val dTimeEpoch = obj["dTimeUTC"]?.jsonPrimitive?.longOrNull ?: Instant.now().epochSecond
                val departureTime = Instant.ofEpochSecond(dTimeEpoch).toString()
                val deepLink = obj["deep_link"]?.jsonPrimitive?.content
                    ?: "https://www.kiwi.com/deep?from=$from&to=$to"

                results.add(
                    buildJsonObject {
                        put("originHub", kotlinx.serialization.json.JsonPrimitive(from))
                        put("destinationHub", kotlinx.serialization.json.JsonPrimitive(to))
                        put("price", kotlinx.serialization.json.JsonPrimitive(price))
                        put("departureTime", kotlinx.serialization.json.JsonPrimitive(departureTime))
                        put("returnTime", kotlinx.serialization.json.JsonNull)
                        put("checkInOnline", kotlinx.serialization.json.JsonPrimitive(true))
                        put("deepLink", kotlinx.serialization.json.JsonPrimitive(deepLink))
                        put("mode", kotlinx.serialization.json.JsonPrimitive("flight"))
                    }
                )
            }
            JsonArray(results)
        } catch (_: Exception) {
            null
        }
    }

    private fun getOrRefreshAmadeusToken(apiKey: String, apiSecret: String): String? {
        val now = Instant.now().epochSecond
        if (cachedAmadeusToken != null && now < tokenExpiryEpoch - 60) {
            return cachedAmadeusToken
        }

        val requestBody = FormBody.Builder()
            .add("grant_type", "client_credentials")
            .add("client_id", apiKey)
            .add("client_secret", apiSecret)
            .build()

        val request = Request.Builder()
            .url("https://test.api.amadeus.com/v1/security/oauth2/token")
            .post(requestBody)
            .build()

        return try {
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val obj = json.parseToJsonElement(body).jsonObject
            val token = obj["access_token"]?.jsonPrimitive?.content ?: return null
            val expiresIn = obj["expires_in"]?.jsonPrimitive?.longOrNull ?: 1799L
            cachedAmadeusToken = token
            tokenExpiryEpoch = now + expiresIn
            token
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Fetches top attractions for a destination city using OpenTripMap if configured.
     */
    suspend fun fetchTopAttractions(cityName: String): List<String> = withContext(Dispatchers.IO) {
        val key = credentials.openTripMapKey
        if (key.isBlank()) return@withContext emptyList()

        val url = "https://api.opentripmap.com/0.1/en/places/geoname?name=$cityName&apikey=$key"
        val request = Request.Builder().url(url).build()

        try {
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()
            val body = response.body?.string() ?: return@withContext emptyList()
            val root = json.parseToJsonElement(body).jsonObject
            val lat = root["lat"]?.jsonPrimitive?.doubleOrNull ?: return@withContext emptyList()
            val lon = root["lon"]?.jsonPrimitive?.doubleOrNull ?: return@withContext emptyList()

            val radiusUrl = "https://api.opentripmap.com/0.1/en/places/radius?radius=8000&lon=$lon&lat=$lat&rate=3&limit=3&format=json&apikey=$key"
            val radiusResponse = httpClient.newCall(Request.Builder().url(radiusUrl).build()).execute()
            if (!radiusResponse.isSuccessful) return@withContext emptyList()
            val radiusBody = radiusResponse.body?.string() ?: return@withContext emptyList()
            val places = json.parseToJsonElement(radiusBody).jsonArray
            places.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.content?.takeIf { n -> n.isNotBlank() } }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
