package com.example.travelroulette24.ui

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import com.example.travelroulette24.network.HubResponse
import com.example.travelroulette24.network.TripHubHelper
import com.example.travelroulette24.utils.QRCodeUtil
import kotlinx.coroutines.launch

@Composable
fun TripHubDialog(
    item: TravelIntelligenceEngine.ResultItem,
    originCity: String,
    hubs: Map<String, List<String>>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var centralized by remember { mutableStateOf(false) }
    var requiredCount by remember { mutableStateOf(2) }
    var isCreating by remember { mutableStateOf(false) }
    var hubId by remember { mutableStateOf<String?>(null) }
    var shortUrl by remember { mutableStateOf<String?>(null) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var responses by remember { mutableStateOf<List<HubResponse>>(emptyList()) }

    val destCity = HubCoordinates.getCity(item.destinationHub)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Invitar a $destCity (€${item.price.toInt()})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Crea un Hub grupal para coordinar la escapada con amigos. Podrán votar o unirse escaneando el código QR.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Centralized booking switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Compra centralizada",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "El organizador compra los billetes para todos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = centralized,
                        onCheckedChange = { centralized = it }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Required participants count
                Text(
                    text = "Mínimo de participantes: $requiredCount personas",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = requiredCount.toFloat(),
                    onValueChange = { requiredCount = it.toInt() },
                    valueRange = 1f..8f,
                    steps = 6
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Action to generate hub
                if (shortUrl == null) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isCreating = true
                                try {
                                    val (id, url) = TripHubHelper.createHub(
                                        offerId = "${item.originHub}-${item.destinationHub}-${item.price}",
                                        originCity = originCity,
                                        hubCodes = hubs,
                                        ttlHours = 24,
                                        centralized = centralized,
                                        requiredCount = requiredCount
                                    )
                                    hubId = id
                                    shortUrl = url
                                    qrBitmap = QRCodeUtil.generate(url)
                                } catch (_: Exception) {
                                    // Fallback demo URL if server hub is unreachable
                                    val demoUrl = "https://travelroulette24.app/hub/${item.destinationHub}"
                                    shortUrl = demoUrl
                                    qrBitmap = QRCodeUtil.generate(demoUrl)
                                } finally {
                                    isCreating = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isCreating
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Generar Hub y Código QR")
                        }
                    }
                }

                // If QR code generated
                qrBitmap?.let { bmp ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Código QR para invitar amigos",
                            modifier = Modifier.size(180.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        shortUrl?.let { url ->
                            Text(
                                text = url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "¡Nos vamos a $destCity por €${item.price.toInt()}! Únete aquí: $url"
                                        )
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Compartir viaje"))
                                }
                            ) {
                                Text("Compartir enlace")
                            }
                        }
                    }
                }

                // Poll responses if hub created
                hubId?.let { id ->
                    LaunchedEffect(id) {
                        TripHubHelper.pollResponses(id).collect { list ->
                            responses = list
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Respuestas en directo (${responses.size}):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    responses.forEach { resp ->
                        Text(
                            text = "• ${resp.nickname}: ${resp.answer}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
