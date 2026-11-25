package com.example.travelroulette24.utils

import com.example.travelroulette24.budget.BudgetGuardian
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DealNotificationDispatcherTest {

    private fun offer(
        dest: String,
        price: Double,
        isOpportunity: Boolean = false
    ) = TravelIntelligenceEngine.ResultItem(
        mode = "flight",
        originHub = "MAD",
        destinationHub = dest,
        price = price,
        departureTime = "2026-06-16T10:00:00Z",
        returnTime = null,
        checkInOnline = true,
        isOpportunity = isOpportunity,
        isBudgetFriendly = price <= 80.0,
        deepLink = "https://example.com"
    )

    @Test
    fun `findLatentWishlistDeals triggers when wishlist destination fits active budget`() {
        val lisCheap = offer("LIS", price = 45.0)
        val bcnExpensive = offer("BCN", price = 140.0)
        val cdgNotWishlist = offer("CDG", price = 25.0, isOpportunity = true)

        val budget = BudgetGuardian.Budget(
            maxPerTrip = 80.0,
            maxPerRoundTrip = 144.0,
            maxChainStep = 80.0,
            enabled = true
        )

        val matches = DealNotificationDispatcher.findLatentWishlistDeals(
            results = listOf(lisCheap, bcnExpensive, cdgNotWishlist),
            favoriteDestinations = listOf("LIS", "BCN"),
            budget = budget
        )

        assertEquals(listOf(lisCheap), matches)
    }

    @Test
    fun `findLatentWishlistDeals triggers when offer beats historical acceptance even above budget`() {
        val romaHistDeal = offer("FCO", price = 95.0, isOpportunity = false)
        val budget = BudgetGuardian.Budget(
            maxPerTrip = 70.0,
            maxPerRoundTrip = 126.0,
            maxChainStep = 70.0,
            enabled = true
        )

        val matches = DealNotificationDispatcher.findLatentWishlistDeals(
            results = listOf(romaHistDeal),
            favoriteDestinations = listOf("FCO"),
            budget = budget,
            historicalLowByHub = mapOf("FCO" to 90.0),
            tolerancePercent = 15 // maxAcceptable = 103.50
        )

        assertTrue(matches.contains(romaHistDeal))
    }

    @Test
    fun `findLatentWishlistDeals without budget triggers on historical opportunity and ignores overpriced non-deals`() {
        val fcoOpportunity = offer("FCO", price = 120.0, isOpportunity = true)
        val bcnOverpriced = offer("BCN", price = 220.0, isOpportunity = false)

        val matches = DealNotificationDispatcher.findLatentWishlistDeals(
            results = listOf(fcoOpportunity, bcnOverpriced),
            favoriteDestinations = listOf("FCO", "BCN"),
            budget = null,
            historicalLowByHub = mapOf("FCO" to 80.0, "BCN" to 70.0),
            tolerancePercent = 15
        )

        assertEquals(listOf(fcoOpportunity), matches)
    }
}
