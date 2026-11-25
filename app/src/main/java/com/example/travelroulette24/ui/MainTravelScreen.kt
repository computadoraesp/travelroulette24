package com.example.travelroulette24.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.travelroulette24.MainActivity
import com.example.travelroulette24.PreferenceHelper
import com.example.travelroulette24.R
import com.example.travelroulette24.data.PriceAlertManager
import com.example.travelroulette24.data.SavedTripsManager
import com.example.travelroulette24.data.byok.ApiCredentialsManager
import com.example.travelroulette24.data.byok.ApiQuotaGovernor
import com.example.travelroulette24.data.byok.ByokTravelService
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import com.example.travelroulette24.location.LocalLocationHubResolver
import com.example.travelroulette24.utils.CurrencyHelper
import com.example.travelroulette24.utils.DealNotificationDispatcher
import com.example.travelroulette24.workers.DealAlertScheduler

@Composable
fun MainTravelScreen(
    viewModel: TravelViewModel,
    locationResolver: LocalLocationHubResolver,
    credentialsManager: ApiCredentialsManager,
    quotaGovernor: ApiQuotaGovernor? = null,
    byokTravelService: ByokTravelService? = null,
    savedTripsManager: SavedTripsManager,
    priceAlertManager: PriceAlertManager,
    onRequestLocationRefresh: () -> Unit = {}
) {
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* handled */ }
    var hasLocationPermissionState by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    val gpsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermissionState = granted
        if (granted) onRequestLocationRefresh()
    }
    val deviceLocation by MainActivity.deviceLocationFlow.collectAsState()
    val isLocatingGps by MainActivity.isLocatingFlow.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val savedTrips by savedTripsManager.savedTripsFlow.collectAsState()
    val alertConfig by priceAlertManager.configFlow.collectAsState()
    val originFallback = stringResource(R.string.origin_fallback)

    val initialSystemOrigin = remember {
        deviceLocation ?: locationResolver.resolveSystemDefaultOrigin().let { (c, lat, lon) ->
            MainActivity.DeviceLocationState(
                city = c,
                hubs = locationResolver.findNearestHubs(lat, lon, maxRadiusKm = credentialsManager.searchRadiusKm.toDouble()),
                lat = lat,
                lon = lon,
                isRealGps = false
            )
        }
    }

    var city by remember { mutableStateOf(initialSystemOrigin.city) }
    var hubs by remember { mutableStateOf(initialSystemOrigin.hubs) }
    var currentCoords by remember { mutableStateOf(Pair(initialSystemOrigin.lat, initialSystemOrigin.lon)) }
    var selectedItemForHub by remember { mutableStateOf<TravelIntelligenceEngine.ResultItem?>(null) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var showSavedTripsDialog by remember { mutableStateOf(false) }
    var showPriceAlertDialog by remember { mutableStateOf(false) }
    var showFavoriteTripDialog by remember { mutableStateOf(false) }
    var onlyDealsFilter by remember { mutableStateOf(false) }
    var opportunitySearchQuery by remember { mutableStateOf("") }
    val budgetActive = uiState.budget?.enabled == true
    val budgetAmountEur = uiState.budget?.maxPerTrip ?: 100.0
    var stayRange by remember { mutableStateOf(24f..72f) }
    var showApiWizardMain by remember { mutableStateOf(false) }
    var isRouletteWheelSpinning by remember { mutableStateOf(false) }
    var spinEpoch by remember { mutableIntStateOf(0) }
    var highlightedDestinationItem by remember { mutableStateOf<TravelIntelligenceEngine.ResultItem?>(null) }
    var isRealModeState by remember { mutableStateOf(credentialsManager.isRealMode) }
    var showLuggageDialog by remember { mutableStateOf(false) }
    var showUserManualDialog by remember { mutableStateOf(false) }
    var filtersExpanded by remember { mutableStateOf(false) }
    val favoriteDestinations by PreferenceHelper.favoriteDestinationsFlow.collectAsState()

    LaunchedEffect(Unit) { savedTripsManager.pruneExpiredTrips() }

    val currencyCode = uiState.currency
    val currencyRate = uiState.currencyRate
    val currencySymbol = CurrencyHelper.symbolFor(currencyCode)
    val budgetAmountDisplay = CurrencyHelper.convertAmount(budgetAmountEur, currencyRate)

    val matchingDeals = remember(uiState.displayResults, alertConfig, favoriteDestinations, uiState.historicalLowByHub) {
        priceAlertManager.getMatchingDeals(
            results = uiState.displayResults,
            config = alertConfig,
            favoriteDestinations = favoriteDestinations,
            historicalLowByHub = uiState.historicalLowByHub
        )
    }

    val wishlistDeals = remember(uiState.displayResults, favoriteDestinations, uiState.budget, uiState.historicalLowByHub) {
        DealNotificationDispatcher.findLatentWishlistDeals(
            results = uiState.displayResults,
            favoriteDestinations = favoriteDestinations,
            budget = uiState.budget,
            historicalLowByHub = uiState.historicalLowByHub,
            tolerancePercent = PreferenceHelper.getFavoriteTolerancePercent()
        )
    }

    val handleToggleDesiredHub: (String) -> Unit = { destHub ->
        val result = PreferenceHelper.toggleDesiredDestination(destHub)
        DealAlertScheduler.syncSchedule(context)
        if (result == PreferenceHelper.WishlistToggleResult.ADDED &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (result == PreferenceHelper.WishlistToggleResult.LIMIT_REACHED) {
            showFavoriteTripDialog = true
        }
    }

    LaunchedEffect(wishlistDeals, matchingDeals, alertConfig.isEnabled, uiState.isLoading, isRouletteWheelSpinning, currencyCode, currencyRate) {
        if (!uiState.isLoading && !isRouletteWheelSpinning) {
            val activeRadarDeals = if (alertConfig.isEnabled) matchingDeals else emptyList()
            if (wishlistDeals.isNotEmpty() || activeRadarDeals.isNotEmpty()) {
                DealNotificationDispatcher.notifyIfNeeded(
                    context = context,
                    wishlistDeals = wishlistDeals,
                    radarDeals = activeRadarDeals,
                    currencyCode = currencyCode,
                    currencyRate = currencyRate
                )
            }
        }
    }

    LaunchedEffect(deviceLocation) {
        val loc = deviceLocation
        if (loc != null) {
            city = loc.city
            hubs = loc.hubs
            currentCoords = Pair(loc.lat, loc.lon)
            viewModel.loadTop20WithCoordinates(loc.lat, loc.lon)
        } else if (uiState.results.isEmpty() && !uiState.isLoading) {
            viewModel.loadTop20WithCoordinates(currentCoords.first, currentCoords.second)
        }
    }

    val activeWinnerForEffect = highlightedDestinationItem ?: uiState.displayResults.firstOrNull()
    LaunchedEffect(activeWinnerForEffect?.destinationHub, uiState.isRealMode) {
        viewModel.loadWinnerIntelligence(activeWinnerForEffect)
    }

    LaunchedEffect(uiState.displayResults) {
        val current = highlightedDestinationItem
        if (current != null && uiState.displayResults.none {
                it.destinationHub == current.destinationHub &&
                    it.originHub == current.originHub &&
                    it.mode == current.mode
            }
        ) {
            highlightedDestinationItem = null
        }
    }

    LaunchedEffect(uiState.isLoading, uiState.displayResults.size, uiState.error) {
        if (!uiState.isLoading && (uiState.displayResults.isEmpty() || uiState.error != null)) {
            isRouletteWheelSpinning = false
        }
    }

    if (showSettingsScreen) {
        SettingsScreen(
            credentialsManager = credentialsManager,
            quotaGovernor = quotaGovernor,
            byokTravelService = byokTravelService,
            onBack = {
                isRealModeState = credentialsManager.isRealMode
                viewModel.syncPreferencesFromCredentials()
                hubs = locationResolver.findNearestHubs(currentCoords.first, currentCoords.second, maxRadiusKm = credentialsManager.searchRadiusKm.toDouble())
                showSettingsScreen = false
                DealAlertScheduler.syncSchedule(context)
                viewModel.loadTop20WithCoordinates(currentCoords.first, currentCoords.second)
                val nowHasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                hasLocationPermissionState = nowHasPerm
                if (nowHasPerm) onRequestLocationRefresh()
            },
            onSaved = {
                isRealModeState = credentialsManager.isRealMode
                viewModel.syncPreferencesFromCredentials()
                hubs = locationResolver.findNearestHubs(currentCoords.first, currentCoords.second, maxRadiusKm = credentialsManager.searchRadiusKm.toDouble())
                highlightedDestinationItem = null
                DealAlertScheduler.syncSchedule(context)
                viewModel.loadTop20WithCoordinates(currentCoords.first, currentCoords.second)
            }
        )
        return
    }

    val handleChainTravel: (String) -> Unit = { destinationHub ->
        val destCoords = HubCoordinates.getCoordinates(destinationHub)
        val destCity = HubCoordinates.getCity(destinationHub)
        currentCoords = destCoords
        city = destCity
        hubs = locationResolver.findNearestHubs(destCoords.first, destCoords.second, maxRadiusKm = credentialsManager.searchRadiusKm.toDouble())
        viewModel.chainTravel(destCity, destCoords.first, destCoords.second, originFallback)
    }
    val handleSetRealMode: (Boolean) -> Unit = { real ->
        isRealModeState = real
        credentialsManager.isRealMode = real
        highlightedDestinationItem = null
        DealAlertScheduler.syncSchedule(context)
        viewModel.syncPreferencesFromCredentials()
        viewModel.loadTop20WithCoordinates(currentCoords.first, currentCoords.second)
    }
    val refreshWithCurrentFilters: (String, Int, Int) -> Unit = { modeParam, paxParam, windowParam ->
        viewModel.loadTop20WithCoordinates(
            latitude = currentCoords.first,
            longitude = currentCoords.second,
            mode = modeParam,
            passengers = paxParam,
            stayMinHours = stayRange.start.toInt(),
            stayMaxHours = stayRange.endInclusive.toInt(),
            departureWindowHours = windowParam
        )
    }
    val forceRefreshSearch: () -> Unit = {
        isRouletteWheelSpinning = false
        highlightedDestinationItem = null
        refreshWithCurrentFilters(uiState.mode, uiState.passengers, uiState.departureWindowHours)
    }
    val triggerSpinSearch: () -> Unit = {
        isRouletteWheelSpinning = true
        if (viewModel.canInstantSpin(currentCoords.first, currentCoords.second, stayRange.start.toInt(), stayRange.endInclusive.toInt())) {
            spinEpoch++
        } else {
            refreshWithCurrentFilters(uiState.mode, uiState.passengers, uiState.departureWindowHours)
        }
    }
    val currentWinner = highlightedDestinationItem ?: uiState.displayResults.firstOrNull()

    val controlsBlock: @Composable () -> Unit = {
        OriginHubsCard(
            displayCity = uiState.originCity ?: city,
            isRealGps = deviceLocation?.isRealGps,
            isLocatingGps = isLocatingGps,
            hasLocationPermission = hasLocationPermissionState,
            onRefreshLocationClick = {
                if (hasLocationPermissionState) {
                    onRequestLocationRefresh()
                } else {
                    gpsPermissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                }
            },
            hubs = hubs,
            chainHistory = uiState.chainHistory
        )
        RouletteConfigCard(
            filtersExpanded = filtersExpanded,
            onToggleExpanded = { filtersExpanded = !filtersExpanded },
            mode = uiState.mode,
            onSelectMode = { selectedMode ->
                viewModel.setMode(selectedMode)
                refreshWithCurrentFilters(selectedMode, uiState.passengers, uiState.departureWindowHours)
            },
            stayRange = stayRange,
            onStayRangeChange = { stayRange = it },
            onStayRangeFinished = {
                viewModel.setStayDuration(stayRange.start.toInt(), stayRange.endInclusive.toInt())
                refreshWithCurrentFilters(uiState.mode, uiState.passengers, uiState.departureWindowHours)
            },
            passengers = uiState.passengers,
            onSelectPassengers = { count ->
                viewModel.setPassengers(count)
                refreshWithCurrentFilters(uiState.mode, count, uiState.departureWindowHours)
            },
            departureWindowHours = uiState.departureWindowHours,
            onSelectDepartureWindow = { hours ->
                viewModel.setDepartureWindowHours(hours)
                refreshWithCurrentFilters(uiState.mode, uiState.passengers, hours)
            },
            selectedTransportModes = uiState.selectedTransportModes,
            budget = uiState.budget,
            currencyRate = currencyRate,
            currencySymbol = currencySymbol,
            onBudgetChange = { newBudget -> viewModel.setBudget(newBudget) }
        )
        QuickStrategyBar(
            selectedSortOption = uiState.sortOption,
            onSelectSortOption = { viewModel.setSortOption(it) },
            budgetActive = budgetActive,
            budgetAmountDisplay = budgetAmountDisplay,
            currencySymbol = currencySymbol
        )
        RouletteWheelActionSection(
            items = uiState.displayResults,
            isLoading = uiState.isLoading,
            isSpinningState = isRouletteWheelSpinning,
            spinEpoch = spinEpoch,
            activeTransportIcon = uiState.activeTransportIcon,
            currencyCode = currencyCode,
            currencyRate = currencyRate,
            onSpinFinished = { selectedWinner ->
                isRouletteWheelSpinning = false
                highlightedDestinationItem = selectedWinner
            },
            onSpinClick = triggerSpinSearch
        )
    }

    val winnerBlock: @Composable (TravelIntelligenceEngine.ResultItem) -> Unit = { winner ->
        val winnerHistLow = uiState.historicalLowByHub[winner.destinationHub.uppercase()]
            ?: uiState.historicalLowByHub[HubCoordinates.getCity(winner.destinationHub).uppercase()]
        WinningRecommendationCard(
            item = winner,
            budget = uiState.budget,
            currencyCode = currencyCode,
            currencyRate = currencyRate,
            isRealMode = uiState.isRealMode,
            liveWeather = uiState.winningLiveWeather,
            attractions = uiState.winningAttractions,
            aiItinerary = uiState.winningItinerary,
            isLoadingItinerary = uiState.isLoadingItinerary,
            hasGeminiKey = credentialsManager.hasGemini(),
            destinationPhoto = uiState.winningPhoto,
            liveFlightRadar = uiState.winningFlightRadar,
            historicalLowEur = winnerHistLow,
            isBestValue = (uiState.bestValueItem == winner),
            onGenerateItinerary = { viewModel.generateAiItinerary(winner) },
            isSaved = savedTripsManager.isSaved(winner),
            onToggleSave = { savedTripsManager.toggleSave(winner) },
            isDesired = PreferenceHelper.isDestinationDesired(winner.destinationHub, favoriteDestinations),
            onToggleDesired = { handleToggleDesiredHub(winner.destinationHub) },
            onBook = { deepLink -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(deepLink))) },
            onChainTravel = handleChainTravel,
            onInviteFriends = { selectedItemForHub = it },
            onSpinAgain = triggerSpinSearch,
            onOpenLuggageChecklist = { showLuggageDialog = true },
            modifier = Modifier.padding(bottom = 4.dp)
        )
    }

    Scaffold(
        topBar = {
            MainTopBar(
                activeTransportIcon = uiState.activeTransportIcon,
                selectedTransportModes = uiState.selectedTransportModes,
                onToggleTransportMode = { mode ->
                    viewModel.toggleTransportMode(mode)
                    refreshWithCurrentFilters(uiState.mode, uiState.passengers, uiState.departureWindowHours)
                },
                isRealMode = isRealModeState,
                onToggleRealMode = { handleSetRealMode(!isRealModeState) },
                wishlistMatchCount = wishlistDeals.size,
                wishlistTotalCount = favoriteDestinations.size,
                onOpenWishlist = { showFavoriteTripDialog = true },
                savedTripsCount = savedTrips.size,
                onOpenSavedTrips = {
                    savedTripsManager.pruneExpiredTrips()
                    showSavedTripsDialog = true
                },
                isRadarEnabled = alertConfig.isEnabled,
                radarMatchCount = matchingDeals.size,
                onOpenPriceAlert = { showPriceAlertDialog = true },
                onOpenLuggage = { showLuggageDialog = true },
                onOpenApiWizard = { showApiWizardMain = true },
                onOpenManual = { showUserManualDialog = true },
                onOpenSettings = { showSettingsScreen = true }
            )
        }
    ) { innerPadding ->
        MainTravelAdaptiveBody(
            innerPadding = innerPadding,
            uiState = uiState,
            currentWinner = currentWinner,
            isRouletteWheelSpinning = isRouletteWheelSpinning,
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
            onToggleOnlyDeals = { onlyDealsFilter = !onlyDealsFilter },
            opportunitySearchQuery = opportunitySearchQuery,
            onOpportunitySearchQueryChange = { opportunitySearchQuery = it },
            onRefreshFares = forceRefreshSearch,
            onEnableAllTransports = {
                viewModel.setTransportModes(TravelViewModel.ALL_TRANSPORT_MODES)
                forceRefreshSearch()
            },
            onOpenWishlist = { showFavoriteTripDialog = true },
            onOpenWizard = { showApiWizardMain = true },
            onSwitchToDemo = { handleSetRealMode(false) },
            onRetry = { viewModel.loadTop20WithCoordinates(currentCoords.first, currentCoords.second) },
            onToggleDesiredHub = handleToggleDesiredHub,
            onChainTravel = handleChainTravel,
            onInviteFriends = { selectedItemForHub = it },
            onInspectOnWinner = { resultItem ->
                highlightedDestinationItem = resultItem
                viewModel.loadWinnerIntelligence(resultItem)
            },
            controlsBlock = controlsBlock,
            winnerBlock = winnerBlock
        )
    }

    MainDialogsHost(
        showSavedTripsDialog = showSavedTripsDialog,
        onDismissSavedTrips = { showSavedTripsDialog = false },
        savedTrips = savedTrips,
        savedTripsManager = savedTripsManager,
        showPriceAlertDialog = showPriceAlertDialog,
        onDismissPriceAlert = { showPriceAlertDialog = false },
        alertConfig = alertConfig,
        priceAlertManager = priceAlertManager,
        onRequestNotificationPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        selectedItemForHub = selectedItemForHub,
        onDismissTripHub = { selectedItemForHub = null },
        originCity = uiState.originCity ?: city,
        hubs = hubs,
        isRealMode = uiState.isRealMode,
        credentialsManager = credentialsManager,
        currencyCode = currencyCode,
        currencyRate = currencyRate,
        showApiWizardMain = showApiWizardMain,
        onDismissApiWizard = { showApiWizardMain = false },
        onApiWizardCompleted = {
            showApiWizardMain = false
            isRealModeState = credentialsManager.isRealMode
            highlightedDestinationItem = null
            DealAlertScheduler.syncSchedule(context)
            viewModel.syncPreferencesFromCredentials()
            viewModel.loadTop20WithCoordinates(currentCoords.first, currentCoords.second)
        },
        showLuggageDialog = showLuggageDialog,
        onDismissLuggage = { showLuggageDialog = false },
        luggageDestinationCity = highlightedDestinationItem?.destinationHub?.let { HubCoordinates.getCity(it) }
            ?: uiState.displayResults.firstOrNull()?.let { HubCoordinates.getCity(it.destinationHub) },
        winningLiveWeather = uiState.winningLiveWeather,
        showUserManualDialog = showUserManualDialog,
        onDismissUserManual = { showUserManualDialog = false },
        onOpenWizardFromManual = { showApiWizardMain = true },
        showFavoriteTripDialog = showFavoriteTripDialog,
        onDismissFavoriteTrip = { showFavoriteTripDialog = false },
        onOpenFavoriteTrip = { showFavoriteTripDialog = true },
        favoriteDestinations = favoriteDestinations,
        allResults = uiState.displayResults,
        historicalLowByHub = uiState.historicalLowByHub,
        onRefreshFavoriteSearch = { refreshWithCurrentFilters(uiState.mode, uiState.passengers, uiState.departureWindowHours) },
        onBookFavoriteOffer = { deepLink -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(deepLink))) }
    )
}
