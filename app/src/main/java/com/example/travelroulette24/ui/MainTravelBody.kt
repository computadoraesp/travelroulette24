package com.example.travelroulette24.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.travelroulette24.data.PriceAlertManager
import com.example.travelroulette24.data.SavedTripsManager
import com.example.travelroulette24.engine.TravelIntelligenceEngine

@Composable
internal fun MainTravelAdaptiveBody(
    innerPadding: PaddingValues,
    uiState: TravelViewModel.UiState,
    currentWinner: TravelIntelligenceEngine.ResultItem?,
    isRouletteWheelSpinning: Boolean,
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
    onInspectOnWinner: (TravelIntelligenceEngine.ResultItem) -> Unit,
    controlsBlock: @Composable () -> Unit,
    winnerBlock: @Composable (TravelIntelligenceEngine.ResultItem) -> Unit
) {
    val opportunitiesFeed: LazyListScope.() -> Unit = {
        mainOpportunitiesFeed(
            uiState = uiState,
            currentWinner = currentWinner,
            alertConfig = alertConfig,
            priceAlertManager = priceAlertManager,
            savedTripsManager = savedTripsManager,
            favoriteDestinations = favoriteDestinations,
            wishlistDeals = wishlistDeals,
            matchingDeals = matchingDeals,
            currencyCode = currencyCode,
            currencyRate = currencyRate,
            currencySymbol = currencySymbol,
            onlyDealsFilter = onlyDealsFilter,
            onToggleOnlyDeals = onToggleOnlyDeals,
            opportunitySearchQuery = opportunitySearchQuery,
            onOpportunitySearchQueryChange = onOpportunitySearchQueryChange,
            onRefreshFares = onRefreshFares,
            onEnableAllTransports = onEnableAllTransports,
            onOpenWishlist = onOpenWishlist,
            onOpenWizard = onOpenWizard,
            onSwitchToDemo = onSwitchToDemo,
            onRetry = onRetry,
            onToggleDesiredHub = onToggleDesiredHub,
            onChainTravel = onChainTravel,
            onInviteFriends = onInviteFriends,
            onInspectOnWinner = onInspectOnWinner
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        val isExpandedLayout = maxWidth >= 840.dp
        if (isExpandedLayout) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    controlsBlock()
                }
                LazyColumn(
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (currentWinner != null && !uiState.isLoading && !isRouletteWheelSpinning) {
                        item { winnerBlock(currentWinner) }
                    }
                    opportunitiesFeed()
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            controlsBlock()
                        }
                    }
                    if (currentWinner != null && !uiState.isLoading && !isRouletteWheelSpinning) {
                        item { winnerBlock(currentWinner) }
                    }
                    opportunitiesFeed()
                }
            }
        }
    }
}
