package com.example.travelroulette24.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelroulette24.PreferenceHelper
import com.example.travelroulette24.R
import com.example.travelroulette24.data.PriceAlertManager
import com.example.travelroulette24.data.SavedTripsManager
import com.example.travelroulette24.engine.TravelIntelligenceEngine

@Composable
internal fun RouletteWheelActionSection(
    items: List<TravelIntelligenceEngine.ResultItem>,
    isLoading: Boolean,
    isSpinningState: Boolean,
    spinEpoch: Int,
    activeTransportIcon: String,
    currencyCode: String,
    currencyRate: Double,
    onSpinFinished: (TravelIntelligenceEngine.ResultItem) -> Unit,
    onSpinClick: () -> Unit
) {
    AnimatedRouletteWheel(
        items = items,
        isSpinning = isLoading || isSpinningState,
        spinEpoch = spinEpoch,
        activeTransportIcon = activeTransportIcon,
        currencyCode = currencyCode,
        currencyRate = currencyRate,
        onSpinFinished = onSpinFinished,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    val infiniteTransition = rememberInfiniteTransition(label = "spin_transition")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 800, easing = LinearEasing)),
        label = "spin_angle"
    )

    Button(
        onClick = onSpinClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("spin_roulette_button"),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        enabled = !isLoading && !isSpinningState
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .rotate(if (isLoading || isSpinningState) spinAngle else 0f),
                contentAlignment = Alignment.Center
            ) { Text(text = activeTransportIcon, fontSize = 20.sp) }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isLoading || isSpinningState) stringResource(R.string.btn_spinning)
                else stringResource(R.string.btn_spin_roulette),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

