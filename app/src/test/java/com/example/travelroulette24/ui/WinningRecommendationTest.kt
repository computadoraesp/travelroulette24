package com.example.travelroulette24.ui

import com.example.travelroulette24.engine.TravelIntelligenceEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WinningRecommendationTest {

    @Test
    fun testHubCoordinatesCalculationForWinningRecommendation() {
        val origin = "MAD"
        val destination = "BCN"
        val distance = HubCoordinates.calculateDistanceKm(origin, destination)
        assertTrue("Distance between Madrid and Barcelona should be approx 500km", distance in 450..550)
        assertEquals("Madrid", HubCoordinates.getCity(origin))
        assertEquals("Barcelona", HubCoordinates.getCity(destination))
    }

    @Test
    fun testWinningItemModelIntegrity() {
        val winningItem = TravelIntelligenceEngine.ResultItem(
            mode = "flight",
            originHub = "CDG",
            destinationHub = "FCO",
            price = 45.0,
            departureTime = "2026-09-25T08:30:00Z",
            returnTime = null,
            checkInOnline = true,
            isOpportunity = true,
            isBudgetFriendly = true,
            deepLink = "https://example.com/book/cdg-fco"
        )

        assertEquals("CDG", winningItem.originHub)
        assertEquals("FCO", winningItem.destinationHub)
        assertEquals(45.0, winningItem.price, 0.001)
        assertTrue(winningItem.isOpportunity)
        assertTrue(winningItem.checkInOnline)
        assertNotNull(winningItem.deepLink)
    }
}
