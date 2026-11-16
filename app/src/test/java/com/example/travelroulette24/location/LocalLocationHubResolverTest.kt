package com.example.travelroulette24.location

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class LocalLocationHubResolverTest {

    @Test
    fun `findNearestHubs returns correct hubs within radius for Paris coordinates`() {
        val mockContext = mock(Context::class.java)
        val resolver = LocalLocationHubResolver(mockContext)

        // Paris Coordinates: 48.8566, 2.3522
        val hubs = resolver.findNearestHubs(48.8566, 2.3522)

        assertTrue(hubs["airports"]!!.contains("CDG"))
        assertTrue(hubs["airports"]!!.contains("ORY"))
        assertTrue(hubs["trainStations"]!!.contains("GDL"))
        assertTrue(hubs["ports"]!!.contains("Bercy"))
    }

    @Test
    fun `findNearestHubs returns Barcelona hubs when coordinates match Barcelona`() {
        val mockContext = mock(Context::class.java)
        val resolver = LocalLocationHubResolver(mockContext)

        // Barcelona Coordinates: 41.3851, 2.1734
        val hubs = resolver.findNearestHubs(41.3851, 2.1734)

        assertTrue(hubs["airports"]!!.contains("BCN"))
        assertTrue(hubs["trainStations"]!!.contains("Sants"))
        assertTrue(hubs["ports"]!!.contains("BCN-Port"))
    }

    @Test
    fun `findNearestHubs returns Rome hubs and city fallback without defaulting to Paris`() {
        val mockContext = mock(Context::class.java)
        val resolver = LocalLocationHubResolver(mockContext)

        // Rome Coordinates: 41.9028, 12.4964
        val hubs = resolver.findNearestHubs(41.9028, 12.4964)
        val fallbackCity = resolver.resolveFallbackCityName(41.9028, 12.4964)

        assertTrue(hubs["airports"]!!.contains("FCO"))
        assertTrue(hubs["trainStations"]!!.contains("Termini"))
        assertFalse(hubs["airports"]!!.contains("CDG"))
        assertEquals("Roma", fallbackCity)
    }

    @Test
    fun `findNearestHubs picks closest global airport when outside 150km European centers`() {
        val mockContext = mock(Context::class.java)
        val resolver = LocalLocationHubResolver(mockContext)

        // Tokyo Coordinates: 35.6762, 139.6503
        val hubs = resolver.findNearestHubs(35.6762, 139.6503)
        val fallbackCity = resolver.resolveFallbackCityName(35.6762, 139.6503)

        assertTrue(hubs["airports"]!!.contains("HND"))
        assertFalse(hubs["airports"]!!.contains("CDG"))
        assertEquals("Tokyo", fallbackCity)
    }
}
