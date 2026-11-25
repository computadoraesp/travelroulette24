package com.example.travelroulette24.data.byok

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Dedicated surface & custom transport service handling:
 * - Custom user-configured REST APIs (flight, train, ferry) + ping connectivity tester
 * - Navitia train offers
 * - Open-HAFAS live rail departures (explicitly marked with isEstimatedPrice = true)
 */
internal class SurfaceTransportService(
    private val credentials: ApiCredentialsManager,
    private val quotaGovernor: ApiQuotaGovernor? = null
) {
    private val httpClient = ByokHttpClient.client
    private val json = ByokHttpClient.json

    fun fetchFromCustomProviders(
        customMode: String,
        quotaPrefix: String,
        cacheKey: String,
        originHubs: List<String>,
        forceFresh: Boolean = false
    ): JsonArray? {
        if (!forceFresh) {
            val cached = quotaGovernor?.getCachedOffers(cacheKey)
            if (cached != null && cached.isNotEmpty()) {
                return cached
            }
        }

        val providers = credentials.getCustomProvidersForMode(customMode)
        for (provider in providers) {
            val quotaId = "$quotaPrefix${provider.id}"
            val canExecute = quotaGovernor?.canExecuteRequest(
                provider = quotaId,
                isPaidTier = provider.isPaidTier,
                customDailyLimit = provider.dailyLimit
            ) ?: true

            if (canExecute) {
                val offers = fetchCustomApiOffers(provider, originHubs)
                if (offers != null && offers.isNotEmpty()) {
                    quotaGovernor?.recordRequest(quotaId)
                    quotaGovernor?.putCachedOffers(cacheKey, offers)
                    return offers
                }
            }
        }
        return null
    }

    fun fetchCustomApiOffers(
        provider: CustomTravelProvider,
        originHubs: List<String>
    ): JsonArray? {
        val origin = originHubs.firstOrNull() ?: "MAD"
        val request = buildAuthorizedRequest(
            endpointUrl = provider.endpointUrl,
            authHeader = provider.authHeader,
            apiKey = provider.apiKey,
            mode = provider.mode,
            origin = origin
        )

        return try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val element = json.parseToJsonElement(body)
                parseCustomOffers(element, provider.mode, provider.name, origin)
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun testProviderConnection(
        endpointUrl: String,
        authHeader: String,
        apiKey: String,
        mode: String = "flight",
        testOrigin: String = "MAD"
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (endpointUrl.isBlank()) {
            return@withContext false to "Endpoint URL cannot be empty"
        }

        val request = buildAuthorizedRequest(
            endpointUrl = endpointUrl,
            authHeader = authHeader,
            apiKey = apiKey,
            mode = mode,
            origin = testOrigin
        )

        try {
            httpClient.newCall(request).execute().use { response ->
                val code = response.code
                val body = response.body?.string().orEmpty()

                if (response.isSuccessful) {
                    val element = try { json.parseToJsonElement(body) } catch (_: Exception) { null }
                    val count = when (element) {
                        is JsonArray -> element.size
                        is JsonObject -> {
                            element["data"]?.jsonArray?.size
                                ?: element["results"]?.jsonArray?.size
                                ?: element["offers"]?.jsonArray?.size
                                ?: element["trips"]?.jsonArray?.size
                                ?: 1
                        }
                        else -> 0
                    }
                    if (count > 0) {
                        true to "✓ HTTP $code ($count)"
                    } else {
                        false to "HTTP $code (Non-JSON / HTML response)"
                    }
                } else {
                    val sample = if (body.length > 100) body.take(100) + "..." else body
                    false to "HTTP $code: ${response.message}. $sample"
                }
            }
        } catch (e: Exception) {
            false to (e.localizedMessage ?: e.message ?: "Timeout")
        }
    }

    fun fetchNavitiaTrainOffers(token: String, originHubs: List<String>): JsonArray? {
        val origin = originHubs.firstOrNull { it.length > 2 } ?: "GDL"
        val url = "https://api.navitia.io/v1/coverage/default/journeys?from=admin:fr:$origin&datetime=${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}T080000"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", token)
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val root = json.parseToJsonElement(body).jsonObject
                val journeys = root["journeys"]?.jsonArray ?: return null

                val results = mutableListOf<JsonObject>()
                val now = Instant.now()
                journeys.take(10).forEachIndexed { idx, _ ->
                    val depTime = now.plusSeconds(3600L * (1 + idx)).toString()
                    results.add(
                        buildJsonObject {
                            put("originHub", JsonPrimitive(origin))
                            put("destinationHub", JsonPrimitive("TrainDest-$idx"))
                            put("price", JsonPrimitive(25.0 + (idx * 6)))
                            put("departureTime", JsonPrimitive(depTime))
                            put("returnTime", JsonNull)
                            put("checkInOnline", JsonPrimitive(true))
                            put("deepLink", JsonPrimitive("https://www.sncf-connect.com/?provider=Navitia"))
                            put("mode", JsonPrimitive("train"))
                            put("isEstimatedPrice", JsonPrimitive(true))
                        }
                    )
                }
                if (results.isNotEmpty()) JsonArray(results) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun fetchOpenHafasTrainOffers(
        originCity: String,
        originHubs: List<String>,
        mode: String,
        stayMinHours: Int
    ): JsonArray? {
        val searchQuery = originCity.ifBlank { originHubs.firstOrNull() ?: return null }
        val originLabel = originHubs.firstOrNull { it.length > 3 } ?: originHubs.firstOrNull() ?: searchQuery

        return try {
            val locUrl = "https://v6.db.transport.rest/locations?query=${ByokHttpClient.encodeParam(searchQuery)}&stops=true&results=1"
            val locReq = Request.Builder().url(locUrl).build()
            val stationId = httpClient.newCall(locReq).execute().use { locRes ->
                if (!locRes.isSuccessful) return null
                val locBody = locRes.body?.string() ?: return null
                val locArr = json.parseToJsonElement(locBody).jsonArray
                locArr.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.content ?: return null
            }

            val depUrl = "https://v6.db.transport.rest/stops/${ByokHttpClient.encodeParam(stationId)}/departures?duration=720&results=12"
            val depReq = Request.Builder().url(depUrl).build()
            val departures = httpClient.newCall(depReq).execute().use { depRes ->
                if (!depRes.isSuccessful) return null
                val depBody = depRes.body?.string() ?: return null
                val depRoot = json.parseToJsonElement(depBody).jsonObject
                depRoot["departures"]?.jsonArray ?: return null
            }

            val results = mutableListOf<JsonObject>()
            val seenDestinations = mutableSetOf<String>()
            val now = Instant.now()

            departures.forEachIndexed { idx, item ->
                val obj = item.jsonObject
                val destName = obj["destination"]?.jsonObject?.get("name")?.jsonPrimitive?.content
                    ?: obj["direction"]?.jsonPrimitive?.content
                    ?: return@forEachIndexed

                val cleanDest = destName.substringBefore("(").trim().take(24)
                if (cleanDest.isBlank() || cleanDest.equals(searchQuery, ignoreCase = true)) return@forEachIndexed
                if (!seenDestinations.add(cleanDest)) return@forEachIndexed

                val whenIso = obj["when"]?.jsonPrimitive?.content
                    ?: obj["plannedWhen"]?.jsonPrimitive?.content
                    ?: now.plusSeconds(3600L * (2 + idx)).toString()

                val parsedWhen = try {
                    Instant.parse(whenIso)
                } catch (_: Exception) {
                    now.plusSeconds(3600L * (2 + idx))
                }
                val returnIso = if (mode == "round_trip") {
                    parsedWhen.plusSeconds(stayMinHours.coerceAtLeast(12) * 3600L).toString()
                } else null

                val estimatedRailFare = 19.0 + ((idx * 7) % 45)
                val deepLink = "https://www.thetrainline.com/book/results?origin=${ByokHttpClient.encodeParam(searchQuery)}&destination=${ByokHttpClient.encodeParam(cleanDest)}&provider=Open-HAFAS"

                results.add(
                    buildJsonObject {
                        put("originHub", JsonPrimitive(originLabel))
                        put("destinationHub", JsonPrimitive(cleanDest))
                        put("price", JsonPrimitive(estimatedRailFare))
                        put("departureTime", JsonPrimitive(parsedWhen.toString()))
                        if (returnIso != null) {
                            put("returnTime", JsonPrimitive(returnIso))
                        } else {
                            put("returnTime", JsonNull)
                        }
                        put("checkInOnline", JsonPrimitive(true))
                        put("deepLink", JsonPrimitive(deepLink))
                        put("mode", JsonPrimitive("train"))
                        put("isEstimatedPrice", JsonPrimitive(true))
                    }
                )
            }

            if (results.isNotEmpty()) JsonArray(results) else null
        } catch (_: Exception) {
            null
        }
    }

    private fun buildAuthorizedRequest(
        endpointUrl: String,
        authHeader: String,
        apiKey: String,
        mode: String,
        origin: String
    ): Request {
        val separator = if (endpointUrl.contains("?")) "&" else "?"
        val fullUrl = "$endpointUrl${separator}origin=${ByokHttpClient.encodeParam(origin)}&mode=${ByokHttpClient.encodeParam(mode)}"

        val reqBuilder = Request.Builder().url(fullUrl)
        if (apiKey.isNotBlank() && authHeader.isNotBlank()) {
            val headerVal = if (authHeader.equals("Authorization", ignoreCase = true) &&
                !apiKey.startsWith("Bearer ", ignoreCase = true)
            ) {
                "Bearer $apiKey"
            } else {
                apiKey
            }
            reqBuilder.addHeader(authHeader, headerVal)
        }
        return reqBuilder.build()
    }

    private fun parseCustomOffers(
        rootElement: JsonElement,
        defaultMode: String,
        providerName: String,
        defaultOrigin: String
    ): JsonArray? {
        val itemsArray = when (rootElement) {
            is JsonArray -> rootElement
            is JsonObject -> {
                rootElement["data"]?.jsonArray
                    ?: rootElement["results"]?.jsonArray
                    ?: rootElement["offers"]?.jsonArray
                    ?: rootElement["options"]?.jsonArray
                    ?: rootElement["trips"]?.jsonArray
                    ?: rootElement["routes"]?.jsonArray
                    ?: rootElement["connections"]?.jsonArray
                    ?: rootElement["items"]?.jsonArray
            }
            else -> null
        } ?: return null

        val results = mutableListOf<JsonObject>()
        val now = Instant.now()

        for ((idx, item) in itemsArray.withIndex()) {
            val obj = item as? JsonObject ?: continue

            val origin = obj["originHub"]?.jsonPrimitive?.content
                ?: obj["origin"]?.jsonPrimitive?.content
                ?: obj["from"]?.jsonPrimitive?.content
                ?: obj["departure_hub"]?.jsonPrimitive?.content
                ?: defaultOrigin

            val dest = obj["destinationHub"]?.jsonPrimitive?.content
                ?: obj["destination"]?.jsonPrimitive?.content
                ?: obj["to"]?.jsonPrimitive?.content
                ?: obj["arrival_hub"]?.jsonPrimitive?.content
                ?: continue

            val price = obj["price"]?.jsonPrimitive?.doubleOrNull
                ?: obj["amount"]?.jsonPrimitive?.doubleOrNull
                ?: obj["total"]?.jsonPrimitive?.doubleOrNull
                ?: obj["fare"]?.jsonPrimitive?.doubleOrNull
                ?: 29.99

            val departureStr = obj["departureTime"]?.jsonPrimitive?.content
                ?: obj["departure"]?.jsonPrimitive?.content
                ?: obj["depart_at"]?.jsonPrimitive?.content
                ?: now.plusSeconds(3600L * (2 + idx * 2)).toString()

            val returnStr = obj["returnTime"]?.jsonPrimitive?.content
                ?: obj["return"]?.jsonPrimitive?.content

            val checkIn = obj["checkInOnline"]?.jsonPrimitive?.content?.toBooleanStrictOrNull()
                ?: true

            val deepLink = obj["deepLink"]?.jsonPrimitive?.content
                ?: obj["booking_url"]?.jsonPrimitive?.content
                ?: obj["url"]?.jsonPrimitive?.content
                ?: "https://www.google.com/travel?q=$origin+to+$dest&provider=${ByokHttpClient.encodeParam(providerName)}"

            val mode = obj["mode"]?.jsonPrimitive?.content ?: defaultMode

            results.add(
                buildJsonObject {
                    put("originHub", JsonPrimitive(origin))
                    put("destinationHub", JsonPrimitive(dest))
                    put("price", JsonPrimitive(price))
                    put("departureTime", JsonPrimitive(departureStr))
                    if (returnStr != null) {
                        put("returnTime", JsonPrimitive(returnStr))
                    } else {
                        put("returnTime", JsonNull)
                    }
                    put("checkInOnline", JsonPrimitive(checkIn))
                    put("deepLink", JsonPrimitive(deepLink))
                    put("mode", JsonPrimitive(mode))
                }
            )
        }

        return if (results.isNotEmpty()) JsonArray(results) else null
    }
}
