package com.example.travelroulette24.data

import com.example.travelroulette24.R
import com.example.travelroulette24.ui.DestinationWeather
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.time.Duration
import java.time.Instant

/**
 * Isolated Demo / Simulation Data Source.
 *
 * Strictly separated from real/live API logic ([com.example.travelroulette24.data.byok.ByokTravelService]
 * and [BackendApi]). This data source is ONLY invoked when the app is in Demo Mode
 * (`isRealMode == false`), never mixed into Real Mode results.
 */
object DemoTravelDataSource {

    private val flightDestinations = listOf(
        "BCN", "MAD", "LHR", "FCO", "AMS", "BER", "LIS",
        "DUB", "VIE", "PRG", "PMI", "OPO", "ATH", "BRU"
    )
    private val trainDestinations = listOf(
        "GDL", "Sants", "Atocha", "StPancras", "LYS", "MRS", "Midi", "AMS-CS", "BerlinHbf"
    )
    private val portDestinations = listOf(
        "Bercy", "BCN-Port", "PMI-Port", "VLC-Port", "Piraeus", "RTM-Port"
    )

    private val trainOrigins = setOf(
        "GDL", "Sants", "Atocha", "StPancras", "LYS", "PartDieu",
        "JoaquinSorolla", "SantaJusta", "MariaZambrano", "Oriente",
        "Termini", "Centrale", "AMS-CS", "Midi", "BerlinHbf", "Penn", "Retiro", "TokyoStn"
    )
    private val portOrigins = setOf(
        "Bercy", "BCN-Port", "MRS-Port", "VLC-Port", "AGP-Port",
        "PMI-Port", "LIS-Port", "DUB-Port", "Civitavecchia", "RTM-Port",
        "Piraeus", "MIA-Port", "BUE-Port"
    )

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
            val isTrainOrigin = origin in trainOrigins
            val isPortOrigin = origin in portOrigins

            val (destList, travelMode) = when {
                isPortOrigin -> portDestinations to "ferry"
                isTrainOrigin -> trainDestinations to "train"
                else -> flightDestinations to "flight"
            }

