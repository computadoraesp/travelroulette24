package com.example.travelroulette24.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * Travel Intelligence Engine for TravelRoulette24.
 *
 * Deterministic, pure function of its inputs:
 * - Validates required input fields (FAILURE RULE).
 * - Filters real-time options by travel mode rules:
 *     - ONE-WAY: returnTime must be null.
 *     - ROUND TRIP: (returnTime - departureTime) must be within
 *       [stayDurationHours.min, stayDurationHours. Max].
 * - Departs within the next `departureWindowHours` from `generatedAt`.
 * - Computes `isOpportunity` per the OPPORTUNITY RULE:
 *     price <= min12Months OR price <= min30Days (only using backend-provided
 *     historical records; never inventing data).
 * - Sorts by price ascending (ties broken deterministically by
 *   departureTime, then originHub, then destinationHub, then deepLink) and
 *   caps at 20 results.
 * - Emits the strict OUTPUT FORMAT JSON, or { "error": "Missing required fields" }.
 *
 * Travel chaining is supported by construction: for one_way mode, the caller
 * can re-invoke [generate] with the destination city as the new origin once
 * the user arrives.
 */
object TravelIntelligenceEngine {

    const val MAX_RESULTS = 20
    const val ERROR_MISSING_FIELDS = "Missing required fields"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // ---------------- Input / Output models (strict schemas) ----------------

    @Serializable
    data class TransportationHubs(
        val airports: List<String> = emptyList(),
        val trainStations: List<String> = emptyList(),
        val ports: List<String> = emptyList()
    )

    @Serializable
    data class StayDurationHours(
        val min: Int,
        val max: Int
    )

    @Serializable
    data class HistoricalPrice(
        val originHub: String,
        val destinationHub: String,
        @SerialName("min12Months") val min12Months: Double,
        @SerialName("min30Days") val min30Days: Double,
        val average: Double
    )

    @Serializable
    data class Budget(
        val maxPerTrip: Double? = null,
        val maxPerRoundTrip: Double? = null,
        val maxChainStep: Double? = null,
        val enabled: Boolean = false
    )

    @Serializable
    data class RealTimeOption(
        val originHub: String,
        val destinationHub: String,
        val price: Double,
        val departureTime: String,
        val returnTime: String? = null,
        val checkInOnline: Boolean,
        val deepLink: String,
        val mode: String // "flight" | "train" | "ferry"
    )

    @Serializable
    data class EngineInput(
        val city: String? = null,
        val transportationHubs: TransportationHubs? = null,
        val mode: String? = null, // "round_trip" | "one_way"
        val passengers: Int? = null,
        val stayDurationHours: StayDurationHours? = null,
        val departureWindowHours: Int? = null,
        val budget: Budget? = null,
        val historicalPriceData: List<HistoricalPrice> = emptyList(),
        val realTimeOptions: List<RealTimeOption> = emptyList()
    )

    @Serializable
    data class ResultItem(
        val mode: String,
        val originHub: String,
        val destinationHub: String,
        val price: Double,
        val departureTime: String,
        val returnTime: String?,
        val checkInOnline: Boolean,
        val isOpportunity: Boolean,
        val isBudgetFriendly: Boolean,
        val deepLink: String
    )

    @Serializable
    data class EngineOutput(
        val originCity: String,
        val generatedAt: String,
        val results: List<ResultItem>
    )

    @Serializable
    data class ErrorOutput(
        val error: String
    )

    // ---------------- Public API ----------------

