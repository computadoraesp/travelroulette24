package com.example.travelroulette24.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelroulette24.R
import com.example.travelroulette24.budget.BudgetGuardian
import com.example.travelroulette24.utils.CurrencyHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OriginHubsCard(
    displayCity: String,
    isRealGps: Boolean?,
    isLocatingGps: Boolean,
    hasLocationPermission: Boolean,
    onRefreshLocationClick: () -> Unit,
    hubs: Map<String, List<String>>,
    excludedOriginHubs: Set<String> = emptySet(),
    onToggleOriginHub: (String, List<String>) -> Unit = { _, _ -> },
    chainHistory: List<String>
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.origin_label_format, displayCity),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                val isApproxOrigin = isRealGps == false
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (hasLocationPermission || isApproxOrigin) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            else MaterialTheme.colorScheme.errorContainer
                        )
                        .clickable { onRefreshLocationClick() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("refresh_gps_button")
                ) {
                    Text(
                        text = when {
                            isLocatingGps -> stringResource(R.string.gps_locating)
                            isRealGps == true -> stringResource(R.string.gps_auto)
                            isApproxOrigin -> stringResource(R.string.gps_approx_ip)
                            hasLocationPermission -> stringResource(R.string.gps_auto)
                            else -> stringResource(R.string.gps_permission_needed)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasLocationPermission || isApproxOrigin) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val airportList = hubs["airports"].orEmpty()
            val trainList = hubs["trainStations"].orEmpty()
            val portList = hubs["ports"].orEmpty()
            val hasAmbiguity = airportList.size >= 2 || trainList.size >= 2 || portList.size >= 2

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("✈️" to airportList, "🚆" to trainList, "⛴️" to portList).forEach { (icon, catList) ->
                    when {
                        catList.size == 1 -> HubPill(icon = icon, hubs = catList.first())
                        catList.size >= 2 -> catList.forEach { code ->
                            val isSelected = excludedOriginHubs.none { it.equals(code, ignoreCase = true) }
                            FilterChip(
                                selected = isSelected,
                                onClick = { onToggleOriginHub(code, catList) },
                                label = {
                                    Text(
                                        text = "$icon $code",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.testTag("origin_hub_chip_$code"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }
            if (hasAmbiguity) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.ambiguous_terminals_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (chainHistory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                val fullChain = "${chainHistory.joinToString(" ➔ ")} ➔ $displayCity"
                Text(
                    text = stringResource(R.string.chain_history_format, fullChain),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
internal fun RouletteConfigCard(
    filtersExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    mode: String,
    onSelectMode: (String) -> Unit,
    stayRange: ClosedFloatingPointRange<Float>,
    onStayRangeChange: (ClosedFloatingPointRange<Float>) -> Unit,
    onStayRangeFinished: () -> Unit,
    passengers: Int,
    onSelectPassengers: (Int) -> Unit,
    departureWindowHours: Int,
    onSelectDepartureWindow: (Int) -> Unit,
    selectedTransportModes: Set<String>,
    budget: BudgetGuardian.Budget?,
    currencyRate: Double,
    currencySymbol: String,
    onBudgetChange: (BudgetGuardian.Budget?) -> Unit,
    minDistanceVetoEnabled: Boolean = false,
    minDistanceKm: Int = 300,
    hasHistoricalCoverageForMinKm: Boolean = true,
    onMinDistanceVetoChange: (Boolean, Int) -> Unit = { _, _ -> }
) {
    val modeSummary = if (mode == TravelViewModel.MODE_ONE_WAY) {
        stringResource(R.string.chip_mode_one_way)
    } else {
        stringResource(R.string.chip_mode_round_trip)
    }
    val normalizedModes = TravelViewModel.normalizeTransportModes(selectedTransportModes)
    val transportSummary = if (normalizedModes.size == TravelViewModel.ALL_TRANSPORT_MODES.size) {
        stringResource(R.string.chip_transport_all)
    } else {
        listOfNotNull(
            if (TravelViewModel.TRANSPORT_FLIGHT in normalizedModes) stringResource(R.string.chip_transport_flights) else null,
            if (TravelViewModel.TRANSPORT_TRAIN in normalizedModes) stringResource(R.string.chip_transport_trains) else null,
            if (TravelViewModel.TRANSPORT_FERRY in normalizedModes) stringResource(R.string.chip_transport_ferries) else null
        ).joinToString(" + ")
    }
    val budgetActive = budget?.enabled == true
    val budgetAmountDisplay = CurrencyHelper.convertAmount(budget?.maxPerTrip ?: 100.0, currencyRate).toInt()
    val budgetSummary = if (budgetActive) {
        stringResource(R.string.budget_active_currency_format, currencySymbol, budgetAmountDisplay)
    } else {
        stringResource(R.string.budget_free_mode)
    }
    val vetoSummary = if (minDistanceVetoEnabled) " · ≥${minDistanceKm}km" else ""

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onToggleExpanded() }
                    .padding(vertical = 2.dp)
                    .testTag("toggle_filters_card_button"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.roulette_config_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            val windowBadgeText = when (departureWindowHours) {
                                48 -> stringResource(R.string.badge_departure_window_48h)
                                72 -> stringResource(R.string.badge_departure_window_72h)
                                else -> stringResource(R.string.badge_departure_window_24h)
                            }
                            Text(
                                text = windowBadgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    if (!filtersExpanded) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$modeSummary · $passengers pax · $transportSummary · $budgetSummary$vetoSummary",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = if (filtersExpanded) "▲" else "▼",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            AnimatedVisibility(visible = filtersExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = mode == TravelViewModel.MODE_ONE_WAY,
                            onClick = { onSelectMode(TravelViewModel.MODE_ONE_WAY) },
                            label = { Text(stringResource(R.string.chip_mode_one_way)) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )

                        FilterChip(
                            selected = mode == TravelViewModel.MODE_ROUND_TRIP,
                            onClick = { onSelectMode(TravelViewModel.MODE_ROUND_TRIP) },
                            label = { Text(stringResource(R.string.chip_mode_round_trip)) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }

                    if (mode == TravelViewModel.MODE_ROUND_TRIP) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(
                                R.string.stay_desired_format,
                                stayRange.start.toInt(),
                                stayRange.endInclusive.toInt()
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        RangeSlider(
                            value = stayRange,
                            onValueChange = onStayRangeChange,
                            valueRange = 12f..120f,
                            onValueChangeFinished = onStayRangeFinished
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.passengers_label),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (1..4).forEach { count ->
                                FilterChip(
                                    selected = passengers == count,
                                    onClick = { onSelectPassengers(count) },
                                    label = { Text("$count") }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.departure_window_label),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            24 to stringResource(R.string.departure_window_24h_chip),
                            48 to stringResource(R.string.departure_window_48h_chip),
                            72 to stringResource(R.string.departure_window_72h_chip)
                        ).forEach { (hours, chipLabel) ->
                            FilterChip(
                                selected = departureWindowHours == hours,
                                onClick = { onSelectDepartureWindow(hours) },
                                label = {
                                    Text(
                                        text = chipLabel,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("departure_window_chip_$hours"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    MainBudgetGuardianControls(
                        budget = budget,
                        currencySymbol = currencySymbol,
                        currencyRate = currencyRate,
                        onBudgetChange = onBudgetChange
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    MainDistanceVetoControls(
                        enabled = minDistanceVetoEnabled,
                        minDistanceKm = minDistanceKm,
                        hasHistoricalCoverage = hasHistoricalCoverageForMinKm,
                        onChange = onMinDistanceVetoChange
                    )
                }
            }
        }
    }
}

@Composable
internal fun QuickStrategyBar(
    selectedSortOption: TravelViewModel.SortOption,
    onSelectSortOption: (TravelViewModel.SortOption) -> Unit,
    budgetActive: Boolean,
    budgetAmountDisplay: Int,
    currencySymbol: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quick_sort_strategy_row"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(TravelViewModel.SortOption.values()) { option ->
                FilterChip(
                    selected = selectedSortOption == option,
                    onClick = { onSelectSortOption(option) },
                    label = { Text("${option.icon} ${getSortOptionLabel(option)}", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("sort_chip_${option.name.lowercase()}")
                )
            }
        }
        val budgetSuffix = if (budgetActive) {
            stringResource(R.string.selection_desc_budget_currency, currencySymbol, budgetAmountDisplay)
        } else {
            stringResource(R.string.selection_criteria_desc_free)
        }
        Text(
            text = "${getSortOptionDescription(selectedSortOption)}$budgetSuffix",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

@Composable
private fun HubPill(icon: String, hubs: String) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$icon $hubs",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}
