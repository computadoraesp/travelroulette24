package com.example.travelroulette24.data

import com.example.travelroulette24.R
import com.example.travelroulette24.ui.DestinationWeather
import com.example.travelroulette24.ui.HubCoordinates
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.time.Duration
import java.time.Instant
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Isolated Demo / Simulation Data Source.
 *
 * Strictly separated from real/live API logic ([com.example.travelroulette24.data.byok.ByokTravelService]
 * and [BackendApi]). Generates distance-proportional, geographically global offers across
 * Americas, Europe, Antarctica gateways, Asia-Pacific, and Africa.
 */
object DemoTravelDataSource {

    private val globalFlightPool = listOf(
        // Europe
        "BCN", "MAD", "LHR", "FCO", "AMS", "BER", "LIS",
        "DUB", "VIE", "PRG", "PMI", "OPO", "ATH", "BRU",
        // Americas
        "JFK", "MIA", "LAX", "ORD", "YYZ", "MEX", "CUN", "MTY",
        "BOG", "MDE", "CTG", "LIM", "CUZ", "PTY", "SJO", "UIO",
        "EZE", "AEP", "COR", "SCL", "GRU", "GIG", "MVD", "HAV", "PUJ",
        // Antarctica & Sub-Antarctic Gateways
        "USH", "PUQ", "TNM", "MCM", "PMC",
        // Asia, Middle East, Africa & Oceania
        "HND", "ICN", "DXB", "CMN", "RAK", "CPT", "SYD", "AKL"
    )

    private val globalTrainPool = listOf(
        "GDL", "Sants", "Atocha", "StPancras", "LYS", "PartDieu",
        "Midi", "AMS-CS", "BerlinHbf", "Termini", "Centrale",
        "Penn", "Retiro", "FinDelMundo", "TokyoStn"
    )

    private val globalPortPool = listOf(
        "Bercy", "BCN-Port", "PMI-Port", "VLC-Port", "Piraeus", "RTM-Port",
        "Civitavecchia", "LIS-Port", "DUB-Port",
        "MIA-Port", "Cozumel-Port", "BUE-Port", "Colonia-Port", "Valparaiso-Port",
        "Ushuaia-Port", "PuntaArenas-Port"
    )

    private val trainOrigins = globalTrainPool.toSet() + setOf("JoaquinSorolla", "SantaJusta", "MariaZambrano", "Oriente")
    private val portOrigins = globalPortPool.toSet() + setOf("MRS-Port", "AGP-Port")

    fun generateDemoOptions(
        originHubs: List<String>,
        mode: String,
        stayMinHours: Int,
        stayMaxHours: Int,
        departureWindowHours: Int,
        now: Instant = Instant.now()
    ): JsonArray {
        val generatedList = mutableListOf<JsonObject>()
        var count = 0

        for (origin in originHubs) {
            val isTrainOrigin = origin in trainOrigins ||
                origin.contains("Stn", ignoreCase = true) ||
                origin.contains("Estaci", ignoreCase = true) ||
                origin.contains("Station", ignoreCase = true)
            val isPortOrigin = origin in portOrigins ||
                origin.contains("Port", ignoreCase = true) ||
                origin.contains("Puerto", ignoreCase = true)

            val originCity = HubCoordinates.getCity(origin)
            val originCoords = HubCoordinates.getCoordinatesOrNull(origin)

            val (destList, travelMode) = when {
                isPortOrigin -> {
                    val filtered = selectGeoRelevantDestinations(origin, originCity, originCoords, globalPortPool, maxRegionalKm = 4200, desiredCount = 8)
                    filtered to "ferry"
                }
                isTrainOrigin -> {
                    val filtered = selectGeoRelevantDestinations(origin, originCity, originCoords, globalTrainPool, maxRegionalKm = 2800, desiredCount = 9)
                    filtered to "train"
                }
                else -> {
                    val filtered = selectGeoRelevantFlights(origin, originCity, originCoords)
                    filtered to "flight"
                }
            }

            for (dest in destList) {
                if (origin.equals(dest, ignoreCase = true)) continue
                if (originCity.equals(HubCoordinates.getCity(dest), ignoreCase = true)) continue
                count++
                val windowMinutes = ((departureWindowHours - 1).coerceAtLeast(1) * 60)
                val departureOffsetMinutes = 30 + (count * 45) % windowMinutes
                val departure = now.plus(Duration.ofMinutes(departureOffsetMinutes.toLong()))
                val returnTime = if (mode == "round_trip") {
                    val span = (stayMaxHours - stayMinHours + 1).coerceAtLeast(1)
                    val stayHours = stayMinHours + (count % span)
                    departure.plus(Duration.ofHours(stayHours.toLong())).toString()
                } else null

                val distKm = HubCoordinates.calculateDistanceKm(origin, dest).coerceAtLeast(80)
                val jitter = (count * 7) % 19
                val isPromo = (count % 5 == 0)
                val basePrice = when (travelMode) {
                    "train" -> {
                        val raw = 14.0 + (distKm * 0.042) + jitter
                        if (isPromo) (raw * 0.65).coerceAtLeast(14.0) else raw
                    }
                    "ferry" -> {
                        val raw = 16.0 + (distKm * 0.036) + jitter
                        if (isPromo) (raw * 0.68).coerceAtLeast(16.0) else raw
                    }
                    else -> {
                        val raw = when {
                            distKm < 900 -> 24.0 + (distKm * 0.055) + jitter
                            distKm < 3200 -> 55.0 + (distKm * 0.048) + jitter
                            else -> 140.0 + (distKm * 0.038) + jitter
                        }
                        if (isPromo) (raw * 0.62).coerceAtLeast(19.0) else raw
                    }
                }.let { (it * 100.0).roundToInt() / 100.0 }

                val deepLinkUrl = when (travelMode) {
                    "train" -> "https://www.thetrainline.com/book/results?origin=$origin&destination=$dest&source=demo"
                    "ferry" -> "https://www.ferryhopper.com/en/search?origin=$origin&destination=$dest&source=demo"
                    else -> "https://www.google.com/travel/flights?q=flights+from+$origin+to+$dest&source=demo"
                }

                generatedList.add(
                    buildJsonObject {
                        put("originHub", JsonPrimitive(origin))
                        put("destinationHub", JsonPrimitive(dest))
                        put("price", JsonPrimitive(basePrice))
                        put("departureTime", JsonPrimitive(departure.toString()))
                        if (returnTime != null) {
                            put("returnTime", JsonPrimitive(returnTime))
                        } else {
                            put("returnTime", JsonNull)
                        }
                        put("checkInOnline", JsonPrimitive(count % 3 != 0))
                        put("deepLink", JsonPrimitive(deepLinkUrl))
                        put("mode", JsonPrimitive(travelMode))
                    }
                )
            }
        }
        return JsonArray(generatedList)
    }

