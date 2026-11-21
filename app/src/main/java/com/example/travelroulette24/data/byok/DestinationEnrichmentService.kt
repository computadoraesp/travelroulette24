package com.example.travelroulette24.data.byok

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.travelroulette24.R
import com.example.travelroulette24.ui.DestinationWeather
import com.example.travelroulette24.utils.CurrencyHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale

/**
 * Dedicated destination enrichment service handling:
 * - Open-Meteo live weather (30-min cache)
 * - ExchangeRate-API currency conversion (2-hour cache)
 * - OpenTripMap top attractions (60-min cache + QuotaGovernor)
 * - Unsplash destination hero photos (60-min cache + QuotaGovernor)
 * - AviationStack live flight radar status (30-min cache + QuotaGovernor)
 * - Gemini express itinerary generation (60-min cache + QuotaGovernor)
 *
 * All OkHttp responses are safely closed via `.use { }`.
 */
internal class DestinationEnrichmentService(
    private val credentials: ApiCredentialsManager,
    private val quotaGovernor: ApiQuotaGovernor? = null
) {
    private val httpClient = ByokHttpClient.client
    private val json = ByokHttpClient.json

    private val weatherCache = mutableMapOf<String, Pair<Long, DestinationWeather>>()
    private val currencyRateCache = mutableMapOf<String, Pair<Long, Double>>()
    private val photoCache = mutableMapOf<String, Pair<Long, Bitmap>>()
    private val flightRadarCache = mutableMapOf<String, Pair<Long, ByokTravelService.LiveFlightRadarStatus>>()
    private val attractionsCache = mutableMapOf<String, Pair<Long, List<String>>>()
    private val itineraryCache = mutableMapOf<String, Pair<Long, String>>()

    suspend fun fetchTopAttractions(cityName: String): List<String> = withContext(Dispatchers.IO) {
        val key = credentials.openTripMapKey
        if (key.isBlank() || cityName.isBlank()) return@withContext emptyList()

        val cacheKey = cityName.trim().lowercase(Locale.US)
        val now = System.currentTimeMillis()
        attractionsCache[cacheKey]?.let { (ts, cached) ->
            if (now - ts < 60 * 60 * 1000L) return@withContext cached
        }

        val canExecute = quotaGovernor?.canExecuteRequest(
            provider = "opentripmap",
            isPaidTier = false,
            monthlyQuota = 1000
        ) ?: true
        if (!canExecute) return@withContext emptyList()

        val url = "https://api.opentripmap.com/0.1/en/places/geoname?name=${ByokHttpClient.encodeParam(cityName)}&apikey=$key"
        val request = Request.Builder().url(url).build()

        try {
            val (lat, lon) = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val root = json.parseToJsonElement(body).jsonObject
                val parsedLat = root["lat"]?.jsonPrimitive?.doubleOrNull ?: return@withContext emptyList()
                val parsedLon = root["lon"]?.jsonPrimitive?.doubleOrNull ?: return@withContext emptyList()
                parsedLat to parsedLon
            }

            val radiusUrl = "https://api.opentripmap.com/0.1/en/places/radius?radius=8000&lon=$lon&lat=$lat&rate=3&limit=4&format=json&apikey=$key"
            httpClient.newCall(Request.Builder().url(radiusUrl).build()).execute().use { radiusResponse ->
                if (!radiusResponse.isSuccessful) return@withContext emptyList()
                val radiusBody = radiusResponse.body?.string() ?: return@withContext emptyList()
                val places = json.parseToJsonElement(radiusBody).jsonArray
                val names = places.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.content?.takeIf { n -> n.isNotBlank() } }.distinct()
                quotaGovernor?.recordRequest("opentripmap")
                attractionsCache[cacheKey] = now to names
                names
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchLiveWeather(latitude: Double, longitude: Double): DestinationWeather? = withContext(Dispatchers.IO) {
        val cacheKey = "${String.format(Locale.US, "%.2f", latitude)}_${String.format(Locale.US, "%.2f", longitude)}"
        val now = System.currentTimeMillis()
        weatherCache[cacheKey]?.let { (ts, cached) ->
            if (now - ts < 30 * 60 * 1000L) return@withContext cached
        }

        val url = String.format(
            Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,weather_code",
            latitude,
            longitude
        )

        try {
            httpClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val root = json.parseToJsonElement(body).jsonObject
                val current = root["current"]?.jsonObject ?: return@withContext null
                val tempC = current["temperature_2m"]?.jsonPrimitive?.doubleOrNull?.toInt() ?: return@withContext null
                val code = current["weather_code"]?.jsonPrimitive?.intOrNull ?: 0
                val (icon, label, condRes) = mapWmoWeatherCode(code)
                val liveWeather = DestinationWeather(tempC = tempC, icon = icon, condition = label, isLive = true, conditionRes = condRes)
                weatherCache[cacheKey] = now to liveWeather
                liveWeather
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchCurrencyRate(targetCurrency: String): Double = withContext(Dispatchers.IO) {
        val code = targetCurrency.uppercase()
        if (code == CurrencyHelper.EUR) return@withContext 1.0

        val now = System.currentTimeMillis()
        currencyRateCache[code]?.let { (ts, rate) ->
            if (now - ts < 120 * 60 * 1000L) return@withContext rate
        }

        val key = credentials.exchangeRateKey
        val url = if (key.isNotBlank()) {
            "https://v6.exchangerate-api.com/v6/$key/latest/EUR"
        } else {
            "https://open.er-api.com/v6/latest/EUR"
        }

        try {
            httpClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) return@withContext CurrencyHelper.fallbackRateFromEur(code)
                val body = response.body?.string() ?: return@withContext CurrencyHelper.fallbackRateFromEur(code)
                val root = json.parseToJsonElement(body).jsonObject
                val ratesObj = root["conversion_rates"]?.jsonObject ?: root["rates"]?.jsonObject
                val rate = ratesObj?.get(code)?.jsonPrimitive?.doubleOrNull
                    ?: return@withContext CurrencyHelper.fallbackRateFromEur(code)
                currencyRateCache[code] = now to rate
                rate
            }
        } catch (_: Exception) {
            CurrencyHelper.fallbackRateFromEur(code)
        }
    }

    suspend fun fetchGeminiExpressItinerary(
        cityName: String,
        mode: String,
        stayHours: Int,
        budgetFormatted: String
    ): String? = withContext(Dispatchers.IO) {
        val key = credentials.geminiApiKey
        if (key.isBlank() || cityName.isBlank()) return@withContext null

        val lang = Locale.getDefault().displayLanguage.ifBlank { "English" }
        val cacheKey = "${cityName.trim().lowercase(Locale.US)}_${mode.lowercase(Locale.US)}_${stayHours}_${lang.lowercase(Locale.US)}"
        val now = System.currentTimeMillis()
        itineraryCache[cacheKey]?.let { (ts, cached) ->
            if (now - ts < 60 * 60 * 1000L) return@withContext cached
        }

        val canExecute = quotaGovernor?.canExecuteRequest(
            provider = "gemini",
            isPaidTier = false,
            monthlyQuota = 1500
        ) ?: true
        if (!canExecute) return@withContext null

        val promptText = "Create a concise, practical ${stayHours}h express travel plan for $cityName " +
            "(arriving by $mode, fare $budgetFormatted). Include: 1) Morning/afternoon highlights, " +
            "2) Affordable local food spot, 3) Quick city transit tip. Respond in $lang in max 6 bullet points."

        val payload = buildJsonObject {
            put(
                "contents",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put(
                                "parts",
                                buildJsonArray {
                                    add(buildJsonObject { put("text", JsonPrimitive(promptText)) })
                                }
                            )
                        }
                    )
                }
            )
        }.toString()

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${ByokHttpClient.encodeParam(key)}"
        val request = Request.Builder()
            .url(url)
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val root = json.parseToJsonElement(body).jsonObject
                val candidates = root["candidates"]?.jsonArray ?: return@withContext null
                val first = candidates.firstOrNull()?.jsonObject ?: return@withContext null
                val content = first["content"]?.jsonObject ?: return@withContext null
                val parts = content["parts"]?.jsonArray ?: return@withContext null
                val text = parts.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content?.trim()
                if (!text.isNullOrBlank()) {
                    quotaGovernor?.recordRequest("gemini")
                    itineraryCache[cacheKey] = now to text
                }
                text
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchLiveFlightStatus(
        originHub: String,
        destHub: String
    ): ByokTravelService.LiveFlightRadarStatus? = withContext(Dispatchers.IO) {
        val key = credentials.aviationStackKey
        if (key.isBlank()) return@withContext null

        val depIata = originHub.trim().uppercase(Locale.US)
        val arrIata = destHub.trim().uppercase(Locale.US)
        if (depIata.length != 3 || arrIata.length != 3) return@withContext null

        val cacheKey = "${depIata}_$arrIata"
        val now = System.currentTimeMillis()
        flightRadarCache[cacheKey]?.let { (ts, cached) ->
            if (now - ts < 30 * 60 * 1000L) return@withContext cached
        }

        val canExecute = quotaGovernor?.canExecuteRequest(
            provider = "aviationstack",
            isPaidTier = false,
            monthlyQuota = 500
        ) ?: true
        if (!canExecute) return@withContext null

        val url = "https://api.aviationstack.com/v1/flights?access_key=${ByokHttpClient.encodeParam(key)}&dep_iata=${ByokHttpClient.encodeParam(depIata)}&arr_iata=${ByokHttpClient.encodeParam(arrIata)}&limit=1"
        val request = Request.Builder().url(url).build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val root = json.parseToJsonElement(body).jsonObject
                val first = root["data"]?.jsonArray?.firstOrNull()?.jsonObject ?: return@withContext null

                val flightObj = first["flight"]?.jsonObject
                val flightNumber = flightObj?.get("iata")?.jsonPrimitive?.content
                    ?: flightObj?.get("number")?.jsonPrimitive?.content
                    ?: return@withContext null
                val airline = first["airline"]?.jsonObject?.get("name")?.jsonPrimitive?.content
                val status = first["flight_status"]?.jsonPrimitive?.content ?: "scheduled"
                val depObj = first["departure"]?.jsonObject
                val terminal = depObj?.get("terminal")?.let { if (it is JsonNull) null else it.jsonPrimitive.content }
                val gate = depObj?.get("gate")?.let { if (it is JsonNull) null else it.jsonPrimitive.content }
                val delay = depObj?.get("delay")?.let { if (it is JsonNull) 0 else it.jsonPrimitive.intOrNull ?: 0 } ?: 0

                val radar = ByokTravelService.LiveFlightRadarStatus(
                    flightNumber = flightNumber,
                    airline = airline,
                    status = status,
                    terminal = terminal,
                    gate = gate,
                    delayMinutes = delay
                )
                quotaGovernor?.recordRequest("aviationstack")
                flightRadarCache[cacheKey] = now to radar
                radar
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchDestinationPhoto(cityName: String): Bitmap? = withContext(Dispatchers.IO) {
        val key = credentials.unsplashKey
        if (key.isBlank() || cityName.isBlank()) return@withContext null

        val cacheKey = cityName.trim().lowercase(Locale.US)
        val now = System.currentTimeMillis()
        photoCache[cacheKey]?.let { (ts, cachedBmp) ->
            if (now - ts < 60 * 60 * 1000L) return@withContext cachedBmp
        }

        val canExecute = quotaGovernor?.canExecuteRequest(
            provider = "unsplash",
            isPaidTier = false,
            monthlyQuota = 1500
        ) ?: true
        if (!canExecute) return@withContext null

        val queryUrl = "https://api.unsplash.com/search/photos?query=${ByokHttpClient.encodeParam("$cityName landmark travel")}&per_page=1&orientation=landscape"
        val searchReq = Request.Builder()
            .url(queryUrl)
            .addHeader("Authorization", "Client-ID $key")
            .build()

        try {
            val imgUrl = httpClient.newCall(searchReq).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val root = json.parseToJsonElement(body).jsonObject
                val first = root["results"]?.jsonArray?.firstOrNull()?.jsonObject ?: return@withContext null
                val urls = first["urls"]?.jsonObject ?: return@withContext null
                urls["small"]?.jsonPrimitive?.content
                    ?: urls["regular"]?.jsonPrimitive?.content
                    ?: return@withContext null
            }

            httpClient.newCall(Request.Builder().url(imgUrl).build()).execute().use { imgRes ->
                if (!imgRes.isSuccessful) return@withContext null
                val bytes = imgRes.body?.bytes() ?: return@withContext null
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext null

                quotaGovernor?.recordRequest("unsplash")
                photoCache[cacheKey] = now to bmp
                bmp
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun mapWmoWeatherCode(code: Int): Triple<String, String, Int> = when (code) {
        0 -> Triple("☀️", "Clear", R.string.weather_cond_clear)
        1, 2 -> Triple("🌤️", "Partly cloudy", R.string.weather_cond_partly_cloudy)
        3 -> Triple("☁️", "Cloudy", R.string.weather_cond_cloudy)
        45, 48 -> Triple("🌫️", "Fog", R.string.weather_cond_fog)
        51, 53, 55, 61, 63, 65 -> Triple("🌧️", "Rain", R.string.weather_cond_rain)
        71, 73, 75 -> Triple("❄️", "Snow", R.string.weather_cond_snow)
        80, 81, 82 -> Triple("🌦️", "Showers", R.string.weather_cond_showers)
        95, 96, 99 -> Triple("⛈️", "Storm", R.string.weather_cond_storm)
        else -> Triple("🌤️", "Live weather", R.string.weather_cond_live)
    }
}
