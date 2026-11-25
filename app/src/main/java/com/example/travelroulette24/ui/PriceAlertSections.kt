package com.example.travelroulette24.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelroulette24.R
import com.example.travelroulette24.data.PriceAlertManager
import com.example.travelroulette24.data.WorldDestinationsCatalog
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PriceAlertModeSection(
    alertMode: PriceAlertManager.AlertMode,
    onSelectAlertMode: (PriceAlertManager.AlertMode) -> Unit,
    minKmPerUnit: Float,
    onMinKmChange: (Float) -> Unit,
    maxPrice: Float,
    onMaxPriceChange: (Float) -> Unit,
    priceRange: ClosedFloatingPointRange<Float>,
    presetPrices: List<Float>,
    enforceMaxPriceInValueMode: Boolean,
    onEnforceMaxPriceChange: (Boolean) -> Unit,
    currencySymbol: String,
    kmUnitLabel: String
) {
    val presetKmRatios = listOf(10f, 15f, 25f, 35f)

    Text(
        text = stringResource(R.string.price_alert_mode_label),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(8.dp))

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(
            PriceAlertManager.AlertMode.LOWEST_PRICE to stringResource(R.string.price_alert_mode_lowest),
            PriceAlertManager.AlertMode.BEST_KM_PER_PRICE to stringResource(R.string.price_alert_mode_km_value),
            PriceAlertManager.AlertMode.HISTORICAL_LOW to stringResource(R.string.price_alert_mode_historical)
        ).forEach { (mode, label) ->
            val isSelected = alertMode == mode
            FilterChip(
                selected = isSelected,
                onClick = { onSelectAlertMode(mode) },
                label = {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.testTag("price_alert_mode_${mode.name.lowercase()}")
            )
        }
    }

    if (alertMode == PriceAlertManager.AlertMode.BEST_KM_PER_PRICE) {
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.price_alert_min_km_label),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(
                    R.string.price_alert_km_unit_format,
                    minKmPerUnit.roundToInt(),
                    kmUnitLabel
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Slider(
            value = minKmPerUnit.coerceIn(5f, 50f),
            onValueChange = onMinKmChange,
            valueRange = 5f..50f,
            steps = 44,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.testTag("price_alert_km_slider")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presetKmRatios.forEach { ratio ->
                val isSelected = minKmPerUnit.roundToInt() == ratio.roundToInt()
                FilterChip(
                    selected = isSelected,
                    onClick = { onMinKmChange(ratio) },
                    label = {
                        Text(
                            text = "${ratio.toInt()} km/$kmUnitLabel",
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.price_alert_also_cap_price),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = enforceMaxPriceInValueMode,
                onCheckedChange = onEnforceMaxPriceChange,
                modifier = Modifier.testTag("price_alert_cap_price_switch")
            )
        }
    }

    if (alertMode == PriceAlertManager.AlertMode.HISTORICAL_LOW) {
        Spacer(modifier = Modifier.height(14.dp))
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            )
        ) {
            Text(
                text = stringResource(R.string.price_alert_historical_info_card),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(12.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.price_alert_also_cap_price),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = enforceMaxPriceInValueMode,
                onCheckedChange = onEnforceMaxPriceChange,
                modifier = Modifier.testTag("price_alert_cap_price_switch")
            )
        }
    }

    val showPriceSlider = alertMode == PriceAlertManager.AlertMode.LOWEST_PRICE || enforceMaxPriceInValueMode
    if (showPriceSlider) {
        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.price_alert_max_budget),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "$currencySymbol${maxPrice.roundToInt()}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Slider(
            value = maxPrice.coerceIn(priceRange.start, priceRange.endInclusive),
            onValueChange = onMaxPriceChange,
            valueRange = priceRange,
            steps = 49,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.testTag("price_alert_slider")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presetPrices.forEach { price ->
                val isSelected = maxPrice.roundToInt() == price.roundToInt()
                val labelStr = if (price >= 10000f) "$currencySymbol${(price / 1000f).roundToInt()}k" else "$currencySymbol${price.roundToInt()}"
                FilterChip(
                    selected = isSelected,
                    onClick = { onMaxPriceChange(price) },
                    label = { Text(text = labelStr, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PriceAlertDestinationSection(
    editableFavorites: List<String>,
    selectedDestination: String?,
    onSelectDestination: (String?) -> Unit,
    onRemoveDesiredDestination: (String) -> Unit,
    onAddDesiredDestination: (String) -> Unit,
    showInlineAdd: Boolean,
    onToggleInlineAdd: () -> Unit,
    addQuery: String,
    onAddQueryChange: (String) -> Unit,
    isSearchingCities: Boolean,
    predictedDestinations: List<WorldDestinationsCatalog.WorldDestination>,
    onEditFavoritesClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.price_alert_destination_filter),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        TextButton(
            onClick = onEditFavoritesClick,
            modifier = Modifier.testTag("price_alert_edit_favorites_button")
        ) {
            Text(
                text = if (editableFavorites.isEmpty()) {
                    stringResource(R.string.price_alert_manage_favorites_btn)
                } else {
                    stringResource(R.string.price_alert_edit_favorites_btn)
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
    Spacer(modifier = Modifier.height(6.dp))

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val anySelected = selectedDestination == null
        FilterChip(
            selected = anySelected,
            onClick = { onSelectDestination(null) },
            label = {
                Text(
                    text = stringResource(R.string.price_alert_any_destination),
                    fontSize = 11.sp,
                    fontWeight = if (anySelected) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            modifier = Modifier.testTag("price_alert_dest_any")
        )

        if (editableFavorites.isNotEmpty()) {
            val allFavSelected = selectedDestination == PriceAlertManager.TARGET_ALL_FAVORITES
            FilterChip(
                selected = allFavSelected,
                onClick = { onSelectDestination(PriceAlertManager.TARGET_ALL_FAVORITES) },
                label = {
                    Text(
                        text = stringResource(R.string.price_alert_all_favorites, editableFavorites.size),
                        fontSize = 11.sp,
                        fontWeight = if (allFavSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.testTag("price_alert_dest_all_favorites")
            )

            editableFavorites.forEach { favCode ->
                val cityLabel = WorldDestinationsCatalog.findByCode(favCode)?.city
                    ?: HubCoordinates.getCity(favCode)
                val isSelected = selectedDestination.equals(favCode, ignoreCase = true)
                val removeCd = stringResource(R.string.cd_remove_desired_destination, cityLabel)
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectDestination(favCode) },
                    label = {
                        Text(
                            text = "❤️ $cityLabel",
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    trailingIcon = {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .clickable { onRemoveDesiredDestination(favCode) }
                                .semantics { contentDescription = removeCd }
                                .testTag("price_alert_remove_fav_$favCode"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✕",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.testTag("price_alert_dest_fav_$favCode")
                )
            }
        }

        if (editableFavorites.size < 3) {
            FilterChip(
                selected = showInlineAdd,
                onClick = onToggleInlineAdd,
                label = {
                    Text(
                        text = stringResource(R.string.price_alert_add_dest_btn),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                modifier = Modifier.testTag("price_alert_toggle_add_dest")
            )
        }
    }

    if (showInlineAdd && editableFavorites.size < 3) {
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = addQuery,
            onValueChange = onAddQueryChange,
            label = { Text(stringResource(R.string.fav_trip_search_place_label)) },
            placeholder = { Text(stringResource(R.string.fav_trip_search_place_placeholder)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("price_alert_add_dest_input")
        )

        if (isSearchingCities) {
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        val trimmed = addQuery.trim().uppercase()
        if (predictedDestinations.isNotEmpty() || trimmed.length >= 2) {
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                predictedDestinations.forEach { dest ->
                    if (editableFavorites.none { it.equals(dest.code, ignoreCase = true) }) {
                        FilterChip(
                            selected = false,
                            onClick = { onAddDesiredDestination(dest.code) },
                            label = {
                                val labelText = if (dest.country.isNotBlank()) "+ ${dest.city}, ${dest.country}" else "+ ${dest.city}"
                                Text(
                                    text = labelText,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.testTag("price_alert_add_result_${dest.code}")
                        )
                    }
                }

                if (trimmed.length >= 2 &&
                    editableFavorites.none { it.equals(trimmed, ignoreCase = true) } &&
                    predictedDestinations.none { it.code.equals(trimmed, ignoreCase = true) }
                ) {
                    FilterChip(
                        selected = false,
                        onClick = { onAddDesiredDestination(trimmed) },
                        label = {
                            Text(
                                text = stringResource(R.string.fav_trip_add_custom_btn, trimmed),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier.testTag("price_alert_add_custom_chip")
                    )
                }
            }
        }
    }
}
