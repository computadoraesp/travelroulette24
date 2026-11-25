package com.example.travelroulette24.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatAndCurrencyUtilsTest {

    @Test
    fun `currency helper returns expected symbols and fallback rates`() {
        assertEquals("€", CurrencyHelper.symbolFor("EUR"))
        assertEquals("$", CurrencyHelper.symbolFor("USD"))
        assertEquals("£", CurrencyHelper.symbolFor("GBP"))
        assertEquals("€", CurrencyHelper.symbolFor("unknown"))

        assertEquals(1.0, CurrencyHelper.fallbackRateFromEur("EUR"), 0.0001)
        assertEquals(1.09, CurrencyHelper.fallbackRateFromEur("USD"), 0.0001)
        assertEquals(0.85, CurrencyHelper.fallbackRateFromEur("GBP"), 0.0001)
    }

    @Test
    fun `currency helper converts and formats amounts accurately`() {
        assertEquals("$109", CurrencyHelper.formatPrice(100.0, "USD", 1.09))
        assertEquals("£85", CurrencyHelper.formatPrice(100.0, "GBP", 0.85))
        assertEquals("€50", CurrencyHelper.formatPrice(50.0, "EUR", 1.0))
        // Minimum coerced display price is 1
        assertEquals(1, CurrencyHelper.convertAmount(0.1, 1.0))

        // Bidirectional conversion back to EUR canonical base
        assertEquals(100.0, CurrencyHelper.toEur(109.0, 1.09), 0.01)
        assertEquals(100.0, CurrencyHelper.toEur(85.0, 0.85), 0.01)
        // km per display currency unit inversely scales by exchange rate
        assertEquals(10, CurrencyHelper.kmPerDisplayCurrency(10.9, 1.09))
        assertEquals(20, CurrencyHelper.kmPerDisplayCurrency(17.0, 0.85))
    }

    @Test
    fun `travel format utils maps modes and handles ISO timestamps gracefully`() {
        assertEquals("✈️", TravelFormatUtils.modeEmoji("flight"))
        assertEquals("🚆", TravelFormatUtils.modeEmoji("TRAIN"))
        assertEquals("⛴️", TravelFormatUtils.modeEmoji("ferry"))
        assertEquals("⛴️", TravelFormatUtils.modeEmoji("boat"))

        val formatted = TravelFormatUtils.formatIsoDate("2026-09-25T10:15:00Z", compact = false)
        assertTrue(formatted.contains(":"))

        val compact = TravelFormatUtils.formatIsoDate("2026-09-25T10:15:00Z", compact = true)
        assertTrue(compact.contains("•"))

        // Non-ISO fallback returns raw string instead of crashing
        assertEquals("Tomorrow morning", TravelFormatUtils.formatIsoDate("Tomorrow morning"))
    }
}
