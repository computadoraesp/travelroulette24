package com.example.travelroulette24.data

import com.example.travelroulette24.data.byok.ByokHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import java.util.Locale

/**
 * Dynamic global city autocomplete backed by the free, zero-key Open-Meteo Geocoding API.
 * Predicts any city or place worldwide in the user's language and caches resolved coordinates
 * for distance and Travel Value Index (km/€) calculations.
 */
object WorldDestinationsCatalog {

    data class WorldDestination(
        val code: String,
        val city: String,
        val country: String,
        val lat: Double,
        val lon: Double
    )

    // Runtime cache of dynamically resolved cities (keyed by uppercase code/name)
    private val resolvedCache = mutableMapOf<String, WorldDestination>()

    fun findByCode(code: String): WorldDestination? =
        resolvedCache[code.trim().uppercase(Locale.US)]

    fun cacheDestination(dest: WorldDestination) {
        resolvedCache[dest.code.trim().uppercase(Locale.US)] = dest
        resolvedCache[dest.city.trim().uppercase(Locale.US)] = dest
    }

    /**
     * Queries Open-Meteo Geocoding API (free, no API key) to predict real cities worldwide
     * matching [query] in [languageCode] (e.g. "es", "en", "fr", "de", "ja", "ko", "zh", "ar").
     */
    suspend fun searchGlobalCities(
        query: String,
        languageCode: String = Locale.getDefault().language.ifBlank { "en" },
        limit: Int = 6
    ): List<WorldDestination> = withContext(Dispatchers.IO) {
        val cleaned = query.trim()
        if (cleaned.length < 2) return@withContext emptyList()

        val encodedName = ByokHttpClient.encodeParam(cleaned)
        val lang = languageCode.take(2).lowercase(Locale.US)
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=$encodedName&count=$limit&language=$lang&format=json"
        val request = Request.Builder().url(url).build()

        try {
            ByokHttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val root = ByokHttpClient.json.parseToJsonElement(body).jsonObject
                val results = root["results"]?.jsonArray ?: return@withContext emptyList()

                results.mapNotNull { el ->
                    val obj = el.jsonObject
                    val name = obj["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val country = obj["country"]?.jsonPrimitive?.content.orEmpty()
                    val lat = obj["latitude"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
                    val lon = obj["longitude"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
                    val dest = WorldDestination(
                        code = name.uppercase(Locale.US),
                        city = name,
                        country = country,
                        lat = lat,
                        lon = lon
                    )
                    cacheDestination(dest)
                    dest
                }.distinctBy { "${it.city.lowercase(Locale.US)}_${it.country.lowercase(Locale.US)}" }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
