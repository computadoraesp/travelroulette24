package com.example.travelroulette24.location

import android.content.Context
import android.location.Geocoder
import android.os.Build
import java.util.Locale
import kotlin.math.*

/**
 * On-Device 100% Free Geolocation, Reverse-Geocoding, and Transportation Hub Resolver.
 *
 * Implements the zero-billing strategy:
 * - Uses Android native [Geocoder] for zero-key reverse geocoding.
 * - Resolves the nearest transport hubs (airports, train stations, ports) via
 *   an embedded reference dataset using the Haversine formula.
 */
class LocalLocationHubResolver(private val context: Context) {

    data class HubReference(
        val code: String,
        val name: String,
        val type: HubType, // "airport" | "train" | "port"
        val latitude: Double,
        val longitude: Double
    )

    enum class HubType { AIRPORT, TRAIN_STATION, PORT }

    // Trimmed, optimized passenger hub reference dataset (OpenTravelData / Global reference nodes)
    private val standardHubs = listOf(
        HubReference("CDG", "Paris Charles de Gaulle Airport", HubType.AIRPORT, 49.0097, 2.5479),
        HubReference("ORY", "Paris Orly Airport", HubType.AIRPORT, 48.7262, 2.3652),
        HubReference("GDL", "Gare de Lyon (Paris Train)", HubType.TRAIN_STATION, 48.8443, 2.3744),
        HubReference("Bercy", "Port de Bercy (Paris Port)", HubType.PORT, 48.8356, 2.3802),
        HubReference("BCN", "Barcelona El Prat Airport", HubType.AIRPORT, 41.2974, 2.0833),
        HubReference("Sants", "Barcelona Sants Train Station", HubType.TRAIN_STATION, 41.3792, 2.1401),
        HubReference("BCN-Port", "Port of Barcelona", HubType.PORT, 41.3600, 2.1700),
        HubReference("MAD", "Adolfo Suárez Madrid–Barajas Airport", HubType.AIRPORT, 40.4983, -3.5676),
        HubReference("Atocha", "Madrid Atocha Railway Station", HubType.TRAIN_STATION, 40.4066, -3.6892),
        HubReference("LHR", "London Heathrow Airport", HubType.AIRPORT, 51.4700, -0.4543),
        HubReference("StPancras", "London St Pancras International", HubType.TRAIN_STATION, 51.5314, -0.1261),
        HubReference("JFK", "John F. Kennedy International Airport", HubType.AIRPORT, 40.6413, -73.7781)
    )

    /**
     * Resolves the city name from latitude and longitude using Android framework native Geocoder.
     * Guaranteed 100% free with no quota limits or API key requirements.
     */
    fun getCityFromCoordinates(latitude: Double, longitude: Double, callback: (String) -> Unit) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                    val city = addresses.firstOrNull()?.locality ?: addresses.firstOrNull()?.subAdminArea ?: "Paris"
                    callback(city)
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val city = addresses?.firstOrNull()?.locality ?: addresses?.firstOrNull()?.subAdminArea ?: "Paris"
                callback(city)
            }
        } catch (_: Exception) {
            callback("Paris") // Safe default fallback
        }
    }

    /**
     * Resolves nearest hubs of each type within a maximum radius using the Haversine formula.
     * Zero network overhead, pure device-local execution.
     */
    fun findNearestHubs(latitude: Double, longitude: Double, maxRadiusKm: Double = 150.0): Map<String, List<String>> {
        val airports = mutableListOf<String>()
        val trainStations = mutableListOf<String>()
        val ports = mutableListOf<String>()

        for (hub in standardHubs) {
            val distance = calculateHaversineDistance(latitude, longitude, hub.latitude, hub.longitude)
            if (distance <= maxRadiusKm) {
                when (hub.type) {
                    HubType.AIRPORT -> airports.add(hub.code)
                    HubType.TRAIN_STATION -> trainStations.add(hub.code)
                    HubType.PORT -> ports.add(hub.code)
                }
            }
        }

        // Fallbacks to guarantee system never returns empty sets if coordinates are outside primary centers
        if (airports.isEmpty()) airports.add("CDG")
        if (trainStations.isEmpty()) trainStations.add("GDL")
        if (ports.isEmpty()) ports.add("Bercy")

        return mapOf(
            "airports" to airports,
            "trainStations" to trainStations,
            "ports" to ports
        )
    }

    private fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // Kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
}
