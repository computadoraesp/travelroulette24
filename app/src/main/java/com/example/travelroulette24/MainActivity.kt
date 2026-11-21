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
import com.example.travelroulette24.ui.ValueComparisonSection
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

import com.example.travelroulette24.ui.MainTravelScreen
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
            val themeMode by PreferenceHelper.themeModeFlow.collectAsState()
            TravelRoulette24Theme(themeMode = themeMode) {
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

