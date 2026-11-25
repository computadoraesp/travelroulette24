package com.example.travelroulette24.ui

import com.example.travelroulette24.PreferenceHelper
import com.example.travelroulette24.data.WorldDestinationsCatalog
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Hub code to global city name and coordinate resolution helper.
 * Supports Americas, Europe, Antarctica gateways, dynamic API/Overpass terminal
 * registration, and Open-Meteo geocoding fallback without hardcoding Paris.
 */
object HubCoordinates {
    data class HubInfo(val city: String, val lat: Double, val lon: Double)

    private val hubMap = mapOf(
        "CDG" to HubInfo("Paris", 48.8566, 2.3522),
        "ORY" to HubInfo("Paris", 48.7262, 2.3652),
        "GDL" to HubInfo("Paris", 48.8443, 2.3744),
        "Bercy" to HubInfo("Paris", 48.8356, 2.3802),
        "LYS" to HubInfo("Lyon", 45.7640, 4.8357),
        "PartDieu" to HubInfo("Lyon", 45.7606, 4.8594),
        "MRS" to HubInfo("Marseille", 43.2965, 5.3698),
        "MRS-Port" to HubInfo("Marseille", 43.3065, 5.3600),
        "NCE" to HubInfo("Nice", 43.7102, 7.2620),
        "BCN" to HubInfo("Barcelona", 41.3851, 2.1734),
        "Sants" to HubInfo("Barcelona", 41.3792, 2.1401),
        "BCN-Port" to HubInfo("Barcelona", 41.3600, 2.1700),
        "MAD" to HubInfo("Madrid", 40.4168, -3.7038),
        "Atocha" to HubInfo("Madrid", 40.4066, -3.6892),
        "VLC" to HubInfo("Valencia", 39.4699, -0.3763),
        "JoaquinSorolla" to HubInfo("Valencia", 39.4610, -0.3818),
        "VLC-Port" to HubInfo("Valencia", 39.4494, -0.3189),
        "SVQ" to HubInfo("Seville", 37.3891, -5.9845),
        "SantaJusta" to HubInfo("Seville", 37.3917, -5.9754),
        "AGP" to HubInfo("Malaga", 36.7213, -4.4214),
        "MariaZambrano" to HubInfo("Malaga", 36.7115, -4.4317),
        "AGP-Port" to HubInfo("Malaga", 36.7128, -4.4170),
        "BIO" to HubInfo("Bilbao", 43.2630, -2.9350),
        "IBZ" to HubInfo("Ibiza", 38.9067, 1.4206),
        "PMI" to HubInfo("Mallorca", 39.5696, 2.6502),
        "PMI-Port" to HubInfo("Mallorca", 39.5611, 2.6331),
        "LIS" to HubInfo("Lisbon", 38.7223, -9.1393),
        "Oriente" to HubInfo("Lisbon", 38.7678, -9.0994),
        "LIS-Port" to HubInfo("Lisbon", 38.7078, -9.1306),
        "OPO" to HubInfo("Porto", 41.1579, -8.6291),
        "LHR" to HubInfo("London", 51.4700, -0.4543),
        "LGW" to HubInfo("London", 51.1537, -0.1821),
        "StPancras" to HubInfo("London", 51.5314, -0.1261),
        "MAN" to HubInfo("Manchester", 53.4808, -2.2426),
        "DUB" to HubInfo("Dublin", 53.3498, -6.2603),
        "DUB-Port" to HubInfo("Dublin", 53.3472, -6.2156),
        "FCO" to HubInfo("Rome", 41.8003, 12.2389),
        "Termini" to HubInfo("Rome", 41.9009, 12.5020),
        "Civitavecchia" to HubInfo("Rome", 42.0931, 11.7872),
        "MXP" to HubInfo("Milan", 45.6301, 8.7255),
        "Centrale" to HubInfo("Milan", 45.4859, 9.2043),
        "FLR" to HubInfo("Florence", 43.7696, 11.2558),
        "VCE" to HubInfo("Venice", 45.4408, 12.3155),
        "NAP" to HubInfo("Naples", 40.8518, 14.2681),
        "AMS" to HubInfo("Amsterdam", 52.3105, 4.7683),
        "AMS-CS" to HubInfo("Amsterdam", 52.3791, 4.9003),
        "RTM-Port" to HubInfo("Rotterdam", 51.9244, 4.4777),
        "BRU" to HubInfo("Brussels", 50.9010, 4.4856),
        "Midi" to HubInfo("Brussels", 50.8357, 4.3366),
        "BER" to HubInfo("Berlin", 52.3667, 13.5033),
        "BerlinHbf" to HubInfo("Berlin", 52.5251, 13.3694),
        "MUC" to HubInfo("Munich", 48.1351, 11.5820),
        "FRA" to HubInfo("Frankfurt", 50.1109, 8.6821),
        "GVA" to HubInfo("Geneva", 46.2044, 6.1432),
        "ZRH" to HubInfo("Zurich", 47.3769, 8.5417),
        "VIE" to HubInfo("Vienna", 48.2082, 16.3738),
        "PRG" to HubInfo("Prague", 50.0755, 14.4378),
        "WAW" to HubInfo("Warsaw", 52.2297, 21.0122),
        "BUD" to HubInfo("Budapest", 47.4979, 19.0402),
        "CPH" to HubInfo("Copenhagen", 55.6761, 12.5683),
        "ARN" to HubInfo("Stockholm", 59.3293, 18.0686),
        "OSL" to HubInfo("Oslo", 59.9139, 10.7522),
        "HEL" to HubInfo("Helsinki", 60.1699, 24.9384),
        "ATH" to HubInfo("Athens", 37.9838, 23.7275),
        "Piraeus" to HubInfo("Athens", 37.9420, 23.6465),
        "IST" to HubInfo("Istanbul", 41.0082, 28.9784),
        "CMN" to HubInfo("Casablanca", 33.5731, -7.5898),
        "RAK" to HubInfo("Marrakech", 31.6295, -7.9811),

        // Americas (including secondary airports/stations/ports for regional ambiguity & coverage)
        "JFK" to HubInfo("New York", 40.6413, -73.7781),
        "LGA" to HubInfo("New York", 40.7769, -73.8740),
        "Penn" to HubInfo("New York", 40.7506, -73.9935),
        "MIA" to HubInfo("Miami", 25.7959, -80.2870),
        "MIA-Port" to HubInfo("Miami", 25.7743, -80.1709),
        "LAX" to HubInfo("Los Angeles", 33.9416, -118.4085),
        "MEX" to HubInfo("Mexico City", 19.4361, -99.0719),
        "NLU" to HubInfo("Mexico City", 19.7400, -99.0100),
        "CUN" to HubInfo("Cancun", 21.0365, -86.8771),
        "Cozumel-Port" to HubInfo("Cozumel", 20.5083, -86.9458),
        "BOG" to HubInfo("Bogota", 4.7016, -74.1469),
        "MDE" to HubInfo("Medellín", 6.1645, -75.4231),
        "CTG" to HubInfo("Cartagena", 10.4424, -75.5130),
        "EZE" to HubInfo("Buenos Aires", -34.8222, -58.5358),
        "AEP" to HubInfo("Buenos Aires", -34.5592, -58.4156),
        "Retiro" to HubInfo("Buenos Aires", -34.5914, -58.3746),
        "BUE-Port" to HubInfo("Buenos Aires", -34.5880, -58.3680),
        "Colonia-Port" to HubInfo("Colonia del Sacramento", -34.4626, -57.8398),
        "MVD" to HubInfo("Montevideo", -34.8384, -56.0308),
        "COR" to HubInfo("Córdoba", -31.3236, -64.2080),
        "SCL" to HubInfo("Santiago", -33.3930, -70.7858),
        "Valparaiso-Port" to HubInfo("Valparaíso", -33.0472, -71.6127),
        "LIM" to HubInfo("Lima", -12.0219, -77.1143),
        "CUZ" to HubInfo("Cusco", -13.5357, -71.9388),
        "GRU" to HubInfo("São Paulo", -23.4356, -46.4731),
        "CGH" to HubInfo("São Paulo", -23.6261, -46.6564),
        "GIG" to HubInfo("Rio de Janeiro", -22.8099, -43.2506),
        "SDU" to HubInfo("Rio de Janeiro", -22.9105, -43.1631),
        "YYZ" to HubInfo("Toronto", 43.6777, -79.6248),
        "ORD" to HubInfo("Chicago", 41.9742, -87.9073),
        "DFW" to HubInfo("Dallas", 32.8998, -97.0403),
        "MTY" to HubInfo("Monterrey", 25.7785, -100.1069),
        "PTY" to HubInfo("Panama City", 9.0714, -79.3835),
        "SJO" to HubInfo("San José", 9.9939, -84.2088),
        "UIO" to HubInfo("Quito", -0.1292, -78.3575),
        "SDQ" to HubInfo("Santo Domingo", 18.4297, -69.6689),
        "PUJ" to HubInfo("Punta Cana", 18.5674, -68.3634),
        "SJU" to HubInfo("San Juan", 18.4394, -66.0018),
        "HAV" to HubInfo("La Habana", 22.9892, -82.4091),
        "ASU" to HubInfo("Asunción", -25.2400, -57.5192),
        "VVI" to HubInfo("Santa Cruz", -17.6448, -63.1354),

        // Antarctica & Sub-Antarctic Gateways
        "USH" to HubInfo("Ushuaia", -54.8433, -68.2958),
        "Ushuaia-Port" to HubInfo("Ushuaia", -54.8069, -68.3025),
        "FinDelMundo" to HubInfo("Ushuaia", -54.8325, -68.4275),
        "PUQ" to HubInfo("Punta Arenas", -53.0026, -70.8546),
        "PuntaArenas-Port" to HubInfo("Punta Arenas", -53.1638, -70.9171),
        "PMC" to HubInfo("Puerto Montt", -41.4389, -73.0940),
        "TNM" to HubInfo("Antártida (Isla Rey Jorge)", -62.1908, -58.9867),
        "MCM" to HubInfo("McMurdo (Antártida)", -77.8419, 166.6863),

        // Asia, Middle East, Africa & Oceania
        "HND" to HubInfo("Tokyo", 35.5494, 139.7798),
        "TokyoStn" to HubInfo("Tokyo", 35.6812, 139.7671),
        "ICN" to HubInfo("Seoul", 37.4602, 126.4407),
        "DXB" to HubInfo("Dubai", 25.2532, 55.3657),
        "DOH" to HubInfo("Doha", 25.2731, 51.6081),
        "SIN" to HubInfo("Singapore", 1.3644, 103.9915),
        "BKK" to HubInfo("Bangkok", 13.6900, 100.7501),
        "HKG" to HubInfo("Hong Kong", 22.3080, 113.9185),
        "PVG" to HubInfo("Shanghai", 31.1443, 121.8083),
        "DEL" to HubInfo("New Delhi", 28.5562, 77.1000),
        "BOM" to HubInfo("Mumbai", 19.0896, 72.8656),
        "CAI" to HubInfo("Cairo", 30.1219, 31.4056),
        "JNB" to HubInfo("Johannesburg", -26.1392, 28.2460),
        "CPT" to HubInfo("Cape Town", -33.9715, 18.6021),
        "NBO" to HubInfo("Nairobi", -1.3192, 36.9278),
        "SYD" to HubInfo("Sydney", -33.9399, 151.1753),
        "MEL" to HubInfo("Melbourne", -37.6690, 144.8410),
        "AKL" to HubInfo("Auckland", -37.0082, 174.7850),
        "CHC" to HubInfo("Christchurch", -43.4894, 172.5322)
    )