            for (dest in destList) {
                if (origin == dest) continue
                count++
                val windowMinutes = ((departureWindowHours - 1).coerceAtLeast(1) * 60)
                val departureOffsetMinutes = 30 + (count * 45) % windowMinutes
                val departure = now.plus(Duration.ofMinutes(departureOffsetMinutes.toLong()))
                val returnTime = if (mode == "round_trip") {
                    val span = (stayMaxHours - stayMinHours + 1).coerceAtLeast(1)
                    val stayHours = stayMinHours + (count % span)
                    departure.plus(Duration.ofHours(stayHours.toLong())).toString()
                } else null

                val basePrice = when (travelMode) {
                    "train" -> 19.0 + (count * 7) % 75
                    "ferry" -> 25.0 + (count * 9) % 65
                    else -> 18.0 + (count * 11) % 90
                }

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

    fun generateDemoHistory(routes: List<Pair<String, String>>): JsonArray {
        var idx = 0
        return JsonArray(routes.map { (origin, dest) ->
            idx++
            val min12 = if (idx % 2 == 0) 25.0 else 15.0
            val min30 = if (idx % 3 == 0) 30.0 else 20.0
            buildJsonObject {
                put("originHub", JsonPrimitive(origin))
                put("destinationHub", JsonPrimitive(dest))
                put("min12Months", JsonPrimitive(min12))
                put("min30Days", JsonPrimitive(min30))
                put("average", JsonPrimitive(55.0 + (idx * 5) % 40))
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
        "ALC" to DestinationWeather(23, "☀️", "Clear", conditionRes = R.string.weather_cond_clear),
        "SCQ" to DestinationWeather(17, "🌧️", "Light rain", conditionRes = R.string.weather_cond_light_rain),
        "PAR" to DestinationWeather(16, "⛅", "Partly cloudy", conditionRes = R.string.weather_cond_partly_cloudy),
        "CDG" to DestinationWeather(16, "⛅", "Partly cloudy", conditionRes = R.string.weather_cond_partly_cloudy),
        "ORY" to DestinationWeather(16, "⛅", "Partly cloudy", conditionRes = R.string.weather_cond_partly_cloudy),
        "LON" to DestinationWeather(15, "☁️", "Cloudy", conditionRes = R.string.weather_cond_cloudy),
        "LHR" to DestinationWeather(15, "☁️", "Cloudy", conditionRes = R.string.weather_cond_cloudy),
        "LGW" to DestinationWeather(15, "☁️", "Cloudy", conditionRes = R.string.weather_cond_cloudy),
        "STN" to DestinationWeather(14, "☁️", "Cool & cloudy", conditionRes = R.string.weather_cond_cool_cloudy),
        "ROM" to DestinationWeather(20, "☀️", "Sunny", conditionRes = R.string.weather_cond_sunny),
        "FCO" to DestinationWeather(20, "☀️", "Sunny", conditionRes = R.string.weather_cond_sunny),
        "BER" to DestinationWeather(14, "🌥️", "Cool with sunny spells", conditionRes = R.string.weather_cond_cool_clear),
        "AMS" to DestinationWeather(15, "🌦️", "Sea breeze & clouds", conditionRes = R.string.weather_cond_sea_breeze),
        "LIS" to DestinationWeather(21, "☀️", "Sunny", conditionRes = R.string.weather_cond_sunny),
        "OPO" to DestinationWeather(19, "🌤️", "Mild with breeze", conditionRes = R.string.weather_cond_mild_breeze),
        "MIL" to DestinationWeather(19, "⛅", "Clear", conditionRes = R.string.weather_cond_clear),
        "MXP" to DestinationWeather(19, "⛅", "Clear", conditionRes = R.string.weather_cond_clear),
        "VCE" to DestinationWeather(18, "🌤️", "Pleasant", conditionRes = R.string.weather_cond_pleasant),
        "ATH" to DestinationWeather(23, "☀️", "Mediterranean sun", conditionRes = R.string.weather_cond_mediterranean_sun),
        "DUB" to DestinationWeather(13, "🌧️", "Cool & rainy", conditionRes = R.string.weather_cond_cool_rain),
        "VIE" to DestinationWeather(16, "⛅", "Mild", conditionRes = R.string.weather_cond_mild),
        "PRG" to DestinationWeather(15, "🌥️", "Cool & dry", conditionRes = R.string.weather_cond_cool_dry),
        "BRU" to DestinationWeather(15, "☁️", "Cloudy", conditionRes = R.string.weather_cond_cloudy),
        "ZRH" to DestinationWeather(14, "🌤️", "Cool & dry", conditionRes = R.string.weather_cond_cool_dry),
        "GVA" to DestinationWeather(15, "🌤️", "Clear", conditionRes = R.string.weather_cond_clear),
        "TFN" to DestinationWeather(24, "☀️", "Sunny & mild", conditionRes = R.string.weather_cond_sunny_mild),
        "TFS" to DestinationWeather(25, "☀️", "Warm & sunny", conditionRes = R.string.weather_cond_warm_sunny),
        "LPA" to DestinationWeather(24, "☀️", "Sunny & mild", conditionRes = R.string.weather_cond_sunny_mild),
        "MAH" to DestinationWeather(23, "☀️", "Sunny", conditionRes = R.string.weather_cond_sunny),
        "FUE" to DestinationWeather(25, "💨", "Sunny & breezy", conditionRes = R.string.weather_cond_sunny_breeze)
    )

    fun getSimulatedWeather(hubCode: String): DestinationWeather {
        return simulatedWeatherMap[hubCode.uppercase()]
            ?: DestinationWeather(
                tempC = 20 + (kotlin.math.abs(hubCode.hashCode()) % 6),
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
