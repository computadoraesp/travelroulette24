package com.example.travelroulette24.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.travelroulette24.R

@Composable
internal fun FlightsAndAggregatorsSection(
    amadeusKey: String,
    onAmadeusKeyChange: (String) -> Unit,
    amadeusSecret: String,
    onAmadeusSecretChange: (String) -> Unit,
    amadeusIsPaid: Boolean,
    onAmadeusIsPaidChange: (Boolean) -> Unit,
    amadeusUseProd: Boolean,
    onAmadeusUseProdChange: (Boolean) -> Unit,
    travelPayoutsToken: String,
    onTravelPayoutsTokenChange: (String) -> Unit,
    kiwiKey: String,
    onKiwiKeyChange: (String) -> Unit,
    kiwiIsPaid: Boolean,
    onKiwiIsPaidChange: (Boolean) -> Unit,
    rapidKey: String,
    onRapidKeyChange: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    expandAllEpoch: Int,
    expandAllTarget: Boolean
) {
    val configuredFlightsCount = listOf(
        amadeusKey.isNotBlank() && amadeusSecret.isNotBlank(),
        travelPayoutsToken.isNotBlank(),
        kiwiKey.isNotBlank(),
        rapidKey.isNotBlank()
    ).count { it }

    CollapsibleSettingsSection(
        title = stringResource(R.string.settings_section_flights_title),
        summary = "Amadeus · TravelPayouts · Kiwi · RapidAPI",
        badgeText = stringResource(R.string.settings_configured_count_format, configuredFlightsCount, 4),
        isBadgeHighlighted = configuredFlightsCount > 0,
        expandAllEpoch = expandAllEpoch,
        expandAllTarget = expandAllTarget
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SettingsApiCard(
                title = stringResource(R.string.settings_amadeus_title),
                badge = if (amadeusIsPaid) stringResource(R.string.custom_plan_paid) else stringResource(R.string.settings_amadeus_free_badge),
                description = stringResource(R.string.settings_amadeus_desc),
                isConfigured = amadeusKey.isNotBlank() && amadeusSecret.isNotBlank(),
                onGetFreeKey = { onOpenUrl("https://developers.amadeus.com/register") }
            ) {
                PlanTierSelector(
                    isPaid = amadeusIsPaid,
                    onPlanChange = onAmadeusIsPaidChange,
                    freeDescription = stringResource(R.string.settings_amadeus_free_desc),
                    paidDescription = stringResource(R.string.settings_amadeus_paid_desc)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = amadeusKey,
                    onValueChange = onAmadeusKeyChange,
                    label = { Text(stringResource(R.string.settings_amadeus_key_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings_amadeus_key")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amadeusSecret,
                    onValueChange = onAmadeusSecretChange,
                    label = { Text(stringResource(R.string.settings_amadeus_secret_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings_amadeus_secret")
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
                        onCheckedChange = onAmadeusUseProdChange,
                        modifier = Modifier.testTag("settings_amadeus_prod_switch")
                    )
                }
            }

            SettingsApiCard(
                title = stringResource(R.string.settings_travelpayouts_title),
                badge = stringResource(R.string.settings_travelpayouts_badge),
                description = stringResource(R.string.settings_travelpayouts_desc),
                isConfigured = travelPayoutsToken.isNotBlank(),
                onGetFreeKey = { onOpenUrl("https://www.travelpayouts.com/programs/100/tools/api") }
            ) {
                OutlinedTextField(
                    value = travelPayoutsToken,
                    onValueChange = onTravelPayoutsTokenChange,
                    label = { Text(stringResource(R.string.settings_travelpayouts_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings_travelpayouts_token")
                )
            }

            SettingsApiCard(
                title = stringResource(R.string.settings_kiwi_title),
                badge = if (kiwiIsPaid) stringResource(R.string.custom_plan_paid) else stringResource(R.string.settings_kiwi_free_badge),
                description = stringResource(R.string.settings_kiwi_desc),
                isConfigured = kiwiKey.isNotBlank(),
                onGetFreeKey = { onOpenUrl("https://tequila.kiwi.com/portal/login/register") }
            ) {
                PlanTierSelector(
                    isPaid = kiwiIsPaid,
                    onPlanChange = onKiwiIsPaidChange,
                    freeDescription = stringResource(R.string.settings_kiwi_free_desc),
                    paidDescription = stringResource(R.string.settings_kiwi_paid_desc)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = kiwiKey,
                    onValueChange = onKiwiKeyChange,
                    label = { Text(stringResource(R.string.settings_kiwi_key_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings_kiwi_key")
                )
            }

            SettingsApiCard(
                title = stringResource(R.string.settings_rapidapi_title),
                badge = stringResource(R.string.settings_rapidapi_badge),
                description = stringResource(R.string.settings_rapidapi_desc),
                isConfigured = rapidKey.isNotBlank(),
                onGetFreeKey = { onOpenUrl("https://rapidapi.com/auth/sign-up") }
            ) {
                OutlinedTextField(
                    value = rapidKey,
                    onValueChange = onRapidKeyChange,
                    label = { Text(stringResource(R.string.settings_rapidapi_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings_rapidapi_key")
                )
            }
        }
    }
}

@Composable
internal fun AiRadarCurrencySection(
    geminiKey: String,
    onGeminiKeyChange: (String) -> Unit,
    aviationKey: String,
    onAviationKeyChange: (String) -> Unit,
    exchangeRateKey: String,
    onExchangeRateKeyChange: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    expandAllEpoch: Int,
    expandAllTarget: Boolean
) {
    val configuredAiCount = listOf(
        geminiKey.isNotBlank(),
        aviationKey.isNotBlank(),
        exchangeRateKey.isNotBlank()
    ).count { it }

    CollapsibleSettingsSection(
        title = stringResource(R.string.settings_section_intelligence_title),
        summary = "Gemini · AviationStack · ExchangeRate",
        badgeText = stringResource(R.string.settings_configured_count_format, configuredAiCount, 3),
        isBadgeHighlighted = configuredAiCount > 0,
        expandAllEpoch = expandAllEpoch,
        expandAllTarget = expandAllTarget
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SettingsApiCard(
                title = stringResource(R.string.settings_gemini_title),
                badge = stringResource(R.string.settings_gemini_badge),
                description = stringResource(R.string.settings_gemini_desc),
                isConfigured = geminiKey.isNotBlank(),
                onGetFreeKey = { onOpenUrl("https://aistudio.google.com/app/apikey") }
            ) {
                OutlinedTextField(
                    value = geminiKey,
                    onValueChange = onGeminiKeyChange,
                    label = { Text(stringResource(R.string.settings_gemini_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings_gemini_key")
                )
            }

            SettingsApiCard(
                title = stringResource(R.string.settings_aviation_title),
                badge = stringResource(R.string.settings_aviation_badge),
                description = stringResource(R.string.settings_aviation_desc),
                isConfigured = aviationKey.isNotBlank(),
                onGetFreeKey = { onOpenUrl("https://aviationstack.com/signup/free") }
            ) {
                OutlinedTextField(
                    value = aviationKey,
                    onValueChange = onAviationKeyChange,
                    label = { Text(stringResource(R.string.settings_aviation_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings_aviation_key")
                )
            }

            SettingsApiCard(
                title = stringResource(R.string.settings_exchange_title),
                badge = stringResource(R.string.settings_exchange_badge),
                description = stringResource(R.string.settings_exchange_desc),
                isConfigured = exchangeRateKey.isNotBlank(),
                onGetFreeKey = { onOpenUrl("https://www.exchangerate-api.com/") }
            ) {
                OutlinedTextField(
                    value = exchangeRateKey,
                    onValueChange = onExchangeRateKeyChange,
                    label = { Text(stringResource(R.string.settings_exchange_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings_exchangerate_key")
                )
            }
        }
    }
}

@Composable
internal fun TransitAndTourismSection(
    navitiaToken: String,
    onNavitiaTokenChange: (String) -> Unit,
    openHafas: Boolean,
    onOpenHafasChange: (Boolean) -> Unit,
    openTripMapKey: String,
    onOpenTripMapKeyChange: (String) -> Unit,
    unsplashKey: String,
    onUnsplashKeyChange: (String) -> Unit,
    tripHubUrl: String,
    onTripHubUrlChange: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    expandAllEpoch: Int,
    expandAllTarget: Boolean
) {
    val configuredTrainsCount = listOf(navitiaToken.isNotBlank(), openHafas).count { it }
    CollapsibleSettingsSection(
        title = stringResource(R.string.settings_section_trains_title),
        summary = "Navitia · Open-HAFAS",
        badgeText = stringResource(R.string.settings_configured_count_format, configuredTrainsCount, 2),
        isBadgeHighlighted = configuredTrainsCount > 0,
        expandAllEpoch = expandAllEpoch,
        expandAllTarget = expandAllTarget
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SettingsApiCard(
                title = stringResource(R.string.settings_navitia_title),
                badge = stringResource(R.string.settings_navitia_badge),
                description = stringResource(R.string.settings_navitia_desc),
                isConfigured = navitiaToken.isNotBlank(),
                onGetFreeKey = { onOpenUrl("https://www.navitia.io/register/") }
            ) {
                OutlinedTextField(
                    value = navitiaToken,
                    onValueChange = onNavitiaTokenChange,
                    label = { Text(stringResource(R.string.settings_navitia_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings_navitia_token")
                )
            }

            CollapsibleSettingsSection(
                title = stringResource(R.string.settings_hafas_title),
                summary = stringResource(R.string.settings_hafas_desc),
                badgeText = stringResource(R.string.settings_hafas_badge),
                isBadgeHighlighted = openHafas
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_hafas_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Switch(
                        checked = openHafas,
                        onCheckedChange = onOpenHafasChange
                    )
                }
            }
        }
    }

    val configuredTourismCount = listOf(
        openTripMapKey.isNotBlank(),
        unsplashKey.isNotBlank(),
        tripHubUrl.isNotBlank()
    ).count { it }
    CollapsibleSettingsSection(
        title = stringResource(R.string.settings_section_tourism_title),
        summary = "OpenTripMap · Unsplash · Trip Hub",
        badgeText = stringResource(R.string.settings_configured_count_format, configuredTourismCount, 3),
        isBadgeHighlighted = configuredTourismCount > 0,
        expandAllEpoch = expandAllEpoch,
        expandAllTarget = expandAllTarget
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SettingsApiCard(
                title = stringResource(R.string.settings_otm_title),
                badge = stringResource(R.string.settings_otm_badge),
                description = stringResource(R.string.settings_otm_desc),
                isConfigured = openTripMapKey.isNotBlank(),
                onGetFreeKey = { onOpenUrl("https://opentripmap.io/product") }
            ) {
                OutlinedTextField(
                    value = openTripMapKey,
                    onValueChange = onOpenTripMapKeyChange,
                    label = { Text(stringResource(R.string.settings_otm_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SettingsApiCard(
                title = stringResource(R.string.settings_unsplash_title),
                badge = stringResource(R.string.settings_unsplash_badge),
                description = stringResource(R.string.settings_unsplash_desc),
                isConfigured = unsplashKey.isNotBlank(),
                onGetFreeKey = { onOpenUrl("https://unsplash.com/developers") }
            ) {
                OutlinedTextField(
                    value = unsplashKey,
                    onValueChange = onUnsplashKeyChange,
                    label = { Text(stringResource(R.string.settings_unsplash_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            CollapsibleSettingsSection(
                title = stringResource(R.string.settings_triphub_title),
                summary = stringResource(R.string.settings_triphub_desc),
                badgeText = stringResource(R.string.settings_triphub_badge),
                isBadgeHighlighted = tripHubUrl.isNotBlank()
            ) {
                Text(
                    text = stringResource(R.string.settings_triphub_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = tripHubUrl,
                    onValueChange = onTripHubUrlChange,
                    label = { Text(stringResource(R.string.settings_triphub_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings_triphub_url")
                )
            }
        }
    }
}