    private val dynamicHubs = ConcurrentHashMap<String, HubInfo>()

    fun registerDynamicHub(code: String, city: String, lat: Double, lon: Double) {
        val cleanCode = code.trim()
        val cleanCity = city.trim().ifBlank { cleanCode }
        if (cleanCode.isBlank() || lat.isNaN() || lon.isNaN()) return
        val info = HubInfo(city = cleanCity, lat = lat, lon = lon)
        dynamicHubs[cleanCode] = info
        dynamicHubs[cleanCode.uppercase(Locale.US)] = info
        WorldDestinationsCatalog.cacheDestination(
            WorldDestinationsCatalog.WorldDestination(
                code = cleanCode.uppercase(Locale.US),
                city = cleanCity,
                country = "",
                lat = lat,
                lon = lon
            )
        )
    }

    fun hasKnownHub(hub: String): Boolean {
        val trimmed = hub.trim()
        return hubMap.containsKey(trimmed) ||
            dynamicHubs.containsKey(trimmed) ||
            dynamicHubs.containsKey(trimmed.uppercase(Locale.US)) ||
            WorldDestinationsCatalog.findByCode(trimmed) != null
    }

    fun getCity(hub: String): String {
        val trimmed = hub.trim()
        return hubMap[trimmed]?.city
            ?: dynamicHubs[trimmed]?.city
            ?: dynamicHubs[trimmed.uppercase(Locale.US)]?.city
            ?: WorldDestinationsCatalog.findByCode(trimmed)?.city
            ?: trimmed
    }

