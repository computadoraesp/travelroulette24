package com.example.travelroulette24.data.byok

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

class ApiCredentialsManagerTest {

    private val context = Mockito.mock(Context::class.java)
    private val prefs = Mockito.mock(SharedPreferences::class.java)
    private val editor = Mockito.mock(SharedPreferences.Editor::class.java)

    private val inMemoryStore = mutableMapOf<String, Any?>()

    @Before
    fun setup() {
        whenever(context.getSharedPreferences(eq("byok_credentials_prefs"), eq(Context.MODE_PRIVATE)))
            .thenReturn(prefs)
        whenever(prefs.edit()).thenReturn(editor)
        whenever(editor.putString(any(), any())).thenAnswer { invocation ->
            val key = invocation.getArgument<String>(0)
            val value = invocation.getArgument<String>(1)
            inMemoryStore[key] = value
            editor
        }
        whenever(editor.putBoolean(any(), any())).thenAnswer { invocation ->
            val key = invocation.getArgument<String>(0)
            val value = invocation.getArgument<Boolean>(1)
            inMemoryStore[key] = value
            editor
        }
        whenever(prefs.getString(any(), any())).thenAnswer { invocation ->
            val key = invocation.getArgument<String>(0)
            val defaultVal = invocation.getArgument<String?>(1)
            inMemoryStore[key] as? String ?: defaultVal
        }
        whenever(prefs.getBoolean(any(), any())).thenAnswer { invocation ->
            val key = invocation.getArgument<String>(0)
            val defaultVal = invocation.getArgument<Boolean>(1)
            inMemoryStore[key] as? Boolean ?: defaultVal
        }
        whenever(editor.remove(any())).thenAnswer { invocation ->
            val key = invocation.getArgument<String>(0)
            inMemoryStore.remove(key)
            editor
        }
        whenever(editor.putInt(any(), any())).thenAnswer { invocation ->
            val key = invocation.getArgument<String>(0)
            val value = invocation.getArgument<Int>(1)
            inMemoryStore[key] = value
            editor
        }
        whenever(prefs.getInt(any(), any())).thenAnswer { invocation ->
            val key = invocation.getArgument<String>(0)
            val defaultVal = invocation.getArgument<Int>(1)
            inMemoryStore[key] as? Int ?: defaultVal
        }
    }

    @Test
    fun testCredentialsManager_emptyByDefault() {
        val manager = ApiCredentialsManager(context)
        assertFalse(manager.hasAnyFlightKey())
        assertFalse(manager.hasNavitia())
        assertFalse(manager.hasOpenTripMap())
    }

    @Test
    fun testCredentialsManager_amadeusKeys() {
        val manager = ApiCredentialsManager(context)
        manager.amadeusApiKey = "test_amadeus_key"
        manager.amadeusApiSecret = "test_amadeus_secret"

        assertEquals("test_amadeus_key", manager.amadeusApiKey)
        assertEquals("test_amadeus_secret", manager.amadeusApiSecret)
        assertTrue(manager.hasAnyFlightKey())
    }

    @Test
    fun testCredentialsManager_kiwiKey() {
        val manager = ApiCredentialsManager(context)
        manager.kiwiApiKey = "kiwi_test_token"

        assertEquals("kiwi_test_token", manager.kiwiApiKey)
        assertTrue(manager.hasAnyFlightKey())
    }

    @Test
    fun testCredentialsManager_navitiaAndOpenTripMap() {
        val manager = ApiCredentialsManager(context)
        manager.navitiaToken = "navitia_token_123"
        manager.openTripMapKey = "opentripmap_abc"

        assertTrue(manager.hasNavitia())
        assertTrue(manager.hasOpenTripMap())
    }

