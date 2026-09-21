package com.example.travelroulette24.data

import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

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
    @GET("v1/hubs/{city}")
    suspend fun resolveHubs(
        @Path("city") city: String,
        @Query("apiKey") apiKey: String
    ): JsonElement

    /**
     * Fetches real-time travel options (flight/train/ferry) departing from
     * the given hubs within the configured departure window hours.
     */
    @POST("v1/options")
    suspend fun fetchRealTimeOptions(
        @Body request: RealTimeOptionsRequest,
        @Query("apiKey") apiKey: String
    ): JsonElement

    /**
     * Fetches historical price records (min12Months, min30Days, average)
     * for the given routes. Powers Opportunity Alerts.
     */
    @POST("v1/history")
    suspend fun fetchHistoricalPrices(
        @Body request: HistoricalPriceRequest,
        @Query("apiKey") apiKey: String
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