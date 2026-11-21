package com.example.travelroulette24.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoritePriceAcceptanceTest {

    @Test
    fun `evaluateAcceptance flags new record low, within tolerance, and above tolerance`() {
        // Record low: 30 EUR vs historical 35 EUR
        val recordLow = FavoritePriceHistoryDb.evaluateAcceptance(
            currentPrice = 30.0,
            lowestHistoricalPrice = 35.0,
            tolerancePercent = 15
        )
        assertTrue(recordLow.isNewRecordLow)
        assertTrue(recordLow.isWithinAcceptanceRange)
        assertEquals(40.25, recordLow.maxAcceptablePrice, 0.01)

        // Within +15% acceptance range: 39 EUR vs historical 35 EUR (max 40.25)
        val accepted = FavoritePriceHistoryDb.evaluateAcceptance(
            currentPrice = 39.0,
            lowestHistoricalPrice = 35.0,
            tolerancePercent = 15
        )
        assertFalse(accepted.isNewRecordLow)
        assertTrue(accepted.isWithinAcceptanceRange)

        // Above +15% acceptance range: 49 EUR vs historical 35 EUR (+40%)
        val above = FavoritePriceHistoryDb.evaluateAcceptance(
            currentPrice = 49.0,
            lowestHistoricalPrice = 35.0,
            tolerancePercent = 15
        )
        assertFalse(above.isNewRecordLow)
        assertFalse(above.isWithinAcceptanceRange)
        assertEquals(40.0, above.deltaPercent, 0.1)
    }
}
