package com.example.travelroulette24.data

import kotlinx.serialization.json.JsonElement

/**
 * Backend API for TravelRoulette24.
 *
 * All transportation hubs, real-time prices, and historical price records
 * are provided exclusively by the backend — the app never invents data
 * (spec RULES 4-6). Booking is never performed in-app (RULE 9); deep-links
 * open the provider externally.
 */
interface BackendApi {

    /**
     * Resolves transportation hubs (airports, train stations, ports) for a city.
     * Implements spec RULE 1: the user never inputs hub names — the backend
     * resolver infers them from the detected city.
     */
    suspend fun resolveHubs(
        city: String,
        apiKey: String
    ): JsonElement

    /**
     * Fetches real-time travel options (flight/train/ferry) departing from
     * the given hubs within the configured departure window hours.
     */
    suspend fun fetchRealTimeOptions(
        request: RealTimeOptionsRequest,
        apiKey: String
    ): JsonElement

    /**
     * Fetches historical price records (min12Months, min30Days, average)
     * for the given routes. Powers Opportunity Alerts.
     */
    suspend fun fetchHistoricalPrices(
        request: HistoricalPriceRequest,
        apiKey: String
    ): JsonElement

    data class RealTimeOptionsRequest(
        val originHubs: List<String>,
        val departureWindowHours: Int,
        val passengers: Int,
        val modes: List<String> = listOf("flight", "train", "ferry")
    )

    data class HistoricalPriceRequest(
        val routes: List<Route>
    ) {
        data class Route(val originHub: String, val destinationHub: String)
    }
}