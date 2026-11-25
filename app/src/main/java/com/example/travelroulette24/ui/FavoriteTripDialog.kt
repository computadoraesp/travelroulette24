package com.example.travelroulette24.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelroulette24.PreferenceHelper
import com.example.travelroulette24.R
import com.example.travelroulette24.data.FavoritePriceHistoryDb
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Alternate window for "Favorite Trip" (3-destination tracker) backed by local SQLite
 * ([FavoritePriceHistoryDb]) to record historical lowest prices and evaluate acceptance ranges.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FavoriteTripDialog(
    allResults: List<TravelIntelligenceEngine.ResultItem>,
    currencyCode: String,
    currencyRate: Double,
    onRefreshSearch: () -> Unit,
    onBookOffer: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val priceDb = remember(context) { FavoritePriceHistoryDb(context) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFavorites by remember { mutableStateOf(PreferenceHelper.getFavoriteDestinations()) }
    var replacingHub by remember { mutableStateOf<String?>(null) }
    var tolerancePercent by remember { mutableIntStateOf(PreferenceHelper.getFavoriteTolerancePercent()) }
    var historyMap by remember { mutableStateOf<Map<String, FavoritePriceHistoryDb.HistoricalPriceRecord>>(emptyMap()) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var predictedDestinations by remember { mutableStateOf<List<com.example.travelroulette24.data.WorldDestinationsCatalog.WorldDestination>>(emptyList()) }
    var isSearchingCities by remember { mutableStateOf(false) }

    // Live debounced global city prediction via Open-Meteo Geocoding API (no hardcoded cities)
    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.length < 2) {
            predictedDestinations = emptyList()
            isSearchingCities = false
            return@LaunchedEffect
        }
        isSearchingCities = true
        kotlinx.coroutines.delay(300)
        predictedDestinations = com.example.travelroulette24.data.WorldDestinationsCatalog.searchGlobalCities(q, limit = 6)
        isSearchingCities = false
    }

    // Record observed prices for selected favorites into SQLite and reload history
    LaunchedEffect(allResults, selectedFavorites, refreshTrigger) {
        historyMap = withContext(Dispatchers.IO) {
            allResults.forEach { offer ->
                val dest = offer.destinationHub.uppercase()
                val destCity = HubCoordinates.getCity(dest).uppercase()
                if (dest in selectedFavorites || destCity in selectedFavorites) {
                    val key = if (dest in selectedFavorites) dest else destCity
                    priceDb.recordOfferPrice(key, offer.price)
                }
            }
            priceDb.getAllHistory()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.fav_trip_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .testTag("favorite_trip_dialog_content")
            ) {
                Text(
                    text = stringResource(R.string.fav_trip_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Global World Destinations Search Bar
                androidx.compose.material3.OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(stringResource(R.string.fav_trip_search_place_label)) },
                    placeholder = { Text(stringResource(R.string.fav_trip_search_place_placeholder)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fav_search_destination_input")
                )

                if (isSearchingCities) {
                    Spacer(modifier = Modifier.height(4.dp))
                    androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.fav_trip_select_label, selectedFavorites.size),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                // Currently selected chips (tap to remove)
                if (selectedFavorites.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        selectedFavorites.forEach { hub ->
                            val cityLabel = com.example.travelroulette24.data.WorldDestinationsCatalog.findByCode(hub)?.city
                                ?: HubCoordinates.getCity(hub)
                            FilterChip(
                                selected = true,
                                onClick = {
                                    val updated = selectedFavorites - hub
                                    selectedFavorites = updated
                                    PreferenceHelper.setFavoriteDestinations(updated)
                                },
                                label = {
                                    Text(
                                        text = "✕ $cityLabel",
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("fav_selected_chip_$hub")
                            )
                        }
                    }
                }

                // Replacement mode indicator banner
                replacingHub?.let { oldHub ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.fav_trip_replacing_hint, oldHub),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { replacingHub = null }) {
                            Text(stringResource(R.string.btn_cancel), fontSize = 11.sp)
                        }
                    }
                }

                // Live predicted global cities (only shown when user types)
                val canAddOrReplace = replacingHub != null || selectedFavorites.size < 3
                val trimmedQuery = searchQuery.trim().uppercase()
                if (predictedDestinations.isNotEmpty() || (trimmedQuery.length >= 2 && canAddOrReplace)) {
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        predictedDestinations.forEach { dest ->
                            val isSelected = dest.code in selectedFavorites
                            if (!isSelected) {
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        val targetToReplace = replacingHub
                                        val updated = if (targetToReplace != null) {
                                            selectedFavorites.map { if (it == targetToReplace) dest.code else it }
                                        } else if (selectedFavorites.size < 3) {
                                            selectedFavorites + dest.code
                                        } else {
                                            selectedFavorites
                                        }
                                        selectedFavorites = updated
                                        PreferenceHelper.setFavoriteDestinations(updated)
                                        replacingHub = null
                                        searchQuery = ""
                                    },
                                    enabled = canAddOrReplace,
                                    label = {
                                        val labelText = if (dest.country.isNotBlank()) "+ ${dest.city}, ${dest.country}" else "+ ${dest.city}"
                                        Text(
                                            text = labelText,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.testTag("fav_hub_chip_${dest.code}")
                                )
                            }
                        }

                        // Offline / custom fallback: allow adding exact typed city or IATA code
                        if (trimmedQuery.length >= 2 &&
                            canAddOrReplace &&
                            trimmedQuery !in selectedFavorites &&
                            predictedDestinations.none { it.code.equals(trimmedQuery, ignoreCase = true) }
                        ) {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val targetToReplace = replacingHub
                                    val updated = if (targetToReplace != null) {
                                        selectedFavorites.map { if (it == targetToReplace) trimmedQuery else it }
                                    } else {
                                        selectedFavorites + trimmedQuery
                                    }
                                    selectedFavorites = updated
                                    PreferenceHelper.setFavoriteDestinations(updated)
                                    replacingHub = null
                                    searchQuery = ""
                                },
                                label = {
                                    Text(
                                        text = stringResource(R.string.fav_trip_add_custom_btn, trimmedQuery),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag("fav_add_custom_chip")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Acceptance Range Tolerance Slider
                Text(
                    text = stringResource(R.string.fav_trip_tolerance_label, tolerancePercent),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = tolerancePercent.toFloat(),
                    onValueChange = {
                        tolerancePercent = it.toInt()
                        PreferenceHelper.setFavoriteTolerancePercent(tolerancePercent)
                    },
                    valueRange = 5f..40f,
                    steps = 6,
                    modifier = Modifier.testTag("fav_tolerance_slider")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        onRefreshSearch()
                        refreshTrigger++
                    },
                    enabled = selectedFavorites.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fav_search_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.fav_trip_search_btn),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedFavorites.isEmpty()) {
                    Text(
                        text = stringResource(R.string.fav_trip_empty_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    selectedFavorites.forEach { hub ->
                        val bestOffer = allResults
                            .filter { it.destinationHub.equals(hub, ignoreCase = true) }
                            .minByOrNull { it.price }
                        FavoriteResultCard(
                            hub = hub,
                            bestOffer = bestOffer,
                            record = historyMap[hub.uppercase()],
                            currencyCode = currencyCode,
                            currencyRate = currencyRate,
                            tolerancePercent = tolerancePercent,
                            onEdit = {
                                replacingHub = hub
                                searchQuery = ""
                            },
                            onRemove = {
                                val updated = selectedFavorites - hub
                                selectedFavorites = updated
                                PreferenceHelper.setFavoriteDestinations(updated)
                                if (replacingHub == hub) replacingHub = null
                            },
                            onBookOffer = onBookOffer
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_close))
            }
        }
    )
}
