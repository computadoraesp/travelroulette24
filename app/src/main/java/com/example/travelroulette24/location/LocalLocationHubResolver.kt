package com.example.travelroulette24.location

import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.example.travelroulette24.PreferenceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.TimeZone
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

        // Asia, Middle East, Africa & Oceania
        HubReference("HND", "Tokyo Haneda Airport", "Tokyo", HubType.AIRPORT, 35.5494, 139.7798),
        HubReference("TokyoStn", "Tokyo Station", "Tokyo", HubType.TRAIN_STATION, 35.6812, 139.7671),
        HubReference("ICN", "Incheon International Airport", "Seoul", HubType.AIRPORT, 37.4602, 126.4407),
        HubReference("DXB", "Dubai International Airport", "Dubai", HubType.AIRPORT, 25.2532, 55.3657),
        HubReference("DOH", "Hamad International Airport", "Doha", HubType.AIRPORT, 25.2731, 51.6081),
        HubReference("SIN", "Singapore Changi Airport", "Singapore", HubType.AIRPORT, 1.3644, 103.9915),
        HubReference("BKK", "Suvarnabhumi Airport", "Bangkok", HubType.AIRPORT, 13.6900, 100.7501),
        HubReference("HKG", "Hong Kong International Airport", "Hong Kong", HubType.AIRPORT, 22.3080, 113.9185),
        HubReference("PVG", "Shanghai Pudong International Airport", "Shanghai", HubType.AIRPORT, 31.1443, 121.8083),
        HubReference("DEL", "Indira Gandhi International Airport", "New Delhi", HubType.AIRPORT, 28.5562, 77.1000),
        HubReference("BOM", "Chhatrapati Shivaji Maharaj International Airport", "Mumbai", HubType.AIRPORT, 19.0896, 72.8656),
        HubReference("CAI", "Cairo International Airport", "Cairo", HubType.AIRPORT, 30.1219, 31.4056),
        HubReference("JNB", "O.R. Tambo International Airport", "Johannesburg", HubType.AIRPORT, -26.1392, 28.2460),
        HubReference("CPT", "Cape Town International Airport", "Cape Town", HubType.AIRPORT, -33.9715, 18.6021),
        HubReference("NBO", "Jomo Kenyatta International Airport", "Nairobi", HubType.AIRPORT, -1.3192, 36.9278),
        HubReference("SYD", "Sydney Kingsford Smith Airport", "Sydney", HubType.AIRPORT, -33.9399, 151.1753),
        HubReference("MEL", "Melbourne Airport", "Melbourne", HubType.AIRPORT, -37.6690, 144.8410),
        HubReference("AKL", "Auckland Airport", "Auckland", HubType.AIRPORT, -37.0082, 174.7850),
        // Additional Americas & Europe regional hubs
        HubReference("YYZ", "Toronto Pearson International Airport", "Toronto", HubType.AIRPORT, 43.6777, -79.6248),
        HubReference("ORD", "Chicago O'Hare International Airport", "Chicago", HubType.AIRPORT, 41.9742, -87.9073),
        HubReference("DFW", "Dallas/Fort Worth International Airport", "Dallas", HubType.AIRPORT, 32.8998, -97.0403),
        HubReference("MTY", "Monterrey International Airport", "Monterrey", HubType.AIRPORT, 25.7785, -100.1069),
        HubReference("MDE", "José María Córdova International Airport", "Medellín", HubType.AIRPORT, 6.1645, -75.4231),
        HubReference("PTY", "Tocumen International Airport", "Panama City", HubType.AIRPORT, 9.0714, -79.3835),
        HubReference("SJO", "Juan Santamaría International Airport", "San José", HubType.AIRPORT, 9.9939, -84.2088),
        HubReference("UIO", "Mariscal Sucre International Airport", "Quito", HubType.AIRPORT, -0.1292, -78.3575),
        HubReference("MVD", "Carrasco International Airport", "Montevideo", HubType.AIRPORT, -34.8384, -56.0308),
        HubReference("COR", "Ingeniero Aeronáutico Ambrosio L.V. Taravella Airport", "Córdoba", HubType.AIRPORT, -31.3236, -64.2080),
        HubReference("HEL", "Helsinki Airport", "Helsinki", HubType.AIRPORT, 60.3172, 24.9633)
    )

    /**
     * Coroutine-safe reverse geocoding that tries Android native [Geocoder] first,
     * then falls back to free zero-key BigDataCloud client reverse geocoding (for devices
     * without Google Play Services / Geocoder backend such as Huawei, AOSP, FireOS, emulators),
     * and finally to nearest hub city.
     */
    suspend fun resolveCityFromCoordinates(latitude: Double, longitude: Double): String {
        val fallbackCity = resolveFallbackCityName(latitude, longitude)
        return withContext(Dispatchers.IO) {
            val androidCity = try {
                if (!Geocoder.isPresent()) null
                else {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        withTimeoutOrNull(3000L) {
                            suspendCancellableCoroutine<String?> { cont ->
                                try {
                                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                                        val city = addresses.firstOrNull()?.let { addr ->
                                            addr.locality ?: addr.subAdminArea ?: addr.adminArea
                                        }
                                        if (cont.isActive) cont.resume(city)
                                    }
                                } catch (_: Exception) {
                                    if (cont.isActive) cont.resume(null)
                                }
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                        addresses?.firstOrNull()?.let { addr ->
                            addr.locality ?: addr.subAdminArea ?: addr.adminArea
                        }
                    }
                }
            } catch (_: Exception) {
                null
            }
            if (!androidCity.isNullOrBlank()) return@withContext androidCity

            val networkReverseCity = reverseGeocodeViaBigDataCloud(latitude, longitude)
            if (!networkReverseCity.isNullOrBlank()) return@withContext networkReverseCity

            fallbackCity
        }
    }

    private fun reverseGeocodeViaBigDataCloud(latitude: Double, longitude: Double): String? {
        return try {
            val lang = Locale.getDefault().language.ifBlank { "en" }
            val url = URL(
                "https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=$latitude&longitude=$longitude&localityLanguage=$lang"
            )
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 2800
                readTimeout = 2800
                requestMethod = "GET"
            }
            conn.inputStream.bufferedReader().use { reader ->
                val obj = JSONObject(reader.readText())
                obj.optString("city").takeIf { it.isNotBlank() }
                    ?: obj.optString("locality").takeIf { it.isNotBlank() }
                    ?: obj.optString("principalSubdivision").takeIf { it.isNotBlank() }
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Zero-permission, offline instant system seed based on last known origin,
     * device TimeZone ID, and Locale country so Demo Mode & initial launch never
     * blindly default to an unrelated city.
     */
    fun resolveSystemDefaultOrigin(): Triple<String, Double, Double> {
        PreferenceHelper.getLastKnownOrigin()?.let { return it }

        val tzId = TimeZone.getDefault().id
        val tzMatch = timezoneCityMap[tzId]
        if (tzMatch != null) return tzMatch

        val country = Locale.getDefault().country.uppercase(Locale.US)
        val countryMatch = countryCapitalMap[country]
        if (countryMatch != null) return countryMatch

        return Triple("Madrid", 40.4168, -3.7038)
    }

    /**
     * Zero-permission, zero-key IP geolocation fallback for devices where hardware GPS/Network
     * location is denied, disabled, indoors, or unsupported (e.g. Wi-Fi tablets, Huawei/HMS, AOSP, emulators).
     */
    suspend fun detectApproximateOriginByIp(): Triple<String, Double, Double>? = withContext(Dispatchers.IO) {
        // Provider 1: ipwho.is (HTTPS, free, zero key)
        try {
            val conn = (URL("https://ipwho.is/").openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                requestMethod = "GET"
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val obj = JSONObject(body)
            if (obj.optBoolean("success", true)) {
                val city = obj.optString("city").takeIf { it.isNotBlank() }
                    ?: obj.optString("region").takeIf { it.isNotBlank() }
                val lat = obj.optDouble("latitude", Double.NaN)
                val lon = obj.optDouble("longitude", Double.NaN)
                if (!city.isNullOrBlank() && !lat.isNaN() && !lon.isNaN()) {
                    return@withContext Triple(city, lat, lon)
                }
            }
        } catch (_: Exception) {
            // Try secondary IP provider
        }

        // Provider 2: geojs.io (HTTPS, free, zero key)
        try {
            val conn = (URL("https://get.geojs.io/v1/ip/geo.json").openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                requestMethod = "GET"
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val obj = JSONObject(body)
            val city = obj.optString("city").takeIf { it.isNotBlank() }
                ?: obj.optString("region").takeIf { it.isNotBlank() }
            val lat = obj.optString("latitude").toDoubleOrNull()
            val lon = obj.optString("longitude").toDoubleOrNull()
            if (!city.isNullOrBlank() && lat != null && lon != null) {
                return@withContext Triple(city, lat, lon)
            }
        } catch (_: Exception) {
            // Fall through
        }

        null
    }

    private val timezoneCityMap: Map<String, Triple<String, Double, Double>> = mapOf(
        "Europe/Madrid" to Triple("Madrid", 40.4168, -3.7038),
        "Atlantic/Canary" to Triple("Las Palmas", 28.1235, -15.4363),
        "Europe/Paris" to Triple("Paris", 48.8566, 2.3522),
        "Europe/London" to Triple("London", 51.5074, -0.1278),
        "Europe/Dublin" to Triple("Dublin", 53.3498, -6.2603),
        "Europe/Lisbon" to Triple("Lisboa", 38.7223, -9.1393),
        "Europe/Rome" to Triple("Roma", 41.9028, 12.4964),
        "Europe/Berlin" to Triple("Berlin", 52.5200, 13.4050),
        "Europe/Amsterdam" to Triple("Amsterdam", 52.3676, 4.9041),
        "Europe/Brussels" to Triple("Brussels", 50.8503, 4.3517),
        "Europe/Zurich" to Triple("Zurich", 47.3769, 8.5417),
        "Europe/Vienna" to Triple("Vienna", 48.2082, 16.3738),
        "Europe/Prague" to Triple("Prague", 50.0755, 14.4378),
        "Europe/Warsaw" to Triple("Warsaw", 52.2297, 21.0122),
        "Europe/Budapest" to Triple("Budapest", 47.4979, 19.0402),
        "Europe/Copenhagen" to Triple("Copenhagen", 55.6761, 12.5683),
        "Europe/Stockholm" to Triple("Stockholm", 59.3293, 18.0686),
        "Europe/Oslo" to Triple("Oslo", 59.9139, 10.7522),
        "Europe/Helsinki" to Triple("Helsinki", 60.1699, 24.9384),
        "Europe/Athens" to Triple("Athens", 37.9838, 23.7275),
        "Europe/Istanbul" to Triple("Istanbul", 41.0082, 28.9784),
        "America/Mexico_City" to Triple("Ciudad de México", 19.4326, -99.1332),
        "America/Monterrey" to Triple("Monterrey", 25.6866, -100.3161),
        "America/Cancun" to Triple("Cancún", 21.1619, -86.8515),
        "America/Bogota" to Triple("Bogotá", 4.7110, -74.0721),
        "America/Lima" to Triple("Lima", -12.0464, -77.0428),
        "America/Santiago" to Triple("Santiago", -33.4489, -70.6693),
        "America/Argentina/Buenos_Aires" to Triple("Buenos Aires", -34.6037, -58.3816),
        "America/Buenos_Aires" to Triple("Buenos Aires", -34.6037, -58.3816),
        "America/Argentina/Cordoba" to Triple("Córdoba", -31.4201, -64.1888),
        "America/Sao_Paulo" to Triple("São Paulo", -23.5505, -46.6333),
        "America/Montevideo" to Triple("Montevideo", -34.9011, -56.1645),
        "America/Guayaquil" to Triple("Quito", -0.1807, -78.4678),
        "America/Panama" to Triple("Panama City", 8.9824, -79.5199),
        "America/Costa_Rica" to Triple("San José", 9.9281, -84.0907),
        "America/New_York" to Triple("New York", 40.7128, -74.0060),
        "America/Chicago" to Triple("Chicago", 41.8781, -87.6298),
        "America/Los_Angeles" to Triple("Los Angeles", 34.0522, -118.2437),
        "America/Toronto" to Triple("Toronto", 43.6532, -79.3832),
        "Africa/Casablanca" to Triple("Casablanca", 33.5731, -7.5898),
        "Africa/Cairo" to Triple("Cairo", 30.0444, 31.2357),
        "Africa/Johannesburg" to Triple("Johannesburg", -26.2041, 28.0473),
        "Africa/Nairobi" to Triple("Nairobi", -1.2921, 36.8219),
        "Asia/Dubai" to Triple("Dubai", 25.2048, 55.2708),
        "Asia/Qatar" to Triple("Doha", 25.2854, 51.5310),
        "Asia/Tokyo" to Triple("Tokyo", 35.6762, 139.6503),
        "Asia/Seoul" to Triple("Seoul", 37.5665, 126.9780),
        "Asia/Singapore" to Triple("Singapore", 1.3521, 103.8198),
        "Asia/Bangkok" to Triple("Bangkok", 13.7563, 100.5018),
        "Asia/Hong_Kong" to Triple("Hong Kong", 22.3193, 114.1694),
        "Asia/Shanghai" to Triple("Shanghai", 31.2304, 121.4737),
        "Asia/Kolkata" to Triple("Mumbai", 19.0760, 72.8777),
        "Australia/Sydney" to Triple("Sydney", -33.8688, 151.2093),
        "Australia/Melbourne" to Triple("Melbourne", -37.8136, 144.9631),
        "Pacific/Auckland" to Triple("Auckland", -36.8485, 174.7633)
    )

    private val countryCapitalMap: Map<String, Triple<String, Double, Double>> = mapOf(
        "ES" to Triple("Madrid", 40.4168, -3.7038),
        "FR" to Triple("Paris", 48.8566, 2.3522),
        "GB" to Triple("London", 51.5074, -0.1278),
        "DE" to Triple("Berlin", 52.5200, 13.4050),
        "IT" to Triple("Roma", 41.9028, 12.4964),
        "PT" to Triple("Lisboa", 38.7223, -9.1393),
        "MX" to Triple("Ciudad de México", 19.4326, -99.1332),
        "CO" to Triple("Bogotá", 4.7110, -74.0721),
        "PE" to Triple("Lima", -12.0464, -77.0428),
        "AR" to Triple("Buenos Aires", -34.6037, -58.3816),
        "CL" to Triple("Santiago", -33.4489, -70.6693),
        "BR" to Triple("São Paulo", -23.5505, -46.6333),
        "US" to Triple("New York", 40.7128, -74.0060),
        "CA" to Triple("Toronto", 43.6532, -79.3832),
        "JP" to Triple("Tokyo", 35.6762, 139.6503),
        "KR" to Triple("Seoul", 37.5665, 126.9780),
        "CN" to Triple("Shanghai", 31.2304, 121.4737),
        "EG" to Triple("Cairo", 30.0444, 31.2357),
        "MA" to Triple("Casablanca", 33.5731, -7.5898),
        "AU" to Triple("Sydney", -33.8688, 151.2093)
    )

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
