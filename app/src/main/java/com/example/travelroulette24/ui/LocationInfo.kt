package com.example.travelroulette24.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Simple UI component that displays the resolved city name and the nearest transportation hubs.
 *
 * * `city` – Name of the city resolved from the device location. May be null while location is being
 *   obtained.
 * * `hubs` – Map where the key is one of "airports", "trainStations", "ports" and the value is a list of
 *   hub codes (e.g. IATA codes). The map can be empty if no hubs were found.
 */
@Composable
fun LocationInfo(
    city: String?,
    hubs: Map<String, List<String>>, // expected keys: "airports", "trainStations", "ports"
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(text = "Detected city: ${city ?: "Locating…"}")
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        Spacer(modifier = Modifier.height(8.dp))
        // Show each hub type
        listOf("airports" to "Airports", "trainStations" to "Train stations", "ports" to "Ports").forEach { (key, label) ->
            val list = hubs[key].orEmpty()
            Text(text = "$label: ${if (list.isEmpty()) "None" else list.joinToString(", ")}")
        }
    }
}
