package com.example.travelroulette24.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelroulette24.data.byok.ApiCredentialsManager

@Composable
fun ApiSettingsDialog(
    credentialsManager: ApiCredentialsManager,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current

    var amadeusKey by remember { mutableStateOf(credentialsManager.amadeusApiKey) }
    var amadeusSecret by remember { mutableStateOf(credentialsManager.amadeusApiSecret) }
    var kiwiKey by remember { mutableStateOf(credentialsManager.kiwiApiKey) }
    var rapidKey by remember { mutableStateOf(credentialsManager.rapidApiKey) }
    var navitiaToken by remember { mutableStateOf(credentialsManager.navitiaToken) }
    var openHafas by remember { mutableStateOf(credentialsManager.openHafasEnabled) }
    var openTripMapKey by remember { mutableStateOf(credentialsManager.openTripMapKey) }
    var unsplashKey by remember { mutableStateOf(credentialsManager.unsplashKey) }

    // Custom APIs for each mode
    var customFlightUrl by remember { mutableStateOf(credentialsManager.customFlightApiUrl) }
    var customFlightKey by remember { mutableStateOf(credentialsManager.customFlightApiKey) }
    var customFlightEnabled by remember { mutableStateOf(credentialsManager.customFlightApiEnabled) }

    var customTrainUrl by remember { mutableStateOf(credentialsManager.customTrainApiUrl) }
    var customTrainKey by remember { mutableStateOf(credentialsManager.customTrainApiKey) }
    var customTrainEnabled by remember { mutableStateOf(credentialsManager.customTrainApiEnabled) }

    var customBoatUrl by remember { mutableStateOf(credentialsManager.customBoatApiUrl) }
    var customBoatKey by remember { mutableStateOf(credentialsManager.customBoatApiKey) }
    var customBoatEnabled by remember { mutableStateOf(credentialsManager.customBoatApiEnabled) }

    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔑", fontSize = 22.sp)
                Spacer(modifier = Modifier.size(8.dp))
                Column {
                    Text(
                        text = "Centro de APIs Gratis (BYOK)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Trae tus claves gratuitas. Se guardan solo en tu móvil.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section: Vuelos
                Text(
                    text = "✈️ PROVEEDORES DE VUELOS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                // 0. Custom Flight API
                ApiProviderCard(
                    title = "API Personalizada de Vuelos",
                    description = "Tu propio servidor REST o proveedor privado de vuelos comerciales.",
                    isConfigured = customFlightUrl.isNotBlank() && customFlightEnabled,
                    onGetFreeKey = { customFlightUrl = "https://api.vuelos-ejemplo.com/v1/search" }
                ) {
                    OutlinedTextField(
                        value = customFlightUrl,
                        onValueChange = { customFlightUrl = it },
                        label = { Text("URL Endpoint Vuelos") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customFlightKey,
                        onValueChange = { customFlightKey = it },
                        label = { Text("Token / API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 1. Amadeus
                ApiProviderCard(
                    title = "Amadeus Self-Service",
                    description = "2.000 búsquedas/mes gratis. Precios de aerolíneas mundiales.",
                    isConfigured = amadeusKey.isNotBlank() && amadeusSecret.isNotBlank(),
                    onGetFreeKey = { openUrl("https://developers.amadeus.com/register") }
                ) {
                    OutlinedTextField(
                        value = amadeusKey,
                        onValueChange = { amadeusKey = it },
                        label = { Text("API Key") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_amadeus_key")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amadeusSecret,
                        onValueChange = { amadeusSecret = it },
                        label = { Text("API Secret") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_amadeus_secret")
                    )
                }

                // 2. Kiwi Tequila
                ApiProviderCard(
                    title = "Kiwi.com (Tequila)",
                    description = "Especialista en low-cost y vuelos 'a cualquier lugar'.",
                    isConfigured = kiwiKey.isNotBlank(),
                    onGetFreeKey = { openUrl("https://tequila.kiwi.com/portal/login/register") }
                ) {
                    OutlinedTextField(
                        value = kiwiKey,
                        onValueChange = { kiwiKey = it },
                        label = { Text("Tequila API Key") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_kiwi_key")
                    )
                }

                // 3. RapidAPI (Google Flights / Skyscanner)
                ApiProviderCard(
                    title = "RapidAPI (Google Flights & Skyscanner)",
                    description = "Una sola cuenta para múltiples agregadores de vuelos.",
                    isConfigured = rapidKey.isNotBlank(),
                    onGetFreeKey = { openUrl("https://rapidapi.com/auth/sign-up") }
                ) {
                    OutlinedTextField(
                        value = rapidKey,
                        onValueChange = { rapidKey = it },
                        label = { Text("X-RapidAPI-Key") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_rapidapi_key")
                    )
                }

                HorizontalDivider()

                // Section: Trenes y Tierra
                Text(
                    text = "🚆 TRENES Y TRANSPORTE TERRESTRE",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                // Custom Train API
                ApiProviderCard(
                    title = "API Personalizada de Trenes",
                    description = "Tu propio servidor REST de billetes o proveedor ferroviario privado.",
                    isConfigured = customTrainUrl.isNotBlank() && customTrainEnabled,
                    onGetFreeKey = { customTrainUrl = "https://api.trenes-ejemplo.com/v1/trips" }
                ) {
                    OutlinedTextField(
                        value = customTrainUrl,
                        onValueChange = { customTrainUrl = it },
                        label = { Text("URL Endpoint Trenes") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customTrainKey,
                        onValueChange = { customTrainKey = it },
                        label = { Text("Token / API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 4. Navitia.io
                ApiProviderCard(
                    title = "Navitia.io (Trenes Europa)",
                    description = "50.000 llamadas/mes gratis (SNCF, Renfe, Eurostar, DB).",
                    isConfigured = navitiaToken.isNotBlank(),
                    onGetFreeKey = { openUrl("https://www.navitia.io/register/") }
                ) {
                    OutlinedTextField(
                        value = navitiaToken,
                        onValueChange = { navitiaToken = it },
                        label = { Text("Token Navitia") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_navitia_token")
                    )
                }

                // 5. Open HAFAS (Comunitario)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Open HAFAS (Trenes Abiertos)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "100% abierto y comunitario, no requiere API key",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = openHafas,
                            onCheckedChange = { openHafas = it }
                        )
                    }
                }

                HorizontalDivider()

                // Section: Barcos y Ferries
                Text(
                    text = "🚢 BARCOS Y FERRIES",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                // Custom Boat API
                ApiProviderCard(
                    title = "API Personalizada de Barcos",
                    description = "Conecta navieras marítimas (Baleària, Grimaldi, FerryHopper, etc.).",
                    isConfigured = customBoatUrl.isNotBlank() && customBoatEnabled,
                    onGetFreeKey = { customBoatUrl = "https://api.ferries-ejemplo.com/v1/routes" }
                ) {
                    OutlinedTextField(
                        value = customBoatUrl,
                        onValueChange = { customBoatUrl = it },
                        label = { Text("URL Endpoint Barcos / Ferries") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customBoatKey,
                        onValueChange = { customBoatKey = it },
                        label = { Text("Token / API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider()
                Text(
                    text = "🏛️ INTELIGENCIA DE DESTINO",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                // 6. OpenTripMap
                ApiProviderCard(
                    title = "OpenTripMap (Atracciones)",
                    description = "Puntos de interés y monumentos qué visitar en 24h.",
                    isConfigured = openTripMapKey.isNotBlank(),
                    onGetFreeKey = { openUrl("https://opentripmap.io/product") }
                ) {
                    OutlinedTextField(
                        value = openTripMapKey,
                        onValueChange = { openTripMapKey = it },
                        label = { Text("OpenTripMap API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 7. Unsplash
                ApiProviderCard(
                    title = "Unsplash (Fotografías)",
                    description = "50 fotos/hora gratis para ilustrar las ciudades de destino.",
                    isConfigured = unsplashKey.isNotBlank(),
                    onGetFreeKey = { openUrl("https://unsplash.com/developers") }
                ) {
                    OutlinedTextField(
                        value = unsplashKey,
                        onValueChange = { unsplashKey = it },
                        label = { Text("Unsplash Access Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    credentialsManager.amadeusApiKey = amadeusKey
                    credentialsManager.amadeusApiSecret = amadeusSecret
                    credentialsManager.kiwiApiKey = kiwiKey
                    credentialsManager.rapidApiKey = rapidKey
                    credentialsManager.navitiaToken = navitiaToken
                    credentialsManager.openHafasEnabled = openHafas
                    credentialsManager.openTripMapKey = openTripMapKey
                    credentialsManager.unsplashKey = unsplashKey

                    credentialsManager.customFlightApiUrl = customFlightUrl
                    credentialsManager.customFlightApiKey = customFlightKey
                    credentialsManager.customFlightApiEnabled = customFlightUrl.isNotBlank()

                    credentialsManager.customTrainApiUrl = customTrainUrl
                    credentialsManager.customTrainApiKey = customTrainKey
                    credentialsManager.customTrainApiEnabled = customTrainUrl.isNotBlank()

                    credentialsManager.customBoatApiUrl = customBoatUrl
                    credentialsManager.customBoatApiKey = customBoatKey
                    credentialsManager.customBoatApiEnabled = customBoatUrl.isNotBlank()

                    if (customFlightUrl.isNotBlank() || customTrainUrl.isNotBlank() || customBoatUrl.isNotBlank() || amadeusKey.isNotBlank() || kiwiKey.isNotBlank()) {
                        credentialsManager.isRealMode = true
                    }

                    onSaved()
                    onDismiss()
                },
                modifier = Modifier.testTag("save_byok_button")
            ) {
                Text("Guardar claves")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun ApiProviderCard(
    title: String,
    description: String,
    isConfigured: Boolean,
    onGetFreeKey: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isConfigured) MaterialTheme.colorScheme.tertiaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isConfigured) "✓ Activa" else "Sin clave",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isConfigured) MaterialTheme.colorScheme.onTertiaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onGetFreeKey,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🔗 Conseguir clave gratis", fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            content()
        }
    }
}