    fun getCoordinatesOrNull(hub: String): Pair<Double, Double>? {
        val trimmed = hub.trim()
        return hubMap[trimmed]?.let { it.lat to it.lon }
            ?: dynamicHubs[trimmed]?.let { it.lat to it.lon }
            ?: dynamicHubs[trimmed.uppercase(Locale.US)]?.let { it.lat to it.lon }
            ?: WorldDestinationsCatalog.findByCode(trimmed)?.let { it.lat to it.lon }
    }

    fun getCoordinates(hub: String): Pair<Double, Double> =
        getCoordinatesOrNull(hub)
            ?: PreferenceHelper.getLastKnownOrigin()?.let { it.second to it.third }
            ?: (0.0 to 0.0)

    /**
     * Resolves unknown destination/origin hubs dynamically via Open-Meteo Geocoding API
     * so distance, km/€ Value Index, and distance vetoes work across any global city.
     */
    suspend fun resolveUnknownHubsAsync(hubs: Collection<String>) {
        val unresolved = hubs.asSequence()
            .map { it.trim() }
            .filter { it.length >= 2 && !hasKnownHub(it) }
            .distinct()
            .take(15)
            .toList()
        if (unresolved.isEmpty()) return

        coroutineScope {
            unresolved.map { hub ->
                async {
                    val found = WorldDestinationsCatalog.searchGlobalCities(hub, limit = 1).firstOrNull()
                    if (found != null) {
                        registerDynamicHub(
                            code = hub,
                            city = found.city,
                            lat = found.lat,
                            lon = found.lon
                        )
                    }
                }
            }.awaitAll()
        }
    }

    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c).toInt().coerceAtLeast(1)
    }

    fun calculateDistanceKmOrNull(hub1: String, hub2: String): Int? {
        val c1 = getCoordinatesOrNull(hub1) ?: return null
        val c2 = getCoordinatesOrNull(hub2) ?: return null
        return calculateDistanceKm(c1.first, c1.second, c2.first, c2.second)
    }

    fun calculateDistanceKmOrNull(originLat: Double, originLon: Double, destHub: String): Int? {
        val c2 = getCoordinatesOrNull(destHub) ?: return null
        return calculateDistanceKm(originLat, originLon, c2.first, c2.second)
    }

    /**
     * Calculates distance in kilometers between two known hubs using Haversine formula.
     */
    fun calculateDistanceKm(hub1: String, hub2: String): Int {
        return calculateDistanceKmOrNull(hub1, hub2) ?: 0
    }
}
