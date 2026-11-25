package com.example.travelroulette24.data

import com.example.travelroulette24.budget.BudgetGuardian
import com.example.travelroulette24.data.byok.ByokTravelService
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import com.example.travelroulette24.location.LocalLocationHubResolver
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Repository assembling the travel intelligence pipeline with strict separation
 * between Demo Mode (simulation via [DemoTravelDataSource]) and Real Mode
 * (live BYOK / Open-HAFAS / [BackendApi], never mixed with simulated offers).
 */
class TravelRepository(
    private val api: BackendApi? = null,
    private val apiKey: String = "",
    private val localResolver: LocalLocationHubResolver? = null,
    private val byokService: ByokTravelService? = null,
    private val priceHistoryDb: FavoritePriceHistoryDb? = null,
    private val isRealModeProvider: () -> Boolean = { byokService?.credentials?.isRealMode ?: true }
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    enum class DataSourceOrigin {
        DEMO_SIMULATION,
        REAL_LIVE,
        REAL_NO_DATA
    }

    var lastOrigin: DataSourceOrigin = DataSourceOrigin.DEMO_SIMULATION
        private set

    fun isCurrentlyRealMode(): Boolean = isRealModeProvider()

    /** Builds the full engine input from coordinates using configured search radius. */
    suspend fun buildEngineInputFromCoordinates(
        latitude: Double,
        longitude: Double,
        mode: String,
        passengers: Int,
        stayMinHours: Int,
        stayMaxHours: Int,
        departureWindowHours: Int,
        budget: BudgetGuardian.Budget? = null,
        allowedModes: Set<String> = setOf("flight", "train", "ferry")
    ): String {
        val radiusKm = byokService?.credentials?.searchRadiusKm?.toDouble() ?: 150.0
        val resolvedHubsMap = localResolver?.findNearestHubs(latitude, longitude, maxRadiusKm = radiusKm)
            ?: mapOf("airports" to listOf("CDG"), "trainStations" to listOf("GDL"), "ports" to listOf("Bercy"))

        val hubsJsonObj = buildJsonObject {
            put("airports", JsonArray(resolvedHubsMap["airports"].orEmpty().map { JsonPrimitive(it) }))
            put("trainStations", JsonArray(resolvedHubsMap["trainStations"].orEmpty().map { JsonPrimitive(it) }))
            put("ports", JsonArray(resolvedHubsMap["ports"].orEmpty().map { JsonPrimitive(it) }))
        }

        val resolvedCity = localResolver?.resolveCityFromCoordinates(latitude, longitude) ?: "Paris"
        val hubNames = resolvedHubsMap.values.flatten()

        return assembleEngineJson(
            city = resolvedCity,
            hubsJson = hubsJsonObj,
            hubNames = hubNames,
            mode = mode,
            passengers = passengers,
            stayMinHours = stayMinHours,
            stayMaxHours = stayMaxHours,
            departureWindowHours = departureWindowHours,
            budget = budget,
            allowedModes = allowedModes
        )
    }

    /** Fetches hubs, options, and history for a named city and assembles the engine input. */
    suspend fun buildEngineInput(
        city: String,
        mode: String,
        passengers: Int,
        stayMinHours: Int,
        stayMaxHours: Int,
        departureWindowHours: Int,
        budget: BudgetGuardian.Budget? = null,
        allowedModes: Set<String> = setOf("flight", "train", "ferry")
    ): String {
        val hubs = try {
            api?.resolveHubs(city, apiKey)?.jsonObject ?: buildJsonObject {
                put("airports", JsonArray(listOf("CDG", "ORY").map { JsonPrimitive(it) }))
                put("trainStations", JsonArray(listOf("GDL").map { JsonPrimitive(it) }))
                put("ports", JsonArray(listOf("Bercy").map { JsonPrimitive(it) }))
            }
        } catch (_: Exception) {
            buildJsonObject {
                put("airports", JsonArray(listOf("CDG", "ORY").map { JsonPrimitive(it) }))
                put("trainStations", JsonArray(listOf("GDL").map { JsonPrimitive(it) }))
                put("ports", JsonArray(listOf("Bercy").map { JsonPrimitive(it) }))
            }
        }

        val hubNames = extractHubs(hubs).ifEmpty { listOf("CDG", "GDL") }

        return assembleEngineJson(
            city = city,
            hubsJson = hubs,
            hubNames = hubNames,
            mode = mode,
            passengers = passengers,
            stayMinHours = stayMinHours,
            stayMaxHours = stayMaxHours,
            departureWindowHours = departureWindowHours,
            budget = budget,
            allowedModes = allowedModes
        )
    }

    /**
     * Unified pipeline shared by [buildEngineInputFromCoordinates] and [buildEngineInput].
     * Strictly routes Demo Mode to [DemoTravelDataSource] and Real Mode exclusively to live APIs.
     */
    private suspend fun assembleEngineJson(
        city: String,
        hubsJson: JsonObject,
        hubNames: List<String>,
        mode: String,
        passengers: Int,
        stayMinHours: Int,
        stayMaxHours: Int,
        departureWindowHours: Int,
        budget: BudgetGuardian.Budget?,
        allowedModes: Set<String> = setOf("flight", "train", "ferry")
    ): String {
        val validModes = setOf("flight", "train", "ferry")
        val normalizedAllowed = allowedModes
            .map { it.trim().lowercase() }
            .map { if (it == "boat") "ferry" else it }
            .filter { it in validModes }
            .toSet()
            .ifEmpty { validModes }
        val realMode = isRealModeProvider()

        val options: JsonArray
        val history: JsonArray

        if (!realMode) {
            // ⚪ STRICT DEMO MODE: isolated simulated data, zero network API consumption
            lastOrigin = DataSourceOrigin.DEMO_SIMULATION
            val rawDemo = DemoTravelDataSource.generateDemoOptions(
                originHubs = hubNames,
                mode = mode,
                stayMinHours = stayMinHours,
                stayMaxHours = stayMaxHours,
                departureWindowHours = departureWindowHours
            )
            options = if (normalizedAllowed.size == validModes.size) {
                rawDemo
            } else {
                JsonArray(
                    rawDemo.filter { el ->
                        val itemMode = el.jsonObject["mode"]?.jsonPrimitive?.content?.lowercase()
                        val mapped = if (itemMode == "boat") "ferry" else itemMode
                        mapped in normalizedAllowed
                    }
                )
            }
            val routes = extractRoutes(options)
            history = if (routes.isEmpty()) JsonArray(emptyList()) else DemoTravelDataSource.generateDemoHistory(routes)
        } else {
            // 🟢 STRICT REAL MODE: only live BYOK / Open-HAFAS / BackendApi, fetched in parallel
            val liveByokOffers = coroutineScope {
                val flightsDeferred = async {
                    if ("flight" in normalizedAllowed) {
                        byokService?.fetchFlightOffers(hubNames, mode, stayMinHours, stayMaxHours, departureWindowHours)?.toList().orEmpty()
                    } else emptyList()
                }
                val trainsDeferred = async {
                    if ("train" in normalizedAllowed) {
                        byokService?.fetchTrainOffers(hubNames, mode, stayMinHours, stayMaxHours, originCity = city)?.toList().orEmpty()
                    } else emptyList()
                }
                val boatsDeferred = async {
                    if ("ferry" in normalizedAllowed) {
                        byokService?.fetchBoatOffers(hubNames, mode, stayMinHours, stayMaxHours)?.toList().orEmpty()
                    } else emptyList()
                }
                flightsDeferred.await() + trainsDeferred.await() + boatsDeferred.await()
            }

            options = if (liveByokOffers.isNotEmpty()) {
                lastOrigin = DataSourceOrigin.REAL_LIVE
                JsonArray(liveByokOffers)
            } else {
                val backendOptions = try {
                    api?.fetchRealTimeOptions(
                        BackendApi.RealTimeOptionsRequest(
                            originHubs = hubNames,
                            departureWindowHours = departureWindowHours,
                            passengers = passengers
                        ),
                        apiKey
                    )?.jsonArray ?: JsonArray(emptyList())
                } catch (_: Exception) {
                    JsonArray(emptyList())
                }
                val filteredBackend = if (normalizedAllowed.size == validModes.size) {
                    backendOptions
                } else {
                    JsonArray(
                        backendOptions.filter { el ->
                            val itemMode = el.jsonObject["mode"]?.jsonPrimitive?.content?.lowercase()
                            val mapped = if (itemMode == "boat") "ferry" else itemMode
                            mapped in normalizedAllowed
                        }
                    )
                }
                lastOrigin = if (filteredBackend.isNotEmpty()) DataSourceOrigin.REAL_LIVE else DataSourceOrigin.REAL_NO_DATA
                filteredBackend
            }

            val routes = extractRoutes(options)
            val remoteHistory = if (routes.isEmpty()) {
                JsonArray(emptyList())
            } else {
                try {
                    api?.fetchHistoricalPrices(
                        BackendApi.HistoricalPriceRequest(
                            routes.map { BackendApi.HistoricalPriceRequest.Route(it.first, it.second) }
                        ),
                        apiKey
                    )?.jsonArray ?: JsonArray(emptyList())
                } catch (_: Exception) {
                    JsonArray(emptyList())
                }
            }
            history = if (remoteHistory.isNotEmpty() || routes.isEmpty() || priceHistoryDb == null) {
                remoteHistory
            } else {
                val localMap = priceHistoryDb.getAllHistory()
                JsonArray(
                    routes.distinct().mapNotNull { (origin, dest) ->
                        val rec = localMap[dest.trim().uppercase()] ?: return@mapNotNull null
                        if (rec.observations < 1 || rec.lowestPrice <= 0.0) return@mapNotNull null
                        buildJsonObject {
                            put("originHub", JsonPrimitive(origin))
                            put("destinationHub", JsonPrimitive(dest))
                            put("min12Months", JsonPrimitive(rec.lowestPrice))
                            put("min30Days", JsonPrimitive(rec.lowestPrice))
                            put("average", JsonPrimitive(rec.lastPrice))
                        }
                    }
                )
            }
        }

        return buildJsonObject {
            put("city", JsonPrimitive(city))
            put("transportationHubs", hubsJson)
            put("mode", JsonPrimitive(mode))
            put("passengers", JsonPrimitive(passengers))
            put(
                "stayDurationHours",
                buildJsonObject {
                    put("min", JsonPrimitive(stayMinHours))
                    put("max", JsonPrimitive(stayMaxHours))
                }
            )
            put("departureWindowHours", JsonPrimitive(departureWindowHours))
            if (budget != null) {
                put("budget", json.encodeToJsonElement(BudgetGuardian.Budget.serializer(), budget))
            }
            put("historicalPriceData", history)
            put("realTimeOptions", options)
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
            price = { it.totalPrice }
        )
    }

    /** Generates a budget-compliant opportunity alert for a single leg/option. */
    fun budgetAlertFor(
        result: TravelIntelligenceEngine.ResultItem,
        budget: BudgetGuardian.Budget?,
        currency: String = "€"
    ): String? = BudgetGuardian.maybeAlert(
        budget = budget,
        price = result.totalPrice,
        currency = currency,
        isOpportunity = result.isOpportunity,
        mode = result.mode,
        destinationHub = result.destinationHub,
        isRoundTrip = result.returnTime != null
    )

    private fun extractHubs(hubs: JsonObject): List<String> = buildList {
        for (key in listOf("airports", "trainStations", "ports")) {
            (hubs[key] as? JsonArray)?.forEach { hub ->
                add(hub.jsonPrimitive.content)
            }
        }
    }

    private fun extractRoutes(options: JsonArray): List<Pair<String, String>> =
        options.mapNotNull { el ->
            val obj = el.jsonObject
            val origin = obj["originHub"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val dest = obj["destinationHub"]?.jsonPrimitive?.content ?: return@mapNotNull null
            origin to dest
        }.distinct()
}