package com.example.travelroulette24.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.travelroulette24.data.PriceAlertManager
import com.example.travelroulette24.data.SavedTripsManager
import com.example.travelroulette24.data.byok.ApiCredentialsManager
import com.example.travelroulette24.engine.TravelIntelligenceEngine

/**
 * Centralized host for all modal dialogs triggered from [MainTravelScreen]:
 * SavedTrips, PriceAlert, TripHub, ApiWizard, ExpressLuggage, and User Manual.
 */
@Composable
fun MainDialogsHost(
    showSavedTripsDialog: Boolean,
    onDismissSavedTrips: () -> Unit,
    savedTrips: List<TravelIntelligenceEngine.ResultItem>,
    savedTripsManager: SavedTripsManager,
    showPriceAlertDialog: Boolean,
    onDismissPriceAlert: () -> Unit,
    alertConfig: PriceAlertManager.AlertConfig,
    priceAlertManager: PriceAlertManager,
    onRequestNotificationPermission: () -> Unit,
    selectedItemForHub: TravelIntelligenceEngine.ResultItem?,
    onDismissTripHub: () -> Unit,
    originCity: String,
    hubs: Map<String, List<String>>,
    isRealMode: Boolean,
    credentialsManager: ApiCredentialsManager,
    currencyCode: String,
    currencyRate: Double,
    showApiWizardMain: Boolean,
    onDismissApiWizard: () -> Unit,
    onApiWizardCompleted: () -> Unit,
    showLuggageDialog: Boolean,
    onDismissLuggage: () -> Unit,
    luggageDestinationCity: String?,
    winningLiveWeather: DestinationWeather?,
    showUserManualDialog: Boolean,
    onDismissUserManual: () -> Unit,
    onOpenWizardFromManual: () -> Unit,
    showFavoriteTripDialog: Boolean,
    onDismissFavoriteTrip: () -> Unit,
    allResults: List<TravelIntelligenceEngine.ResultItem>,
    onRefreshFavoriteSearch: () -> Unit,
    onBookFavoriteOffer: (String) -> Unit
) {
    val context = LocalContext.current

    if (showSavedTripsDialog) {
        SavedTripsDialog(
            savedTrips = savedTrips,
            currencyCode = currencyCode,
            currencyRate = currencyRate,
            onRemoveTrip = { item -> savedTripsManager.removeTrip(item) },
            onDismiss = onDismissSavedTrips
        )
    }

    if (showPriceAlertDialog) {
        PriceAlertDialog(
            currentConfig = alertConfig,
            currencyCode = currencyCode,
            onSaveConfig = { newConfig ->
                priceAlertManager.updateConfig(newConfig)
                if (newConfig.isEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        onRequestNotificationPermission()
                    }
                }
            },
            onDismiss = onDismissPriceAlert
        )
    }

    selectedItemForHub?.let { item ->
        TripHubDialog(
            item = item,
            originCity = originCity,
            hubs = hubs,
            isRealMode = isRealMode,
            tripHubBaseUrl = credentialsManager.tripHubUrl,
            currencyCode = currencyCode,
            currencyRate = currencyRate,
            onDismiss = onDismissTripHub
        )
    }

    if (showApiWizardMain) {
        ApiWizardDialog(
            credentialsManager = credentialsManager,
            onDismiss = onDismissApiWizard,
            onCompleted = onApiWizardCompleted
        )
    }

    if (showLuggageDialog) {
        ExpressLuggageDialog(
            destinationCity = luggageDestinationCity,
            destinationWeather = winningLiveWeather,
            onDismiss = onDismissLuggage
        )
    }

    if (showUserManualDialog) {
        ManualDialog(
            onDismiss = onDismissUserManual,
            onOpenWizard = onOpenWizardFromManual
        )
    }

    if (showFavoriteTripDialog) {
        FavoriteTripDialog(
            allResults = allResults,
            currencyCode = currencyCode,
            currencyRate = currencyRate,
            onRefreshSearch = onRefreshFavoriteSearch,
            onBookOffer = onBookFavoriteOffer,
            onDismiss = onDismissFavoriteTrip
        )
    }
}
