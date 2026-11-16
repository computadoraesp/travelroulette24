package com.example.travelroulette24.location

import android.content.Context
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.*

/**
 * On-Device 100% Free Geolocation, Reverse-Geocoding, and Transportation Hub Resolver.
 *
 * Implements the zero-billing strategy:
 * - Uses Android native [Geocoder] for zero-key reverse geocoding.
 * - Resolves the nearest transport hubs (airports, train stations, ports) via
 *   an embedded global reference dataset using the Haversine formula.
 * - Falls back to nearest hub city by coordinates if offline/Geocoder unavailable.
 */
class LocalLocationHubResolver(private val context: Context) {

    data class HubReference(
        val code: String,
        val name: String,
        val city: String,
        val type: HubType,
        val latitude: Double,
        val longitude: Double
    )

    enum class HubType { AIRPORT, TRAIN_STATION, PORT }

    private val standardHubs = listOf(
        // France
        HubReference("CDG", "Paris Charles de Gaulle Airport", "Paris", HubType.AIRPORT, 49.0097, 2.5479),
        HubReference("ORY", "Paris Orly Airport", "Paris", HubType.AIRPORT, 48.7262, 2.3652),
        HubReference("GDL", "Gare de Lyon (Paris Train)", "Paris", HubType.TRAIN_STATION, 48.8443, 2.3744),
        HubReference("Bercy", "Port de Bercy (Paris Port)", "Paris", HubType.PORT, 48.8356, 2.3802),
        HubReference("LYS", "Lyon-Saint Exupéry Airport", "Lyon", HubType.AIRPORT, 45.7256, 5.0811),
        HubReference("PartDieu", "Lyon Part-Dieu Station", "Lyon", HubType.TRAIN_STATION, 45.7606, 4.8594),
        HubReference("MRS", "Marseille Provence Airport", "Marseille", HubType.AIRPORT, 43.4393, 5.2214),
        HubReference("MRS-Port", "Port of Marseille", "Marseille", HubType.PORT, 43.3065, 5.3600),
        HubReference("NCE", "Nice Côte d'Azur Airport", "Nice", HubType.AIRPORT, 43.6584, 7.2159),

        // Spain
        HubReference("BCN", "Barcelona El Prat Airport", "Barcelona", HubType.AIRPORT, 41.2974, 2.0833),
        HubReference("Sants", "Barcelona Sants Train Station", "Barcelona", HubType.TRAIN_STATION, 41.3792, 2.1401),
        HubReference("BCN-Port", "Port of Barcelona", "Barcelona", HubType.PORT, 41.3600, 2.1700),
        HubReference("MAD", "Adolfo Suárez Madrid–Barajas Airport", "Madrid", HubType.AIRPORT, 40.4983, -3.5676),
        HubReference("Atocha", "Madrid Atocha Railway Station", "Madrid", HubType.TRAIN_STATION, 40.4066, -3.6892),
        HubReference("VLC", "Valencia Airport", "Valencia", HubType.AIRPORT, 39.4893, -0.4816),
        HubReference("JoaquinSorolla", "Valencia Joaquín Sorolla", "Valencia", HubType.TRAIN_STATION, 39.4610, -0.3818),
        HubReference("VLC-Port", "Port of Valencia", "Valencia", HubType.PORT, 39.4494, -0.3189),
        HubReference("SVQ", "Seville Airport", "Sevilla", HubType.AIRPORT, 37.4180, -5.8931),
        HubReference("SantaJusta", "Sevilla Santa Justa", "Sevilla", HubType.TRAIN_STATION, 37.3917, -5.9754),
        HubReference("AGP", "Málaga-Costa del Sol Airport", "Málaga", HubType.AIRPORT, 36.6749, -4.4991),
        HubReference("MariaZambrano", "Málaga María Zambrano", "Málaga", HubType.TRAIN_STATION, 36.7115, -4.4317),
        HubReference("AGP-Port", "Port of Málaga", "Málaga", HubType.PORT, 36.7128, -4.4170),
        HubReference("BIO", "Bilbao Airport", "Bilbao", HubType.AIRPORT, 43.3011, -2.9106),
        HubReference("PMI", "Palma de Mallorca Airport", "Palma de Mallorca", HubType.AIRPORT, 39.5517, 2.7388),
        HubReference("PMI-Port", "Port of Palma", "Palma de Mallorca", HubType.PORT, 39.5611, 2.6331),
        HubReference("IBZ", "Ibiza Airport", "Ibiza", HubType.AIRPORT, 38.8729, 1.3731),

        // Portugal
        HubReference("LIS", "Lisbon Humberto Delgado Airport", "Lisboa", HubType.AIRPORT, 38.7742, -9.1342),
        HubReference("Oriente", "Lisboa Oriente Station", "Lisboa", HubType.TRAIN_STATION, 38.7678, -9.0994),
        HubReference("LIS-Port", "Port of Lisbon", "Lisboa", HubType.PORT, 38.7078, -9.1306),
        HubReference("OPO", "Porto Airport", "Porto", HubType.AIRPORT, 41.2481, -8.6814),

        // UK & Ireland
        HubReference("LHR", "London Heathrow Airport", "London", HubType.AIRPORT, 51.4700, -0.4543),
        HubReference("LGW", "London Gatwick Airport", "London", HubType.AIRPORT, 51.1537, -0.1821),
        HubReference("StPancras", "London St Pancras International", "London", HubType.TRAIN_STATION, 51.5314, -0.1261),
        HubReference("MAN", "Manchester Airport", "Manchester", HubType.AIRPORT, 53.3588, -2.2727),
        HubReference("DUB", "Dublin Airport", "Dublin", HubType.AIRPORT, 53.4264, -6.2499),
        HubReference("DUB-Port", "Dublin Port", "Dublin", HubType.PORT, 53.3472, -6.2156),

        // Italy
        HubReference("FCO", "Rome Fiumicino Airport", "Roma", HubType.AIRPORT, 41.8003, 12.2389),
        HubReference("Termini", "Roma Termini Station", "Roma", HubType.TRAIN_STATION, 41.9009, 12.5020),
        HubReference("Civitavecchia", "Port of Civitavecchia (Rome)", "Roma", HubType.PORT, 42.0931, 11.7872),
        HubReference("MXP", "Milan Malpensa Airport", "Milán", HubType.AIRPORT, 45.6301, 8.7255),
        HubReference("Centrale", "Milano Centrale Station", "Milán", HubType.TRAIN_STATION, 45.4859, 9.2043),
        HubReference("FLR", "Florence Airport", "Florencia", HubType.AIRPORT, 43.8100, 11.2051),
        HubReference("VCE", "Venice Marco Polo Airport", "Venecia", HubType.AIRPORT, 45.5053, 12.3519),
        HubReference("NAP", "Naples International Airport", "Nápoles", HubType.AIRPORT, 40.8860, 14.2908),

        // Benelux, Germany, Switzerland, Austria, Central & Northern Europe
        HubReference("AMS", "Amsterdam Schiphol Airport", "Amsterdam", HubType.AIRPORT, 52.3105, 4.7683),
        HubReference("AMS-CS", "Amsterdam Centraal", "Amsterdam", HubType.TRAIN_STATION, 52.3791, 4.9003),
        HubReference("RTM-Port", "Port of Rotterdam", "Rotterdam", HubType.PORT, 51.9244, 4.4777),
        HubReference("BRU", "Brussels Airport", "Brussels", HubType.AIRPORT, 50.9010, 4.4856),
        HubReference("Midi", "Bruxelles-Midi", "Brussels", HubType.TRAIN_STATION, 50.8357, 4.3366),
        HubReference("BER", "Berlin Brandenburg Airport", "Berlin", HubType.AIRPORT, 52.3667, 13.5033),
        HubReference("BerlinHbf", "Berlin Hauptbahnhof", "Berlin", HubType.TRAIN_STATION, 52.5251, 13.3694),
        HubReference("MUC", "Munich Airport", "Munich", HubType.AIRPORT, 48.3537, 11.7750),
        HubReference("FRA", "Frankfurt Airport", "Frankfurt", HubType.AIRPORT, 50.0379, 8.5622),
        HubReference("ZRH", "Zurich Airport", "Zurich", HubType.AIRPORT, 47.4582, 8.5555),
        HubReference("GVA", "Geneva Airport", "Geneva", HubType.AIRPORT, 46.2381, 6.1089),
        HubReference("VIE", "Vienna International Airport", "Vienna", HubType.AIRPORT, 48.1103, 16.5697),
        HubReference("PRG", "Václav Havel Airport Prague", "Prague", HubType.AIRPORT, 50.1008, 14.2600),
        HubReference("WAW", "Warsaw Chopin Airport", "Warsaw", HubType.AIRPORT, 52.1657, 20.9671),
        HubReference("BUD", "Budapest Ferenc Liszt Airport", "Budapest", HubType.AIRPORT, 47.4369, 19.2556),
        HubReference("CPH", "Copenhagen Airport", "Copenhagen", HubType.AIRPORT, 55.6180, 12.6508),
        HubReference("ARN", "Stockholm Arlanda Airport", "Stockholm", HubType.AIRPORT, 59.6498, 17.9238),
        HubReference("OSL", "Oslo Airport", "Oslo", HubType.AIRPORT, 60.1976, 11.1004),

        // Greece & Turkey & North Africa
        HubReference("ATH", "Athens International Airport", "Athens", HubType.AIRPORT, 37.9364, 23.9445),
        HubReference("Piraeus", "Port of Piraeus", "Athens", HubType.PORT, 37.9420, 23.6465),
        HubReference("IST", "Istanbul Airport", "Istanbul", HubType.AIRPORT, 41.2753, 28.7519),
        HubReference("CMN", "Casablanca Mohammed V Airport", "Casablanca", HubType.AIRPORT, 33.3675, -7.5898),
        HubReference("RAK", "Marrakesh Menara Airport", "Marrakech", HubType.AIRPORT, 31.6069, -8.0363),

        // Americas
        HubReference("JFK", "John F. Kennedy International Airport", "New York", HubType.AIRPORT, 40.6413, -73.7781),
        HubReference("Penn", "New York Penn Station", "New York", HubType.TRAIN_STATION, 40.7506, -73.9935),
        HubReference("MIA", "Miami International Airport", "Miami", HubType.AIRPORT, 25.7959, -80.2870),
        HubReference("MIA-Port", "PortMiami", "Miami", HubType.PORT, 25.7743, -80.1709),
        HubReference("LAX", "Los Angeles International Airport", "Los Angeles", HubType.AIRPORT, 33.9416, -118.4085),
        HubReference("MEX", "Mexico City International Airport", "Ciudad de México", HubType.AIRPORT, 19.4361, -99.0719),
        HubReference("CUN", "Cancún International Airport", "Cancún", HubType.AIRPORT, 21.0365, -86.8771),
        HubReference("BOG", "El Dorado International Airport", "Bogotá", HubType.AIRPORT, 4.7016, -74.1469),
        HubReference("EZE", "Ministro Pistarini International Airport", "Buenos Aires", HubType.AIRPORT, -34.8222, -58.5358),
        HubReference("Retiro", "Estación Retiro", "Buenos Aires", HubType.TRAIN_STATION, -34.5914, -58.3746),
        HubReference("BUE-Port", "Puerto de Buenos Aires", "Buenos Aires", HubType.PORT, -34.5880, -58.3680),
        HubReference("SCL", "Arturo Merino Benítez Airport", "Santiago", HubType.AIRPORT, -33.3930, -70.7858),
        HubReference("LIM", "Jorge Chávez International Airport", "Lima", HubType.AIRPORT, -12.0219, -77.1143),
        HubReference("GRU", "São Paulo/Guarulhos International Airport", "São Paulo", HubType.AIRPORT, -23.4356, -46.4731),

        // Asia & Middle East
        HubReference("HND", "Tokyo Haneda Airport", "Tokyo", HubType.AIRPORT, 35.5494, 139.7798),
        HubReference("TokyoStn", "Tokyo Station", "Tokyo", HubType.TRAIN_STATION, 35.6812, 139.7671),
        HubReference("ICN", "Incheon International Airport", "Seoul", HubType.AIRPORT, 37.4602, 126.4407),
        HubReference("DXB", "Dubai International Airport", "Dubai", HubType.AIRPORT, 25.2532, 55.3657),
        HubReference("SIN", "Singapore Changi Airport", "Singapore", HubType.AIRPORT, 1.3644, 103.9915),
        HubReference("BKK", "Suvarnabhumi Airport", "Bangkok", HubType.AIRPORT, 13.6900, 100.7501)
    )

