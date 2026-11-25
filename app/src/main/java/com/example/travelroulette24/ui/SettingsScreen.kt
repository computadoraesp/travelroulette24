package com.example.travelroulette24.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.travelroulette24.R
import com.example.travelroulette24.data.byok.ApiCredentialsManager
import com.example.travelroulette24.data.byok.ApiQuotaGovernor
import com.example.travelroulette24.data.byok.ByokTravelService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    credentialsManager: ApiCredentialsManager,
    quotaGovernor: ApiQuotaGovernor? = null,
    byokTravelService: ByokTravelService? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val form = rememberSettingsFormState(credentialsManager)

    var showLegalDialog by remember { mutableStateOf(false) }
    var showApiWizard by remember { mutableStateOf(false) }
    var showManualDialog by remember { mutableStateOf(false) }
    var cacheClearedMessage by remember { mutableStateOf(false) }
    var quotaResetMessage by remember { mutableStateOf(false) }
    var quotaRefreshTick by remember { mutableIntStateOf(0) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
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
    ) { granted -> hasNotificationPermission = granted }

    val openAppSettings = {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        )
    }

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    val amadeusQuota = remember(quotaRefreshTick, form.amadeusIsPaid) {
        quotaGovernor?.getQuotaStatus("amadeus", form.amadeusIsPaid, monthlyQuota = 2000)
    }
    val kiwiQuota = remember(quotaRefreshTick, form.kiwiIsPaid) {
        quotaGovernor?.getQuotaStatus("kiwi", form.kiwiIsPaid, monthlyQuota = 2000)
    }
    val customQuota = remember(quotaRefreshTick, form.customIsPaid, form.customDailyLimit) {
        quotaGovernor?.getQuotaStatus("custom_flight_flight_primary", form.customIsPaid, monthlyQuota = 1500, customDailyLimit = form.customDailyLimit)
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
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
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
                            form.saveTo(credentialsManager)
                            onSaved()
                        },
                        modifier = Modifier.padding(end = 8.dp).testTag("settings_save_button")
                    ) {
                        Text(stringResource(R.string.btn_save), fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        var expandAllEpoch by remember { mutableIntStateOf(0) }
        var expandAllTarget by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        expandAllTarget = !expandAllTarget
                        expandAllEpoch++
                    },
                    modifier = Modifier.testTag("settings_toggle_expand_all")
                ) {
                    Text(
                        text = if (expandAllTarget) stringResource(R.string.settings_collapse_all)
                        else stringResource(R.string.settings_expand_all),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            ThemeAndAppearanceSection(expandAllEpoch = expandAllEpoch, expandAllTarget = expandAllTarget)

            ModeAndQuotaSection(
                isRealMode = form.isRealMode,
                hasActiveKeys = form.hasActiveKeys,
                onToggleRealMode = { checked ->
                    form.isRealMode = checked
                    credentialsManager.isRealMode = checked
                },
                onActivatePublicProviders = {
                    credentialsManager.activatePublicLiveProviders()
                    form.isRealMode = true
                    form.openHafas = true
                },
                onOpenWizard = { showApiWizard = true },
                onOpenManual = { showManualDialog = true },
                amadeusIsPaid = form.amadeusIsPaid,
                amadeusQuota = amadeusQuota,
                kiwiIsPaid = form.kiwiIsPaid,
                kiwiQuota = kiwiQuota,
                customUrl = form.customUrl,
                customIsPaid = form.customIsPaid,
                customDailyLimit = form.customDailyLimit,
                customQuota = customQuota,
                hasGovernor = quotaGovernor != null,
                onResetProviderStats = { providerId ->
                    quotaGovernor?.resetStats(providerId)
                    quotaRefreshTick++
                    quotaResetMessage = true
                },
                onClearCache = {
                    quotaGovernor?.clearCache()
                    cacheClearedMessage = true
                },
                cacheClearedMessage = cacheClearedMessage,
                quotaResetMessage = quotaResetMessage,
                expandAllEpoch = expandAllEpoch,
                expandAllTarget = expandAllTarget
            )

            val activeCustomCount = listOf(
                form.customUrl.isNotBlank() && form.customFlightEnabled,
                form.customTrainUrl.isNotBlank() && form.customTrainEnabled,
                form.customBoatUrl.isNotBlank() && form.customBoatEnabled
            ).count { it }
            CollapsibleSettingsSection(
                title = stringResource(R.string.custom_api_section_title),
                summary = stringResource(R.string.custom_api_section_subtitle),
                badgeText = stringResource(R.string.settings_configured_count_format, activeCustomCount, 3),
                isBadgeHighlighted = activeCustomCount > 0,
                expandAllEpoch = expandAllEpoch,
                expandAllTarget = expandAllTarget
            ) {
                CustomProvidersSection(
                    customFlightName = form.customFlightName, onFlightNameChange = { form.customFlightName = it },
                    customFlightUrl = form.customUrl, onFlightUrlChange = { form.customUrl = it },
                    customFlightHeader = form.customHeader, onFlightHeaderChange = { form.customHeader = it },
                    customFlightKey = form.customKey, onFlightKeyChange = { form.customKey = it },
                    customFlightEnabled = form.customFlightEnabled, onFlightEnabledChange = { form.customFlightEnabled = it },
                    customFlightIsPaid = form.customIsPaid, onFlightIsPaidChange = { form.customIsPaid = it },
                    customFlightDailyLimit = form.customDailyLimit, onFlightDailyLimitChange = { form.customDailyLimit = it },
                    customFlightNotes = form.customFlightNotes, onFlightNotesChange = { form.customFlightNotes = it },
                    customTrainName = form.customTrainName, onTrainNameChange = { form.customTrainName = it },
                    customTrainUrl = form.customTrainUrl, onTrainUrlChange = { form.customTrainUrl = it },
                    customTrainHeader = form.customTrainHeader, onTrainHeaderChange = { form.customTrainHeader = it },
                    customTrainKey = form.customTrainKey, onTrainKeyChange = { form.customTrainKey = it },
                    customTrainEnabled = form.customTrainEnabled, onTrainEnabledChange = { form.customTrainEnabled = it },
                    customTrainIsPaid = form.customTrainIsPaid, onTrainIsPaidChange = { form.customTrainIsPaid = it },
                    customTrainDailyLimit = form.customTrainDailyLimit, onTrainDailyLimitChange = { form.customTrainDailyLimit = it },
                    customTrainNotes = form.customTrainNotes, onTrainNotesChange = { form.customTrainNotes = it },
                    customBoatName = form.customBoatName, onBoatNameChange = { form.customBoatName = it },
                    customBoatUrl = form.customBoatUrl, onBoatUrlChange = { form.customBoatUrl = it },
                    customBoatHeader = form.customBoatHeader, onBoatHeaderChange = { form.customBoatHeader = it },
                    customBoatKey = form.customBoatKey, onBoatKeyChange = { form.customBoatKey = it },
                    customBoatEnabled = form.customBoatEnabled, onBoatEnabledChange = { form.customBoatEnabled = it },
                    customBoatIsPaid = form.customBoatIsPaid, onBoatIsPaidChange = { form.customBoatIsPaid = it },
                    customBoatDailyLimit = form.customBoatDailyLimit, onBoatDailyLimitChange = { form.customBoatDailyLimit = it },
                    customBoatNotes = form.customBoatNotes, onBoatNotesChange = { form.customBoatNotes = it },
                    byokTravelService = byokTravelService
                )
            }

            FlightsAndAggregatorsSection(
                amadeusKey = form.amadeusKey, onAmadeusKeyChange = { form.amadeusKey = it },
                amadeusSecret = form.amadeusSecret, onAmadeusSecretChange = { form.amadeusSecret = it },
                amadeusIsPaid = form.amadeusIsPaid, onAmadeusIsPaidChange = { form.amadeusIsPaid = it },
                amadeusUseProd = form.amadeusUseProd, onAmadeusUseProdChange = { form.amadeusUseProd = it },
                travelPayoutsToken = form.travelPayoutsToken, onTravelPayoutsTokenChange = { form.travelPayoutsToken = it },
                kiwiKey = form.kiwiKey, onKiwiKeyChange = { form.kiwiKey = it },
                kiwiIsPaid = form.kiwiIsPaid, onKiwiIsPaidChange = { form.kiwiIsPaid = it },
                rapidKey = form.rapidKey, onRapidKeyChange = { form.rapidKey = it },
                onOpenUrl = ::openUrl,
                expandAllEpoch = expandAllEpoch,
                expandAllTarget = expandAllTarget
            )

            AiRadarCurrencySection(
                geminiKey = form.geminiKey, onGeminiKeyChange = { form.geminiKey = it },
                aviationKey = form.aviationKey, onAviationKeyChange = { form.aviationKey = it },
                exchangeRateKey = form.exchangeRateKey, onExchangeRateKeyChange = { form.exchangeRateKey = it },
                onOpenUrl = ::openUrl,
                expandAllEpoch = expandAllEpoch,
                expandAllTarget = expandAllTarget
            )

            TransitAndTourismSection(
                navitiaToken = form.navitiaToken, onNavitiaTokenChange = { form.navitiaToken = it },
                openHafas = form.openHafas, onOpenHafasChange = { form.openHafas = it },
                openTripMapKey = form.openTripMapKey, onOpenTripMapKeyChange = { form.openTripMapKey = it },
                unsplashKey = form.unsplashKey, onUnsplashKeyChange = { form.unsplashKey = it },
                tripHubUrl = form.tripHubUrl, onTripHubUrlChange = { form.tripHubUrl = it },
                onOpenUrl = ::openUrl,
                expandAllEpoch = expandAllEpoch,
                expandAllTarget = expandAllTarget
            )

            SearchPreferencesSection(
                selectedCurrency = form.selectedCurrency,
                onSelectCurrency = { form.selectedCurrency = it },
                searchRadius = form.searchRadius,
                onSelectRadius = { form.searchRadius = it },
                expandAllEpoch = expandAllEpoch,
                expandAllTarget = expandAllTarget
            )

            PermissionsPrivacySection(
                hasLocationPermission = hasLocationPermission,
                hasNotificationPermission = hasNotificationPermission,
                onRequestLocation = { perms -> locationLauncher.launch(perms) },
                onRequestNotifications = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onOpenAppSettings = openAppSettings,
                expandAllEpoch = expandAllEpoch,
                expandAllTarget = expandAllTarget
            )

            LegalAndSecureWipeSection(
                onOpenLegalDialog = { showLegalDialog = true },
                onClearAllKeys = {
                    form.clearAll(context, credentialsManager)
                    quotaGovernor?.clearCache()
                    onSaved()
                },
                expandAllEpoch = expandAllEpoch,
                expandAllTarget = expandAllTarget
            )

            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
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
                form.amadeusKey = credentialsManager.amadeusApiKey
                form.amadeusSecret = credentialsManager.amadeusApiSecret
                form.kiwiKey = credentialsManager.kiwiApiKey
                form.geminiKey = credentialsManager.geminiApiKey
                onSaved()
            }
        )
    }

    if (showManualDialog) {
        ManualDialog(onDismiss = { showManualDialog = false }, onOpenWizard = { showApiWizard = true })
    }

    if (showLegalDialog) {
        LegalLicensesDialog(onDismiss = { showLegalDialog = false })
    }
}