    /**
     * Generates the deterministic Top 20 JSON output from the raw input JSON.
     * Returns the failure JSON when required fields are missing.
     *
     * @param nowUtc the current instant (injected for testability/determinism).
     */
    fun generate(inputJson: String, nowUtc: Instant = Instant.now()): String {
        val input = try {
            json.decodeFromString<EngineInput>(inputJson)
        } catch (_: Exception) {
            return json.encodeToString(ErrorOutput.serializer(), ErrorOutput(ERROR_MISSING_FIELDS))
        }

        if (!hasRequiredFields(input)) {
            return json.encodeToString(ErrorOutput.serializer(), ErrorOutput(ERROR_MISSING_FIELDS))
        }

        val city = input.city!!
        val mode = input.mode!!
        val windowEnd = nowUtc.plusSeconds(input.departureWindowHours!! * 3600L)

        val historicalByRoute = input.historicalPriceData.associateBy { it.originHub to it.destinationHub }

        val filtered = input.realTimeOptions.asSequence()
            .filter { it.mode in setOf("flight", "train", "ferry") }
            .filter { it.price >= 0.0 }
            .mapNotNull { opt ->
                val departure = parseInstant(opt.departureTime) ?: return@mapNotNull null
                // Departure must be within the window [now, now + departureWindowHours].
                if (departure.isBefore(nowUtc) || departure.isAfter(windowEnd)) return@mapNotNull null

                val returnTime: String? = when (mode) {
                    "one_way" -> {
                        if (opt.returnTime != null) return@mapNotNull null // ONE-WAY RULE
                        null
                    }
                    "round_trip" -> {
                        val ret = opt.returnTime ?: return@mapNotNull null
                        val retInstant = parseInstant(ret) ?: return@mapNotNull null
                        val stayHours = Duration.between(departure, retInstant).toHours().toDouble()
                        val stay = input.stayDurationHours!!
                        if (stayHours < stay.min || stayHours > stay.max) return@mapNotNull null
                        ret
                    }
                    else -> return@mapNotNull null
                }

                // OPPORTUNITY RULE — only backend-provided historical data is consulted.
                val hist = historicalByRoute[opt.originHub to opt.destinationHub]
                val isOpportunity = hist != null &&
                    (opt.price <= hist.min12Months || opt.price <= hist.min30Days)

                // BUDGET RULE
                val budget = input.budget
                val isBudgetFriendly = when {
                    budget == null -> true
                    mode == "round_trip" -> {
                        val limit = budget.maxPerRoundTrip ?: budget.maxPerTrip
                        limit == null || opt.price <= limit
                    }
                    else -> {
                        val limit = budget.maxPerTrip ?: budget.maxChainStep
                        limit == null || opt.price <= limit
                    }
                }

                // STRICT RULE 13: Filter out options exceeding budget if enabled.
                if (budget?.enabled == true && !isBudgetFriendly) return@mapNotNull null

                ResultItem(
                    mode = opt.mode,
                    originHub = opt.originHub,
                    destinationHub = opt.destinationHub,
                    price = opt.price,
                    departureTime = opt.departureTime,
                    returnTime = returnTime,
                    checkInOnline = opt.checkInOnline,
                    isOpportunity = isOpportunity,
                    isBudgetFriendly = isBudgetFriendly,
                    deepLink = opt.deepLink
                )
            }
            .toList()
            // Deterministic ordering: price, then departureTime, then hubs, then deepLink.
            .sortedWith(
                compareBy({ it.price }, { it.departureTime }, { it.originHub }, { it.destinationHub }, { it.deepLink })
            )
            .take(MAX_RESULTS) // RULE 7: exactly 20 unless fewer exist.

        val output = EngineOutput(
            originCity = city,
            generatedAt = nowUtc.toString(),
            results = filtered
        )
        return json.encodeToString(EngineOutput.serializer(), output)
    }

    // ---------------- Helpers ----------------

    private fun hasRequiredFields(input: EngineInput): Boolean {
        if (input.city.isNullOrBlank()) return false
        if (input.transportationHubs == null) return false
        if (input.mode != "round_trip" && input.mode != "one_way") return false
        if (input.passengers == null || input.passengers <= 0) return false
        if (input.mode == "round_trip") {
            val stay = input.stayDurationHours ?: return false
            if (stay.min < 0 || stay.max < stay.min) return false
        }
        if (input.departureWindowHours == null || input.departureWindowHours <= 0) return false
        return true
    }

    private fun parseInstant(iso: String): Instant? = try {
        Instant.parse(iso)
    } catch (_: DateTimeParseException) {
        null
    }
}