package com.example.travelroulette24.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.travelroulette24.R
import com.example.travelroulette24.data.byok.ApiCredentialsManager
import com.example.travelroulette24.data.byok.ApiQuotaGovernor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    credentialsManager: ApiCredentialsManager,
    quotaGovernor: ApiQuotaGovernor? = null,
    byokTravelService: com.example.travelroulette24.data.byok.ByokTravelService? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current

    var showLegalDialog by remember { mutableStateOf(false) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasLocationPermission = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    val openAppSettings = {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    // API Keys state
    var isRealMode by remember { mutableStateOf(credentialsManager.isRealMode) }
    var amadeusKey by remember { mutableStateOf(credentialsManager.amadeusApiKey) }
    var amadeusSecret by remember { mutableStateOf(credentialsManager.amadeusApiSecret) }
    var amadeusIsPaid by remember { mutableStateOf(credentialsManager.amadeusIsPaidTier) }
    var amadeusUseProd by remember { mutableStateOf(credentialsManager.amadeusUseProduction) }
    var travelPayoutsToken by remember { mutableStateOf(credentialsManager.travelPayoutsToken) }

    var kiwiKey by remember { mutableStateOf(credentialsManager.kiwiApiKey) }
    var kiwiIsPaid by remember { mutableStateOf(credentialsManager.kiwiIsPaidTier) }

    var rapidKey by remember { mutableStateOf(credentialsManager.rapidApiKey) }

    // Custom Flight API state
    var customFlightName by remember { mutableStateOf(credentialsManager.customFlightProviderName) }
    var customUrl by remember { mutableStateOf(credentialsManager.customFlightApiUrl) }
    var customKey by remember { mutableStateOf(credentialsManager.customFlightApiKey) }
    var customHeader by remember { mutableStateOf(credentialsManager.customFlightApiHeader) }
    var customFlightEnabled by remember { mutableStateOf(credentialsManager.customFlightApiEnabled) }
    var customIsPaid by remember { mutableStateOf(credentialsManager.customFlightIsPaidTier) }
    var customDailyLimit by remember { mutableIntStateOf(credentialsManager.customFlightDailyLimit) }
    var customFlightNotes by remember { mutableStateOf(credentialsManager.customFlightNotes) }

    // Custom Train API state
    var customTrainName by remember { mutableStateOf(credentialsManager.customTrainProviderName) }
    var customTrainUrl by remember { mutableStateOf(credentialsManager.customTrainApiUrl) }
    var customTrainHeader by remember { mutableStateOf(credentialsManager.customTrainApiHeader) }
    var customTrainKey by remember { mutableStateOf(credentialsManager.customTrainApiKey) }
    var customTrainEnabled by remember { mutableStateOf(credentialsManager.customTrainApiEnabled) }
    var customTrainIsPaid by remember { mutableStateOf(credentialsManager.customTrainIsPaidTier) }
    var customTrainDailyLimit by remember { mutableIntStateOf(credentialsManager.customTrainDailyLimit) }
    var customTrainNotes by remember { mutableStateOf(credentialsManager.customTrainNotes) }

    // Custom Boat / Ferry API state
    var customBoatName by remember { mutableStateOf(credentialsManager.customBoatProviderName) }
    var customBoatUrl by remember { mutableStateOf(credentialsManager.customBoatApiUrl) }
    var customBoatHeader by remember { mutableStateOf(credentialsManager.customBoatApiHeader) }
    var customBoatKey by remember { mutableStateOf(credentialsManager.customBoatApiKey) }
    var customBoatEnabled by remember { mutableStateOf(credentialsManager.customBoatApiEnabled) }
    var customBoatIsPaid by remember { mutableStateOf(credentialsManager.customBoatIsPaidTier) }
    var customBoatDailyLimit by remember { mutableIntStateOf(credentialsManager.customBoatDailyLimit) }
    var customBoatNotes by remember { mutableStateOf(credentialsManager.customBoatNotes) }

    // Additional APIs
    var geminiKey by remember { mutableStateOf(credentialsManager.geminiApiKey) }
    var aviationKey by remember { mutableStateOf(credentialsManager.aviationStackKey) }
    var exchangeRateKey by remember { mutableStateOf(credentialsManager.exchangeRateKey) }
    var tripHubUrl by remember { mutableStateOf(credentialsManager.tripHubUrl) }

    // Transit & Tourism
    var navitiaToken by remember { mutableStateOf(credentialsManager.navitiaToken) }
    var openHafas by remember { mutableStateOf(credentialsManager.openHafasEnabled) }
    var openTripMapKey by remember { mutableStateOf(credentialsManager.openTripMapKey) }
    var unsplashKey by remember { mutableStateOf(credentialsManager.unsplashKey) }

    // Preferences
    var selectedCurrency by remember { mutableStateOf(credentialsManager.currency) }
    var searchRadius by remember { mutableIntStateOf(credentialsManager.searchRadiusKm) }

    var cacheClearedMessage by remember { mutableStateOf(false) }
    var quotaResetMessage by remember { mutableStateOf(false) }
    var quotaRefreshTick by remember { mutableIntStateOf(0) }
    var showApiWizard by remember { mutableStateOf(false) }
    var showManualDialog by remember { mutableStateOf(false) }

    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    val hasActiveKeys = (amadeusKey.isNotBlank() && amadeusSecret.isNotBlank()) ||
            kiwiKey.isNotBlank() || travelPayoutsToken.isNotBlank() || rapidKey.isNotBlank() || customUrl.isNotBlank()

    // Quota metrics from Governor (reactive on quotaRefreshTick)
    val amadeusQuota = remember(quotaRefreshTick, amadeusIsPaid) {
        quotaGovernor?.getQuotaStatus("amadeus", amadeusIsPaid, monthlyQuota = 2000)
    }
    val kiwiQuota = remember(quotaRefreshTick, kiwiIsPaid) {
        quotaGovernor?.getQuotaStatus("kiwi", kiwiIsPaid, monthlyQuota = 2000)
    }
    val customQuota = remember(quotaRefreshTick, customIsPaid, customDailyLimit) {
        quotaGovernor?.getQuotaStatus("custom_flight_flight_primary", customIsPaid, monthlyQuota = 1500, customDailyLimit = customDailyLimit)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.settings_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.settings_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Text(
                            text = "←",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            credentialsManager.amadeusApiKey = amadeusKey
                            credentialsManager.amadeusApiSecret = amadeusSecret
                            credentialsManager.amadeusIsPaidTier = amadeusIsPaid
                            credentialsManager.amadeusUseProduction = amadeusUseProd
                            credentialsManager.travelPayoutsToken = travelPayoutsToken

                            credentialsManager.kiwiApiKey = kiwiKey
                            credentialsManager.kiwiIsPaidTier = kiwiIsPaid

                            credentialsManager.rapidApiKey = rapidKey

                            credentialsManager.customFlightProviderName = customFlightName
                            credentialsManager.customFlightApiUrl = customUrl
                            credentialsManager.customFlightApiKey = customKey
                            credentialsManager.customFlightApiHeader = customHeader
                            credentialsManager.customFlightApiEnabled = customFlightEnabled
                            credentialsManager.customFlightIsPaidTier = customIsPaid
                            credentialsManager.customFlightDailyLimit = customDailyLimit
                            credentialsManager.customFlightNotes = customFlightNotes

                            credentialsManager.customTrainProviderName = customTrainName
                            credentialsManager.customTrainApiUrl = customTrainUrl
                            credentialsManager.customTrainApiKey = customTrainKey
                            credentialsManager.customTrainApiHeader = customTrainHeader
                            credentialsManager.customTrainApiEnabled = customTrainEnabled
                            credentialsManager.customTrainIsPaidTier = customTrainIsPaid
                            credentialsManager.customTrainDailyLimit = customTrainDailyLimit
                            credentialsManager.customTrainNotes = customTrainNotes

                            credentialsManager.customBoatProviderName = customBoatName
                            credentialsManager.customBoatApiUrl = customBoatUrl
                            credentialsManager.customBoatApiKey = customBoatKey
                            credentialsManager.customBoatApiHeader = customBoatHeader
                            credentialsManager.customBoatApiEnabled = customBoatEnabled
                            credentialsManager.customBoatIsPaidTier = customBoatIsPaid
                            credentialsManager.customBoatDailyLimit = customBoatDailyLimit
                            credentialsManager.customBoatNotes = customBoatNotes

                            credentialsManager.geminiApiKey = geminiKey
                            credentialsManager.aviationStackKey = aviationKey
                            credentialsManager.exchangeRateKey = exchangeRateKey
                            credentialsManager.tripHubUrl = tripHubUrl

                            credentialsManager.navitiaToken = navitiaToken
                            credentialsManager.openHafasEnabled = openHafas
                            credentialsManager.openTripMapKey = openTripMapKey
                            credentialsManager.unsplashKey = unsplashKey

                            credentialsManager.currency = selectedCurrency
                            credentialsManager.searchRadiusKm = searchRadius
                            credentialsManager.isRealMode = isRealMode

                            onSaved()
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("settings_save_button")
                    ) {
                        Text(stringResource(R.string.btn_save), fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 0. Appearance & Accessibility Theme Selector (System / Light / Dark / Okabe-Ito)
            val activeThemeMode by com.example.travelroulette24.PreferenceHelper.themeModeFlow.collectAsState()
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = stringResource(R.string.theme_section_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.theme_section_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val themeOptions = listOf(
                            com.example.travelroulette24.ui.theme.AppThemeMode.SYSTEM to stringResource(R.string.theme_mode_system),
                            com.example.travelroulette24.ui.theme.AppThemeMode.LIGHT to stringResource(R.string.theme_mode_light),
                            com.example.travelroulette24.ui.theme.AppThemeMode.DARK to stringResource(R.string.theme_mode_dark),
                            com.example.travelroulette24.ui.theme.AppThemeMode.COLORBLIND_OKABE_ITO to stringResource(R.string.theme_mode_okabe_ito)
                        )
                        themeOptions.forEach { (mode, label) ->
                            FilterChip(
                                selected = activeThemeMode == mode,
                                onClick = { com.example.travelroulette24.PreferenceHelper.setThemeMode(mode) },
                                label = { Text(label, fontSize = 12.sp) },
                                modifier = Modifier.testTag("theme_mode_chip_${mode.name.lowercase()}")
                            )
                        }
                    }
                    if (activeThemeMode == com.example.travelroulette24.ui.theme.AppThemeMode.COLORBLIND_OKABE_ITO) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                com.example.travelroulette24.ui.theme.OkabeBlue,
                                com.example.travelroulette24.ui.theme.OkabeOrange,
                                com.example.travelroulette24.ui.theme.OkabeBluishGreen,
                                com.example.travelroulette24.ui.theme.OkabeVermillion,
                                com.example.travelroulette24.ui.theme.OkabePurpleOutline
                            ).forEach { swatch ->
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(swatch)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Okabe-Ito",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 1. Selector de Modo: Quitar Demo y Pasar a Real
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mode_selector_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isRealMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                border = BorderStroke(
                    width = 1.5.dp,
                    color = if (isRealMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isRealMode) stringResource(R.string.mode_real_title) else stringResource(R.string.mode_demo_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isRealMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isRealMode) stringResource(R.string.mode_real_desc) else stringResource(R.string.mode_demo_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = isRealMode,
                            onCheckedChange = { checked ->
                                isRealMode = checked
                                credentialsManager.isRealMode = checked
                            },
                            modifier = Modifier.testTag("real_mode_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FilledTonalButton(
                        onClick = {
                            credentialsManager.activatePublicLiveProviders()
                            isRealMode = true
                            openHafas = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("activate_public_providers_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.settings_mode_activate_public), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 2. Connection Status Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isRealMode) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isRealMode) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRealMode) "🟢" else "⚪",
                            fontSize = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isRealMode) {
                                if (hasActiveKeys) stringResource(R.string.settings_real_byok_title)
                                else stringResource(R.string.settings_real_live_title)
                            } else stringResource(R.string.mode_demo_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isRealMode) MaterialTheme.colorScheme.onTertiaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isRealMode) {
                                stringResource(R.string.settings_real_banner_desc)
                            } else {
                                stringResource(R.string.settings_demo_banner_desc)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // WIZARD BANNER - Guía paso a paso para conseguir claves
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_api_wizard_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🧙", fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_wizard_card_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = stringResource(R.string.settings_wizard_card_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showApiWizard = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(stringResource(R.string.btn_open_tutorial), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // MANUAL BANNER - Guía y Manual de Usuario
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_manual_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "📖", fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.manual_dialog_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = stringResource(R.string.manual_dialog_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showManualDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            ),
                            modifier = Modifier.testTag("settings_open_manual_button")
                        ) {
                            Text(stringResource(R.string.settings_open_manual_btn), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. TOKEN & QUOTA GOVERNOR (Control Inteligente de Cuota Diaria vs Mensual)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🛡️", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.settings_quota_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        BadgePill(text = stringResource(R.string.settings_quota_saving_badge))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_quota_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Real-time meters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuotaMetricBox(
                            providerName = "Amadeus",
                            isPaid = amadeusIsPaid,
                            requestsToday = amadeusQuota?.requestsToday ?: 0,
                            dailyBudget = amadeusQuota?.dailyBudget ?: 60,
                            requestsMonth = amadeusQuota?.requestsThisMonth ?: 0,
                            monthlyLimit = 2000,
                            onResetStats = if (quotaGovernor != null) {
                                {
                                    quotaGovernor.resetStats("amadeus")
                                    quotaRefreshTick++
                                    quotaResetMessage = true
                                }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                        QuotaMetricBox(
                            providerName = "Kiwi",
                            isPaid = kiwiIsPaid,
                            requestsToday = kiwiQuota?.requestsToday ?: 0,
                            dailyBudget = kiwiQuota?.dailyBudget ?: 60,
                            requestsMonth = kiwiQuota?.requestsThisMonth ?: 0,
                            monthlyLimit = 2000,
                            onResetStats = if (quotaGovernor != null) {
                                {
                                    quotaGovernor.resetStats("kiwi")
                                    quotaRefreshTick++
                                    quotaResetMessage = true
                                }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (customUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        QuotaMetricBox(
                            providerName = stringResource(R.string.settings_quota_custom_api),
                            isPaid = customIsPaid,
                            requestsToday = customQuota?.requestsToday ?: 0,
                            dailyBudget = customQuota?.dailyBudget ?: customDailyLimit,
                            requestsMonth = customQuota?.requestsThisMonth ?: 0,
                            monthlyLimit = 1500,
                            onResetStats = if (quotaGovernor != null) {
                                {
                                    quotaGovernor.resetStats("custom_flight_flight_primary")
                                    quotaRefreshTick++
                                    quotaResetMessage = true
                                }
                            } else null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                quotaGovernor?.clearCache()
                                cacheClearedMessage = true
                            }
                        ) {
                            Text(stringResource(R.string.settings_clear_cache_btn), fontSize = 12.sp)
                        }

                        if (cacheClearedMessage || quotaResetMessage) {
                            Text(
                                text = if (quotaResetMessage) stringResource(R.string.quota_reset_done)
                                else stringResource(R.string.settings_cache_cleared),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 3. APIS PERSONALIZADAS POR FORMA DE VIAJE (AVIÓN, TREN, BARCO)
            CustomProvidersSection(
                customFlightName = customFlightName,
                onFlightNameChange = { customFlightName = it },
                customFlightUrl = customUrl,
                onFlightUrlChange = { customUrl = it },
                customFlightHeader = customHeader,
                onFlightHeaderChange = { customHeader = it },
                customFlightKey = customKey,
                onFlightKeyChange = { customKey = it },
                customFlightEnabled = customFlightEnabled,
                onFlightEnabledChange = { customFlightEnabled = it },
                customFlightIsPaid = customIsPaid,
                onFlightIsPaidChange = { customIsPaid = it },
                customFlightDailyLimit = customDailyLimit,
                onFlightDailyLimitChange = { customDailyLimit = it },
                customFlightNotes = customFlightNotes,
                onFlightNotesChange = { customFlightNotes = it },

                customTrainName = customTrainName,
                onTrainNameChange = { customTrainName = it },
                customTrainUrl = customTrainUrl,
                onTrainUrlChange = { customTrainUrl = it },
                customTrainHeader = customTrainHeader,
                onTrainHeaderChange = { customTrainHeader = it },
                customTrainKey = customTrainKey,
                onTrainKeyChange = { customTrainKey = it },
                customTrainEnabled = customTrainEnabled,
                onTrainEnabledChange = { customTrainEnabled = it },
                customTrainIsPaid = customTrainIsPaid,
                onTrainIsPaidChange = { customTrainIsPaid = it },
                customTrainDailyLimit = customTrainDailyLimit,
                onTrainDailyLimitChange = { customTrainDailyLimit = it },
                customTrainNotes = customTrainNotes,
                onTrainNotesChange = { customTrainNotes = it },

                customBoatName = customBoatName,
                onBoatNameChange = { customBoatName = it },
                customBoatUrl = customBoatUrl,
                onBoatUrlChange = { customBoatUrl = it },
                customBoatHeader = customBoatHeader,
                onBoatHeaderChange = { customBoatHeader = it },
                customBoatKey = customBoatKey,
                onBoatKeyChange = { customBoatKey = it },
                customBoatEnabled = customBoatEnabled,
                onBoatEnabledChange = { customBoatEnabled = it },
                customBoatIsPaid = customBoatIsPaid,
                onBoatIsPaidChange = { customBoatIsPaid = it },
                customBoatDailyLimit = customBoatDailyLimit,
                onBoatDailyLimitChange = { customBoatDailyLimit = it },
                customBoatNotes = customBoatNotes,
                onBoatNotesChange = { customBoatNotes = it },

                byokTravelService = byokTravelService
            )

            HorizontalDivider()

            // 4. VUELOS Y AGREGADORES CON SELECTOR DE PLAN (GRATIS VS PAGO)
            Text(
                text = stringResource(R.string.settings_section_flights_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Provider: Amadeus Self-Service
            SettingsApiCard(
                title = stringResource(R.string.settings_amadeus_title),
                badge = if (amadeusIsPaid) stringResource(R.string.custom_plan_paid) else stringResource(R.string.settings_amadeus_free_badge),
                description = stringResource(R.string.settings_amadeus_desc),
                isConfigured = amadeusKey.isNotBlank() && amadeusSecret.isNotBlank(),
                onGetFreeKey = { openUrl("https://developers.amadeus.com/register") }
            ) {
                PlanTierSelector(
                    isPaid = amadeusIsPaid,
                    onPlanChange = { amadeusIsPaid = it },
                    freeDescription = stringResource(R.string.settings_amadeus_free_desc),
                    paidDescription = stringResource(R.string.settings_amadeus_paid_desc)
                )

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = amadeusKey,
                    onValueChange = { amadeusKey = it },
                    label = { Text(stringResource(R.string.settings_amadeus_key_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_amadeus_key")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amadeusSecret,
                    onValueChange = { amadeusSecret = it },
                    label = { Text(stringResource(R.string.settings_amadeus_secret_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_amadeus_secret")
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_amadeus_prod_toggle),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = amadeusUseProd,
                        onCheckedChange = { amadeusUseProd = it },
                        modifier = Modifier.testTag("settings_amadeus_prod_switch")
                    )
                }
            }

            // Provider: TravelPayouts (Free Affiliate Flights)
            SettingsApiCard(
                title = stringResource(R.string.settings_travelpayouts_title),
                badge = stringResource(R.string.settings_travelpayouts_badge),
                description = stringResource(R.string.settings_travelpayouts_desc),
                isConfigured = travelPayoutsToken.isNotBlank(),
                onGetFreeKey = { openUrl("https://www.travelpayouts.com/programs/100/tools/api") }
            ) {
                OutlinedTextField(
                    value = travelPayoutsToken,
                    onValueChange = { travelPayoutsToken = it },
                    label = { Text(stringResource(R.string.settings_travelpayouts_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_travelpayouts_token")
                )
            }

            // Provider: Kiwi Tequila
            SettingsApiCard(
                title = stringResource(R.string.settings_kiwi_title),
                badge = if (kiwiIsPaid) stringResource(R.string.custom_plan_paid) else stringResource(R.string.settings_kiwi_free_badge),
                description = stringResource(R.string.settings_kiwi_desc),
                isConfigured = kiwiKey.isNotBlank(),
                onGetFreeKey = { openUrl("https://tequila.kiwi.com/portal/login/register") }
            ) {
                PlanTierSelector(
                    isPaid = kiwiIsPaid,
                    onPlanChange = { kiwiIsPaid = it },
                    freeDescription = stringResource(R.string.settings_kiwi_free_desc),
                    paidDescription = stringResource(R.string.settings_kiwi_paid_desc)
                )

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = kiwiKey,
                    onValueChange = { kiwiKey = it },
                    label = { Text(stringResource(R.string.settings_kiwi_key_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_kiwi_key")
                )
            }

            // Provider: RapidAPI
            SettingsApiCard(
                title = stringResource(R.string.settings_rapidapi_title),
                badge = stringResource(R.string.settings_rapidapi_badge),
                description = stringResource(R.string.settings_rapidapi_desc),
                isConfigured = rapidKey.isNotBlank(),
                onGetFreeKey = { openUrl("https://rapidapi.com/auth/sign-up") }
            ) {
                OutlinedTextField(
                    value = rapidKey,
                    onValueChange = { rapidKey = it },
                    label = { Text(stringResource(R.string.settings_rapidapi_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_rapidapi_key")
                )
            }

            HorizontalDivider()

            // 5. NUEVAS APIS: INTELIGENCIA ARTIFICIAL, RADAR Y DIVISAS
            Text(
                text = stringResource(R.string.settings_section_intelligence_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Google Gemini AI
            SettingsApiCard(
                title = stringResource(R.string.settings_gemini_title),
                badge = stringResource(R.string.settings_gemini_badge),
                description = stringResource(R.string.settings_gemini_desc),
                isConfigured = geminiKey.isNotBlank(),
                onGetFreeKey = { openUrl("https://aistudio.google.com/app/apikey") }
            ) {
                OutlinedTextField(
                    value = geminiKey,
                    onValueChange = { geminiKey = it },
                    label = { Text(stringResource(R.string.settings_gemini_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_gemini_key")
                )
            }

            // AviationStack
            SettingsApiCard(
                title = stringResource(R.string.settings_aviation_title),
                badge = stringResource(R.string.settings_aviation_badge),
                description = stringResource(R.string.settings_aviation_desc),
                isConfigured = aviationKey.isNotBlank(),
                onGetFreeKey = { openUrl("https://aviationstack.com/signup/free") }
            ) {
                OutlinedTextField(
                    value = aviationKey,
                    onValueChange = { aviationKey = it },
                    label = { Text(stringResource(R.string.settings_aviation_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_aviation_key")
                )
            }

            // ExchangeRate-API
            SettingsApiCard(
                title = stringResource(R.string.settings_exchange_title),
                badge = stringResource(R.string.settings_exchange_badge),
                description = stringResource(R.string.settings_exchange_desc),
                isConfigured = exchangeRateKey.isNotBlank(),
                onGetFreeKey = { openUrl("https://www.exchangerate-api.com/") }
            ) {
                OutlinedTextField(
                    value = exchangeRateKey,
                    onValueChange = { exchangeRateKey = it },
                    label = { Text(stringResource(R.string.settings_exchange_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_exchangerate_key")
                )
            }

            HorizontalDivider()

            // 6. TRENES Y TRANSPORTE TERRESTRE (CON TOKENS LIBRES)
            Text(
                text = stringResource(R.string.settings_section_trains_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Provider: Navitia
            SettingsApiCard(
                title = stringResource(R.string.settings_navitia_title),
                badge = stringResource(R.string.settings_navitia_badge),
                description = stringResource(R.string.settings_navitia_desc),
                isConfigured = navitiaToken.isNotBlank(),
                onGetFreeKey = { openUrl("https://www.navitia.io/register/") }
            ) {
                OutlinedTextField(
                    value = navitiaToken,
                    onValueChange = { navitiaToken = it },
                    label = { Text(stringResource(R.string.settings_navitia_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_navitia_token")
                )
            }

            // Provider: Open HAFAS (Open source / Tokens Libres)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.settings_hafas_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BadgePill(text = stringResource(R.string.settings_hafas_badge))
                        }
                        Text(
                            text = stringResource(R.string.settings_hafas_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = openHafas,
                        onCheckedChange = { openHafas = it }
                    )
                }
            }

            HorizontalDivider()

            // 7. DESTINATION INTELLIGENCE
            Text(
                text = stringResource(R.string.settings_section_tourism_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // OpenTripMap
            SettingsApiCard(
                title = stringResource(R.string.settings_otm_title),
                badge = stringResource(R.string.settings_otm_badge),
                description = stringResource(R.string.settings_otm_desc),
                isConfigured = openTripMapKey.isNotBlank(),
                onGetFreeKey = { openUrl("https://opentripmap.io/product") }
            ) {
                OutlinedTextField(
                    value = openTripMapKey,
                    onValueChange = { openTripMapKey = it },
                    label = { Text(stringResource(R.string.settings_otm_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Unsplash
            SettingsApiCard(
                title = stringResource(R.string.settings_unsplash_title),
                badge = stringResource(R.string.settings_unsplash_badge),
                description = stringResource(R.string.settings_unsplash_desc),
                isConfigured = unsplashKey.isNotBlank(),
                onGetFreeKey = { openUrl("https://unsplash.com/developers") }
            ) {
                OutlinedTextField(
                    value = unsplashKey,
                    onValueChange = { unsplashKey = it },
                    label = { Text(stringResource(R.string.settings_unsplash_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Collaborative Trip Hub Endpoint (optional serverless URL)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.settings_triphub_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        BadgePill(text = stringResource(R.string.settings_triphub_badge))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.settings_triphub_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = tripHubUrl,
                        onValueChange = { tripHubUrl = it },
                        label = { Text(stringResource(R.string.settings_triphub_label)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_triphub_url")
                    )
                }
            }

            HorizontalDivider()

            // 8. PREFERENCIAS DE BÚSQUEDA
            Text(
                text = stringResource(R.string.settings_section_prefs_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = stringResource(R.string.settings_currency_title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "EUR" to stringResource(R.string.settings_currency_eur),
                            "USD" to stringResource(R.string.settings_currency_usd),
                            "GBP" to stringResource(R.string.settings_currency_gbp)
                        ).forEach { (code, label) ->
                            val selected = selectedCurrency == code
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedCurrency = code }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.settings_radius_title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(50, 100, 150, 250).forEach { km ->
                            val label = stringResource(R.string.settings_radius_km_format, km)
                            val selected = searchRadius == km
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { searchRadius = km }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.onSecondary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // 9. PERMISOS Y PRIVACIDAD DEL SISTEMA
            Text(
                text = stringResource(R.string.permissions_privacy_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.permissions_privacy_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Permiso de Ubicación
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.permissions_location_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (hasLocationPermission) MaterialTheme.colorScheme.tertiaryContainer
                                    else MaterialTheme.colorScheme.errorContainer
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (hasLocationPermission) stringResource(R.string.permissions_status_granted)
                                else stringResource(R.string.permissions_status_denied),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hasLocationPermission) MaterialTheme.colorScheme.onTertiaryContainer
                                else MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.permissions_location_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!hasLocationPermission) {
                            Button(
                                onClick = {
                                    locationLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("request_location_permission_button")
                            ) {
                                Text(stringResource(R.string.permissions_btn_grant_location), fontSize = 12.sp)
                            }
                        }
                        OutlinedButton(
                            onClick = openAppSettings,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_app_settings_location_button")
                        ) {
                            Text(stringResource(R.string.permissions_btn_open_settings), fontSize = 12.sp)
                        }
                    }
                }
            }

            // Permiso de Notificaciones (Radar de Chollos)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.permissions_notifications_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (hasNotificationPermission) MaterialTheme.colorScheme.tertiaryContainer
                                    else MaterialTheme.colorScheme.errorContainer
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (hasNotificationPermission) stringResource(R.string.permissions_status_granted)
                                else stringResource(R.string.permissions_status_denied),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hasNotificationPermission) MaterialTheme.colorScheme.onTertiaryContainer
                                else MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.permissions_notifications_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!hasNotificationPermission) {
                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("request_notifications_permission_button")
                            ) {
                                Text(stringResource(R.string.permissions_btn_grant_notifications), fontSize = 12.sp)
                            }
                        }
                        OutlinedButton(
                            onClick = openAppSettings,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_app_settings_notifications_button")
                        ) {
                            Text(stringResource(R.string.permissions_btn_open_settings), fontSize = 12.sp)
                        }
                    }
                }
            }

            HorizontalDivider()

            // 10. LICENCIAS DE SOFTWARE Y TÉRMINOS LEGALES
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = stringResource(R.string.legal_licenses_section_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.legal_licenses_section_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FilledTonalButton(
                        onClick = { showLegalDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_licenses_and_legal_button")
                    ) {
                        Text(stringResource(R.string.btn_view_licenses_and_legal), fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider()

            // 11. PRIVACIDAD Y BORRADO SEGURO
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🔒", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.settings_privacy_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.settings_privacy_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            credentialsManager.clearAllKeys()
                            amadeusKey = ""
                            amadeusSecret = ""
                            amadeusIsPaid = false
                            amadeusUseProd = false
                            travelPayoutsToken = ""
                            kiwiKey = ""
                            kiwiIsPaid = false
                            rapidKey = ""
                            customFlightName = context.getString(R.string.custom_provider_default_flight)
                            customUrl = ""
                            customKey = ""
                            customFlightEnabled = false
                            customFlightNotes = ""
                            customTrainName = context.getString(R.string.custom_provider_default_train)
                            customTrainUrl = ""
                            customTrainKey = ""
                            customTrainEnabled = false
                            customTrainNotes = ""
                            customBoatName = context.getString(R.string.custom_provider_default_boat)
                            customBoatUrl = ""
                            customBoatKey = ""
                            customBoatEnabled = false
                            customBoatNotes = ""
                            geminiKey = ""
                            aviationKey = ""
                            exchangeRateKey = ""
                            tripHubUrl = ""
                            navitiaToken = ""
                            openTripMapKey = ""
                            unsplashKey = ""
                            quotaGovernor?.clearCache()
                            onSaved()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_all_keys_button")
                    ) {
                        Text(stringResource(R.string.settings_clear_keys))
                    }
                }
            }

            // FOOTER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.settings_footer_line1),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.settings_footer_line2),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }

    if (showApiWizard) {
        ApiWizardDialog(
            credentialsManager = credentialsManager,
            onDismiss = { showApiWizard = false },
            onCompleted = {
                showApiWizard = false
                amadeusKey = credentialsManager.amadeusApiKey
                amadeusSecret = credentialsManager.amadeusApiSecret
                kiwiKey = credentialsManager.kiwiApiKey
                geminiKey = credentialsManager.geminiApiKey
                onSaved()
            }
        )
    }

    if (showManualDialog) {
        ManualDialog(
            onDismiss = { showManualDialog = false },
            onOpenWizard = { showApiWizard = true }
        )
    }

    if (showLegalDialog) {
        LegalLicensesDialog(
            onDismiss = { showLegalDialog = false }
        )
    }
}

@Composable
private fun QuotaMetricBox(
    providerName: String,
    isPaid: Boolean,
    requestsToday: Int,
    dailyBudget: Int,
    requestsMonth: Int,
    monthlyLimit: Int,
    onResetStats: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = providerName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isPaid) stringResource(R.string.settings_tier_pro) else stringResource(R.string.settings_tier_free),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isPaid) stringResource(R.string.settings_today_unlimited, requestsToday)
                else stringResource(R.string.settings_today_req, requestsToday, dailyBudget),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isPaid) stringResource(R.string.settings_month_unlimited, requestsMonth)
                    else stringResource(R.string.settings_month_req, requestsMonth, monthlyLimit),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (onResetStats != null && (requestsToday > 0 || requestsMonth > 0)) {
                    Text(
                        text = stringResource(R.string.quota_reset_btn),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onResetStats() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanTierSelector(
    isPaid: Boolean,
    onPlanChange: (Boolean) -> Unit,
    freeDescription: String,
    paidDescription: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (!isPaid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                    .border(
                        1.dp,
                        if (!isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onPlanChange(false) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.settings_plan_free),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (!isPaid) FontWeight.Bold else FontWeight.Normal,
                    color = if (!isPaid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isPaid) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface)
                    .border(
                        1.dp,
                        if (isPaid) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onPlanChange(true) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.settings_plan_paid),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isPaid) FontWeight.Bold else FontWeight.Normal,
                    color = if (isPaid) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isPaid) paidDescription else freeDescription,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsApiCard(
    title: String,
    badge: String,
    description: String,
    isConfigured: Boolean,
    onGetFreeKey: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isConfigured) MaterialTheme.colorScheme.tertiaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isConfigured) stringResource(R.string.settings_badge_configured)
                        else stringResource(R.string.settings_badge_nokey),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isConfigured) MaterialTheme.colorScheme.onTertiaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BadgePill(text = badge)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onGetFreeKey,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_btn_get_key), fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            content()
        }
    }
}

@Composable
private fun BadgePill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
