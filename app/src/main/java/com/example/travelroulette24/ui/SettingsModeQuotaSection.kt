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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelroulette24.R
import com.example.travelroulette24.data.byok.ApiQuotaGovernor

@Composable
internal fun ModeAndQuotaSection(
    isRealMode: Boolean,
    hasActiveKeys: Boolean,
    onToggleRealMode: (Boolean) -> Unit,
    onActivatePublicProviders: () -> Unit,
    onOpenWizard: () -> Unit,
    onOpenManual: () -> Unit,
    amadeusIsPaid: Boolean,
    amadeusQuota: ApiQuotaGovernor.QuotaStatus?,
    kiwiIsPaid: Boolean,
    kiwiQuota: ApiQuotaGovernor.QuotaStatus?,
    customUrl: String,
    customIsPaid: Boolean,
    customDailyLimit: Int,
    customQuota: ApiQuotaGovernor.QuotaStatus?,
    hasGovernor: Boolean,
    onResetProviderStats: (String) -> Unit,
    onClearCache: () -> Unit,
    cacheClearedMessage: Boolean,
    quotaResetMessage: Boolean,
    expandAllEpoch: Int,
    expandAllTarget: Boolean
) {
    CollapsibleSettingsSection(
        title = if (isRealMode) stringResource(R.string.mode_real_title) else stringResource(R.string.mode_demo_title),
        summary = if (isRealMode) {
            if (hasActiveKeys) stringResource(R.string.settings_real_byok_title)
            else stringResource(R.string.settings_real_live_title)
        } else stringResource(R.string.mode_demo_desc),
        badgeText = if (isRealMode) "REAL" else "DEMO",
        isBadgeHighlighted = isRealMode,
        expandAllEpoch = expandAllEpoch,
        expandAllTarget = expandAllTarget,
        containerColor = if (isRealMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        borderColor = if (isRealMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        testTag = "mode_selector_card"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isRealMode) stringResource(R.string.mode_real_title) else stringResource(R.string.mode_demo_title),
                    style = MaterialTheme.typography.titleSmall,
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
                onCheckedChange = onToggleRealMode,
                modifier = Modifier.testTag("real_mode_switch")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        FilledTonalButton(
            onClick = onActivatePublicProviders,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("activate_public_providers_button"),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(stringResource(R.string.settings_mode_activate_public), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isRealMode) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRealMode) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRealMode) "🟢" else "⚪",
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isRealMode) {
                            if (hasActiveKeys) stringResource(R.string.settings_real_byok_title)
                            else stringResource(R.string.settings_real_live_title)
                        } else stringResource(R.string.mode_demo_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isRealMode) stringResource(R.string.settings_real_banner_desc)
                        else stringResource(R.string.settings_demo_banner_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    CollapsibleSettingsSection(
        title = "🧙 ${stringResource(R.string.settings_wizard_card_title)}",
        summary = stringResource(R.string.settings_wizard_card_desc),
        expandAllEpoch = expandAllEpoch,
        expandAllTarget = expandAllTarget,
        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
        testTag = "open_api_wizard_card"
    ) {
        Text(
            text = stringResource(R.string.settings_wizard_card_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onOpenWizard,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(stringResource(R.string.btn_open_tutorial), fontWeight = FontWeight.Bold)
        }
    }

    CollapsibleSettingsSection(
        title = "📖 ${stringResource(R.string.manual_dialog_title)}",
        summary = stringResource(R.string.manual_dialog_subtitle),
        expandAllEpoch = expandAllEpoch,
        expandAllTarget = expandAllTarget,
        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
        testTag = "open_manual_card"
    ) {
        Text(
            text = stringResource(R.string.manual_dialog_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onOpenManual,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("settings_open_manual_button")
        ) {
            Text(stringResource(R.string.settings_open_manual_btn), fontWeight = FontWeight.Bold)
        }
    }

    CollapsibleSettingsSection(
        title = "🛡️ ${stringResource(R.string.settings_quota_title)}",
        summary = "Amadeus: ${amadeusQuota?.requestsToday ?: 0} · Kiwi: ${kiwiQuota?.requestsToday ?: 0}",
        badgeText = stringResource(R.string.settings_quota_saving_badge),
        isBadgeHighlighted = true,
        expandAllEpoch = expandAllEpoch,
        expandAllTarget = expandAllTarget,
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    ) {
        Text(
            text = stringResource(R.string.settings_quota_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

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
                onResetStats = if (hasGovernor) ({ onResetProviderStats("amadeus") }) else null,
                modifier = Modifier.weight(1f)
            )
            QuotaMetricBox(
                providerName = "Kiwi",
                isPaid = kiwiIsPaid,
                requestsToday = kiwiQuota?.requestsToday ?: 0,
                dailyBudget = kiwiQuota?.dailyBudget ?: 60,
                requestsMonth = kiwiQuota?.requestsThisMonth ?: 0,
                monthlyLimit = 2000,
                onResetStats = if (hasGovernor) ({ onResetProviderStats("kiwi") }) else null,
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
                onResetStats = if (hasGovernor) ({ onResetProviderStats("custom_flight_flight_primary") }) else null,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onClearCache) {
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