internal fun LazyListScope.mainOpportunitiesFeed(
    uiState: TravelViewModel.UiState,
    currentWinner: TravelIntelligenceEngine.ResultItem?,
    alertConfig: PriceAlertManager.AlertConfig,
    priceAlertManager: PriceAlertManager,
    savedTripsManager: SavedTripsManager,
    favoriteDestinations: List<String>,
    wishlistDeals: List<TravelIntelligenceEngine.ResultItem>,
    matchingDeals: List<TravelIntelligenceEngine.ResultItem>,
    currencyCode: String,
    currencyRate: Double,
    currencySymbol: String,
    onlyDealsFilter: Boolean,
    onToggleOnlyDeals: () -> Unit,
    opportunitySearchQuery: String,
    onOpportunitySearchQueryChange: (String) -> Unit,
    onRefreshFares: () -> Unit,
    onEnableAllTransports: () -> Unit,
    onOpenWishlist: () -> Unit,
    onOpenWizard: () -> Unit,
    onSwitchToDemo: () -> Unit,
    onRetry: () -> Unit,
    onToggleDesiredHub: (String) -> Unit,
    onChainTravel: (String) -> Unit,
    onInviteFriends: (TravelIntelligenceEngine.ResultItem) -> Unit,
    onInspectOnWinner: (TravelIntelligenceEngine.ResultItem) -> Unit
) {
    if (uiState.isLoading) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.loading_departures_tomorrow),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else if (uiState.error != null) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.error_obtaining_trips),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = uiState.error.takeIf { it.isNotBlank() } ?: stringResource(R.string.error_unknown),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onRetry) {
                        Text(stringResource(R.string.btn_retry))
                    }
                }
            }
        }
    } else if (uiState.displayResults.isEmpty()) {
        item {
            val isStrictTransportSubset = TravelViewModel.normalizeTransportModes(uiState.selectedTransportModes).size <
                TravelViewModel.ALL_TRANSPORT_MODES.size
            when {
                isStrictTransportSubset -> {
                    FilteredTransportEmptyCard(
                        activeIcon = uiState.activeTransportIcon,
                        onEnableAllTransports = onEnableAllTransports
                    )
                }
                uiState.isRealMode -> {
                    RealEmptyResultsCard(
                        onOpenWizard = onOpenWizard,
                        onSwitchToDemo = onSwitchToDemo
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎲", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.empty_roulette_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    } else {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.top_opportunities_header, uiState.displayResults.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.order_label_format, getSortOptionLabel(uiState.sortOption)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    androidx.compose.material3.OutlinedButton(
                        onClick = onRefreshFares,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("refresh_fares_button")
                    ) {
                        Text(
                            text = stringResource(R.string.btn_refresh_fares),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        if (wishlistDeals.isNotEmpty()) {
            item {
                WishlistMatchBanner(
                    wishlistDeals = wishlistDeals,
                    currencyCode = currencyCode,
                    currencyRate = currencyRate,
                    onClick = onOpenWishlist
                )
            }
        }

        if (alertConfig.isEnabled && matchingDeals.isNotEmpty()) {
            item {
                DealRadarBanner(
                    alertConfig = alertConfig,
                    matchingCount = matchingDeals.size,
                    currencySymbol = currencySymbol,
                    currencyRate = currencyRate,
                    onlyDealsFilter = onlyDealsFilter,
                    onToggleOnlyDeals = onToggleOnlyDeals
                )
            }
        }

        item {
            OutlinedTextField(
                value = opportunitySearchQuery,
                onValueChange = onOpportunitySearchQueryChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.opportunities_search_placeholder),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = { Text("🔍", fontSize = 16.sp) },
                trailingIcon = {
                    if (opportunitySearchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onOpportunitySearchQueryChange("") },
                            modifier = Modifier.testTag("clear_opportunities_search_button")
                        ) {
                            Text(
                                text = "✕",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("opportunities_search_input")
            )
        }

        val basePool = if (onlyDealsFilter && alertConfig.isEnabled) matchingDeals else uiState.displayResults
        val trimmedQuery = opportunitySearchQuery.trim()
        val displayedOpportunities = if (trimmedQuery.isBlank()) {
            basePool
        } else {
            val q = trimmedQuery.lowercase()
            basePool.filter { item ->
                val destCity = HubCoordinates.getCity(item.destinationHub).lowercase()
                val originCity = HubCoordinates.getCity(item.originHub).lowercase()
                item.destinationHub.lowercase().contains(q) ||
                    destCity.contains(q) ||
                    item.originHub.lowercase().contains(q) ||
                    originCity.contains(q) ||
                    item.mode.lowercase().contains(q)
            }
        }

        if (displayedOpportunities.isEmpty() && trimmedQuery.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("opportunities_search_empty_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.opportunities_search_empty, trimmedQuery),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(
                            onClick = { onOpportunitySearchQueryChange("") },
                            modifier = Modifier.testTag("opportunities_search_clear_button")
                        ) {
                            Text(stringResource(R.string.opportunities_search_clear), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        items(
            items = displayedOpportunities,
            key = { "${it.originHub}-${it.destinationHub}-${it.departureTime}-${it.price}-${it.passengers}" }
        ) { resultItem ->
            val isWinningPick = currentWinner?.let {
                it.destinationHub == resultItem.destinationHub &&
                    it.originHub == resultItem.originHub &&
                    it.departureTime == resultItem.departureTime
            } ?: false

            Column(
                modifier = if (isWinningPick) {
                    Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .border(2.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                        .padding(4.dp)
                } else Modifier
            ) {
                if (isWinningPick) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🏆", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.winning_pick_banner),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                OpportunityCard(
                    item = resultItem,
                    currencyCode = currencyCode,
                    currencyRate = currencyRate,
                    isSaved = savedTripsManager.isSaved(resultItem),
                    onToggleSave = { savedTripsManager.toggleSave(resultItem) },
                    isDesired = PreferenceHelper.isDestinationDesired(resultItem.destinationHub, favoriteDestinations),
                    onToggleDesired = { onToggleDesiredHub(resultItem.destinationHub) },
                    isBargainDeal = priceAlertManager.matchesAlert(
                        item = resultItem,
                        config = alertConfig,
                        favoriteDestinations = favoriteDestinations,
                        historicalLowByHub = uiState.historicalLowByHub
                    ),
                    onChainTravel = onChainTravel,
                    onInviteFriends = onInviteFriends,
                    showValueIndex = (uiState.sortOption == TravelViewModel.SortOption.BEST_VALUE),
                    isBestValue = (uiState.bestValueItem == resultItem),
                    isSelectedForWinner = (currentWinner == resultItem),
                    onInspectOnWinner = { onInspectOnWinner(resultItem) }
                )
            }
        }
    }
}
