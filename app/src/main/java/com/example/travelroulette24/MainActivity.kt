package com.example.travelroulette24

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.travelroulette24.budget.BudgetGuardian
import com.example.travelroulette24.data.PriceAlertManager
import com.example.travelroulette24.data.SavedTripsManager
import com.example.travelroulette24.data.TravelRepository
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import com.example.travelroulette24.location.LocalLocationHubResolver
import com.example.travelroulette24.data.byok.ApiCredentialsManager
import com.example.travelroulette24.data.byok.ApiQuotaGovernor
import com.example.travelroulette24.data.byok.ByokTravelService
import com.example.travelroulette24.ui.AnimatedRouletteWheel
import com.example.travelroulette24.ui.ApiWizardDialog
import com.example.travelroulette24.ui.HubCoordinates
import com.example.travelroulette24.ui.OpportunityCard
import com.example.travelroulette24.ui.PriceAlertDialog
import com.example.travelroulette24.ui.SavedTripsDialog
import com.example.travelroulette24.ui.SettingsScreen
import com.example.travelroulette24.ui.ExpressLuggageDialog
import com.example.travelroulette24.ui.ManualDialog
import com.example.travelroulette24.ui.TravelViewModel
import com.example.travelroulette24.ui.TripHubDialog
import com.example.travelroulette24.ui.WinningRecommendationCard
import com.example.travelroulette24.ui.theme.TravelRoulette24Theme
import com.example.travelroulette24.utils.CurrencyHelper
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val locationResolver by lazy { LocalLocationHubResolver(this) }
    private val credentialsManager by lazy { ApiCredentialsManager(applicationContext) }
    private val quotaGovernor by lazy { ApiQuotaGovernor(applicationContext) }
    private val byokTravelService by lazy { ByokTravelService(credentialsManager, quotaGovernor) }
    private val savedTripsManager by lazy { SavedTripsManager.getInstance(applicationContext) }
    private val priceAlertManager by lazy { PriceAlertManager.getInstance(applicationContext) }

    private val locationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            ) {
                fetchLocation()
            }
        }

    private class SharedPrefsBudgetStorage(private val context: Context) : BudgetGuardian.BudgetStorage {
        private val prefs = context.getSharedPreferences("travel_budget_prefs", Context.MODE_PRIVATE)
        override fun load(): String? = prefs.getString("budget_json", null)
        override fun save(json: String) {
            prefs.edit().putString("budget_json", json).apply()
        }
    }

    private val budgetController by lazy {
        BudgetGuardian.BudgetController(SharedPrefsBudgetStorage(applicationContext))
    }

    private val travelRepository by lazy {
        TravelRepository(
            api = null,
            apiKey = "",
            localResolver = locationResolver,
            byokService = byokTravelService
        )
    }

    private val travelViewModel by viewModels<TravelViewModel>(
        factoryProducer = {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TravelViewModel(travelRepository, budgetController, byokTravelService) as T
                }
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val hasFine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            fetchLocation()
        }

        setContent {
            TravelRoulette24Theme {
                MainTravelScreen(
                    viewModel = travelViewModel,
                    locationResolver = locationResolver,
                    credentialsManager = credentialsManager,
                    quotaGovernor = quotaGovernor,
                    byokTravelService = byokTravelService,
                    savedTripsManager = savedTripsManager,
                    priceAlertManager = priceAlertManager,
                    onRequestLocationRefresh = { fetchLocation(forceRefresh = true) }
                )
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        val hasFine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return hasFine || hasCoarse
    }

    private var activeLocationCancellation: CancellationSignal? = null
    private var activeGmsTokenSource: CancellationTokenSource? = null

    private fun fetchLocation(forceRefresh: Boolean = false) {
        if (!hasLocationPermission()) {
            _isLocatingFlow.value = false
            return
        }

        _isLocatingFlow.value = true
        var receivedRealFix = false

        fun acceptFix(location: Location) {
            if (!receivedRealFix || forceRefresh || location.accuracy <= 200f) {
                receivedRealFix = true
                _isLocatingFlow.value = false
                notifyLocation(location.latitude, location.longitude)
            }
        }

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        // 1. Immediate best cached fix from Android native LocationManager (zero GMS dependency)
        try {
            if (locationManager != null) {
                val providers = buildList {
                    add(LocationManager.GPS_PROVIDER)
                    add(LocationManager.NETWORK_PROVIDER)
                    add(LocationManager.PASSIVE_PROVIDER)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        add(LocationManager.FUSED_PROVIDER)
                    }
                }
                val bestLastKnown = providers.mapNotNull { provider ->
                    try {
                        if (locationManager.isProviderEnabled(provider) || provider == LocationManager.PASSIVE_PROVIDER) {
                            @Suppress("MissingPermission")
                            locationManager.getLastKnownLocation(provider)
                        } else null
                    } catch (_: Exception) {
                        null
                    }
                }.maxByOrNull { it.time }

                if (bestLastKnown != null) {
                    acceptFix(bestLastKnown)
                }
            }
        } catch (_: SecurityException) {
            // Permission revoked mid-flight
        } catch (_: Exception) {
            // Ignore provider errors
        }

        // 2. Request live GPS/Network fix via Android native LocationManager (API 30+, bypasses GMS/keystore issues)
        try {
            if (locationManager != null) {
                activeLocationCancellation?.cancel()
                val cancelSignal = CancellationSignal()
                activeLocationCancellation = cancelSignal

                val activeProvider = when {
                    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        locationManager.isProviderEnabled(LocationManager.FUSED_PROVIDER) -> LocationManager.FUSED_PROVIDER
                    else -> null
                }

                if (activeProvider != null) {
                    @Suppress("MissingPermission")
                    locationManager.getCurrentLocation(
                        activeProvider,
                        cancelSignal,
                        ContextCompat.getMainExecutor(this)
                    ) { loc ->
                        if (loc != null) {
                            acceptFix(loc)
                        } else {
                            _isLocatingFlow.value = false
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Continue to GMS fallback
        }

        // 3. Also query Google Play Services FusedLocationProviderClient (active fix + cached fallback)
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(this)
            @Suppress("MissingPermission")
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    acceptFix(loc)
                }
            }

            activeGmsTokenSource?.cancel()
            val gmsToken = CancellationTokenSource()
            activeGmsTokenSource = gmsToken
            @Suppress("MissingPermission")
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, gmsToken.token)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        acceptFix(loc)
                    } else if (!receivedRealFix) {
                        _isLocatingFlow.value = false
                    }
                }
                .addOnFailureListener {
                    if (!receivedRealFix) {
                        _isLocatingFlow.value = false
                    }
                }
        } catch (_: Exception) {
            if (!receivedRealFix) {
                _isLocatingFlow.value = false
            }
        }
    }

    private fun notifyLocation(lat: Double, lon: Double) {
        lifecycleScope.launch {
            val city = locationResolver.resolveCityFromCoordinates(lat, lon)
            val hubs = locationResolver.findNearestHubs(
                latitude = lat,
                longitude = lon,
                maxRadiusKm = credentialsManager.searchRadiusKm.toDouble()
            )
            _deviceLocationFlow.value = DeviceLocationState(
                city = city,
                hubs = hubs,
                lat = lat,
                lon = lon,
                isRealGps = true
            )
        }
    }

    data class DeviceLocationState(
        val city: String,
        val hubs: Map<String, List<String>>,
        val lat: Double,
        val lon: Double,
        val isRealGps: Boolean = true
    )

    companion object {
        private val _deviceLocationFlow = MutableStateFlow<DeviceLocationState?>(null)
        val deviceLocationFlow: StateFlow<DeviceLocationState?> = _deviceLocationFlow.asStateFlow()

        private val _isLocatingFlow = MutableStateFlow(false)
        val isLocatingFlow: StateFlow<Boolean> = _isLocatingFlow.asStateFlow()
    }
}

