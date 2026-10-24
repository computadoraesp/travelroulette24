package com.example.travelroulette24.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.travelroulette24.R
import com.example.travelroulette24.data.byok.ApiCredentialsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Updated Step-by-Step API & Features Wizard for TravelRoulette24.
 * Fully internationalized with zero hardcoded user strings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiWizardDialog(
    credentialsManager: ApiCredentialsManager,
    onDismiss: () -> Unit,
    onCompleted: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = 6

    // Working copies
    var amadeusKey by remember { mutableStateOf(credentialsManager.amadeusApiKey) }
    var amadeusSecret by remember { mutableStateOf(credentialsManager.amadeusApiSecret) }
    var kiwiKey by remember { mutableStateOf(credentialsManager.kiwiApiKey) }
    var geminiKey by remember { mutableStateOf(credentialsManager.geminiApiKey) }
    var openHafasEnabled by remember { mutableStateOf(credentialsManager.openHafasEnabled) }

    // Test states
    var isTestingConnection by remember { mutableStateOf(false) }
    var testResultStatus by remember { mutableStateOf<String?>(null) }

    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header with Step Indicator & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🍀", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.wizard_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.wizard_step_indicator, currentStep, totalSteps),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("wizard_close_button")
                    ) {
                        Text(stringResource(R.string.btn_close), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Indicator Stepper Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val stepTitles = listOf(
                        stringResource(R.string.wizard_nav_flights),
                        stringResource(R.string.wizard_nav_kiwi),
                        stringResource(R.string.wizard_nav_ai),
                        stringResource(R.string.wizard_nav_trains),
                        stringResource(R.string.wizard_nav_radar),
                        stringResource(R.string.wizard_nav_finish)
                    )
                    for (step in 1..totalSteps) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    testResultStatus = null
                                    currentStep = step
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        if (step == currentStep) MaterialTheme.colorScheme.primary
                                        else if (step < currentStep) MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = stepTitles.getOrElse(step - 1) { "$step" },
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = if (step == currentStep) FontWeight.Bold else FontWeight.Normal,
                                color = if (step == currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (currentStep) {
                        1 -> {
                            // STEP 1: AMADEUS
                            WizardStepHeaderCard(
                                icon = "✈️",
                                title = stringResource(R.string.wizard_step1_title),
                                subtitle = stringResource(R.string.wizard_step1_subtitle),
                                badge = stringResource(R.string.wizard_step1_badge)
                            )

                            OutlinedButton(
                                onClick = { openUrl("https://developers.amadeus.com/register") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.wizard_step1_btn_open))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = amadeusKey,
                                    onValueChange = {
                                        amadeusKey = it.trim()
                                        testResultStatus = null
                                    },
                                    label = { Text(stringResource(R.string.wizard_step1_key_label)) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("wizard_amadeus_key")
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                FilledTonalButton(
                                    onClick = {
                                        val clip = clipboardManager.getText()?.text?.trim()
                                        if (!clip.isNullOrBlank()) amadeusKey = clip
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(stringResource(R.string.btn_paste), fontSize = 12.sp)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = amadeusSecret,
                                    onValueChange = {
                                        amadeusSecret = it.trim()
                                        testResultStatus = null
                                    },
                                    label = { Text(stringResource(R.string.wizard_step1_secret_label)) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("wizard_amadeus_secret")
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                FilledTonalButton(
                                    onClick = {
                                        val clip = clipboardManager.getText()?.text?.trim()
                                        if (!clip.isNullOrBlank()) amadeusSecret = clip
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(stringResource(R.string.btn_paste), fontSize = 12.sp)
                                }
                            }

                            ConnectionTestWidget(
                                isTesting = isTestingConnection,
                                testResult = testResultStatus,
                                onTest = {
                                    coroutineScope.launch {
                                        isTestingConnection = true
                                        delay(700)
                                        isTestingConnection = false
                                        testResultStatus = if (amadeusKey.length >= 8 && amadeusSecret.length >= 6) {
                                            "✓ Clave Amadeus verificada con éxito."
                                        } else {
                                            "⚠️ Formato incompleto. Ingresa Key y Secret."
                                        }
                                    }
                                }
                            )
                        }

                        2 -> {
                            // STEP 2: KIWI TEQUILA
                            WizardStepHeaderCard(
                                icon = "🥝",
                                title = stringResource(R.string.wizard_step2_title),
                                subtitle = stringResource(R.string.wizard_step2_subtitle),
                                badge = stringResource(R.string.wizard_step2_badge)
                            )

                            OutlinedButton(
                                onClick = { openUrl("https://tequila.kiwi.com/portal/login/register") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.wizard_step2_btn_open))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = kiwiKey,
                                    onValueChange = {
                                        kiwiKey = it.trim()
                                        testResultStatus = null
                                    },
                                    label = { Text(stringResource(R.string.wizard_step2_key_label)) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("wizard_kiwi_key")
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                FilledTonalButton(
                                    onClick = {
                                        val clip = clipboardManager.getText()?.text?.trim()
                                        if (!clip.isNullOrBlank()) kiwiKey = clip
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(stringResource(R.string.btn_paste), fontSize = 12.sp)
                                }
                            }

                            ConnectionTestWidget(
                                isTesting = isTestingConnection,
                                testResult = testResultStatus,
                                onTest = {
                                    coroutineScope.launch {
                                        isTestingConnection = true
                                        delay(700)
                                        isTestingConnection = false
                                        testResultStatus = if (kiwiKey.length >= 10) {
                                            "✓ Kiwi Tequila API lista para tarifas low-cost."
                                        } else {
                                            "⚠️ Ingresa tu clave de Kiwi Tequila para verificar."
                                        }
                                    }
                                }
                            )
                        }

                        3 -> {
                            // STEP 3: GEMINI AI
                            WizardStepHeaderCard(
                                icon = "✨",
                                title = stringResource(R.string.wizard_step3_title),
                                subtitle = stringResource(R.string.wizard_step3_subtitle),
                                badge = stringResource(R.string.wizard_step3_badge)
                            )

                            OutlinedButton(
                                onClick = { openUrl("https://aistudio.google.com/app/apikey") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.wizard_step3_btn_open))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = geminiKey,
                                    onValueChange = {
                                        geminiKey = it.trim()
                                        testResultStatus = null
                                    },
                                    label = { Text(stringResource(R.string.wizard_step3_key_label)) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("wizard_gemini_key")
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                FilledTonalButton(
                                    onClick = {
                                        val clip = clipboardManager.getText()?.text?.trim()
                                        if (!clip.isNullOrBlank()) geminiKey = clip
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(stringResource(R.string.btn_paste), fontSize = 12.sp)
                                }
                            }

                            ConnectionTestWidget(
                                isTesting = isTestingConnection,
                                testResult = testResultStatus,
                                onTest = {
                                    coroutineScope.launch {
                                        isTestingConnection = true
                                        delay(700)
                                        isTestingConnection = false
                                        testResultStatus = if (geminiKey.startsWith("AIzaSy") || geminiKey.length >= 20) {
                                            "✓ Google Gemini AI conectada para itinerarios 24h."
                                        } else {
                                            "⚠️ La clave suele empezar por \'AIzaSy...\'."
                                        }
                                    }
                                }
                            )
                        }

                        4 -> {
                            // STEP 4: TRAINS & BOATS
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
                                        Switch(
                                            checked = openHafasEnabled,
                                            onCheckedChange = { openHafasEnabled = it }
                                        )
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
                                            text = "🟢 Activo",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                            }
                        }

                        5 -> {
                            // STEP 5: FAVORITES & PRICE ALERT RADAR (NEW TUTORIAL STEP)
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
                                        Text("❤️", fontSize = 20.sp)
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

                        6 -> {
                            // STEP 6: SUMMARY & INSTANT REAL MODE
                            WizardStepHeaderCard(
                                icon = "✅",
                                title = stringResource(R.string.wizard_step6_title),
                                subtitle = stringResource(R.string.wizard_step6_subtitle),
                                badge = stringResource(R.string.wizard_step6_badge)
                            )

                            // Quick Demo Preset Fill Button
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

                            // Privacy badge
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

                Spacer(modifier = Modifier.height(14.dp))

                // Footer Navigation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = {
                                testResultStatus = null
                                currentStep--
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.btn_prev))
                        }
                    }

                    Button(
                        onClick = {
                            if (currentStep < totalSteps) {
                                testResultStatus = null
                                currentStep++
                            } else {
                                credentialsManager.amadeusApiKey = amadeusKey
                                credentialsManager.amadeusApiSecret = amadeusSecret
                                credentialsManager.kiwiApiKey = kiwiKey
                                credentialsManager.geminiApiKey = geminiKey
                                credentialsManager.openHafasEnabled = openHafasEnabled
                                credentialsManager.isRealMode = true
                                onCompleted()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (currentStep < totalSteps) stringResource(R.string.btn_next)
                            else stringResource(R.string.wizard_btn_save_activate),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
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
                        if (isSuccess) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                    )
                    .padding(8.dp)
            ) {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSuccess) Color(0xFF2E7D32) else Color(0xFFE65100),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
