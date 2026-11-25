package com.example.travelroulette24.ui

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.travelroulette24.PreferenceHelper
import com.example.travelroulette24.R
import com.example.travelroulette24.ui.theme.AppThemeMode
import com.example.travelroulette24.ui.theme.OkabeBlue
import com.example.travelroulette24.ui.theme.OkabeBluishGreen
import com.example.travelroulette24.ui.theme.OkabeOrange
import com.example.travelroulette24.ui.theme.OkabePurpleOutline
import com.example.travelroulette24.ui.theme.OkabeVermillion

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ThemeAndAppearanceSection(
    expandAllEpoch: Int,
    expandAllTarget: Boolean
) {
    val activeThemeMode by PreferenceHelper.themeModeFlow.collectAsState()
    val activeThemeLabel = when (activeThemeMode) {
        AppThemeMode.SYSTEM -> stringResource(R.string.theme_mode_system)
        AppThemeMode.LIGHT -> stringResource(R.string.theme_mode_light)
        AppThemeMode.DARK -> stringResource(R.string.theme_mode_dark)
        AppThemeMode.COLORBLIND_OKABE_ITO -> stringResource(R.string.theme_mode_okabe_ito)
    }

    CollapsibleSettingsSection(
        title = stringResource(R.string.theme_section_title),
        summary = activeThemeLabel,
        badgeText = activeThemeLabel,
        expandAllEpoch = expandAllEpoch,
        expandAllTarget = expandAllTarget,
        testTag = "settings_theme_section"
    ) {
        Text(
            text = stringResource(R.string.theme_section_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val themeOptions = listOf(
                AppThemeMode.SYSTEM to stringResource(R.string.theme_mode_system),
                AppThemeMode.LIGHT to stringResource(R.string.theme_mode_light),
                AppThemeMode.DARK to stringResource(R.string.theme_mode_dark),
                AppThemeMode.COLORBLIND_OKABE_ITO to stringResource(R.string.theme_mode_okabe_ito)
            )
            themeOptions.forEach { (mode, label) ->
                FilterChip(
                    selected = activeThemeMode == mode,
                    onClick = { PreferenceHelper.setThemeMode(mode) },
                    label = { Text(label, fontSize = 12.sp) },
                    modifier = Modifier.testTag("theme_mode_chip_${mode.name.lowercase()}")
                )
            }
        }
        if (activeThemeMode == AppThemeMode.COLORBLIND_OKABE_ITO) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    OkabeBlue,
                    OkabeOrange,
                    OkabeBluishGreen,
                    OkabeVermillion,
                    OkabePurpleOutline
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SearchPreferencesSection(
    selectedCurrency: String,
    onSelectCurrency: (String) -> Unit,
    searchRadius: Int,
    onSelectRadius: (Int) -> Unit,
    expandAllEpoch: Int,
    expandAllTarget: Boolean
) {
    var showCurrencyPicker by remember { mutableStateOf(false) }
    val localeDefaultCurrency = remember {
        com.example.travelroulette24.utils.CurrencyHelper.defaultCurrencyForLocale()
    }
    val activeCurrencyName = com.example.travelroulette24.utils.CurrencyHelper.displayNameFor(selectedCurrency)
    val activeCurrencySymbol = com.example.travelroulette24.utils.CurrencyHelper.symbolFor(selectedCurrency).trim()

    CollapsibleSettingsSection(
        title = stringResource(R.string.settings_section_prefs_title),
        summary = "$selectedCurrency · ${stringResource(R.string.settings_radius_km_format, searchRadius)}",
        badgeText = "$selectedCurrency · ${searchRadius}km",
        expandAllEpoch = expandAllEpoch,
        expandAllTarget = expandAllTarget
    ) {
        Text(
            text = stringResource(R.string.settings_currency_title),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { showCurrencyPicker = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("open_currency_picker_button"),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = "🌍 $selectedCurrency ($activeCurrencySymbol) · $activeCurrencyName",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val quickCurrencies = listOf(selectedCurrency, localeDefaultCurrency, "USD", "EUR", "MXN", "COP", "ARS", "GBP")
            .distinct()
            .take(5)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickCurrencies.forEach { code ->
                val selected = selectedCurrency.equals(code, ignoreCase = true)
                FilterChip(
                    selected = selected,
                    onClick = { onSelectCurrency(code) },
                    label = {
                        Text(
                            text = "${com.example.travelroulette24.utils.CurrencyHelper.symbolFor(code).trim()} $code",
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("currency_quick_chip_$code")
                )
            }
            FilterChip(
                selected = false,
                onClick = { showCurrencyPicker = true },
                label = { Text(stringResource(R.string.currency_all_world_chip), fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("currency_all_world_chip")
            )
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
                        .clickable { onSelectRadius(km) }
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

    if (showCurrencyPicker) {
        CurrencyPickerDialog(
            selectedCurrency = selectedCurrency,
            onSelectCurrency = onSelectCurrency,
            onDismiss = { showCurrencyPicker = false }
        )
    }
}

@Composable
internal fun PermissionsPrivacySection(
    hasLocationPermission: Boolean,
    hasNotificationPermission: Boolean,
    onRequestLocation: (Array<String>) -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenAppSettings: () -> Unit,
    expandAllEpoch: Int,
    expandAllTarget: Boolean
) {
    val permLocationLabel = if (hasLocationPermission) stringResource(R.string.permissions_status_granted) else stringResource(R.string.permissions_status_denied)
    val permNotifLabel = if (hasNotificationPermission) stringResource(R.string.permissions_status_granted) else stringResource(R.string.permissions_status_denied)

    CollapsibleSettingsSection(
        title = stringResource(R.string.permissions_privacy_title),
        summary = "📍 $permLocationLabel · 🔔 $permNotifLabel",
        isBadgeHighlighted = hasLocationPermission && hasNotificationPermission,
        expandAllEpoch = expandAllEpoch,
        expandAllTarget = expandAllTarget
    ) {
        Text(
            text = stringResource(R.string.permissions_privacy_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CollapsibleSettingsSection(
                title = stringResource(R.string.permissions_location_title),
                summary = stringResource(R.string.permissions_location_desc),
                badgeText = permLocationLabel,
                isBadgeHighlighted = hasLocationPermission
            ) {
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
                                onRequestLocation(
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
                        onClick = onOpenAppSettings,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_app_settings_location_button")
                    ) {
                        Text(stringResource(R.string.permissions_btn_open_settings), fontSize = 12.sp)
                    }
                }
            }

            CollapsibleSettingsSection(
                title = stringResource(R.string.permissions_notifications_title),
                summary = stringResource(R.string.permissions_notifications_desc),
                badgeText = permNotifLabel,
                isBadgeHighlighted = hasNotificationPermission
            ) {
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
                    if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Button(
                            onClick = onRequestNotifications,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("request_notifications_permission_button")
                        ) {
                            Text(stringResource(R.string.permissions_btn_grant_notifications), fontSize = 12.sp)
                        }
                    }
                    OutlinedButton(
                        onClick = onOpenAppSettings,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_app_settings_notifications_button")
                    ) {
                        Text(stringResource(R.string.permissions_btn_open_settings), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
internal fun LegalAndSecureWipeSection(
    onOpenLegalDialog: () -> Unit,
    onClearAllKeys: () -> Unit,
    expandAllEpoch: Int,
    expandAllTarget: Boolean
) {
    CollapsibleSettingsSection(
        title = stringResource(R.string.legal_licenses_section_title),
        summary = stringResource(R.string.legal_licenses_section_subtitle),
        expandAllEpoch = expandAllEpoch,
        expandAllTarget = expandAllTarget,
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    ) {
        Text(
            text = stringResource(R.string.legal_licenses_section_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        FilledTonalButton(
            onClick = onOpenLegalDialog,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("open_licenses_and_legal_button")
        ) {
            Text(stringResource(R.string.btn_view_licenses_and_legal), fontWeight = FontWeight.Bold)
        }
    }

    CollapsibleSettingsSection(
        title = "🔒 ${stringResource(R.string.settings_privacy_title)}",
        summary = stringResource(R.string.settings_privacy_desc),
        expandAllEpoch = expandAllEpoch,
        expandAllTarget = expandAllTarget,
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Text(
            text = stringResource(R.string.settings_privacy_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onClearAllKeys,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("clear_all_keys_button")
        ) {
            Text(stringResource(R.string.settings_clear_keys))
        }
    }
}
