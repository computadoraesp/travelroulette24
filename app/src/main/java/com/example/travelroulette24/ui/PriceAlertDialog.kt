package com.example.travelroulette24.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.text.style.TextOverflow
import com.example.travelroulette24.PreferenceHelper
import com.example.travelroulette24.R
import com.example.travelroulette24.data.PriceAlertManager
import com.example.travelroulette24.data.WorldDestinationsCatalog
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import com.example.travelroulette24.utils.CurrencyHelper
import com.example.travelroulette24.workers.DealAlertScheduler

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PriceAlertDialog(
    currentConfig: PriceAlertManager.AlertConfig,
    favoriteDestinations: List<String> = PreferenceHelper.getFavoriteDestinations(),
    currentResults: List<TravelIntelligenceEngine.ResultItem> = emptyList(),
    historicalLowByHub: Map<String, Double> = emptyMap(),
    priceAlertManager: PriceAlertManager? = null,
    currencyCode: String = CurrencyHelper.EUR,
    currencyRate: Double = CurrencyHelper.fallbackRateFromEur(currencyCode),
    onOpenFavorites: () -> Unit = {},
    onSaveConfig: (PriceAlertManager.AlertConfig) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val safeRate = if (currencyRate > 0.0) currencyRate else 1.0
    val currencySymbol = CurrencyHelper.symbolFor(currencyCode)
    var isEnabled by remember { mutableStateOf(currentConfig.isEnabled) }
    var alertMode by remember { mutableStateOf(currentConfig.alertMode) }
    var maxPrice by remember {
        mutableFloatStateOf((currentConfig.maxPriceEur * safeRate).toFloat().coerceIn(15f, 350f))
    }
    var minKmPerEur by remember {
        mutableFloatStateOf((currentConfig.minKmPerEur / safeRate).toFloat().coerceIn(5f, 50f))
    }
    var enforceMaxPriceInValueMode by remember { mutableStateOf(currentConfig.enforceMaxPriceInValueMode) }

    val flowFavorites by PreferenceHelper.favoriteDestinationsFlow.collectAsState()
    var editableFavorites by remember(favoriteDestinations, flowFavorites) {
        mutableStateOf(
            if (flowFavorites.isNotEmpty() || favoriteDestinations.isEmpty()) flowFavorites else favoriteDestinations
        )
    }

    var showInlineAdd by remember { mutableStateOf(false) }
    var addQuery by remember { mutableStateOf("") }
    var predictedDestinations by remember {
        mutableStateOf<List<WorldDestinationsCatalog.WorldDestination>>(emptyList())
    }
    var isSearchingCities by remember { mutableStateOf(false) }

    LaunchedEffect(addQuery, showInlineAdd) {
        if (!showInlineAdd) {
            predictedDestinations = emptyList()
            isSearchingCities = false
            return@LaunchedEffect
        }
        val q = addQuery.trim()
        if (q.length < 2) {
            predictedDestinations = emptyList()
            isSearchingCities = false
            return@LaunchedEffect
        }
        isSearchingCities = true
        kotlinx.coroutines.delay(300)
        predictedDestinations = WorldDestinationsCatalog.searchGlobalCities(q, limit = 6)
        isSearchingCities = false
    }

    val validInitialDest = remember(currentConfig.targetDestination, editableFavorites) {
        currentConfig.targetDestination?.takeIf { target ->
            target == PriceAlertManager.TARGET_ALL_FAVORITES ||
                editableFavorites.any { it.equals(target, ignoreCase = true) }
        }
    }
    var selectedDestination by remember { mutableStateOf(validInitialDest) }

    fun removeDesiredDestination(code: String) {
        val updated = editableFavorites.filterNot { it.equals(code, ignoreCase = true) }
        editableFavorites = updated
        PreferenceHelper.setFavoriteDestinations(updated)
        DealAlertScheduler.syncSchedule(context)
        if (selectedDestination.equals(code, ignoreCase = true) ||
            (updated.isEmpty() && selectedDestination == PriceAlertManager.TARGET_ALL_FAVORITES)
        ) {
            selectedDestination = null
        }
    }

    fun addDesiredDestination(code: String) {
        val normalized = code.trim().uppercase()
        if (normalized.isBlank() || editableFavorites.size >= 3) return
        if (editableFavorites.any { it.equals(normalized, ignoreCase = true) }) return
        val updated = editableFavorites + normalized
        editableFavorites = updated
        PreferenceHelper.setFavoriteDestinations(updated)
        DealAlertScheduler.syncSchedule(context)
        selectedDestination = normalized
        addQuery = ""
        showInlineAdd = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(0.95f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔔", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.price_alert_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.price_alert_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    FilledTonalIconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("price_alert_close_button")
                    ) {
                        Text(stringResource(R.string.btn_close), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Switch Toggle Card
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (isEnabled) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = stringResource(R.string.price_alert_switch_title),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = when {
                                    !isEnabled -> stringResource(R.string.price_alert_switch_inactive_desc)
                                    alertMode == PriceAlertManager.AlertMode.BEST_KM_PER_PRICE ->
                                        stringResource(R.string.price_alert_switch_active_km_desc, currencySymbol)
                                    alertMode == PriceAlertManager.AlertMode.HISTORICAL_LOW ->
                                        stringResource(R.string.price_alert_switch_active_historical_desc)
                                    else -> stringResource(R.string.price_alert_switch_active_desc)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { isEnabled = it },
                            modifier = Modifier.testTag("price_alert_switch")
                        )
                    }
                }

                if (isEnabled) {
                    Spacer(modifier = Modifier.height(18.dp))

                    PriceAlertModeSection(
                        alertMode = alertMode,
                        onSelectAlertMode = { alertMode = it },
                        minKmPerEur = minKmPerEur,
                        onMinKmChange = { minKmPerEur = it },
                        maxPrice = maxPrice,
                        onMaxPriceChange = { maxPrice = it },
                        enforceMaxPriceInValueMode = enforceMaxPriceInValueMode,
                        onEnforceMaxPriceChange = { enforceMaxPriceInValueMode = it },
                        currencySymbol = currencySymbol
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    PriceAlertDestinationSection(
                        editableFavorites = editableFavorites,
                        selectedDestination = selectedDestination,
                        onSelectDestination = { selectedDestination = it },
                        onRemoveDesiredDestination = ::removeDesiredDestination,
                        onAddDesiredDestination = ::addDesiredDestination,
                        showInlineAdd = showInlineAdd,
                        onToggleInlineAdd = { showInlineAdd = !showInlineAdd },
                        addQuery = addQuery,
                        onAddQueryChange = { addQuery = it },
                        isSearchingCities = isSearchingCities,
                        predictedDestinations = predictedDestinations,
                        onEditFavoritesClick = {
                            onDismiss()
                            onOpenFavorites()
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val activeManager = priceAlertManager ?: remember(context) { PriceAlertManager.getInstance(context) }
                    val draftConfig = remember(
                        isEnabled,
                        alertMode,
                        maxPrice,
                        minKmPerEur,
                        enforceMaxPriceInValueMode,
                        selectedDestination,
                        safeRate
                    ) {
                        PriceAlertManager.AlertConfig(
                            isEnabled = isEnabled,
                            alertMode = alertMode,
                            maxPriceEur = (maxPrice / safeRate).toFloat().coerceAtLeast(10f),
                            minKmPerEur = (minKmPerEur * safeRate).toFloat().coerceAtLeast(3f),
                            enforceMaxPriceInValueMode = enforceMaxPriceInValueMode,
                            targetDestination = selectedDestination
                        )
                    }
                    val previewMatches = remember(currentResults, draftConfig, editableFavorites, historicalLowByHub) {
                        activeManager.getMatchingDeals(
                            results = currentResults,
                            config = draftConfig,
                            favoriteDestinations = editableFavorites,
                            historicalLowByHub = historicalLowByHub
                        )
                    }

                    PriceAlertLivePreviewCard(
                        matches = previewMatches,
                        currencyCode = currencyCode,
                        currencyRate = safeRate
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Explanatory note
                    Text(
                        text = stringResource(R.string.price_alert_info_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions: Save & Cancel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.btn_cancel))
                    }

                    Button(
                        onClick = {
                            onSaveConfig(
                                PriceAlertManager.AlertConfig(
                                    isEnabled = isEnabled,
                                    alertMode = alertMode,
                                    maxPriceEur = (maxPrice / safeRate).toFloat().coerceAtLeast(10f),
                                    minKmPerEur = (minKmPerEur * safeRate).toFloat().coerceAtLeast(3f),
                                    enforceMaxPriceInValueMode = enforceMaxPriceInValueMode,
                                    targetDestination = selectedDestination
                                )
                            )
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp)
                            .testTag("save_price_alert_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(stringResource(R.string.price_alert_save_btn), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PriceAlertLivePreviewCard(
    matches: List<TravelIntelligenceEngine.ResultItem>,
    currencyCode: String,
    currencyRate: Double
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("price_alert_live_preview_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (matches.isNotEmpty()) {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.price_alert_preview_title),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (matches.isNotEmpty()) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (matches.isNotEmpty()) {
                    stringResource(R.string.price_alert_preview_match_format, matches.size)
                } else {
                    stringResource(R.string.price_alert_preview_empty)
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (matches.isNotEmpty()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            if (matches.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    matches.take(3).forEach { item ->
                        val cityLabel = HubCoordinates.getCity(item.destinationHub)
                        val priceLabel = CurrencyHelper.formatPrice(item.totalPrice, currencyCode, currencyRate)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        ) {
                            Text(
                                text = "$cityLabel · $priceLabel",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
