package com.example.travelroulette24.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.travelroulette24.data.byok.ApiCredentialsManager

/**
 * Interactive Step-by-Step Wizard for obtaining and configuring API Keys.
 *
 * Provides a guided 4-step walkthrough:
 * Step 1: Amadeus Self-Service (Flight Inspiration Search)
 * Step 2: Kiwi.com Tequila API (Low-cost European flights)
 * Step 3: Google Gemini API (AI 24h itineraries from AI Studio)
 * Step 4: Review and Save
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

    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = 4

    // Working copies
    var amadeusKey by remember { mutableStateOf(credentialsManager.amadeusApiKey) }
    var amadeusSecret by remember { mutableStateOf(credentialsManager.amadeusApiSecret) }
    var kiwiKey by remember { mutableStateOf(credentialsManager.kiwiApiKey) }
    var geminiKey by remember { mutableStateOf(credentialsManager.geminiApiKey) }

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
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with Step Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🧙 Asistente de Conexión de APIs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Paso $currentStep de $totalSteps",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Text("✕", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Progress Indicator Pills
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (step in 1..totalSteps) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (step <= currentStep) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Step Content Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (currentStep) {
                        1 -> {
                            // STEP 1: AMADEUS
                            WizardStepCard(
                                icon = "✈️",
                                title = "1. Obtener API de Amadeus (Gratis)",
                                subtitle = "2.000 búsquedas al mes para vuelos de aerolíneas tradicionales y globales."
                            )

                            WizardInstructionItem(
                                stepNumber = "1",
                                text = "Pulsa el botón de abajo para ir al portal oficial de desarrolladores de Amadeus."
                            )
                            WizardInstructionItem(
                                stepNumber = "2",
                                text = "Regístrate gratis con tu email. Verifica el correo recibido."
                            )
                            WizardInstructionItem(
                                stepNumber = "3",
                                text = "Entra en 'My Self-Service Workspace' > 'Create New App'. Asigna el nombre 'TravelRoulette'."
                            )
                            WizardInstructionItem(
                                stepNumber = "4",
                                text = "Copia tu 'API Key' y 'API Secret' y pégalos aquí mismo:"
                            )

                            OutlinedButton(
                                onClick = { openUrl("https://developers.amadeus.com/register") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("🔗 Abrir Registro Gratis de Amadeus")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = amadeusKey,
                                onValueChange = { amadeusKey = it.trim() },
                                label = { Text("Amadeus API Key") },
                                placeholder = { Text("Ej: aBcD1234eFgHiJ...") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wizard_amadeus_key")
                            )

                            OutlinedTextField(
                                value = amadeusSecret,
                                onValueChange = { amadeusSecret = it.trim() },
                                label = { Text("Amadeus API Secret") },
                                placeholder = { Text("Ej: sEcReT987654...") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wizard_amadeus_secret")
                            )
                        }

                        2 -> {
                            // STEP 2: KIWI TEQUILA
                            WizardStepCard(
                                icon = "🥝",
                                title = "2. Obtener API de Kiwi Tequila (Gratis)",
                                subtitle = "Especialista en aerolíneas low-cost (Ryanair, EasyJet, Vueling) y rutas 'A Cualquier Lugar'."
                            )

                            WizardInstructionItem(
                                stepNumber = "1",
                                text = "Accede a la plataforma Tequila de Kiwi.com usando el botón inferior."
                            )
                            WizardInstructionItem(
                                stepNumber = "2",
                                text = "Crea una cuenta gratuita de desarrollador y confirma tu cuenta."
                            )
                            WizardInstructionItem(
                                stepNumber = "3",
                                text = "Crea una nueva solución/App y copia la clave del campo 'API Key'."
                            )

                            OutlinedButton(
                                onClick = { openUrl("https://tequila.kiwi.com/portal/login/register") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("🔗 Abrir Portal Tequila de Kiwi")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = kiwiKey,
                                onValueChange = { kiwiKey = it.trim() },
                                label = { Text("Kiwi Tequila API Key") },
                                placeholder = { Text("Ej: VbN940Km9sLLw019...") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wizard_kiwi_key")
                            )
                        }

                        3 -> {
                            // STEP 3: GEMINI AI
                            WizardStepCard(
                                icon = "✨",
                                title = "3. Google Gemini AI (Itinerarios 24h)",
                                subtitle = "Genera planes inteligentes de 24 horas (qué ver, dónde comer, transportes) en un clic."
                            )

                            WizardInstructionItem(
                                stepNumber = "1",
                                text = "Abre Google AI Studio mediante el botón inferior con tu cuenta de Google."
                            )
                            WizardInstructionItem(
                                stepNumber = "2",
                                text = "Pulsa en 'Get API key' > 'Create API key in new project'."
                            )
                            WizardInstructionItem(
                                stepNumber = "3",
                                text = "Copia la clave que empieza por 'AIzaSy...' y pégala aquí:"
                            )

                            OutlinedButton(
                                onClick = { openUrl("https://aistudio.google.com/app/apikey") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("🔗 Abrir Google AI Studio")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = geminiKey,
                                onValueChange = { geminiKey = it.trim() },
                                label = { Text("Google Gemini API Key") },
                                placeholder = { Text("AIzaSy...") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wizard_gemini_key")
                            )
                        }

                        4 -> {
                            // STEP 4: SUMMARY & SAVE
                            WizardStepCard(
                                icon = "✅",
                                title = "4. Resumen y Guardado Seguro",
                                subtitle = "Tus claves se cifran y guardan exclusivamente en la memoria privada de tu teléfono."
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
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StatusRow(
                                        title = "Amadeus API",
                                        isConfigured = amadeusKey.isNotBlank() && amadeusSecret.isNotBlank()
                                    )
                                    StatusRow(
                                        title = "Kiwi Tequila API",
                                        isConfigured = kiwiKey.isNotBlank()
                                    )
                                    StatusRow(
                                        title = "Google Gemini AI",
                                        isConfigured = geminiKey.isNotBlank()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "💡 Recuerda: Si dejas alguna clave vacía, la app utilizará fuentes comunitarias de respaldo (Open HAFAS y simulador) para que nunca te quedes sin resultados.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer Navigation Buttons (Atrás / Siguiente / Finalizar)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("← Anterior")
                        }
                    }

                    Button(
                        onClick = {
                            if (currentStep < totalSteps) {
                                currentStep++
                            } else {
                                // Save all entered credentials
                                credentialsManager.amadeusApiKey = amadeusKey
                                credentialsManager.amadeusApiSecret = amadeusSecret
                                credentialsManager.kiwiApiKey = kiwiKey
                                credentialsManager.geminiApiKey = geminiKey
                                onCompleted()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (currentStep < totalSteps) "Siguiente →" else "✓ Finalizar y Guardar",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WizardStepCard(icon: String, title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 28.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WizardInstructionItem(stepNumber: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatusRow(title: String, isConfigured: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = if (isConfigured) "🟢 Configurada" else "⚪ Sin configurar (Opcional)",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isConfigured) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
