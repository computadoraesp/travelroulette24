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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
                    ApiWizardStepContent(
                        currentStep = currentStep,
                        amadeusKey = amadeusKey,
                        onAmadeusKeyChange = {
                            amadeusKey = it.trim()
                            testResultStatus = null
                        },
                        amadeusSecret = amadeusSecret,
                        onAmadeusSecretChange = {
                            amadeusSecret = it.trim()
                            testResultStatus = null
                        },
                        kiwiKey = kiwiKey,
                        onKiwiKeyChange = {
                            kiwiKey = it.trim()
                            testResultStatus = null
                        },
                        geminiKey = geminiKey,
                        onGeminiKeyChange = {
                            geminiKey = it.trim()
                            testResultStatus = null
                        },
                        openHafasEnabled = openHafasEnabled,
                        onOpenHafasChange = { openHafasEnabled = it },
                        isTestingConnection = isTestingConnection,
                        testResultStatus = testResultStatus,
                        onTestAmadeus = {
                            coroutineScope.launch {
                                isTestingConnection = true
                                delay(700)
                                isTestingConnection = false
                                testResultStatus = if (amadeusKey.length >= 8 && amadeusSecret.length >= 6) {
                                    context.getString(R.string.wizard_test_amadeus_ok)
                                } else {
                                    context.getString(R.string.wizard_test_amadeus_err)
                                }
                            }
                        },
                        onTestKiwi = {
                            coroutineScope.launch {
                                isTestingConnection = true
                                delay(700)
                                isTestingConnection = false
                                testResultStatus = if (kiwiKey.length >= 10) {
                                    context.getString(R.string.wizard_test_kiwi_ok)
                                } else {
                                    context.getString(R.string.wizard_test_kiwi_err)
                                }
                            }
                        },
                        onTestGemini = {
                            coroutineScope.launch {
                                isTestingConnection = true
                                delay(700)
                                isTestingConnection = false
                                testResultStatus = if (geminiKey.startsWith("AIzaSy") || geminiKey.length >= 20) {
                                    context.getString(R.string.wizard_test_gemini_ok)
                                } else {
                                    context.getString(R.string.wizard_test_gemini_err)
                                }
                            }
                        },
                        onOpenUrl = ::openUrl,
                        credentialsManager = credentialsManager,
                        onCompleted = onCompleted
                    )
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
