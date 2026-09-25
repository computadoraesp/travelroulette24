package com.example.travelroulette24

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import com.example.travelroulette24.budget.BudgetGuardian
import com.example.travelroulette24.data.NetworkModule
import com.example.travelroulette24.data.TravelRepository
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import com.example.travelroulette24.location.LocalLocationHubResolver
import com.example.travelroulette24.data.byok.ApiCredentialsManager
import com.example.travelroulette24.data.byok.ApiQuotaGovernor
import com.example.travelroulette24.data.byok.ByokTravelService
import com.example.travelroulette24.ui.ApiSettingsDialog
import com.example.travelroulette24.ui.AnimatedRouletteWheel
import com.example.travelroulette24.ui.ApiWizardDialog
import com.example.travelroulette24.ui.HubCoordinates
import com.example.travelroulette24.ui.OpportunityCard
import com.example.travelroulette24.ui.SettingsScreen
import com.example.travelroulette24.ui.TravelViewModel
import com.example.travelroulette24.ui.TripHubDialog
import com.example.travelroulette24.ui.WinningRecommendationCard
import com.example.travelroulette24.ui.theme.TravelRoulette24Theme
import com.google.android.gms.location.LocationServices

class MainActivity : ComponentActivity() {

    private val locationResolver by lazy { LocalLocationHubResolver(this) }
    private val credentialsManager by lazy { ApiCredentialsManager(applicationContext) }
    private val quotaGovernor by lazy { ApiQuotaGovernor(applicationContext) }
    private val byokTravelService by lazy { ByokTravelService(credentialsManager, quotaGovernor) }

