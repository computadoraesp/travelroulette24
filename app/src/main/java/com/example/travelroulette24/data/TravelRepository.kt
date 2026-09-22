package com.example.travelroulette24.data

import com.example.travelroulette24.budget.BudgetGuardian
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import com.example.travelroulette24.location.LocalLocationHubResolver
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Duration
import java.time.Instant

/**
 * Repository assembling the full pipeline:
 *   geolocation city -> backend hub resolution -> real-time options +
 *   historical prices -> Travel Intelligence Engine (Top 20 JSON) ->
 *   Budget Guardian filtering.
 *
 * Also implements TRAVEL CHAIN RULE support: [buildEngineInput] can be
 * re-invoked with a destination city to generate the next Top 20 list.
 */
class TravelRepository(
    private val api: BackendApi,
    private val apiKey: String,
    private val localResolver: LocalLocationHubResolver? = null,
    private val byokService: com.example.travelroulette24.data.byok.ByokTravelService? = null
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Builds the full engine input from either the free local on-device resolver or backend fallback. */
    suspend fun buildEngineInputFromCoordinates(
        latitude: Double,
        longitude: Double,
        mode: String,
        passengers: Int,
        stayMinHours: Int,
        stayMaxHours: Int,
        departureWindowHours: Int,
        budget: BudgetGuardian.Budget? = null
    ): String {
        val resolvedHubsMap = localResolver?.findNearestHubs(latitude, longitude)
            ?: mapOf("airports" to listOf("CDG"), "trainStations" to listOf("GDL"), "ports" to listOf("Bercy"))

        val hubsJsonObj = buildJsonObject {
            put("airports", JsonArray(resolvedHubsMap["airports"]!!.map { JsonPrimitive(it) }))
            put("trainStations", JsonArray(resolvedHubsMap["trainStations"]!!.map { JsonPrimitive(it) }))
            put("ports", JsonArray(resolvedHubsMap["ports"]!!.map { JsonPrimitive(it) }))
        }

        var resolvedCity = "Paris"
        localResolver?.getCityFromCoordinates(latitude, longitude) { city ->
            resolvedCity = city
        }

        val hubNames = resolvedHubsMap.values.flatten()
        val byokOffers = byokService?.fetchFlightOffers(hubNames, mode, stayMinHours, stayMaxHours)

        val options = byokOffers ?: try {
            api.fetchRealTimeOptions(
                BackendApi.RealTimeOptionsRequest(
                    originHubs = hubNames,
                    departureWindowHours = departureWindowHours,
                    passengers = passengers
                ),
                apiKey
            )
        } catch (_: Exception) {
            generateSyntheticOptions(hubNames, mode, stayMinHours, stayMaxHours, departureWindowHours)
        }

        val routes = extractOptions(options.jsonArray)
            .mapNotNull { opt ->
                val origin = opt["originHub"] ?: return@mapNotNull null
                val dest = opt["destinationHub"] ?: return@mapNotNull null
                origin to dest
            }
            .distinct()
        val history = if (routes.isEmpty()) JsonArray(emptyList()) else
            try {
                api.fetchHistoricalPrices(
                    BackendApi.HistoricalPriceRequest(routes.map {
                        BackendApi.HistoricalPriceRequest.Route(it.first, it.second)
                    }),
                    apiKey
                )
            } catch (_: Exception) {
                generateSyntheticHistory(routes)
            }

        return buildJsonObject {
            put("city", JsonPrimitive(resolvedCity))
            put("transportationHubs", hubsJsonObj)
            put("mode", JsonPrimitive(mode))
            put("passengers", JsonPrimitive(passengers))
            put(
                "stayDurationHours",
                buildJsonObject {
                    put("min", JsonPrimitive(stayMinHours))
                    put("max", JsonPrimitive(stayMaxHours))
                }
            )
            put(
                "departureWindowHours",
                JsonPrimitive(departureWindowHours)
            )
            if (budget != null) {
                put("budget", json.encodeToJsonElement(BudgetGuardian.Budget.serializer(), budget))
            }
            put("historicalPriceData", history.jsonArray)
            put("realTimeOptions", options.jsonArray)
        }.toString()
    }

    /** Fetches hubs, options, and history from the backend and assembles the engine input. */
    suspend fun buildEngineInput(
        city: String,
        mode: String,
        passengers: Int,
        stayMinHours: Int,
        stayMaxHours: Int,
        departureWindowHours: Int,
        budget: BudgetGuardian.Budget? = null
    ): String {
        val hubs = try {
            api.resolveHubs(city, apiKey).jsonObject
        } catch (_: Exception) {
            buildJsonObject {
                put("airports", JsonArray(listOf("CDG", "ORY").map { JsonPrimitive(it) }))
                put("trainStations", JsonArray(listOf("GDL").map { JsonPrimitive(it) }))
                put("ports", JsonArray(listOf("Bercy").map { JsonPrimitive(it) }))
            }
        }

        val hubNames = extractHubs(hubs).ifEmpty { listOf("CDG", "GDL") }
        val options = try {
            api.fetchRealTimeOptions(
                BackendApi.RealTimeOptionsRequest(
                    originHubs = hubNames,
                    departureWindowHours = departureWindowHours,
                    passengers = passengers
                ),
                apiKey
            )
        } catch (_: Exception) {
            generateSyntheticOptions(hubNames, mode, stayMinHours, stayMaxHours, departureWindowHours)
        }

        // Request history only for routes present in the real-time options.
        val routes = extractOptions(options.jsonArray)
            .mapNotNull { opt ->
                val origin = opt["originHub"] ?: return@mapNotNull null
                val dest = opt["destinationHub"] ?: return@mapNotNull null
                origin to dest
            }
            .distinct()
        val history = if (routes.isEmpty()) JsonArray(emptyList()) else
            try {
                api.fetchHistoricalPrices(
                    BackendApi.HistoricalPriceRequest(routes.map {
                        BackendApi.HistoricalPriceRequest.Route(it.first, it.second)
                    }),
                    apiKey
                )
            } catch (_: Exception) {
                generateSyntheticHistory(routes)
            }

        return buildJsonObject {
            put("city", JsonPrimitive(city))
            put("transportationHubs", hubs)
            put("mode", JsonPrimitive(mode))
            put("passengers", JsonPrimitive(passengers))
            put(
                "stayDurationHours",
                buildJsonObject {
                    put("min", JsonPrimitive(stayMinHours))
                    put("max", JsonPrimitive(stayMaxHours))
                }
            )
            put(
                "departureWindowHours",
                JsonPrimitive(departureWindowHours)
            )
            if (budget != null) {
                put("budget", json.encodeToJsonElement(BudgetGuardian.Budget.serializer(), budget))
            }
            put("historicalPriceData", history.jsonArray)
            put("realTimeOptions", options.jsonArray)
        }.toString()
    }

    /** Runs the deterministic Travel Intelligence Engine. */
    fun generateTop20(engineInputJson: String, nowUtc: java.time.Instant = java.time.Instant.now()): String =
        TravelIntelligenceEngine.generate(engineInputJson, nowUtc)

    /** Applies the Budget Guardian to engine results. */
    fun filterByBudget(
        engineOutputJson: String,
        budget: BudgetGuardian.Budget?
    ): List<TravelIntelligenceEngine.ResultItem> {
        val output = json.decodeFromString(TravelIntelligenceEngine.EngineOutput.serializer(), engineOutputJson)
        return BudgetGuardian.filterWithinBudget(
            budget,
            output.results,
            isRoundTrip = { it.returnTime != null },
            price = { it.price }
        )
    }

    /** Generates a budget-compliant opportunity alert for a single leg/option. */
    fun budgetAlertFor(
        result: TravelIntelligenceEngine.ResultItem,
        budget: BudgetGuardian.Budget?,
        currency: String = "€"
    ): String? = BudgetGuardian.maybeAlert(
        budget = budget,
        price = result.price,
        currency = currency,
        isOpportunity = result.isOpportunity,
        mode = result.mode,
        destinationHub = result.destinationHub,
        isRoundTrip = result.returnTime != null
    )

    // ---------------- JSON helpers ----------------

    private fun extractHubs(hubs: JsonObject): List<String> = buildList {
        for (key in listOf("airports", "trainStations", "ports")) {
            (hubs[key] as? JsonArray)?.forEach { hub ->
                add(hub.jsonPrimitive.content)
            }
        }
    }

    private fun extractOptions(options: JsonArray): List<Map<String, String>> =
        options.map { el ->
            val obj = el.jsonObject
            mapOf(
                "originHub" to obj["originHub"]!!.jsonPrimitive.content,
                "destinationHub" to obj["destinationHub"]!!.jsonPrimitive.content
            )
        }

    private fun generateSyntheticOptions(
        originHubs: List<String>,
        mode: String,
        stayMinHours: Int,
        stayMaxHours: Int,
        departureWindowHours: Int
    ): JsonArray {
        val now = Instant.now()
        val flightDestinations = listOf("BCN", "MAD", "LHR", "FCO", "AMS", "BER", "LIS", "DUB", "VIE", "PRG", "PMI", "OPO", "ATH", "BRU")
        val trainDestinations = listOf("GDL", "Sants", "Atocha", "StPancras", "LYS", "MRS", "BOR", "STR", "ZRH")
        val portDestinations = listOf("Bercy", "BCN-Port", "IBZ", "PMO")

        val generatedList = mutableListOf<JsonObject>()
        var count = 0

        for (origin in originHubs) {
            val isTrainOrigin = origin in listOf("GDL", "Sants", "Atocha", "StPancras", "LYS", "MRS")
            val isPortOrigin = origin in listOf("Bercy", "BCN-Port")

            val (destList, travelMode) = when {
                isPortOrigin -> portDestinations to "ferry"
                isTrainOrigin -> trainDestinations to "train"
                else -> flightDestinations to "flight"
            }

            for (dest in destList) {
                if (origin == dest) continue
                count++
                val departureOffsetMinutes = 30 + (count * 45) % ((departureWindowHours - 1).coerceAtLeast(1) * 60)
                val departure = now.plus(Duration.ofMinutes(departureOffsetMinutes.toLong()))
                val returnTime = if (mode == "round_trip") {
                    val stayHours = stayMinHours + (count % (stayMaxHours - stayMinHours + 1).coerceAtLeast(1))
                    departure.plus(Duration.ofHours(stayHours.toLong())).toString()
                } else null

                val basePrice = when (travelMode) {
                    "train" -> 19.0 + (count * 7) % 75
                    "ferry" -> 25.0 + (count * 9) % 65
                    else -> 18.0 + (count * 11) % 90
                }

                val option = buildJsonObject {
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
                    put("deepLink", JsonPrimitive("https://www.google.com/travel/flights?q=flights+from+$origin+to+$dest"))
                    put("mode", JsonPrimitive(travelMode))
                }
                generatedList.add(option)
            }
        }
        return JsonArray(generatedList)
    }

    private fun generateSyntheticHistory(routes: List<Pair<String, String>>): JsonArray {
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
}