@Composable
fun getSortOptionLabel(option: TravelViewModel.SortOption): String {
    return when (option) {
        TravelViewModel.SortOption.CHEAPEST -> stringResource(R.string.sort_option_cheapest)
        TravelViewModel.SortOption.FURTHEST_DISTANCE -> stringResource(R.string.sort_option_furthest)
        TravelViewModel.SortOption.EARLIEST_DEPARTURE -> stringResource(R.string.sort_option_earliest)
    }
}

@Composable
fun getSortOptionDescription(option: TravelViewModel.SortOption): String {
    return when (option) {
        TravelViewModel.SortOption.CHEAPEST -> stringResource(R.string.sort_desc_cheapest)
        TravelViewModel.SortOption.FURTHEST_DISTANCE -> stringResource(R.string.sort_desc_furthest)
        TravelViewModel.SortOption.EARLIEST_DEPARTURE -> stringResource(R.string.sort_desc_earliest)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
        if (granted) {
            onRequestLocationRefresh()
        }
    }
    val deviceLocation by MainActivity.deviceLocationFlow.collectAsState()
    val isLocatingGps by MainActivity.isLocatingFlow.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val savedTrips by savedTripsManager.savedTripsFlow.collectAsState()
    val alertConfig by priceAlertManager.configFlow.collectAsState()
    val defaultCityParis = stringResource(R.string.default_city_paris)
    val originFallback = stringResource(R.string.origin_fallback)

    var city by remember { mutableStateOf(defaultCityParis) }
    var hubs by remember {
        mutableStateOf<Map<String, List<String>>>(
            mapOf(
                "airports" to listOf("CDG", "ORY"),
                "trainStations" to listOf("GDL"),
                "ports" to listOf("Bercy")
            )
        )
    }
    var currentCoords by remember { mutableStateOf(Pair(48.8566, 2.3522)) }
    var selectedItemForHub by remember { mutableStateOf<TravelIntelligenceEngine.ResultItem?>(null) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var showSavedTripsDialog by remember { mutableStateOf(false) }
    var showPriceAlertDialog by remember { mutableStateOf(false) }
    var onlyDealsFilter by remember { mutableStateOf(false) }
    var budgetActive by remember { mutableStateOf(uiState.budget?.enabled ?: false) }
    var budgetAmount by remember { mutableFloatStateOf(uiState.budget?.maxPerTrip?.toFloat() ?: 100f) }
    var budgetInputText by remember { mutableStateOf((uiState.budget?.maxPerTrip?.toInt() ?: 100).toString()) }
    var stayRange by remember { mutableStateOf(24f..72f) }
    var showApiWizardMain by remember { mutableStateOf(false) }
    var isRouletteWheelSpinning by remember { mutableStateOf(false) }
    var highlightedDestinationItem by remember { mutableStateOf<TravelIntelligenceEngine.ResultItem?>(null) }
    var isRealModeState by remember { mutableStateOf(credentialsManager.isRealMode) }
    var showLuggageDialog by remember { mutableStateOf(false) }
    var showUserManualDialog by remember { mutableStateOf(false) }
    val luckyIcons = remember { listOf("🍀", "✈️", "🚀", "🧭", "⭐", "🎲") }
    var luckyIcon by remember { mutableStateOf(PreferenceHelper.getLuckyIcon()) }
    val changeLuckyIconCd = stringResource(R.string.lucky_icon_cd)

    val currencyCode = uiState.currency
    val currencyRate = uiState.currencyRate
    val currencySymbol = CurrencyHelper.symbolFor(currencyCode)

    val matchingDeals = remember(uiState.displayResults, alertConfig) {
        priceAlertManager.getMatchingDeals(uiState.displayResults, alertConfig)
    }

    // Observe reactive GPS updates (survives race conditions if location resolves before composition)
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

    // Load live Open-Meteo weather / OpenTripMap attractions when winning pick or mode changes
    val activeWinnerForEffect = highlightedDestinationItem ?: uiState.displayResults.firstOrNull()
    LaunchedEffect(activeWinnerForEffect?.destinationHub, uiState.isRealMode) {
        viewModel.loadWinnerIntelligence(activeWinnerForEffect)
    }

    if (showSettingsScreen) {
        SettingsScreen(
            credentialsManager = credentialsManager,
            quotaGovernor = quotaGovernor,
            byokTravelService = byokTravelService,
            onBack = {
                isRealModeState = credentialsManager.isRealMode
                viewModel.syncPreferencesFromCredentials()
                hubs = locationResolver.findNearestHubs(
                    currentCoords.first,
                    currentCoords.second,
                    maxRadiusKm = credentialsManager.searchRadiusKm.toDouble()
                )
                showSettingsScreen = false
                val nowHasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                hasLocationPermissionState = nowHasPerm
                if (nowHasPerm) {
                    onRequestLocationRefresh()
                }
            },
            onSaved = {
                isRealModeState = credentialsManager.isRealMode
                viewModel.syncPreferencesFromCredentials()
                hubs = locationResolver.findNearestHubs(
                    currentCoords.first,
                    currentCoords.second,
                    maxRadiusKm = credentialsManager.searchRadiusKm.toDouble()
                )
                highlightedDestinationItem = null
                viewModel.loadTop20WithCoordinates(currentCoords.first, currentCoords.second)
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    val nextIdx = (luckyIcons.indexOf(luckyIcon) + 1) % luckyIcons.size
                                    val next = luckyIcons[nextIdx]
                                    luckyIcon = next
                                    PreferenceHelper.setLuckyIcon(next)
                                }
                                .semantics { contentDescription = changeLuckyIconCd }
                                .padding(4.dp)
                                .testTag("lucky_icon_selector")
                        ) {
                            Text(luckyIcon, fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    // Compact Mode Pill: clicks toggle Real/Demo cleanly without Demo/Real data mixing
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isRealModeState) MaterialTheme.colorScheme.tertiaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                val next = !isRealModeState
                                isRealModeState = next
                                credentialsManager.isRealMode = next
                                highlightedDestinationItem = null
                                viewModel.syncPreferencesFromCredentials()
                                viewModel.loadTop20WithCoordinates(currentCoords.first, currentCoords.second)
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("toggle_real_mode_chip"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRealModeState) stringResource(R.string.mode_real_badge)
                            else stringResource(R.string.mode_demo_badge),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isRealModeState) MaterialTheme.colorScheme.onTertiaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // ⭐ Saved Trips Button with Count Badge
                    Box {
                        IconButton(
                            onClick = { showSavedTripsDialog = true },
                            modifier = Modifier.testTag("open_saved_trips_button")
                        ) {
                            Text("⭐", fontSize = 20.sp)
                        }
                        if (savedTrips.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 4.dp, end = 4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "${savedTrips.size}",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 🔔 Price Alert Radar Button with Deal Indicator Dot
                    Box {
                        IconButton(
                            onClick = { showPriceAlertDialog = true },
                            modifier = Modifier.testTag("open_price_alert_button")
                        ) {
                            Text("🔔", fontSize = 20.sp)
                        }
                        if (alertConfig.isEnabled && matchingDeals.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 6.dp, end = 6.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                            )
                        }
                    }

                    IconButton(
                        onClick = { showLuggageDialog = true },
                        modifier = Modifier.testTag("open_luggage_checklist_topbar_button")
                    ) {
                        Text("🎒", fontSize = 20.sp)
                    }

                    IconButton(
                        onClick = { showUserManualDialog = true },
                        modifier = Modifier.testTag("open_user_manual_topbar_button")
                    ) {
                        Text("📖", fontSize = 20.sp)
                    }

                    IconButton(
                        onClick = { showApiWizardMain = true },
                        modifier = Modifier.testTag("open_api_wizard_header_button")
                    ) {
                        Text("🧙", fontSize = 20.sp)
                    }

                    IconButton(
                        onClick = { showSettingsScreen = true },
                        modifier = Modifier.testTag("open_settings_button")
                    ) {
                        Text("⚙️", fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Current Origin and Hubs Card
            item {
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
                                text = stringResource(R.string.origin_label_format, uiState.originCity ?: city),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (hasLocationPermissionState) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                        else MaterialTheme.colorScheme.errorContainer
                                    )
                                    .clickable {
                                        if (hasLocationPermissionState) {
                                            onRequestLocationRefresh()
                                        } else {
                                            gpsPermissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("refresh_gps_button")
                            ) {
                                Text(
                                    text = when {
                                        isLocatingGps -> stringResource(R.string.gps_locating)
                                        hasLocationPermissionState -> stringResource(R.string.gps_auto)
                                        else -> stringResource(R.string.gps_permission_needed)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (hasLocationPermissionState) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val airportList = hubs["airports"].orEmpty()
                        val trainList = hubs["trainStations"].orEmpty()
                        val portList = hubs["ports"].orEmpty()

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (airportList.isNotEmpty()) {
                                HubPill(icon = "✈️", hubs = airportList.joinToString(","))
                            }
                            if (trainList.isNotEmpty()) {
                                HubPill(icon = "🚆", hubs = trainList.joinToString(","))
                            }
                            if (portList.isNotEmpty()) {
                                HubPill(icon = "⛴️", hubs = portList.joinToString(","))
                            }
                        }

                        if (uiState.chainHistory.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val fullChain = "${uiState.chainHistory.joinToString(" ➔ ")} ➔ ${uiState.originCity ?: city}"
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

            // 2. Roulette Controls Card
            item {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.roulette_config_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.badge_leave_tomorrow),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Explanatory banner: departure is strictly <24h, trip characteristics are user-defined
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🚀", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.express_departure_24h_title),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = stringResource(R.string.express_departure_24h_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Mode selector: One-Way (Chaining) vs Round-Trip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = uiState.mode == TravelViewModel.MODE_ONE_WAY,
                                onClick = {
                                    viewModel.setMode(TravelViewModel.MODE_ONE_WAY)
                                    viewModel.loadTop20WithCoordinates(currentCoords.first, currentCoords.second, TravelViewModel.MODE_ONE_WAY)
                                },
                                label = { Text(stringResource(R.string.chip_mode_one_way)) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )

                            FilterChip(
                                selected = uiState.mode == TravelViewModel.MODE_ROUND_TRIP,
                                onClick = {
                                    viewModel.setMode(TravelViewModel.MODE_ROUND_TRIP)
                                    viewModel.loadTop20WithCoordinates(
                                        currentCoords.first,
                                        currentCoords.second,
                                        TravelViewModel.MODE_ROUND_TRIP,
                                        stayMinHours = stayRange.start.toInt(),
                                        stayMaxHours = stayRange.endInclusive.toInt()
                                    )
                                },
                                label = { Text(stringResource(R.string.chip_mode_round_trip)) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }

                        // Round trip stay window slider
                        if (uiState.mode == TravelViewModel.MODE_ROUND_TRIP) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.stay_desired_format, stayRange.start.toInt(), stayRange.endInclusive.toInt()),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            RangeSlider(
                                value = stayRange,
                                onValueChange = { stayRange = it },
                                valueRange = 12f..120f,
                                onValueChangeFinished = {
                                    viewModel.setStayDuration(stayRange.start.toInt(), stayRange.endInclusive.toInt())
                                    viewModel.loadTop20WithCoordinates(
                                        currentCoords.first,
                                        currentCoords.second,
                                        stayMinHours = stayRange.start.toInt(),
                                        stayMaxHours = stayRange.endInclusive.toInt()
                                    )
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Passengers selector
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
                                        selected = uiState.passengers == count,
                                        onClick = {
                                            viewModel.setPassengers(count)
                                            viewModel.loadTop20WithCoordinates(
                                                currentCoords.first,
                                                currentCoords.second,
                                                passengers = count
                                            )
                                        },
                                        label = { Text("$count") }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Transport Mode Filter: All, Flight, Train, Boat
                        Text(
                            text = stringResource(R.string.transport_mode_selector_label),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "all" to stringResource(R.string.chip_transport_all),
                                "flight" to stringResource(R.string.chip_transport_flights),
                                "train" to stringResource(R.string.chip_transport_trains),
                                "ferry" to stringResource(R.string.chip_transport_ferries)
                            ).forEach { (modeKey, label) ->
                                val isSelected = uiState.selectedTransportFilter == modeKey
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setTransportFilter(modeKey) },
                                    label = { Text(label, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Budget Guardian Filter: Free option vs Filtered option (10 to 5000 with Slider + Direct input field)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.budget_guardian_label),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (budgetActive) stringResource(R.string.budget_active_currency_format, currencySymbol, budgetAmount.toInt())
                                    else stringResource(R.string.budget_free_mode),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (budgetActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (budgetActive) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                            Switch(
                                checked = budgetActive,
                                onCheckedChange = { active ->
                                    budgetActive = active
                                    val newBudget = if (active) {
                                        BudgetGuardian.Budget(
                                            maxPerTrip = budgetAmount.toDouble(),
                                            maxPerRoundTrip = budgetAmount.toDouble() * 1.8,
                                            maxChainStep = budgetAmount.toDouble(),
                                            enabled = true
                                        )
                                    } else null
                                    viewModel.setBudget(newBudget)
                                }
                            )
                        }

                        if (budgetActive) {
                            Spacer(modifier = Modifier.height(10.dp))

                            // Direct Text Input Field + Current Amount Display
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = budgetInputText,
                                    onValueChange = { input ->
                                        val filteredDigits = input.filter { it.isDigit() }.take(5)
                                        budgetInputText = filteredDigits
                                        val parsed = filteredDigits.toFloatOrNull()
                                        if (parsed != null) {
                                             val clamped = parsed.coerceIn(10f, 5000f)
                                             budgetAmount = clamped
                                             val b = BudgetGuardian.Budget(
                                                 maxPerTrip = clamped.toDouble(),
                                                 maxPerRoundTrip = clamped.toDouble() * 1.8,
                                                 maxChainStep = clamped.toDouble(),
                                                 enabled = true
                                             )
                                             viewModel.setBudget(b)
                                        }
                                    },
                                    label = { Text(stringResource(R.string.budget_input_field_label)) },
                                    prefix = { Text("$currencySymbol ", fontWeight = FontWeight.Bold) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("budget_amount_input")
                                )

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$currencySymbol${budgetAmount.toInt()}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = stringResource(R.string.budget_range_min_max_hint),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Continuous Slider between 10 and 5000
                            Slider(
                                value = budgetAmount.coerceIn(10f, 5000f),
                                onValueChange = { newVal ->
                                    budgetAmount = newVal
                                    budgetInputText = newVal.toInt().toString()
                                },
                                onValueChangeFinished = {
                                    val b = BudgetGuardian.Budget(
                                        maxPerTrip = budgetAmount.toDouble(),
                                        maxPerRoundTrip = budgetAmount.toDouble() * 1.8,
                                        maxChainStep = budgetAmount.toDouble(),
                                        enabled = true
                                    )
                                    viewModel.setBudget(b)
                                },
                                valueRange = 10f..5000f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("budget_slider")
                            )

                            // Min / Max labels
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "10 $currencySymbol",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "5000 $currencySymbol",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Quick preset shortcut chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(50f, 100f, 250f, 500f, 1000f).forEach { limit ->
                                    FilterChip(
                                        selected = budgetAmount.toInt() == limit.toInt(),
                                        onClick = {
                                            budgetAmount = limit
                                            budgetInputText = limit.toInt().toString()
                                            val b = BudgetGuardian.Budget(
                                                maxPerTrip = limit.toDouble(),
                                                maxPerRoundTrip = limit.toDouble() * 1.8,
                                                maxChainStep = limit.toDouble(),
                                                enabled = true
                                            )
                                            viewModel.setBudget(b)
                                        },
                                        label = { Text("$currencySymbol${limit.toInt()}") }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Animated Roulette Wheel & Spin Action Button
            item {
                AnimatedRouletteWheel(
                    items = uiState.displayResults,
                    isSpinning = uiState.isLoading || isRouletteWheelSpinning,
                    luckyIcon = luckyIcon,
                    currencyCode = currencyCode,
                    currencyRate = currencyRate,
                    onSpinFinished = { selectedWinner ->
                        isRouletteWheelSpinning = false
                        highlightedDestinationItem = selectedWinner
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val infiniteTransition = rememberInfiniteTransition(label = "spin_transition")
                val spinAngle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 800, easing = LinearEasing)
                    ),
                    label = "spin_angle"
                )

                Button(
                    onClick = {
                        isRouletteWheelSpinning = true
                        viewModel.loadTop20WithCoordinates(
                            latitude = currentCoords.first,
                            longitude = currentCoords.second,
                            stayMinHours = stayRange.start.toInt(),
                            stayMaxHours = stayRange.endInclusive.toInt()
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("spin_roulette_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = !uiState.isLoading && !isRouletteWheelSpinning
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .rotate(if (uiState.isLoading || isRouletteWheelSpinning) spinAngle else 0f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = luckyIcon, fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (uiState.isLoading || isRouletteWheelSpinning) stringResource(R.string.btn_spinning)
                            else stringResource(R.string.btn_spin_roulette),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // 4. Winning Recommendation Card (Tarjeta Ganadora Animada con celebración)
            val currentWinner = highlightedDestinationItem ?: uiState.displayResults.firstOrNull()
            if (currentWinner != null && !uiState.isLoading && !isRouletteWheelSpinning) {
                item {
                    WinningRecommendationCard(
                        item = currentWinner,
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
                        onGenerateItinerary = { viewModel.generateAiItinerary(currentWinner) },
                        isSaved = savedTripsManager.isSaved(currentWinner),
                        onToggleSave = { savedTripsManager.toggleSave(currentWinner) },
                        onBook = { deepLink ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink))
                            context.startActivity(intent)
                        },
                        onChainTravel = { destinationHub ->
                            val destCoords = HubCoordinates.getCoordinates(destinationHub)
                            val destCity = HubCoordinates.getCity(destinationHub)
                            currentCoords = destCoords
                            city = destCity
                            hubs = locationResolver.findNearestHubs(
                                destCoords.first,
                                destCoords.second,
                                maxRadiusKm = credentialsManager.searchRadiusKm.toDouble()
                            )
                            viewModel.chainTravel(destCity, destCoords.first, destCoords.second, originFallback)
                        },
                        onInviteFriends = { item ->
                            selectedItemForHub = item
                        },
                        onSpinAgain = {
                            isRouletteWheelSpinning = true
                            viewModel.loadTop20WithCoordinates(
                                latitude = currentCoords.first,
                                longitude = currentCoords.second,
                                stayMinHours = stayRange.start.toInt(),
                                stayMaxHours = stayRange.endInclusive.toInt()
                            )
                        },
                        onOpenLuggageChecklist = {
                            showLuggageDialog = true
                        },
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            // 5. Selection Criteria (El más barato vs El más lejos por Km vs Salida inmediata)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.selection_criteria_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (budgetActive) stringResource(R.string.budget_active_currency_format, currencySymbol, budgetAmount.toInt())
                                else stringResource(R.string.budget_free_mode),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TravelViewModel.SortOption.values().forEach { option ->
                                FilterChip(
                                    selected = uiState.sortOption == option,
                                    onClick = { viewModel.setSortOption(option) },
                                    label = { Text("${option.icon} ${getSortOptionLabel(option)}", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        val budgetSuffix = if (budgetActive) stringResource(R.string.selection_desc_budget_currency, currencySymbol, budgetAmount.toInt())
                        else stringResource(R.string.selection_criteria_desc_free)
                        Text(
                            text = "${getSortOptionDescription(uiState.sortOption)}$budgetSuffix",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 5. Opportunity Alerts Banner
            if (uiState.alerts.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = stringResource(R.string.alert_historical_low_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            uiState.alerts.take(2).forEach { alert ->
                                Text(
                                    text = "• $alert",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // 6. Loading Indicator or Error or Results
            if (uiState.isLoading) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
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
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.error_obtaining_trips),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = uiState.error?.takeIf { it.isNotBlank() } ?: stringResource(R.string.error_unknown),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    viewModel.loadTop20WithCoordinates(currentCoords.first, currentCoords.second)
                                }
                            ) {
                                Text(stringResource(R.string.btn_retry))
                            }
                        }
                    }
                }
            } else if (uiState.results.isEmpty()) {
                item {
                    if (uiState.isRealMode) {
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("real_mode_empty_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🛰️", fontSize = 40.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = stringResource(R.string.real_empty_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.real_empty_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { showApiWizardMain = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.real_empty_open_wizard), fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        credentialsManager.isRealMode = false
                                        isRealModeState = false
                                        highlightedDestinationItem = null
                                        viewModel.syncPreferencesFromCredentials()
                                        viewModel.loadTop20WithCoordinates(currentCoords.first, currentCoords.second)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.real_empty_switch_demo))
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
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
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.order_label_format, getSortOptionLabel(uiState.sortOption)),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Transport Quick Filter Chips (Todos | Vuelos | Trenes | Ferries)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "all" to stringResource(R.string.chip_transport_all),
                            "flight" to stringResource(R.string.chip_transport_flights),
                            "train" to stringResource(R.string.chip_transport_trains),
                            "ferry" to stringResource(R.string.chip_transport_ferries)
                        ).forEach { (modeKey, label) ->
                            FilterChip(
                                selected = uiState.selectedTransportFilter == modeKey,
                                onClick = { viewModel.setTransportFilter(modeKey) },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 🔔 Bargain Deal Alert Banner (Radar de Alertas de Chollo)
                if (alertConfig.isEnabled && matchingDeals.isNotEmpty()) {
                    item {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🔔", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.bargain_alert_banner_title),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        Text(
                                            text = stringResource(
                                                R.string.bargain_alert_currency_desc,
                                                matchingDeals.size,
                                                currencySymbol,
                                                alertConfig.maxPriceEur.toInt()
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                FilterChip(
                                    selected = onlyDealsFilter,
                                    onClick = { onlyDealsFilter = !onlyDealsFilter },
                                    label = {
                                        Text(
                                            text = if (onlyDealsFilter) stringResource(R.string.bargain_filter_all) else stringResource(R.string.bargain_filter_view_deals),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                val displayedOpportunities = if (onlyDealsFilter && alertConfig.isEnabled) matchingDeals else uiState.displayResults

                // Render each opportunity card
                items(
                    items = displayedOpportunities,
                    key = { "${it.originHub}-${it.destinationHub}-${it.departureTime}-${it.price}" }
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
                                .border(
                                    width = 2.5.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                                .padding(4.dp)
                        } else Modifier
                    ) {
                        if (isWinningPick) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
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
                            isBargainDeal = priceAlertManager.matchesAlert(resultItem, alertConfig),
                            onChainTravel = { destinationHub ->
                                val destCoords = HubCoordinates.getCoordinates(destinationHub)
                                val destCity = HubCoordinates.getCity(destinationHub)
                                currentCoords = destCoords
                                city = destCity
                                hubs = locationResolver.findNearestHubs(
                                    destCoords.first,
                                    destCoords.second,
                                    maxRadiusKm = credentialsManager.searchRadiusKm.toDouble()
                                )
                                viewModel.chainTravel(destCity, destCoords.first, destCoords.second, originFallback)
                            },
                            onInviteFriends = { item ->
                                selectedItemForHub = item
                            }
                        )
                    }
                }
            }
        }
    }

    // ⭐ Saved Trips Dialog
    if (showSavedTripsDialog) {
        SavedTripsDialog(
            savedTrips = savedTrips,
            currencyCode = currencyCode,
            currencyRate = currencyRate,
            onRemoveTrip = { item -> savedTripsManager.removeTrip(item) },
            onDismiss = { showSavedTripsDialog = false }
        )
    }

    // 🔔 Price Alert Radar Dialog
    if (showPriceAlertDialog) {
        PriceAlertDialog(
            currentConfig = alertConfig,
            currencyCode = currencyCode,
            onSaveConfig = { newConfig ->
                priceAlertManager.updateConfig(newConfig)
                if (newConfig.isEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            },
            onDismiss = { showPriceAlertDialog = false }
        )
    }

    // Trip Hub Dialog (for inviting friends & QR code)
    selectedItemForHub?.let { item ->
        TripHubDialog(
            item = item,
            originCity = uiState.originCity ?: city,
            hubs = hubs,
            isRealMode = uiState.isRealMode,
            tripHubBaseUrl = credentialsManager.tripHubUrl,
            currencyCode = currencyCode,
            currencyRate = currencyRate,
            onDismiss = { selectedItemForHub = null }
        )
    }

    // Step-by-Step API Key Wizard Dialog
    if (showApiWizardMain) {
        ApiWizardDialog(
            credentialsManager = credentialsManager,
            onDismiss = { showApiWizardMain = false },
            onCompleted = {
                showApiWizardMain = false
                isRealModeState = credentialsManager.isRealMode
                highlightedDestinationItem = null
                viewModel.syncPreferencesFromCredentials()
                viewModel.loadTop20WithCoordinates(currentCoords.first, currentCoords.second)
            }
        )
    }

    // Express Luggage 24h Checklist Modal Dialog
    if (showLuggageDialog) {
        val destCity = highlightedDestinationItem?.destinationHub?.let { HubCoordinates.getCity(it) }
            ?: uiState.displayResults.firstOrNull()?.let { HubCoordinates.getCity(it.destinationHub) }
        ExpressLuggageDialog(
            destinationCity = destCity,
            destinationWeather = uiState.winningLiveWeather,
            onDismiss = { showLuggageDialog = false }
        )
    }

    // 📖 Interactive User Manual Dialog
    if (showUserManualDialog) {
        ManualDialog(
            onDismiss = { showUserManualDialog = false },
            onOpenWizard = { showApiWizardMain = true }
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
