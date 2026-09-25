package com.example.travelroulette24.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelroulette24.data.byok.ByokTravelService
import com.example.travelroulette24.data.byok.CustomTravelProvider
import kotlinx.coroutines.launch

/**
 * Interactive management panel for custom travel provider APIs per transport mode:
 * - ✈️ Avión (Flight)
 * - 🚆 Tren (Train)
 * - 🚢 Barco / Ferry (Boat)
 */
@Composable
fun CustomProvidersSection(
    // Flight
    customFlightName: String,
    onFlightNameChange: (String) -> Unit,
    customFlightUrl: String,
    onFlightUrlChange: (String) -> Unit,
    customFlightHeader: String,
    onFlightHeaderChange: (String) -> Unit,
    customFlightKey: String,
    onFlightKeyChange: (String) -> Unit,
    customFlightEnabled: Boolean,
    onFlightEnabledChange: (Boolean) -> Unit,
    customFlightIsPaid: Boolean,
    onFlightIsPaidChange: (Boolean) -> Unit,
    customFlightDailyLimit: Int,
    onFlightDailyLimitChange: (Int) -> Unit,

    // Train
    customTrainName: String,
    onTrainNameChange: (String) -> Unit,
    customTrainUrl: String,
    onTrainUrlChange: (String) -> Unit,
    customTrainHeader: String,
    onTrainHeaderChange: (String) -> Unit,
    customTrainKey: String,
    onTrainKeyChange: (String) -> Unit,
    customTrainEnabled: Boolean,
    onTrainEnabledChange: (Boolean) -> Unit,
    customTrainIsPaid: Boolean,
    onTrainIsPaidChange: (Boolean) -> Unit,
    customTrainDailyLimit: Int,
    onTrainDailyLimitChange: (Int) -> Unit,

    // Boat
    customBoatName: String,
    onBoatNameChange: (String) -> Unit,
    customBoatUrl: String,
    onBoatUrlChange: (String) -> Unit,
    customBoatHeader: String,
    onBoatHeaderChange: (String) -> Unit,
    customBoatKey: String,
    onBoatKeyChange: (String) -> Unit,
    customBoatEnabled: Boolean,
    onBoatEnabledChange: (Boolean) -> Unit,
    customBoatIsPaid: Boolean,
    onBoatIsPaidChange: (Boolean) -> Unit,
    customBoatDailyLimit: Int,
    onBoatDailyLimitChange: (Int) -> Unit,

    byokTravelService: ByokTravelService? = null,
    modifier: Modifier = Modifier
) {
    var selectedTabMode by remember { mutableStateOf(CustomTravelProvider.MODE_FLIGHT) }
    var showFormatInfo by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🛠️ APIs Personalizadas por Forma de Viaje",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Conecta tus propios proveedores de Avión, Tren o Barco",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Transport Mode Tabs (Avión, Tren, Barco)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple(CustomTravelProvider.MODE_FLIGHT, "✈️ Avión", customFlightUrl.isNotBlank() && customFlightEnabled),
                Triple(CustomTravelProvider.MODE_TRAIN, "🚆 Tren", customTrainUrl.isNotBlank() && customTrainEnabled),
                Triple(CustomTravelProvider.MODE_FERRY, "🚢 Barco", customBoatUrl.isNotBlank() && customBoatEnabled)
            ).forEach { (mode, label, isActive) ->
                val isSelected = selectedTabMode == mode
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedTabMode = mode },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            if (isActive) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Mode Card Content
        when (selectedTabMode) {
            CustomTravelProvider.MODE_FLIGHT -> {
                SingleModeCustomCard(
                    mode = CustomTravelProvider.MODE_FLIGHT,
                    modeTitle = "Proveedor Personalizado de Vuelos",
                    modeIcon = "✈️",
                    modeDesc = "Conecta tu propio servidor REST, proxy de aerolíneas o agregador privado de vuelos comerciales.",
                    providerName = customFlightName,
                    onNameChange = onFlightNameChange,
                    endpointUrl = customFlightUrl,
                    onUrlChange = onFlightUrlChange,
                    authHeader = customFlightHeader,
                    onHeaderChange = onFlightHeaderChange,
                    apiKey = customFlightKey,
                    onKeyChange = onFlightKeyChange,
                    isEnabled = customFlightEnabled,
                    onEnabledChange = onFlightEnabledChange,
                    isPaid = customFlightIsPaid,
                    onPaidChange = onFlightIsPaidChange,
                    dailyLimit = customFlightDailyLimit,
                    onDailyLimitChange = onFlightDailyLimitChange,
                    onLoadTemplate = {
                        val template = CustomTravelProvider.defaultFlightTemplate()
                        onFlightNameChange(template.name)
                        onFlightUrlChange(template.endpointUrl)
                        onFlightHeaderChange(template.authHeader)
                        onFlightEnabledChange(true)
                    },
                    byokTravelService = byokTravelService
                )
            }
            CustomTravelProvider.MODE_TRAIN -> {
                SingleModeCustomCard(
                    mode = CustomTravelProvider.MODE_TRAIN,
                    modeTitle = "Proveedor Personalizado de Trenes",
                    modeIcon = "🚆",
                    modeDesc = "Conecta APIs privadas o proxys de Renfe, Ouigo, Iryo, SNCF, Deutsche Bahn o agregadores ferroviarios.",
                    providerName = customTrainName,
                    onNameChange = onTrainNameChange,
                    endpointUrl = customTrainUrl,
                    onUrlChange = onTrainUrlChange,
                    authHeader = customTrainHeader,
                    onHeaderChange = onTrainHeaderChange,
                    apiKey = customTrainKey,
                    onKeyChange = onTrainKeyChange,
                    isEnabled = customTrainEnabled,
                    onEnabledChange = onTrainEnabledChange,
                    isPaid = customTrainIsPaid,
                    onPaidChange = onTrainIsPaidChange,
                    dailyLimit = customTrainDailyLimit,
                    onDailyLimitChange = onTrainDailyLimitChange,
                    onLoadTemplate = {
                        val template = CustomTravelProvider.defaultTrainTemplate()
                        onTrainNameChange(template.name)
                        onTrainUrlChange(template.endpointUrl)
                        onTrainHeaderChange(template.authHeader)
                        onTrainEnabledChange(true)
                    },
                    byokTravelService = byokTravelService
                )
            }
            CustomTravelProvider.MODE_FERRY -> {
                SingleModeCustomCard(
                    mode = CustomTravelProvider.MODE_FERRY,
                    modeTitle = "Proveedor Personalizado de Barcos / Ferries",
                    modeIcon = "🚢",
                    modeDesc = "Conecta APIs de navieras marítimas (Baleària, Grimaldi, Fred Olsen, FerryHopper, DirectFerries).",
                    providerName = customBoatName,
                    onNameChange = onBoatNameChange,
                    endpointUrl = customBoatUrl,
                    onUrlChange = onBoatUrlChange,
                    authHeader = customBoatHeader,
                    onHeaderChange = onBoatHeaderChange,
                    apiKey = customBoatKey,
                    onKeyChange = onBoatKeyChange,
                    isEnabled = customBoatEnabled,
                    onEnabledChange = onBoatEnabledChange,
                    isPaid = customBoatIsPaid,
                    onPaidChange = onBoatIsPaidChange,
                    dailyLimit = customBoatDailyLimit,
                    onDailyLimitChange = onBoatDailyLimitChange,
                    onLoadTemplate = {
                        val template = CustomTravelProvider.defaultFerryTemplate()
                        onBoatNameChange(template.name)
                        onBoatUrlChange(template.endpointUrl)
                        onBoatHeaderChange(template.authHeader)
                        onBoatEnabledChange(true)
                    },
                    byokTravelService = byokTravelService
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // JSON Format Help Dropdown
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showFormatInfo = !showFormatInfo },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💡", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "¿Cómo debe responder tu API personalizada?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (showFormatInfo) "▲ Ocultar" else "▼ Ver formato JSON",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                AnimatedVisibility(visible = showFormatInfo) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Text(
                            text = "Tu endpoint recibe '?origin=MAD&mode=train' y debe devolver un array JSON (o un objeto con 'data' / 'results' / 'trips' / 'offers'):",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = """[
  {
    "originHub": "MAD",
    "destinationHub": "BCN",
    "price": 28.50,
    "departureTime": "2026-09-25T10:15:00Z",
    "returnTime": null,
    "checkInOnline": true,
    "deepLink": "https://tu-reserva.com/checkout?id=123",
    "mode": "train"
  }
]""",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Nota: TravelRoulette24 mapea automáticamente alias comunes como 'origin', 'from', 'departure_station', 'destination', 'to', 'fare', 'total', 'amount', etc.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SingleModeCustomCard(
    mode: String,
    modeTitle: String,
    modeIcon: String,
    modeDesc: String,
    providerName: String,
    onNameChange: (String) -> Unit,
    endpointUrl: String,
    onUrlChange: (String) -> Unit,
    authHeader: String,
    onHeaderChange: (String) -> Unit,
    apiKey: String,
    onKeyChange: (String) -> Unit,
    isEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    isPaid: Boolean,
    onPaidChange: (Boolean) -> Unit,
    dailyLimit: Int,
    onDailyLimitChange: (Int) -> Unit,
    onLoadTemplate: () -> Unit,
    byokTravelService: ByokTravelService? = null
) {
    val coroutineScope = rememberCoroutineScope()
    var isTestingConnection by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header with status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(modeIcon, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = modeTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isEnabled && endpointUrl.isNotBlank()) "🟢 Proveedor activo para $mode" else "⚪ Inactivo / Sin configurar",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isEnabled && endpointUrl.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onEnabledChange,
                    modifier = Modifier.testTag("switch_custom_${mode}")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = modeDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Provider Name
            OutlinedTextField(
                value = providerName,
                onValueChange = onNameChange,
                label = { Text("Nombre del Proveedor (ej: Mi API $modeTitle)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_custom_name_${mode}")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Endpoint URL
            OutlinedTextField(
                value = endpointUrl,
                onValueChange = onUrlChange,
                label = { Text("URL del Endpoint REST") },
                placeholder = { Text("https://api.tu-servicio.com/v1/search") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_custom_url_${mode}")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Auth Header & Token
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = authHeader,
                    onValueChange = onHeaderChange,
                    label = { Text("Cabecera") },
                    placeholder = { Text("Authorization o X-Api-Key") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_custom_header_${mode}")
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = onKeyChange,
                    label = { Text("Token / API Key") },
                    placeholder = { Text("Bearer token o key") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("input_custom_key_${mode}")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Plan Selector (Gratuito vs Ilimitado)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!isPaid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                        .border(
                            1.dp,
                            if (!isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onPaidChange(false) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🟢 Plan Gratis (Límite seguro)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (!isPaid) FontWeight.Bold else FontWeight.Normal,
                        color = if (!isPaid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isPaid) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface)
                        .border(
                            1.dp,
                            if (isPaid) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onPaidChange(true) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "💎 Plan Ilimitado",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isPaid) FontWeight.Bold else FontWeight.Normal,
                        color = if (isPaid) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (!isPaid) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Límite diario de seguridad:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(20, 50, 100).forEach { limit ->
                            val isSelected = dailyLimit == limit
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                                    .clickable { onDailyLimitChange(limit) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$limit/día",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Test Connection & Load Template
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onLoadTemplate,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📋 Plantilla", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        if (byokTravelService != null && endpointUrl.isNotBlank()) {
                            isTestingConnection = true
                            testResult = null
                            coroutineScope.launch {
                                val result = byokTravelService.testProviderConnection(
                                    endpointUrl = endpointUrl,
                                    authHeader = authHeader,
                                    apiKey = apiKey,
                                    mode = mode,
                                    testOrigin = if (mode == CustomTravelProvider.MODE_TRAIN) "MAD" else if (mode == CustomTravelProvider.MODE_FERRY) "BCN" else "MAD"
                                )
                                testResult = result
                                isTestingConnection = false
                            }
                        } else {
                            testResult = false to "Introduce una URL antes de probar la conexión."
                        }
                    },
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("test_custom_conn_${mode}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    enabled = !isTestingConnection
                ) {
                    if (isTestingConnection) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onSecondary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Probando...", fontSize = 12.sp)
                    } else {
                        Text("🧪 Probar Conexión", fontSize = 12.sp)
                    }
                }
            }

            // Test Feedback Result Box
            testResult?.let { (success, message) ->
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (success) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (success) MaterialTheme.colorScheme.onTertiaryContainer
                        else MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