    private fun selectGeoRelevantDestinations(
        origin: String,
        originCity: String,
        originCoords: Pair<Double, Double>?,
        pool: List<String>,
        maxRegionalKm: Int,
        desiredCount: Int
    ): List<String> {
        val candidates = pool.filter { dest ->
            !dest.equals(origin, ignoreCase = true) &&
                !HubCoordinates.getCity(dest).equals(originCity, ignoreCase = true)
        }
        if (originCoords == null) return candidates.take(desiredCount)

        val sortedByDist = candidates.sortedBy { dest ->
            HubCoordinates.calculateDistanceKmOrNull(originCoords.first, originCoords.second, dest) ?: Int.MAX_VALUE
        }
        val regional = sortedByDist.filter { dest ->
            val d = HubCoordinates.calculateDistanceKmOrNull(originCoords.first, originCoords.second, dest) ?: Int.MAX_VALUE
            d <= maxRegionalKm
        }
        return if (regional.size >= 3) regional.take(desiredCount) else sortedByDist.take(desiredCount)
    }

    private fun selectGeoRelevantFlights(
        origin: String,
        originCity: String,
        originCoords: Pair<Double, Double>?
    ): List<String> {
        val candidates = globalFlightPool.filter { dest ->
            !dest.equals(origin, ignoreCase = true) &&
                !HubCoordinates.getCity(dest).equals(originCity, ignoreCase = true)
        }
        if (originCoords == null) return candidates.take(24)

        val sortedByDist = candidates.sortedBy { dest ->
            HubCoordinates.calculateDistanceKmOrNull(originCoords.first, originCoords.second, dest) ?: Int.MAX_VALUE
        }
        val regional = sortedByDist.take(10)
        val mediumAndLong = sortedByDist.drop(10).filterIndexed { index, dest ->
            dest in setOf("USH", "PUQ", "TNM", "EZE", "BOG", "MEX", "JFK", "MAD", "CDG", "LHR", "HND") ||
                index % 2 == 0
        }.take(14)
        return (regional + mediumAndLong).distinct()
    }

    fun generateDemoHistory(routes: List<Pair<String, String>>): JsonArray {
        var idx = 0
        return JsonArray(routes.map { (origin, dest) ->
            idx++
            val dist = HubCoordinates.calculateDistanceKm(origin, dest).coerceAtLeast(100)
            val baselineMin12 = (16.0 + dist * 0.032).roundToInt().toDouble()
            val baselineMin30 = (22.0 + dist * 0.036).roundToInt().toDouble()
            val avg = (baselineMin30 * 1.45).roundToInt().toDouble()
            buildJsonObject {
                put("originHub", JsonPrimitive(origin))
                put("destinationHub", JsonPrimitive(dest))
                put("min12Months", JsonPrimitive(baselineMin12))
                put("min30Days", JsonPrimitive(baselineMin30))
                put("average", JsonPrimitive(avg))
            }
        })
    }

