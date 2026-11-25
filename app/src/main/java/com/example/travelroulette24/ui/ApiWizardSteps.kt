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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelroulette24.R
import com.example.travelroulette24.data.byok.ApiCredentialsManager

@Composable
internal fun ApiWizardStepContent(
    currentStep: Int,
    amadeusKey: String,
    onAmadeusKeyChange: (String) -> Unit,
    amadeusSecret: String,
    onAmadeusSecretChange: (String) -> Unit,
    kiwiKey: String,
    onKiwiKeyChange: (String) -> Unit,
    geminiKey: String,
    onGeminiKeyChange: (String) -> Unit,
    openHafasEnabled: Boolean,
    onOpenHafasChange: (Boolean) -> Unit,
    isTestingConnection: Boolean,
    testResultStatus: String?,
    onTestAmadeus: () -> Unit,
    onTestKiwi: () -> Unit,
    onTestGemini: () -> Unit,
    onOpenUrl: (String) -> Unit,
    credentialsManager: ApiCredentialsManager,
    onCompleted: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    when (currentStep) {
        1 -> {
            WizardStepHeaderCard(
                icon = "✈️",
                title = stringResource(R.string.wizard_step1_title),
                subtitle = stringResource(R.string.wizard_step1_subtitle),
                badge = stringResource(R.string.wizard_step1_badge)
            )
            OutlinedButton(
                onClick = { onOpenUrl("https://developers.amadeus.com/register") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.wizard_step1_btn_open))
            }
            PasteableCredentialRow(
                value = amadeusKey,
                onValueChange = onAmadeusKeyChange,
                label = stringResource(R.string.wizard_step1_key_label),
                tag = "wizard_amadeus_key",
                onPaste = {
                    val clip = clipboardManager.getText()?.text?.trim()
                    if (!clip.isNullOrBlank()) onAmadeusKeyChange(clip)
                }
            )
            PasteableCredentialRow(
                value = amadeusSecret,
                onValueChange = onAmadeusSecretChange,
                label = stringResource(R.string.wizard_step1_secret_label),
                tag = "wizard_amadeus_secret",
                onPaste = {
                    val clip = clipboardManager.getText()?.text?.trim()
                    if (!clip.isNullOrBlank()) onAmadeusSecretChange(clip)
                }
            )
            ConnectionTestWidget(isTesting = isTestingConnection, testResult = testResultStatus, onTest = onTestAmadeus)
        }
        2 -> {
            WizardStepHeaderCard(
                icon = "🥝",
                title = stringResource(R.string.wizard_step2_title),
                subtitle = stringResource(R.string.wizard_step2_subtitle),
                badge = stringResource(R.string.wizard_step2_badge)
            )
            OutlinedButton(
                onClick = { onOpenUrl("https://tequila.kiwi.com/portal/login/register") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.wizard_step2_btn_open))
            }
            PasteableCredentialRow(
                value = kiwiKey,
                onValueChange = onKiwiKeyChange,
                label = stringResource(R.string.wizard_step2_key_label),
                tag = "wizard_kiwi_key",
                onPaste = {
                    val clip = clipboardManager.getText()?.text?.trim()
                    if (!clip.isNullOrBlank()) onKiwiKeyChange(clip)
                }
            )
            ConnectionTestWidget(isTesting = isTestingConnection, testResult = testResultStatus, onTest = onTestKiwi)
        }
        3 -> {
            WizardStepHeaderCard(
                icon = "✨",
                title = stringResource(R.string.wizard_step3_title),
                subtitle = stringResource(R.string.wizard_step3_subtitle),
                badge = stringResource(R.string.wizard_step3_badge)
            )
            OutlinedButton(
                onClick = { onOpenUrl("https://aistudio.google.com/app/apikey") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.wizard_step3_btn_open))
            }
            PasteableCredentialRow(
                value = geminiKey,
                onValueChange = onGeminiKeyChange,
                label = stringResource(R.string.wizard_step3_key_label),
                tag = "wizard_gemini_key",
                onPaste = {
                    val clip = clipboardManager.getText()?.text?.trim()
                    if (!clip.isNullOrBlank()) onGeminiKeyChange(clip)
                }
            )
            ConnectionTestWidget(isTesting = isTestingConnection, testResult = testResultStatus, onTest = onTestGemini)
        }
        4 -> {
            WizardStepHeaderCard(
                icon = "🚆",
                title = stringResource(R.string.wizard_step4_title),
                subtitle = stringResource(R.string.wizard_step4_subtitle),
                badge = stringResource(R.string.wizard_step4_badge)
            )
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.wizard_step4_hafas_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.wizard_step4_hafas_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = openHafasEnabled, onCheckedChange = onOpenHafasChange)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.wizard_step4_ferry_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.wizard_step4_ferry_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = stringResource(R.string.wizard_step4_active_status),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
        5 -> {
            WizardStepHeaderCard(
                icon = "⭐",
                title = stringResource(R.string.wizard_step5_title),
                subtitle = stringResource(R.string.wizard_step5_subtitle),
                badge = stringResource(R.string.wizard_step5_badge)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⭐", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.saved_trips_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.saved_trips_empty_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔔", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.price_alert_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.price_alert_info_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        else -> {
            WizardStepHeaderCard(
                icon = "✅",
                title = stringResource(R.string.wizard_step6_title),
                subtitle = stringResource(R.string.wizard_step6_subtitle),
                badge = stringResource(R.string.wizard_step6_badge)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(R.string.wizard_quick_mode_title),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.wizard_quick_mode_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FilledTonalButton(
                        onClick = {
                            credentialsManager.activatePublicLiveProviders()
                            onCompleted()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.wizard_btn_activate_public), fontWeight = FontWeight.Bold)
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔒", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.wizard_privacy_guarantee),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PasteableCredentialRow(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    tag: String,
    onPaste: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .testTag(tag)
        )
        Spacer(modifier = Modifier.width(6.dp))
        FilledTonalButton(
            onClick = onPaste,
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(stringResource(R.string.btn_paste), fontSize = 12.sp)
        }
    }
}

@Composable
private fun WizardStepHeaderCard(icon: String, title: String, subtitle: String, badge: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = icon, fontSize = 26.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ConnectionTestWidget(
    isTesting: Boolean,
    testResult: String?,
    onTest: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = onTest,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isTesting,
            shape = RoundedCornerShape(10.dp)
        ) {
            if (isTesting) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.btn_testing), fontSize = 13.sp)
            } else {
                Text(stringResource(R.string.btn_test_connection), fontSize = 13.sp)
            }
        }

        testResult?.let { msg ->
            Spacer(modifier = Modifier.height(6.dp))
            val isSuccess = msg.startsWith("✓")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSuccess) MaterialTheme.colorScheme.tertiaryContainer
                        else MaterialTheme.colorScheme.errorContainer
                    )
                    .padding(8.dp)
            ) {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSuccess) MaterialTheme.colorScheme.onTertiaryContainer
                    else MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