    /**
     * Coroutine-safe reverse geocoding that properly awaits Android 13+ async Geocoder callbacks
     * and executes blocking legacy Geocoder calls on Dispatchers.IO.
     */
    suspend fun resolveCityFromCoordinates(latitude: Double, longitude: Double): String {
        val fallbackCity = resolveFallbackCityName(latitude, longitude)
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val geocoded = withTimeoutOrNull(3500L) {
                        suspendCancellableCoroutine<String?> { cont ->
                            try {
                                geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                                    val city = addresses.firstOrNull()?.let { addr ->
                                        addr.locality ?: addr.subAdminArea ?: addr.adminArea
                                    }
                                    if (cont.isActive) {
                                        cont.resume(city)
                                    }
                                }
                            } catch (_: Exception) {
                                if (cont.isActive) cont.resume(null)
                            }
                        }
                    }
                    if (!geocoded.isNullOrBlank()) geocoded else fallbackCity
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    val city = addresses?.firstOrNull()?.let { addr ->
                        addr.locality ?: addr.subAdminArea ?: addr.adminArea
                    }
                    if (!city.isNullOrBlank()) city else fallbackCity
                }
            } catch (_: Exception) {
                fallbackCity
            }
        }
    }

    /**
     * Resolves a sensible city name from the closest known transport hub if Geocoder fails or is offline.
     */
    fun resolveFallbackCityName(latitude: Double, longitude: Double): String {
        val closest = standardHubs.minByOrNull {
            calculateHaversineDistance(latitude, longitude, it.latitude, it.longitude)
        }
        if (closest != null) {
            val distanceKm = calculateHaversineDistance(latitude, longitude, closest.latitude, closest.longitude)
            if (distanceKm <= 350.0) {
                return closest.city
            }
        }
        return String.format(Locale.US, "%.2f, %.2f", latitude, longitude)
    }

    /**
     * Resolves nearest hubs of each type within a maximum radius using the Haversine formula.
     * If none exist inside [maxRadiusKm], picks the single closest hub of each type worldwide
     * instead of hardcoding Paris.
     */
    fun findNearestHubs(latitude: Double, longitude: Double, maxRadiusKm: Double = 150.0): Map<String, List<String>> {
        val withDist = standardHubs.map { hub ->
            hub to calculateHaversineDistance(latitude, longitude, hub.latitude, hub.longitude)
        }.sortedBy { it.second }

        val airports = withDist
            .filter { it.first.type == HubType.AIRPORT && it.second <= maxRadiusKm }
            .map { it.first.code }
            .toMutableList()
        val trainStations = withDist
            .filter { it.first.type == HubType.TRAIN_STATION && it.second <= maxRadiusKm }
            .map { it.first.code }
            .toMutableList()
        val ports = withDist
            .filter { it.first.type == HubType.PORT && it.second <= maxRadiusKm }
            .map { it.first.code }
            .toMutableList()

        if (airports.isEmpty()) {
            withDist.firstOrNull { it.first.type == HubType.AIRPORT }?.let { airports.add(it.first.code) }
        }
        if (trainStations.isEmpty()) {
            withDist.firstOrNull { it.first.type == HubType.TRAIN_STATION }?.let { trainStations.add(it.first.code) }
        }
        if (ports.isEmpty()) {
            withDist.firstOrNull { it.first.type == HubType.PORT }?.let { ports.add(it.first.code) }
        }

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