    private val simulatedWeatherMap = mapOf(
        "BCN" to DestinationWeather(22, "☀️", "Sunny & mild", conditionRes = R.string.weather_cond_sunny_mild),
        "MAD" to DestinationWeather(21, "🌤️", "Clear with breeze", conditionRes = R.string.weather_cond_clear_breeze),
        "PMI" to DestinationWeather(24, "☀️", "Mediterranean sun", conditionRes = R.string.weather_cond_mediterranean_sun),
        "IBZ" to DestinationWeather(25, "☀️", "Warm & sunny", conditionRes = R.string.weather_cond_warm_sunny),
        "AGP" to DestinationWeather(23, "☀️", "Sunny", conditionRes = R.string.weather_cond_sunny),
        "VLC" to DestinationWeather(22, "🌤️", "Pleasant", conditionRes = R.string.weather_cond_pleasant),
        "SVQ" to DestinationWeather(26, "☀️", "Warm & sunny", conditionRes = R.string.weather_cond_warm_sunny),
        "BIO" to DestinationWeather(18, "🌦️", "Mild clouds", conditionRes = R.string.weather_cond_cloudy_mild),
        "PAR" to DestinationWeather(16, "⛅", "Partly cloudy", conditionRes = R.string.weather_cond_partly_cloudy),
        "CDG" to DestinationWeather(16, "⛅", "Partly cloudy", conditionRes = R.string.weather_cond_partly_cloudy),
        "ORY" to DestinationWeather(16, "⛅", "Partly cloudy", conditionRes = R.string.weather_cond_partly_cloudy),
        "LHR" to DestinationWeather(15, "☁️", "Cloudy", conditionRes = R.string.weather_cond_cloudy),
        "LGW" to DestinationWeather(15, "☁️", "Cloudy", conditionRes = R.string.weather_cond_cloudy),
        "FCO" to DestinationWeather(20, "☀️", "Sunny", conditionRes = R.string.weather_cond_sunny),
        "BER" to DestinationWeather(14, "🌥️", "Cool with sunny spells", conditionRes = R.string.weather_cond_cool_clear),
        "AMS" to DestinationWeather(15, "🌦️", "Sea breeze & clouds", conditionRes = R.string.weather_cond_sea_breeze),
        "LIS" to DestinationWeather(21, "☀️", "Sunny", conditionRes = R.string.weather_cond_sunny),
        "OPO" to DestinationWeather(19, "🌤️", "Mild with breeze", conditionRes = R.string.weather_cond_mild_breeze),
        "ATH" to DestinationWeather(23, "☀️", "Mediterranean sun", conditionRes = R.string.weather_cond_mediterranean_sun),
        "DUB" to DestinationWeather(13, "🌧️", "Cool & rainy", conditionRes = R.string.weather_cond_cool_rain),
        "BOG" to DestinationWeather(16, "🌥️", "Cool & crisp", conditionRes = R.string.weather_cond_cool_clear),
        "MDE" to DestinationWeather(24, "🌤️", "Pleasant", conditionRes = R.string.weather_cond_pleasant),
        "CTG" to DestinationWeather(30, "☀️", "Warm & sunny", conditionRes = R.string.weather_cond_warm_sunny),
        "MEX" to DestinationWeather(22, "🌤️", "Pleasant", conditionRes = R.string.weather_cond_pleasant),
        "CUN" to DestinationWeather(28, "☀️", "Warm & sunny", conditionRes = R.string.weather_cond_warm_sunny),
        "LIM" to DestinationWeather(20, "☁️", "Cloudy", conditionRes = R.string.weather_cond_cloudy),
        "SCL" to DestinationWeather(19, "☀️", "Clear", conditionRes = R.string.weather_cond_clear),
        "EZE" to DestinationWeather(21, "🌤️", "Mild with breeze", conditionRes = R.string.weather_cond_mild_breeze),
        "AEP" to DestinationWeather(21, "🌤️", "Mild with breeze", conditionRes = R.string.weather_cond_mild_breeze),
        "GRU" to DestinationWeather(24, "⛅", "Partly cloudy", conditionRes = R.string.weather_cond_partly_cloudy),
        "USH" to DestinationWeather(4, "🌬️", "Cool & breezy", conditionRes = R.string.weather_cond_cool_cloudy),
        "PUQ" to DestinationWeather(6, "💨", "Cool & breezy", conditionRes = R.string.weather_cond_cool_cloudy),
        "TNM" to DestinationWeather(-3, "❄️", "Snow", conditionRes = R.string.weather_cond_snow),
        "MCM" to DestinationWeather(-12, "❄️", "Snow", conditionRes = R.string.weather_cond_snow)
    )

    fun getSimulatedWeather(hubCode: String): DestinationWeather {
        return simulatedWeatherMap[hubCode.uppercase()]
            ?: DestinationWeather(
                tempC = 19 + (abs(hubCode.hashCode()) % 7),
                icon = "🌤️",
                condition = "Mild",
                conditionRes = R.string.weather_cond_mild
            )
    }

    fun buildDemoShareUrl(
        destinationHub: String,
        centralized: Boolean,
        requiredCount: Int
    ): String {
        val splitParam = if (centralized) "split" else "individual"
        return "https://travelroulette24.app/demo-hub/$destinationHub?group=$requiredCount&mode=$splitParam"
    }
}
