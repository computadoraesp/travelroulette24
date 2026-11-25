package com.example.travelroulette24.data.byok

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.FormBody
import okhttp3.Request
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Dedicated flight provider service handling:
 * - Kiwi Tequila
 * - Amadeus (Sandbox & Production environments with OAuth2 token caching)
 * - TravelPayouts (free affiliate flight prices + Aviasales deep links)
 * - RapidAPI Skyscanner Search Everywhere
 *
 * All OkHttp responses are safely closed via `.use { }`.
 */
internal class FlightProviderService(
    private val credentials: ApiCredentialsManager,
    private val quotaGovernor: ApiQuotaGovernor? = null
) {
    private val httpClient = ByokHttpClient.client
    private val json = ByokHttpClient.json

    private var cachedAmadeusToken: String? = null
    private var cachedAmadeusHost: String? = null
    private var tokenExpiryEpoch: Long = 0L

    fun fetchBuiltInFlightOffers(
        originHubs: List<String>,
        cacheKey: String,
        departureWindowHours: Int = 24
    ): JsonArray? {
        val kiwiKey = credentials.kiwiApiKey
        if (kiwiKey.isNotBlank()) {
            val canExecute = quotaGovernor?.canExecuteRequest(
                provider = "kiwi",
                isPaidTier = credentials.kiwiIsPaidTier,
                monthlyQuota = 2000
            ) ?: true

            if (canExecute) {
                val result = fetchKiwiOffers(kiwiKey, originHubs, departureWindowHours)
                if (result != null && result.isNotEmpty()) {
                    quotaGovernor?.recordRequest("kiwi")
                    quotaGovernor?.putCachedOffers(cacheKey, result)
                    return result
                }
            }
        }

        val amadeusKey = credentials.amadeusApiKey
        val amadeusSecret = credentials.amadeusApiSecret
        if (amadeusKey.isNotBlank() && amadeusSecret.isNotBlank()) {
            val canExecute = quotaGovernor?.canExecuteRequest(
                provider = "amadeus",
                isPaidTier = credentials.amadeusIsPaidTier,
                monthlyQuota = 2000
            ) ?: true

            if (canExecute) {
                val result = fetchAmadeusInspirations(
                    apiKey = amadeusKey,
                    apiSecret = amadeusSecret,
                    originHubs = originHubs,
                    useProduction = credentials.amadeusUseProduction
                )
                if (result != null && result.isNotEmpty()) {
                    quotaGovernor?.recordRequest("amadeus")
                    quotaGovernor?.putCachedOffers(cacheKey, result)
                    return result
                }
            }
        }

        val travelPayoutsToken = credentials.travelPayoutsToken
        if (travelPayoutsToken.isNotBlank()) {
            val canExecute = quotaGovernor?.canExecuteRequest(
                provider = "travelpayouts",
                isPaidTier = false,
                monthlyQuota = 2000
            ) ?: true

            if (canExecute) {
                val result = fetchTravelPayoutsFlights(travelPayoutsToken, originHubs)
                if (result != null && result.isNotEmpty()) {
                    quotaGovernor?.recordRequest("travelpayouts")
                    quotaGovernor?.putCachedOffers(cacheKey, result)
                    return result
                }
            }
        }

        val rapidKey = credentials.rapidApiKey
        if (rapidKey.isNotBlank()) {
            val canExecute = quotaGovernor?.canExecuteRequest(
                provider = "rapidapi",
                isPaidTier = false,
                monthlyQuota = 500
            ) ?: true

            if (canExecute) {
                val result = fetchRapidApiFlights(rapidKey, originHubs)
                if (result != null && result.isNotEmpty()) {
                    quotaGovernor?.recordRequest("rapidapi")
                    quotaGovernor?.putCachedOffers(cacheKey, result)
                    return result
                }
            }
        }

        return null
    }

    private fun fetchAmadeusInspirations(
        apiKey: String,
        apiSecret: String,
        originHubs: List<String>,
        useProduction: Boolean
    ): JsonArray? {
        val baseHost = if (useProduction) "https://api.amadeus.com" else "https://test.api.amadeus.com"
        val token = getOrRefreshAmadeusToken(apiKey, apiSecret, baseHost) ?: return null
        val origin = originHubs.firstOrNull { it.length == 3 } ?: "PAR"

        val url = "$baseHost/v1/shopping/flight-destinations?origin=$origin"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
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
                            parsed.atTime(10 + (idx % 10), 30).atZone(ZoneOffset.UTC).toInstant().toString()
                        } catch (_: Exception) {
                            now.plusSeconds(3600L * (2 + idx * 2)).toString()
                        }
                    } else {
                        now.plusSeconds(3600L * (2 + idx * 2)).toString()
                    }

                    results.add(
                        buildJsonObject {
                            put("originHub", JsonPrimitive(origin))
                            put("destinationHub", JsonPrimitive(dest))
                            put("price", JsonPrimitive(price))
                            put("departureTime", JsonPrimitive(departureTime))
                            put("returnTime", JsonNull)
                            put("checkInOnline", JsonPrimitive(true))
                            put("deepLink", JsonPrimitive("https://www.google.com/travel/flights?q=flights+from+$origin+to+$dest&provider=Amadeus"))
                            put("mode", JsonPrimitive("flight"))
                        }
                    )
                }
                JsonArray(results)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchTravelPayoutsFlights(token: String, originHubs: List<String>): JsonArray? {
        val origin = originHubs.firstOrNull { it.length == 3 } ?: "MAD"
        val url = "https://api.travelpayouts.com/aviasales/v3/prices_for_dates?origin=${ByokHttpClient.encodeParam(origin)}&currency=eur&one_way=true&limit=20&sorting=price&token=${ByokHttpClient.encodeParam(token)}"
        val request = Request.Builder().url(url).build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val root = json.parseToJsonElement(body).jsonObject
                val data = root["data"]?.jsonArray ?: return null

                val now = Instant.now()
                val results = mutableListOf<JsonObject>()
                data.forEachIndexed { idx, item ->
                    val obj = item.jsonObject
                    val from = obj["origin"]?.jsonPrimitive?.content ?: origin
                    val to = obj["destination"]?.jsonPrimitive?.content ?: return@forEachIndexed
                    val price = obj["price"]?.jsonPrimitive?.doubleOrNull ?: return@forEachIndexed
                    val depAt = obj["departure_at"]?.jsonPrimitive?.content
                    val departureTime = if (!depAt.isNullOrBlank()) {
                        try {
                            Instant.parse(depAt).toString()
                        } catch (_: Exception) {
                            now.plusSeconds(3600L * (2 + idx)).toString()
                        }
                    } else {
                        now.plusSeconds(3600L * (2 + idx)).toString()
                    }
                    val linkPath = obj["link"]?.jsonPrimitive?.content.orEmpty()
                    val deepLink = if (linkPath.startsWith("http")) {
                        linkPath
                    } else if (linkPath.isNotBlank()) {
                        "https://www.aviasales.com$linkPath"
                    } else {
                        "https://www.aviasales.com/search/${from}${to}1?provider=TravelPayouts"
                    }

                    results.add(
                        buildJsonObject {
                            put("originHub", JsonPrimitive(from))
                            put("destinationHub", JsonPrimitive(to))
                            put("price", JsonPrimitive(price))
                            put("departureTime", JsonPrimitive(departureTime))
                            put("returnTime", JsonNull)
                            put("checkInOnline", JsonPrimitive(true))
                            put("deepLink", JsonPrimitive(deepLink))
                            put("mode", JsonPrimitive("flight"))
                        }
                    )
                }
                if (results.isNotEmpty()) JsonArray(results) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchKiwiOffers(
        apiKey: String,
        originHubs: List<String>,
        departureWindowHours: Int = 24
    ): JsonArray? {
        val origin = originHubs.firstOrNull { it.length == 3 } ?: "CDG"
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        val daysAhead = (departureWindowHours / 24).coerceIn(1, 3).toLong()
        val endDate = LocalDate.now().plusDays(daysAhead).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

        val url = "https://api.tequila.kiwi.com/v2/search?fly_from=$origin&date_from=$today&date_to=$endDate&limit=20&sort=price"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", apiKey)
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
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
                        ?: "https://www.kiwi.com/deep?from=$from&to=$to&provider=Kiwi"

                    results.add(
                        buildJsonObject {
                            put("originHub", JsonPrimitive(from))
                            put("destinationHub", JsonPrimitive(to))
                            put("price", JsonPrimitive(price))
                            put("departureTime", JsonPrimitive(departureTime))
                            put("returnTime", JsonNull)
                            put("checkInOnline", JsonPrimitive(true))
                            put("deepLink", JsonPrimitive(deepLink))
                            put("mode", JsonPrimitive("flight"))
                        }
                    )
                }
                JsonArray(results)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchRapidApiFlights(apiKey: String, originHubs: List<String>): JsonArray? {
        val origin = originHubs.firstOrNull { it.length == 3 } ?: "CDG"
        val url = "https://sky-scanner3.p.rapidapi.com/flights/search-everywhere?fromEntityId=${ByokHttpClient.encodeParam(origin)}&cabinClass=economy&currency=EUR"
        val request = Request.Builder()
            .url(url)
            .addHeader("x-rapidapi-key", apiKey)
            .addHeader("x-rapidapi-host", "sky-scanner3.p.rapidapi.com")
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val root = json.parseToJsonElement(body).jsonObject
                val dataObj = root["data"] as? JsonObject ?: return null
                val rawResults = dataObj["everywhereDestination"]?.jsonObject?.get("results")?.jsonArray
                    ?: dataObj["results"]?.jsonArray
                    ?: return null

                val now = Instant.now()
                val results = mutableListOf<JsonObject>()
                rawResults.take(15).forEachIndexed { idx, el ->
                    val obj = el.jsonObject
                    val content = obj["content"]?.jsonObject ?: obj
                    val location = content["location"]?.jsonObject
                    val destCode = location?.get("skyCode")?.jsonPrimitive?.content
                        ?: content["destinationHub"]?.jsonPrimitive?.content
                        ?: location?.get("name")?.jsonPrimitive?.content
                        ?: return@forEachIndexed
                    if (destCode.isBlank() || destCode.equals(origin, ignoreCase = true)) return@forEachIndexed

                    val price = content["flightQuotes"]?.jsonObject
                        ?.get("cheapest")?.jsonObject
                        ?.get("rawPrice")?.jsonPrimitive?.doubleOrNull
                        ?: content["price"]?.jsonPrimitive?.doubleOrNull
                        ?: return@forEachIndexed

                    val depIso = now.plusSeconds(3600L * (3 + idx)).toString()
                    val deepLink = "https://www.skyscanner.net/transport/flights/${origin.lowercase(Locale.US)}/${destCode.lowercase(Locale.US)}/?provider=RapidAPI"

                    results.add(
                        buildJsonObject {
                            put("originHub", JsonPrimitive(origin))
                            put("destinationHub", JsonPrimitive(destCode))
                            put("price", JsonPrimitive(price))
                            put("departureTime", JsonPrimitive(depIso))
                            put("returnTime", JsonNull)
                            put("checkInOnline", JsonPrimitive(true))
                            put("deepLink", JsonPrimitive(deepLink))
                            put("mode", JsonPrimitive("flight"))
                        }
                    )
                }
                if (results.isNotEmpty()) JsonArray(results) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getOrRefreshAmadeusToken(apiKey: String, apiSecret: String, baseHost: String): String? {
        val now = Instant.now().epochSecond
        if (cachedAmadeusToken != null && cachedAmadeusHost == baseHost && now < tokenExpiryEpoch - 60) {
            return cachedAmadeusToken
        }

        val requestBody = FormBody.Builder()
            .add("grant_type", "client_credentials")
            .add("client_id", apiKey)
            .add("client_secret", apiSecret)
            .build()

        val request = Request.Builder()
            .url("$baseHost/v1/security/oauth2/token")
            .post(requestBody)
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val obj = json.parseToJsonElement(body).jsonObject
                val token = obj["access_token"]?.jsonPrimitive?.content ?: return null
                val expiresIn = obj["expires_in"]?.jsonPrimitive?.longOrNull ?: 1799L
                cachedAmadeusToken = token
                cachedAmadeusHost = baseHost
                tokenExpiryEpoch = now + expiresIn
                token
            }
        } catch (_: Exception) {
            null
        }
    }
}
