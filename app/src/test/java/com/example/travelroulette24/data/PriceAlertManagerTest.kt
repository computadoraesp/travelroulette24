package com.example.travelroulette24.data

import android.content.Context
import android.content.SharedPreferences
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

class PriceAlertManagerTest {

    private val context = Mockito.mock(Context::class.java)
    private val prefs = Mockito.mock(SharedPreferences::class.java)
    private val editor = Mockito.mock(SharedPreferences.Editor::class.java)
    private val store = mutableMapOf<String, String?>()

    @Before
    fun setup() {
        whenever(context.getSharedPreferences(eq("travel_roulette_price_alerts"), eq(Context.MODE_PRIVATE)))
            .thenReturn(prefs)
        whenever(prefs.edit()).thenReturn(editor)
        whenever(editor.putString(any(), anyOrNull())).thenAnswer { invocation ->
            store[invocation.getArgument(0)] = invocation.getArgument(1)
            editor
        }
        whenever(prefs.getString(any(), anyOrNull())).thenAnswer { invocation ->
            val key = invocation.getArgument<String>(0)
            val def = invocation.getArgument<String?>(1)
            store[key] ?: def
        }
    }

    private fun item(
        origin: String = "MAD",
        dest: String = "BCN",
        price: Double = 30.0,
        isOpportunity: Boolean = false
    ) = TravelIntelligenceEngine.ResultItem(
        mode = "flight",
        originHub = origin,
        destinationHub = dest,
        price = price,
        departureTime = "2026-05-01T10:00:00Z",
        returnTime = null,
        checkInOnline = true,
        isOpportunity = isOpportunity,
        isBudgetFriendly = true,
        deepLink = "https://example.com"
    )

    @Test
    fun `lowest price mode filters by max price and favorite filter`() {
        val manager = PriceAlertManager(context)
        val cheapBcn = item(origin = "MAD", dest = "BCN", price = 29.0)
        val priceyBcn = item(origin = "MAD", dest = "BCN", price = 60.0)
        val cheapPar = item(origin = "MAD", dest = "CDG", price = 25.0)

        val configAllFavs = PriceAlertManager.AlertConfig(
            isEnabled = true,
            alertMode = PriceAlertManager.AlertMode.LOWEST_PRICE,
            maxPriceEur = 35f,
            targetDestination = PriceAlertManager.TARGET_ALL_FAVORITES
        )
        val favs = listOf("BCN", "HND")

        assertTrue(manager.matchesAlert(cheapBcn, configAllFavs, favoriteDestinations = favs))
        assertFalse(manager.matchesAlert(priceyBcn, configAllFavs, favoriteDestinations = favs))
        assertFalse(manager.matchesAlert(cheapPar, configAllFavs, favoriteDestinations = favs))
    }

    @Test
    fun `best km per price mode matches long haul high ratio offers`() {
        val manager = PriceAlertManager(context)
        // MAD -> HND (~10,760 km) for 200 EUR => ~53.8 km/EUR
        val tokyoDeal = item(origin = "MAD", dest = "HND", price = 200.0)
        // MAD -> BCN (~505 km) for 50 EUR => ~10.1 km/EUR
        val bcnShort = item(origin = "MAD", dest = "BCN", price = 50.0)

        val ratioConfig = PriceAlertManager.AlertConfig(
            isEnabled = true,
            alertMode = PriceAlertManager.AlertMode.BEST_KM_PER_PRICE,
            maxPriceEur = 60f,
            minKmPerEur = 25f,
            enforceMaxPriceInValueMode = false
        )
        assertTrue(manager.matchesAlert(tokyoDeal, ratioConfig))
        assertFalse(manager.matchesAlert(bcnShort, ratioConfig))

        val cappedConfig = ratioConfig.copy(enforceMaxPriceInValueMode = true)
        assertFalse(manager.matchesAlert(tokyoDeal, cappedConfig))
    }

    @Test
    fun `legacy json config with removed FAVORITES mode coerces safely to LOWEST_PRICE`() {
        store["key_alert_config_json"] =
            """{"isEnabled":true,"alertMode":"FAVORITES","maxPriceEur":45.0,"targetDestination":"BCN"}"""
        val manager = PriceAlertManager(context)
        val loaded = manager.configFlow.value
        assertTrue(loaded.isEnabled)
        assertEquals(PriceAlertManager.AlertMode.LOWEST_PRICE, loaded.alertMode)
        assertEquals(45.0f, loaded.maxPriceEur, 0.01f)
        assertEquals("BCN", loaded.targetDestination)
    }

    @Test
    fun `radar matches specific desired destination or any destination cleanly`() {
        val manager = PriceAlertManager(context)
        val cheapBcn = item(origin = "MAD", dest = "BCN", price = 25.0)
        val cheapLis = item(origin = "MAD", dest = "LIS", price = 22.0)

        val specificConfig = PriceAlertManager.AlertConfig(
            isEnabled = true,
            alertMode = PriceAlertManager.AlertMode.LOWEST_PRICE,
            maxPriceEur = 40f,
            targetDestination = "BCN"
        )
        assertTrue(manager.matchesAlert(cheapBcn, specificConfig))
        assertFalse(manager.matchesAlert(cheapLis, specificConfig))

        val openConfig = specificConfig.copy(targetDestination = null)
        assertTrue(manager.matchesAlert(cheapBcn, openConfig))
        assertTrue(manager.matchesAlert(cheapLis, openConfig))
    }

    @Test
    fun `historical low mode matches engine opportunities and local recorded lows with optional cap`() {
        val manager = PriceAlertManager(context)
        val oppDeal = item(origin = "MAD", dest = "FCO", price = 79.0, isOpportunity = true)
        val normalDeal = item(origin = "MAD", dest = "LIS", price = 45.0, isOpportunity = false)
        val localRecordDeal = item(origin = "MAD", dest = "CDG", price = 38.0, isOpportunity = false)

        val histConfig = PriceAlertManager.AlertConfig(
            isEnabled = true,
            alertMode = PriceAlertManager.AlertMode.HISTORICAL_LOW,
            maxPriceEur = 60f,
            enforceMaxPriceInValueMode = false
        )

        assertTrue(manager.matchesAlert(oppDeal, histConfig))
        assertFalse(manager.matchesAlert(normalDeal, histConfig))
        assertTrue(
            manager.matchesAlert(
                localRecordDeal,
                histConfig,
                historicalLowByHub = mapOf("CDG" to 40.0)
            )
        )

        val cappedHistConfig = histConfig.copy(enforceMaxPriceInValueMode = true)
        assertFalse(manager.matchesAlert(oppDeal, cappedHistConfig))
        assertTrue(
            manager.matchesAlert(
                localRecordDeal,
                cappedHistConfig,
                historicalLowByHub = mapOf("CDG" to 40.0)
            )
        )
    }
}
