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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelroulette24.R
import com.example.travelroulette24.data.byok.ByokTravelService
import com.example.travelroulette24.data.byok.CustomTravelProvider

/**
 * Interactive management panel for custom travel provider APIs per transport mode:
 * - ✈️ Flight
 * - 🚆 Train
 * - 🚢 Ferry / Boat
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
    customFlightNotes: String = "",
    onFlightNotesChange: (String) -> Unit = {},

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
    customTrainNotes: String = "",
    onTrainNotesChange: (String) -> Unit = {},

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
    customBoatNotes: String = "",
    onBoatNotesChange: (String) -> Unit = {},

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
                    text = stringResource(R.string.custom_api_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.custom_api_section_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Transport Mode Tabs (Flight, Train, Boat)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple(CustomTravelProvider.MODE_FLIGHT, stringResource(R.string.custom_tab_flight), customFlightUrl.isNotBlank() && customFlightEnabled),
                Triple(CustomTravelProvider.MODE_TRAIN, stringResource(R.string.custom_tab_train), customTrainUrl.isNotBlank() && customTrainEnabled),
                Triple(CustomTravelProvider.MODE_FERRY, stringResource(R.string.custom_tab_boat), customBoatUrl.isNotBlank() && customBoatEnabled)
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
                    modeTitle = stringResource(R.string.custom_flight_title),
                    modeIcon = "✈️",
                    modeDesc = stringResource(R.string.custom_flight_desc),
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
                    notes = customFlightNotes,
                    onNotesChange = onFlightNotesChange,
                    onLoadTemplate = {
                        val template = CustomTravelProvider.defaultFlightTemplate()
                        onFlightNameChange(template.name)
                        onFlightUrlChange(template.endpointUrl)
                        onFlightHeaderChange(template.authHeader)
                        onFlightNotesChange(template.notes)
                        onFlightEnabledChange(true)
                    },
                    byokTravelService = byokTravelService
                )
            }
            CustomTravelProvider.MODE_TRAIN -> {
                SingleModeCustomCard(
                    mode = CustomTravelProvider.MODE_TRAIN,
                    modeTitle = stringResource(R.string.custom_train_title),
                    modeIcon = "🚆",
                    modeDesc = stringResource(R.string.custom_train_desc),
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
                    notes = customTrainNotes,
                    onNotesChange = onTrainNotesChange,
                    onLoadTemplate = {
                        val template = CustomTravelProvider.defaultTrainTemplate()
                        onTrainNameChange(template.name)
                        onTrainUrlChange(template.endpointUrl)
                        onTrainHeaderChange(template.authHeader)
                        onTrainNotesChange(template.notes)
                        onTrainEnabledChange(true)
                    },
                    byokTravelService = byokTravelService
                )
            }
            CustomTravelProvider.MODE_FERRY -> {
                SingleModeCustomCard(
                    mode = CustomTravelProvider.MODE_FERRY,
                    modeTitle = stringResource(R.string.custom_boat_title),
                    modeIcon = "🚢",
                    modeDesc = stringResource(R.string.custom_boat_desc),
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
                    notes = customBoatNotes,
                    onNotesChange = onBoatNotesChange,
                    onLoadTemplate = {
                        val template = CustomTravelProvider.defaultFerryTemplate()
                        onBoatNameChange(template.name)
                        onBoatUrlChange(template.endpointUrl)
                        onBoatHeaderChange(template.authHeader)
                        onBoatNotesChange(template.notes)
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
                            text = stringResource(R.string.custom_json_help_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (showFormatInfo) stringResource(R.string.custom_json_hide) else stringResource(R.string.custom_json_show),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                AnimatedVisibility(visible = showFormatInfo) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Text(
                            text = stringResource(R.string.custom_json_intro),
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
    "deepLink": "https://booking.example.com/checkout?id=123",
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
                            text = stringResource(R.string.custom_json_alias_note),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}