    @Test
    fun testCredentialsManager_preferencesAndClearAll() {
        val manager = ApiCredentialsManager(context)
        manager.currency = "USD"
        manager.searchRadiusKm = 150
        manager.amadeusApiKey = "key123"
        manager.amadeusApiSecret = "secret123"

        assertEquals("USD", manager.currency)
        assertEquals(150, manager.searchRadiusKm)
        assertTrue(manager.hasAnyFlightKey())

        manager.clearAllKeys()
        assertFalse(manager.hasAnyFlightKey())
        assertEquals("", manager.amadeusApiKey)
        assertEquals("", manager.amadeusApiSecret)
    }

    @Test
    fun testCredentialsManager_customApiAndPaidTiers() {
        val manager = ApiCredentialsManager(context)
        manager.customFlightApiUrl = "https://my-flights.internal/api/v1"
        manager.customFlightApiKey = "bearer_secret_abc"
        manager.customFlightApiHeader = "X-Internal-Key"
        manager.customFlightIsPaidTier = true
        manager.customFlightDailyLimit = 100
        manager.amadeusIsPaidTier = true
        manager.geminiApiKey = "AIzaSyTest123"

        assertTrue(manager.hasCustomApi())
        assertTrue(manager.hasAnyFlightKey())
        assertTrue(manager.hasGemini())
        assertEquals("https://my-flights.internal/api/v1", manager.customFlightApiUrl)
        assertEquals("X-Internal-Key", manager.customFlightApiHeader)
        assertTrue(manager.customFlightIsPaidTier)
        assertEquals(100, manager.customFlightDailyLimit)
        assertTrue(manager.amadeusIsPaidTier)
    }

    @Test
    fun testCredentialsManager_trainAndBoatCustomApis() {
        val manager = ApiCredentialsManager(context)
        manager.customTrainApiUrl = "https://api.trains.com/trips"
        manager.customTrainApiKey = "key_train_123"
        manager.customTrainProviderName = "Renfe Proxy"
        manager.customTrainApiEnabled = true

        assertTrue(manager.hasCustomTrain())
        assertTrue(manager.hasAnyTrainKey())
        assertEquals("https://api.trains.com/trips", manager.customTrainApiUrl)
        assertEquals("Renfe Proxy", manager.customTrainProviderName)

        manager.customBoatApiUrl = "https://api.ferries.com/routes"
        manager.customBoatApiKey = "key_boat_456"
        manager.customBoatProviderName = "Baleària API"
        manager.customBoatApiEnabled = true

        assertTrue(manager.hasCustomBoat())
        assertTrue(manager.hasAnyBoatKey())
        assertEquals("https://api.ferries.com/routes", manager.customBoatApiUrl)
        assertEquals("Baleària API", manager.customBoatProviderName)
        assertTrue(manager.hasCustomApi())
    }

    @Test
    fun testCredentialsManager_notesTripHubAndExtendedByokFlags() {
        val manager = ApiCredentialsManager(context)
        assertFalse(manager.hasUnsplash())
        assertFalse(manager.hasAviationStack())
        assertFalse(manager.hasRapidApi())
        assertFalse(manager.hasTripHub())

        manager.unsplashKey = "unsplash_key_123"
        manager.aviationStackKey = "aviation_key_456"
        manager.rapidApiKey = "rapid_key_789"
        manager.tripHubUrl = "https://hub.example.org/"
        manager.customFlightNotes = "Partner EU endpoint"

        assertTrue(manager.hasUnsplash())
        assertTrue(manager.hasAviationStack())
        assertTrue(manager.hasRapidApi())
        assertTrue(manager.hasTripHub())
        assertEquals("https://hub.example.org", manager.tripHubUrl)
        assertEquals("Partner EU endpoint", manager.customFlightNotes)

        manager.clearAllKeys()
        assertFalse(manager.hasUnsplash())
        assertFalse(manager.hasAviationStack())
        assertFalse(manager.hasRapidApi())
        assertFalse(manager.hasTripHub())
        assertEquals("", manager.customFlightNotes)
    }
}
