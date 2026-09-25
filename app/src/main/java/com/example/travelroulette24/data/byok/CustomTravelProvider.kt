package com.example.travelroulette24.data.byok

import kotlinx.serialization.Serializable

/**
 * Represents a custom external travel provider configured by the user
 * for a specific travel mode (flight, train, or boat/ferry).
 */
@Serializable
data class CustomTravelProvider(
    val id: String,
    val name: String,
    val mode: String, // "flight", "train", "ferry"
    val endpointUrl: String,
    val authHeader: String = "Authorization",
    val apiKey: String = "",
    val isEnabled: Boolean = true,
    val isPaidTier: Boolean = false,
    val dailyLimit: Int = 50,
    val notes: String = ""
) {
    companion object {
        const val MODE_FLIGHT = "flight"
        const val MODE_TRAIN = "train"
        const val MODE_FERRY = "ferry"

        fun defaultFlightTemplate() = CustomTravelProvider(
            id = "custom_flight_default",
            name = "Mi Proveedor de Vuelos (Custom)",
            mode = MODE_FLIGHT,
            endpointUrl = "https://api.vuelos-ejemplo.com/v1/search",
            authHeader = "Authorization",
            apiKey = "",
            isEnabled = false,
            notes = "Proveedor REST para vuelos comerciales y aerolíneas privadas."
        )

        fun defaultTrainTemplate() = CustomTravelProvider(
            id = "custom_train_default",
            name = "Mi Proveedor de Trenes (Renfe/SNCF/DB/Iryo)",
            mode = MODE_TRAIN,
            endpointUrl = "https://api.trenes-ejemplo.com/v1/trips",
            authHeader = "X-Api-Key",
            apiKey = "",
            isEnabled = false,
            notes = "Proveedor REST para billetes y horarios ferroviarios."
        )

        fun defaultFerryTemplate() = CustomTravelProvider(
            id = "custom_ferry_default",
            name = "Mi Proveedor de Barcos y Ferries (Baleària/Grimaldi)",
            mode = MODE_FERRY,
            endpointUrl = "https://api.ferries-ejemplo.com/v1/routes",
            authHeader = "Authorization",
            apiKey = "",
            isEnabled = false,
            notes = "Proveedor REST para conexiones marítimas y ferris express."
        )
    }
}
