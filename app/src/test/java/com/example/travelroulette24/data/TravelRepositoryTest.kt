package com.example.travelroulette24.data

import android.content.Context
import com.example.travelroulette24.budget.BudgetGuardian
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import com.example.travelroulette24.location.LocalLocationHubResolver
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito
import java.time.Instant

class TravelRepositoryTest {

    private val now = Instant.parse("2026-09-20T12:00:00Z")

    /** Fake BackendApi returning deterministic canned responses (no network). */
    private class FakeApi : BackendApi {
        var hubsRequested: String? = null
        var optionsRequest: BackendApi.RealTimeOptionsRequest? = null
        var historyRequest: BackendApi.HistoricalPriceRequest? = null

        override suspend fun resolveHubs(city: String, apiKey: String): kotlinx.serialization.json.JsonElement =
            Json.parseToJsonElement(
                """{"airports":["CDG"],"trainStations":["GARE-LYON"],"ports":["PORT-D"]}"""
            ).also { hubsRequested = city }

        override suspend fun fetchRealTimeOptions(
            request: BackendApi.RealTimeOptionsRequest,
            apiKey: String
        ): kotlinx.serialization.json.JsonElement =
            Json.parseToJsonElement(
                """
                [
                  {"originHub":"CDG","destinationHub":"BCN","price":18.0,
                   "departureTime":"2026-09-20T14:00:00Z","returnTime":null,
                   "checkInOnline":true,"deepLink":"https://book.example/1","mode":"flight"},
                  {"originHub":"GARE-LYON","destinationHub":"LYS","price":95.0,
                   "departureTime":"2026-09-20T15:00:00Z","returnTime":null,
                   "checkInOnline":false,"deepLink":"https://book.example/2","mode":"train"}
                ]
                """.trimIndent()
            ).also { optionsRequest = request }

        override suspend fun fetchHistoricalPrices(
            request: BackendApi.HistoricalPriceRequest,
            apiKey: String
        ): kotlinx.serialization.json.JsonElement =
            Json.parseToJsonElement(
                """
                [
                  {"originHub":"CDG","destinationHub":"BCN","min12Months":20.0,"min30Days":25.0,"average":60.0}
                ]
                """.trimIndent()
            ).also { historyRequest = request }
    }

    @Test
    fun `buildEngineInput assembles full pipeline from backend data`() = runTest {
        val api = FakeApi()
        val repo = TravelRepository(api, apiKey = "test-key")

        val input = repo.buildEngineInput(
            city = "Paris",
            mode = "one_way",
            passengers = 2,
            stayMinHours = 24,
            stayMaxHours = 72,
            departureWindowHours = 24
        )

        assertEquals("Paris", api.hubsRequested)
        assertEquals(2, api.optionsRequest!!.passengers)
        assertEquals(listOf("CDG", "GARE-LYON", "PORT-D"), api.optionsRequest!!.originHubs)
        // History requested only for routes present in options (CDG->BCN), deduplicated.
        assertEquals(2, api.historyRequest!!.routes.size)

        val obj = Json.parseToJsonElement(input).jsonObject
        assertEquals("Paris", obj["city"]!!.toString().trim('"'))
        assertTrue(obj.containsKey("transportationHubs"))
        assertTrue(obj.containsKey("historicalPriceData"))
        assertTrue(obj.containsKey("realTimeOptions"))
    }

    @Test
    fun `generateTop20 runs engine on assembled input`() = runTest {
        val repo = TravelRepository(FakeApi(), apiKey = "test-key")
        val input = repo.buildEngineInput("Paris", "one_way", 1, 24, 72, 24)
        val output = repo.generateTop20(input, now)
        val parsed = Json { ignoreUnknownKeys = true }
            .decodeFromString(TravelIntelligenceEngine.EngineOutput.serializer(), output)
        assertEquals("Paris", parsed.originCity)
        assertEquals(2, parsed.results.size)
        // CDG->BCN at 18.0 <= min12Months 20.0 -> opportunity
        val bcx = parsed.results.first { it.destinationHub == "BCN" }
        assertTrue(bcx.isOpportunity)
    }

    @Test
    fun `filterByBudget keeps only affordable results`() = runTest {
        val repo = TravelRepository(FakeApi(), apiKey = "test-key")
        val input = repo.buildEngineInput("Paris", "one_way", 1, 24, 72, 24)
        val output = repo.generateTop20(input, now)

        val within = repo.filterByBudget(output, BudgetGuardian.Budget(maxPerTrip = 25.0, enabled = true))
        assertEquals(listOf(18.0), within.map { it.price })

        val all = repo.filterByBudget(output, null)
        assertEquals(2, all.size)
    }

    @Test
    fun `buildEngineInputFromCoordinates assembles full pipeline via local on-device resolver`() = runTest {
        val api = FakeApi()
        val mockContext = Mockito.mock(Context::class.java)
        val resolver = LocalLocationHubResolver(mockContext)
        val repo = TravelRepository(api, apiKey = "test-key", localResolver = resolver)

        val input = repo.buildEngineInputFromCoordinates(
            latitude = 48.8566,
            longitude = 2.3522,
            mode = "one_way",
            passengers = 1,
            stayMinHours = 24,
            stayMaxHours = 72,
            departureWindowHours = 24
        )

        val obj = Json.parseToJsonElement(input).jsonObject
        assertEquals("Paris", obj["city"]!!.toString().trim('"'))
        assertTrue(obj.containsKey("transportationHubs"))
        assertTrue(obj.containsKey("realTimeOptions"))
    }
}