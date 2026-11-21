package com.example.travelroulette24.ui

/**
 * Hub code to global city name and coordinate resolution helper.
 * Supports seamless Travel Chaining and friendly user-facing labels.
 */
object HubCoordinates {
    data class HubInfo(val city: String, val lat: Double, val lon: Double)

    private val hubMap = mapOf(
        "CDG" to HubInfo("Paris", 48.8566, 2.3522),
        "ORY" to HubInfo("Paris", 48.8566, 2.3522),
        "GDL" to HubInfo("Paris", 48.8566, 2.3522),
        "Bercy" to HubInfo("Paris", 48.8566, 2.3522),
        "LYS" to HubInfo("Lyon", 45.7640, 4.8357),
        "PartDieu" to HubInfo("Lyon", 45.7640, 4.8357),
        "MRS" to HubInfo("Marseille", 43.2965, 5.3698),
        "MRS-Port" to HubInfo("Marseille", 43.2965, 5.3698),
        "NCE" to HubInfo("Nice", 43.7102, 7.2620),
        "BCN" to HubInfo("Barcelona", 41.3851, 2.1734),
        "Sants" to HubInfo("Barcelona", 41.3851, 2.1734),
        "BCN-Port" to HubInfo("Barcelona", 41.3851, 2.1734),
        "MAD" to HubInfo("Madrid", 40.4168, -3.7038),
        "Atocha" to HubInfo("Madrid", 40.4168, -3.7038),
        "VLC" to HubInfo("Valencia", 39.4699, -0.3763),
        "JoaquinSorolla" to HubInfo("Valencia", 39.4699, -0.3763),
        "VLC-Port" to HubInfo("Valencia", 39.4699, -0.3763),
        "SVQ" to HubInfo("Seville", 37.3891, -5.9845),
        "SantaJusta" to HubInfo("Seville", 37.3891, -5.9845),
        "AGP" to HubInfo("Malaga", 36.7213, -4.4214),
        "MariaZambrano" to HubInfo("Malaga", 36.7213, -4.4214),
        "AGP-Port" to HubInfo("Malaga", 36.7213, -4.4214),
        "BIO" to HubInfo("Bilbao", 43.2630, -2.9350),
        "IBZ" to HubInfo("Ibiza", 38.9067, 1.4206),
        "PMI" to HubInfo("Mallorca", 39.5696, 2.6502),
        "PMI-Port" to HubInfo("Mallorca", 39.5696, 2.6502),
        "LIS" to HubInfo("Lisbon", 38.7223, -9.1393),
        "Oriente" to HubInfo("Lisbon", 38.7223, -9.1393),
        "LIS-Port" to HubInfo("Lisbon", 38.7223, -9.1393),
        "OPO" to HubInfo("Porto", 41.1579, -8.6291),
        "LHR" to HubInfo("London", 51.5074, -0.1278),
        "LGW" to HubInfo("London", 51.5074, -0.1278),
        "StPancras" to HubInfo("London", 51.5074, -0.1278),
        "MAN" to HubInfo("Manchester", 53.4808, -2.2426),
        "DUB" to HubInfo("Dublin", 53.3498, -6.2603),
        "DUB-Port" to HubInfo("Dublin", 53.3498, -6.2603),
        "FCO" to HubInfo("Rome", 41.9028, 12.4964),
        "Termini" to HubInfo("Rome", 41.9028, 12.4964),
        "Civitavecchia" to HubInfo("Rome", 41.9028, 12.4964),
        "MXP" to HubInfo("Milan", 45.4642, 9.1900),
        "Centrale" to HubInfo("Milan", 45.4642, 9.1900),
        "FLR" to HubInfo("Florence", 43.7696, 11.2558),
        "VCE" to HubInfo("Venice", 45.4408, 12.3155),
        "NAP" to HubInfo("Naples", 40.8518, 14.2681),
        "AMS" to HubInfo("Amsterdam", 52.3676, 4.9041),
        "AMS-CS" to HubInfo("Amsterdam", 52.3676, 4.9041),
        "RTM-Port" to HubInfo("Rotterdam", 51.9244, 4.4777),
        "BRU" to HubInfo("Brussels", 50.8503, 4.3517),
        "Midi" to HubInfo("Brussels", 50.8503, 4.3517),
        "BER" to HubInfo("Berlin", 52.5200, 13.4050),
        "BerlinHbf" to HubInfo("Berlin", 52.5200, 13.4050),
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
        "ATH" to HubInfo("Athens", 37.9838, 23.7275),
        "Piraeus" to HubInfo("Athens", 37.9838, 23.7275),
        "IST" to HubInfo("Istanbul", 41.0082, 28.9784),
        "CMN" to HubInfo("Casablanca", 33.5731, -7.5898),
        "RAK" to HubInfo("Marrakech", 31.6295, -7.9811),
        "JFK" to HubInfo("New York", 40.7128, -74.0060),
        "Penn" to HubInfo("New York", 40.7128, -74.0060),
        "MIA" to HubInfo("Miami", 25.7617, -80.1918),
        "MIA-Port" to HubInfo("Miami", 25.7617, -80.1918),
        "LAX" to HubInfo("Los Angeles", 34.0522, -118.2437),
        "MEX" to HubInfo("Mexico City", 19.4326, -99.1332),
        "CUN" to HubInfo("Cancun", 21.1619, -86.8515),
        "BOG" to HubInfo("Bogota", 4.7110, -74.0721),
        "EZE" to HubInfo("Buenos Aires", -34.6037, -58.3816),
        "Retiro" to HubInfo("Buenos Aires", -34.6037, -58.3816),
        "BUE-Port" to HubInfo("Buenos Aires", -34.6037, -58.3816),
        "SCL" to HubInfo("Santiago", -33.4489, -70.6693),
        "LIM" to HubInfo("Lima", -12.0464, -77.0428),
        "GRU" to HubInfo("São Paulo", -23.5505, -46.6333),
        "HND" to HubInfo("Tokyo", 35.6762, 139.6503),
        "TokyoStn" to HubInfo("Tokyo", 35.6762, 139.6503),
        "ICN" to HubInfo("Seoul", 37.5665, 126.9780),
        "DXB" to HubInfo("Dubai", 25.2048, 55.2708),
        "SIN" to HubInfo("Singapore", 1.3521, 103.8198),
        "BKK" to HubInfo("Bangkok", 13.7563, 100.5018)
    )

    fun getCity(hub: String): String =
        hubMap[hub]?.city
            ?: com.example.travelroulette24.data.WorldDestinationsCatalog.findByCode(hub)?.city
            ?: hub

    fun getCoordinates(hub: String): Pair<Double, Double> =
        hubMap[hub]?.let { it.lat to it.lon }
            ?: com.example.travelroulette24.data.WorldDestinationsCatalog.findByCode(hub)?.let { it.lat to it.lon }
            ?: (48.8566 to 2.3522)

    /**
     * Calculates distance in kilometers between two hubs using Haversine formula.
     */
    fun calculateDistanceKm(hub1: String, hub2: String): Int {
        val (lat1, lon1) = getCoordinates(hub1)
        val (lat2, lon2) = getCoordinates(hub2)
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return (r * c).toInt().coerceAtLeast(1)
    }
}
