package com.example.travelroulette24

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.travelroulette24.budget.BudgetGuardian
import com.example.travelroulette24.data.FavoritePriceHistoryDb
import com.example.travelroulette24.data.PriceAlertManager
import com.example.travelroulette24.data.SavedTripsManager
import com.example.travelroulette24.data.TravelRepository
import com.example.travelroulette24.data.byok.ApiCredentialsManager
import com.example.travelroulette24.data.byok.ApiQuotaGovernor
import com.example.travelroulette24.data.byok.ByokTravelService
import com.example.travelroulette24.location.LocalLocationHubResolver
import com.example.travelroulette24.ui.MainTravelScreen
import com.example.travelroulette24.ui.TravelViewModel
import com.example.travelroulette24.ui.theme.TravelRoulette24Theme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.delay
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
                fetchLocation(forceRefresh = true)
            } else {
                fetchApproximateOriginByIp()
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

    private val priceHistoryDb by lazy { FavoritePriceHistoryDb(applicationContext) }

    private val travelRepository by lazy {
        TravelRepository(
            api = null,
            apiKey = "",
            localResolver = locationResolver,
            byokService = byokTravelService,
            priceHistoryDb = priceHistoryDb
        )
    }

    private val travelViewModel by viewModels<TravelViewModel>(
        factoryProducer = {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TravelViewModel(
                        repository = travelRepository,
                        budgetController = budgetController,
                        byokService = byokTravelService,
                        priceHistoryDb = priceHistoryDb
                    ) as T
                }
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        seedInitialOrigin()

        val hasFine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            fetchApproximateOriginByIp()
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

    private val activeLocationCancellations = mutableListOf<CancellationSignal>()
    private var activeGmsTokenSource: CancellationTokenSource? = null

    private fun seedInitialOrigin() {
        if (_deviceLocationFlow.value != null) return
        val (city, lat, lon) = locationResolver.resolveSystemDefaultOrigin()
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
            isRealGps = false
        )
    }

    private fun fetchApproximateOriginByIp() {
        lifecycleScope.launch {
            val ipOrigin = locationResolver.detectApproximateOriginByIp()
            if (ipOrigin != null) {
                val (ipCity, lat, lon) = ipOrigin
                val current = _deviceLocationFlow.value
                // Only update if we don't already have an active hardware GPS fix
                if (current == null || !current.isRealGps) {
                    val hubs = locationResolver.findNearestHubs(
                        latitude = lat,
                        longitude = lon,
                        maxRadiusKm = credentialsManager.searchRadiusKm.toDouble()
                    )
                    PreferenceHelper.saveLastKnownOrigin(ipCity, lat, lon)
                    _deviceLocationFlow.value = DeviceLocationState(
                        city = ipCity,
                        hubs = hubs,
                        lat = lat,
                        lon = lon,
                        isRealGps = false
                    )
                }
            }
            _isLocatingFlow.value = false
        }
    }

    private fun fetchLocation(forceRefresh: Boolean = false) {
        if (!hasLocationPermission()) {
            fetchApproximateOriginByIp()
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

        // 2. Request live fixes in PARALLEL across Network + GPS + Fused (works indoors on Wi-Fi/Cell & outdoors on GPS, no GMS needed)
        try {
            if (locationManager != null) {
                activeLocationCancellations.forEach { runCatching { it.cancel() } }
                activeLocationCancellations.clear()

                val activeProviders = buildList {
                    if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        add(LocationManager.NETWORK_PROVIDER)
                    }
                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        add(LocationManager.GPS_PROVIDER)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        locationManager.isProviderEnabled(LocationManager.FUSED_PROVIDER)
                    ) {
                        add(LocationManager.FUSED_PROVIDER)
                    }
                }

                activeProviders.forEach { provider ->
                    try {
                        val signal = CancellationSignal()
                        activeLocationCancellations.add(signal)
                        @Suppress("MissingPermission")
                        locationManager.getCurrentLocation(
                            provider,
                            signal,
                            ContextCompat.getMainExecutor(this)
                        ) { loc ->
                            if (loc != null) {
                                acceptFix(loc)
                            }
                        }
                    } catch (_: Exception) {
                        // Continue with remaining providers
                    }
                }
            }
        } catch (_: Exception) {
            // Continue to GMS + IP fallback
        }

        // 3. Google Play Services FusedLocationProviderClient (both balanced Wi-Fi/Cell for tablets & high accuracy)
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
            fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, gmsToken.token)
                .addOnSuccessListener { loc ->
                    if (loc != null) acceptFix(loc)
                }
            @Suppress("MissingPermission")
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, gmsToken.token)
                .addOnSuccessListener { loc ->
                    if (loc != null) acceptFix(loc)
                }
        } catch (_: Exception) {
            // Device may lack Google Play Services (e.g. Huawei/AOSP/FireOS)
        }

        // 4. Universal fallback: if hardware GPS/Network providers haven't resolved within 2.2s, detect city via IP
        lifecycleScope.launch {
            delay(2200L)
            if (!receivedRealFix) {
                fetchApproximateOriginByIp()
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
            PreferenceHelper.saveLastKnownOrigin(city, lat, lon)
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

