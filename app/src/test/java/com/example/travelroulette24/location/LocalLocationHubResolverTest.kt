package com.example.travelroulette24.location

import android.content.Context
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
}
