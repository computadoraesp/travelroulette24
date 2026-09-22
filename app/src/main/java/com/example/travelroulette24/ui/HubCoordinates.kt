package com.example.travelroulette24.ui

/**
 * Hub code to European city name and coordinate resolution helper.
 * Supports seamless Travel Chaining and friendly user-facing labels.
 */
object HubCoordinates {
    data class HubInfo(val city: String, val lat: Double, val lon: Double)

    private val hubMap = mapOf(
        "CDG" to HubInfo("París", 48.8566, 2.3522),
        "ORY" to HubInfo("París", 48.8566, 2.3522),
        "GDL" to HubInfo("París", 48.8566, 2.3522),
        "Bercy" to HubInfo("París", 48.8566, 2.3522),
        "BCN" to HubInfo("Barcelona", 41.3851, 2.1734),
        "Sants" to HubInfo("Barcelona", 41.3851, 2.1734),
        "BCN-Port" to HubInfo("Barcelona", 41.3851, 2.1734),
        "MAD" to HubInfo("Madrid", 40.4168, -3.7038),
        "Atocha" to HubInfo("Madrid", 40.4168, -3.7038),
        "LHR" to HubInfo("Londres", 51.5074, -0.1278),
        "StPancras" to HubInfo("Londres", 51.5074, -0.1278),
        "FCO" to HubInfo("Roma", 41.9028, 12.4964),
        "AMS" to HubInfo("Ámsterdam", 52.3676, 4.9041),
        "BER" to HubInfo("Berlín", 52.5200, 13.4050),
        "LIS" to HubInfo("Lisboa", 38.7223, -9.1393),
        "DUB" to HubInfo("Dublín", 53.3498, -6.2603),
        "VIE" to HubInfo("Viena", 48.2082, 16.3738),
        "PRG" to HubInfo("Praga", 50.0755, 14.4378),
        "ATH" to HubInfo("Atenas", 37.9838, 23.7275),
        "BRU" to HubInfo("Bruselas", 50.8503, 4.3517),
        "LYS" to HubInfo("Lyon", 45.7640, 4.8357),
        "MRS" to HubInfo("Marsella", 43.2965, 5.3698),
        "IBZ" to HubInfo("Ibiza", 38.9067, 1.4206),
        "PMI" to HubInfo("Mallorca", 39.5696, 2.6502),
        "FLR" to HubInfo("Florencia", 43.7696, 11.2558),
        "GVA" to HubInfo("Ginebra", 46.2044, 6.1432),
        "ZRH" to HubInfo("Zúrich", 47.3769, 8.5417)
    )

    fun getCity(hub: String): String = hubMap[hub]?.city ?: hub

    fun getCoordinates(hub: String): Pair<Double, Double> =
        hubMap[hub]?.let { it.lat to it.lon } ?: (48.8566 to 2.3522)
}