    private val locationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
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
            api = NetworkModule.createApi(),
            apiKey = "tr24_mobile_client",
            localResolver = locationResolver,
            byokService = byokTravelService
        )
    }

    private val travelViewModel by viewModels<TravelViewModel>(
        factoryProducer = {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TravelViewModel(travelRepository, budgetController) as T
                }
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        PreferenceHelper.init(this)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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
                    byokTravelService = byokTravelService
                )
            }
        }
    }

    private fun fetchLocation() {
        try {
            val hasFine = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasFine && !hasCoarse) {
                notifyLocation(48.8566, 2.3522)
                return
            }

            val fusedClient = LocationServices.getFusedLocationProviderClient(this)
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    notifyLocation(loc.latitude, loc.longitude)
                } else {
                    notifyLocation(48.8566, 2.3522)
                }
            }.addOnFailureListener {
                notifyLocation(48.8566, 2.3522)
            }
        } catch (_: SecurityException) {
            notifyLocation(48.8566, 2.3522)
        }
    }

    private fun notifyLocation(lat: Double, lon: Double) {
        locationResolver.getCityFromCoordinates(lat, lon) { city ->
            val hubs = locationResolver.findNearestHubs(lat, lon)
            locationCallback?.invoke(city, hubs, lat, lon)
        }
    }

    companion object {
        var locationCallback: ((String, Map<String, List<String>>, Double, Double) -> Unit)? = null
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainTravelScreen(
    viewModel: TravelViewModel,
    locationResolver: LocalLocationHubResolver,
    credentialsManager: ApiCredentialsManager,
    quotaGovernor: ApiQuotaGovernor? = null,
    byokTravelService: ByokTravelService? = null
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var city by remember { mutableStateOf("París") }
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
    var budgetActive by remember { mutableStateOf(uiState.budget?.enabled ?: false) }
    var budgetAmount by remember { mutableFloatStateOf(uiState.budget?.maxPerTrip?.toFloat() ?: 100f) }
    var budgetInputText by remember { mutableStateOf((uiState.budget?.maxPerTrip?.toInt() ?: 100).toString()) }
    var stayRange by remember { mutableStateOf(24f..72f) }
    var showApiWizardMain by remember { mutableStateOf(false) }
    var isRouletteWheelSpinning by remember { mutableStateOf(false) }
    var highlightedDestinationItem by remember { mutableStateOf<TravelIntelligenceEngine.ResultItem?>(null) }
    var isRealModeState by remember { mutableStateOf(credentialsManager.isRealMode) }

    // Register callback for geolocation updates
    LaunchedEffect(Unit) {
        MainActivity.locationCallback = { resolvedCity, resolvedHubs, lat, lon ->
            city = resolvedCity
            hubs = resolvedHubs
            currentCoords = Pair(lat, lon)
            viewModel.loadTop20WithCoordinates(lat, lon)
        }
        // Initial auto-spin if not already loaded
        if (uiState.results.isEmpty() && !uiState.isLoading) {
            viewModel.loadTop20WithCoordinates(currentCoords.first, currentCoords.second)
        }
    }

    if (showSettingsScreen) {
        SettingsScreen(
            credentialsManager = credentialsManager,
            quotaGovernor = quotaGovernor,
            byokTravelService = byokTravelService,
            onBack = {
                isRealModeState = credentialsManager.isRealMode
                showSettingsScreen = false
            },
            onSaved = {
                isRealModeState = credentialsManager.isRealMode
                viewModel.loadTop20WithCoordinates(currentCoords.first, currentCoords.second)
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎰", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "TravelRoulette24",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Salida express < 24h • ¡Salimos mañana!",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showApiWizardMain = true },
                        modifier = Modifier.testTag("open_api_wizard_header_button")
                    ) {
                        Text(
                            text = "🧙",
                            fontSize = 20.sp
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isRealModeState) MaterialTheme.colorScheme.tertiaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                val next = !isRealModeState
                                isRealModeState = next
                                credentialsManager.isRealMode = next
                                viewModel.loadTop20WithCoordinates(currentCoords.first, currentCoords.second)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("toggle_real_mode_chip"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isRealModeState) "🟢 Modo Real" else "⚪ Modo Demo",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isRealModeState) MaterialTheme.colorScheme.onTertiaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { showSettingsScreen = true },
                        modifier = Modifier.testTag("open_settings_button")
                    ) {
                        Text(
                            text = "⚙️",
                            fontSize = 20.sp
                        )
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
                                text = "📍 Origen: ${uiState.originCity ?: city}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "GPS Automático",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
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

                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("main_mode_status_card"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isRealModeState) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (isRealModeState) credentialsManager.getOverallStatusTitle()
                                                else "⚪ Modo Demo / Simulación",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isRealModeState) MaterialTheme.colorScheme.tertiary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "• ⚙️",
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable { showSettingsScreen = true }
                                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Text(
                                            text = if (isRealModeState) "Búsquedas reales activas. Enlaces a reservas oficiales."
                                            else "Modo simulación. Activa el interruptor para pasar a Real.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isRealModeState) "REAL" else "DEMO",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isRealModeState) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Switch(
                                            checked = isRealModeState,
                                            onCheckedChange = { checked ->
                                                isRealModeState = checked
                                                credentialsManager.isRealMode = checked
                                                viewModel.loadTop20WithCoordinates(currentCoords.first, currentCoords.second)
                                            },
                                            modifier = Modifier.testTag("home_real_mode_switch")
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "✈️ Avión: ${credentialsManager.getFlightStatusLabel()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isRealModeState) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "🚆 Tren: ${credentialsManager.getTrainStatusLabel()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isRealModeState) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "🚢 Barco: ${credentialsManager.getBoatStatusLabel()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isRealModeState) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (uiState.chainHistory.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Cadena de viajes: ${uiState.chainHistory.joinToString(" ➔ ")} ➔ ${uiState.originCity ?: city}",
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
                                text = "Configuración de la Ruleta",
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
                                    text = "⚡ Salimos mañana",
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
                                        text = "Salida express en menos de 24 horas",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "El viaje sale mañana. El tipo de viaje (solo ida o estancia deseada) lo defines tú con los filtros de abajo.",
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
                                label = { Text("✈️ Solo ida (Chaining)") },
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
                                label = { Text("🔄 Ida y vuelta") },
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
                                text = "Estancia deseada: ${stayRange.start.toInt()}h a ${stayRange.endInclusive.toInt()}h",
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
                                text = "Pasajeros:",
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
                            text = "Medio de transporte en ruleta:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "all" to "🌐 Todos",
                                "flight" to "✈️ Vuelos",
                                "train" to "🚆 Trenes",
                                "ferry" to "🚢 Barcos"
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
                                    text = "🛡️ Guardián de Presupuesto",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (budgetActive) "Filtro activo: hasta €${budgetAmount.toInt()}" else "Opción libre (sin límite de precio)",
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
                                    label = { Text("Monto límite (€ / $)") },
                                    prefix = { Text("€ ", fontWeight = FontWeight.Bold) },
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
                                        text = "€${budgetAmount.toInt()}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Rango 10€ - 5000€",
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
                                    text = "10 €",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "5000 €",
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
                                        label = { Text("€${limit.toInt()}") }
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
                            Text(text = "🎰", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (uiState.isLoading || isRouletteWheelSpinning) "¡Girando la ruleta...!" else "GIRAR RULETA DE VIAJES",
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
                        onBook = { deepLink ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink))
                            context.startActivity(intent)
                        },
                        onChainTravel = { destinationHub ->
                            val destCoords = HubCoordinates.getCoordinates(destinationHub)
                            val destCity = HubCoordinates.getCity(destinationHub)
                            currentCoords = destCoords
                            city = destCity
                            viewModel.chainTravel(destCity, destCoords.first, destCoords.second)
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
                                text = "🎯 Criterio de Selección",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (budgetActive) "Filtro: máx €${budgetAmount.toInt()}" else "Opción Libre",
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
                                    label = { Text("${option.icon} ${option.label}", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${uiState.sortOption.description}${if (budgetActive) " dentro del presupuesto fijado de €${budgetAmount.toInt()}" else " (modo libre)"}",
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
                                text = "⚡ ¡Tarifas en mínimo histórico!",
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
                            text = "Buscando salidas para mañana (< 24 horas)...",
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
                                text = "No se pudieron obtener opciones",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = uiState.error ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    viewModel.loadTop20WithCoordinates(currentCoords.first, currentCoords.second)
                                }
                            ) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
            } else if (uiState.results.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎲", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Presiona 'Girar Ruleta' para encontrar los mejores 20 viajes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                            text = "Top ${uiState.displayResults.size} Oportunidades",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Orden: ${uiState.sortOption.label}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Render each opportunity card
                items(
                    items = uiState.displayResults,
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
                                    text = "¡Elección Ganadora de la Ruleta!",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        OpportunityCard(
                            item = resultItem,
                            onChainTravel = { destinationHub ->
                                val destCoords = HubCoordinates.getCoordinates(destinationHub)
                                val destCity = HubCoordinates.getCity(destinationHub)
                                currentCoords = destCoords
                                city = destCity
                                viewModel.chainTravel(destCity, destCoords.first, destCoords.second)
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

    // Trip Hub Dialog (for inviting friends & QR code)
    selectedItemForHub?.let { item ->
        TripHubDialog(
            item = item,
            originCity = uiState.originCity ?: city,
            hubs = hubs,
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
                viewModel.loadTop20WithCoordinates(currentCoords.first, currentCoords.second)
            }
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
