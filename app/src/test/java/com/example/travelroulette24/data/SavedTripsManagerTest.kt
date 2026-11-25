package com.example.travelroulette24.data

import com.example.travelroulette24.engine.TravelIntelligenceEngine
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class SavedTripsManagerTest {

    private fun trip(dest: String, departureIso: String) = TravelIntelligenceEngine.ResultItem(
        mode = "flight",
        originHub = "MAD",
        destinationHub = dest,
        price = 39.0,
        departureTime = departureIso,
        returnTime = null,
        checkInOnline = true,
        isOpportunity = false,
        isBudgetFriendly = true,
        deepLink = "https://example.com"
    )

    @Test
    fun `filterActive24hTrips retains upcoming trips and removes departed trips`() {
        val now = Instant.parse("2026-06-15T12:00:00Z")
        val expiredTrip = trip("BCN", "2026-06-15T11:30:00Z")
        val exactNowTrip = trip("LIS", "2026-06-15T12:00:00Z")
        val upcomingTrip = trip("CDG", "2026-06-16T08:00:00Z")

        val active = SavedTripsManager.filterActive24hTrips(
            trips = listOf(expiredTrip, exactNowTrip, upcomingTrip),
            nowUtc = now
        )

        assertEquals(2, active.size)
        assertEquals(listOf("LIS", "CDG"), active.map { it.destinationHub })
    }
}
