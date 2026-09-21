package com.example.travelroulette24.engine

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TravelIntelligenceEngineTest {

    private val now = Instant.parse("2026-09-20T12:00:00Z")

    private fun input(
        mode: String = "one_way",
        options: String = "[]",
        history: String = "[]",
        city: String = "Paris",
        windowHours: Int = 24,
        stay: String = "\"stayDurationHours\":{\"min\":24,\"max\":72},"
    ): String = """
        {
          "city": "$city",
          "transportationHubs": {"airports":["CDG","ORY"],"trainStations":["GDL"],"ports":["Bercy"]},
          "mode": "$mode",
          "passengers": 1,
          $stay
          "departureWindowHours": $windowHours,
          "historicalPriceData": $history,
          "realTimeOptions": $options
        }
    """.trimIndent()

    private fun option(
        price: Double,
        departure: String = "2026-09-20T14:00:00Z",
        returnTime: String? = null,
        mode: String = "flight",
        origin: String = "CDG",
        dest: String = "BCN"
    ): String {
        val ret = if (returnTime == null) "null" else "\"$returnTime\""
        return """
        {"originHub":"$origin","destinationHub":"$dest","price":$price,
         "departureTime":"$departure","returnTime":$ret,
         "checkInOnline":true,"deepLink":"https://book.example/$origin-$dest-$price","mode":"$mode"}
        """.trimIndent()
    }

    private fun history(min12: Double, min30: Double, origin: String = "CDG", dest: String = "BCN"): String =
        """{"originHub":"$origin","destinationHub":"$dest","min12Months":$min12,"min30Days":$min30,"average":150.0}"""

    @Test
    fun `missing required fields returns error object`() {
        val bad = """
            {"city":"Paris","mode":"one_way"}
        """.trimIndent()
        assertEquals("""{"error":"Missing required fields"}""", TravelIntelligenceEngine.generate(bad, now))
    }

    @Test
    fun `invalid json returns error object`() {
        assertEquals("""{"error":"Missing required fields"}""", TravelIntelligenceEngine.generate("not json", now))
    }

    @Test
    fun `one way excludes options with return time`() {
        val opts = """
            [${option(50.0, returnTime = "2026-09-22T14:00:00Z")},
             ${option(80.0, returnTime = null)}]
        """.trimIndent()
        val out = TravelIntelligenceEngine.generate(input(mode = "one_way", options = opts), now)
        assertTrue(out.contains("\"price\":80.0"))
        assertFalse(out.contains("\"price\":50.0"))
    }

    @Test
    fun `one way output has null return time`() {
        val opts = "[${option(80.0)}]"
        val out = TravelIntelligenceEngine.generate(input(mode = "one_way", options = opts), now)
        assertTrue(out.contains("\"returnTime\":null"))
    }

    @Test
    fun `round trip respects stay duration window`() {
        // 48h stay is within [24,72]; 5h stay is not.
        val opts = """
            [${option(50.0, returnTime = "2026-09-22T14:00:00Z")},
             ${option(80.0, departure = "2026-09-20T14:00:00Z", returnTime = "2026-09-20T19:00:00Z")}]
        """.trimIndent()
        val out = TravelIntelligenceEngine.generate(input(mode = "round_trip", options = opts), now)
        assertTrue(out.contains("\"price\":50.0"))
        assertFalse(out.contains("\"price\":80.0"))
    }

    @Test
    fun `options outside departure window are excluded`() {
        val opts = """
            [${option(50.0, departure = "2026-09-25T14:00:00Z")},
             ${option(80.0)}]
        """.trimIndent()
        val out = TravelIntelligenceEngine.generate(input(mode = "one_way", options = opts), now)
        assertTrue(out.contains("\"price\":80.0"))
        assertFalse(out.contains("\"price\":50.0"))
    }

    @Test
    fun `opportunity rule uses min12Months or min30Days`() {
        val opts = "[${option(99.0)}, ${option(120.0, origin = "ORY", dest = "MAD")}]"
        val hist = """
            [${history(100.0, 150.0)},
             {"originHub":"ORY","destinationHub":"MAD","min12Months":110.0,"min30Days":115.0,"average":180.0}]
        """.trimIndent()
        val out = TravelIntelligenceEngine.generate(input(mode = "one_way", options = opts, history = hist), now)
        val parsed = Json { ignoreUnknownKeys = true }.decodeFromString(TravelIntelligenceEngine.EngineOutput.serializer(), out)
        assertEquals(2, parsed.results.size)
        // 99.0 <= 100.0 (min12Months) -> opportunity
        // 120.0 > 200.0 and > 110.0 -> not opportunity
        val byPrice = parsed.results.associateBy { it.price }
        assertTrue(byPrice.getValue(99.0).isOpportunity)
        assertFalse(byPrice.getValue(120.0).isOpportunity)
    }

    @Test
    fun `no historical record means not opportunity`() {
        val opts = "[${option(1.0)}]"
        val out = TravelIntelligenceEngine.generate(input(mode = "one_way", options = opts), now)
        assertTrue(out.contains("\"isOpportunity\":false"))
    }

    @Test
    fun `results are capped at 20`() {
        val opts = (1..25).joinToString(",") { i -> option(i.toDouble(), departure = "2026-09-20T14:00:0${i % 10}Z") }
        val out = TravelIntelligenceEngine.generate(input(mode = "one_way", options = "[$opts]"), now)
        assertEquals(20, Regex("\"deepLink\"").findAll(out).count())
    }

    @Test
    fun `results are sorted by price ascending`() {
        val opts = "[${option(90.0)}, ${option(10.0)}, ${option(50.0)}]"
        val out = TravelIntelligenceEngine.generate(input(mode = "one_way", options = opts), now)
        val prices = listOf(10.0, 50.0, 90.0)
        var lastIndex = -1
        for (p in prices) {
            val i = out.indexOf("\"price\":$p")
            assertTrue(i > lastIndex)
            lastIndex = i
        }
    }

    @Test
    fun `output includes required fields`() {
        val opts = "[${option(80.0)}]"
        val out = TravelIntelligenceEngine.generate(input(mode = "one_way", options = opts), now)
        assertTrue(out.contains("\"originCity\":\"Paris\""))
        assertTrue(out.contains("\"generatedAt\":\"2026-09-20T12:00:00Z\""))
        assertTrue(out.contains("\"mode\":\"flight\""))
        assertTrue(out.contains("\"checkInOnline\":true"))
        assertTrue(out.contains("\"deepLink\":"))
    }
}