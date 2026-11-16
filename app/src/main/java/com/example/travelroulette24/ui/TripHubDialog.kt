package com.example.travelroulette24.ui

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.travelroulette24.R
import com.example.travelroulette24.data.DemoTravelDataSource
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import com.example.travelroulette24.network.HubResponse
import com.example.travelroulette24.network.TripHubHelper
import com.example.travelroulette24.utils.CurrencyHelper
import com.example.travelroulette24.utils.QRCodeUtil
import com.example.travelroulette24.utils.ShortLinkHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TripHubDialog(
    item: TravelIntelligenceEngine.ResultItem,
    originCity: String,
    hubs: Map<String, List<String>>,
    isRealMode: Boolean = false,
    tripHubBaseUrl: String = "",
    currencyCode: String = CurrencyHelper.EUR,
    currencyRate: Double = 1.0,
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
    var liveFallbackNotice by remember { mutableStateOf(false) }
    var rsvpNickname by remember { mutableStateOf("") }
    var rsvpSent by remember { mutableStateOf(false) }
    var isSendingRsvp by remember { mutableStateOf(false) }

    val destCity = HubCoordinates.getCity(item.destinationHub)
    val formattedPrice = CurrencyHelper.formatPrice(item.price, currencyCode, currencyRate)

    // Live polling of participant RSVPs when connected to a live Trip Hub server in Real Mode
    LaunchedEffect(hubId, tripHubBaseUrl, isRealMode) {
        val id = hubId
        if (isRealMode && !id.isNullOrBlank() && tripHubBaseUrl.isNotBlank()) {
            TripHubHelper.pollResponses(baseUrl = tripHubBaseUrl, hubId = id).collect { incoming ->
                responses = incoming
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.hub_dialog_title),
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
                    text = stringResource(R.string.hub_dialog_qr_instruction),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Wired group size slider
                Text(
                    text = stringResource(R.string.hub_companions_label, requiredCount),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = requiredCount.toFloat(),
                    onValueChange = { requiredCount = it.roundToInt().coerceIn(2, 8) },
                    valueRange = 2f..8f,
                    steps = 5,
                    enabled = shortUrl == null
                )

                // Wired shared-pool switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.hub_centralized_label),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.hub_centralized_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = centralized,
                        onCheckedChange = { centralized = it },
                        enabled = shortUrl == null
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action to generate hub (strictly separated Demo vs Real)
                if (shortUrl == null) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isCreating = true
                                liveFallbackNotice = false
                                if (!isRealMode) {
                                    val demoUrl = DemoTravelDataSource.buildDemoShareUrl(
                                        destinationHub = item.destinationHub,
                                        centralized = centralized,
                                        requiredCount = requiredCount
                                    )
                                    shortUrl = demoUrl
                                    qrBitmap = QRCodeUtil.generate(demoUrl)
                                    isCreating = false
                                } else if (tripHubBaseUrl.isNotBlank()) {
                                    try {
                                        val (id, url) = TripHubHelper.createHub(
                                            baseUrl = tripHubBaseUrl,
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
                                        liveFallbackNotice = true
                                        val directUrl = withContext(Dispatchers.IO) {
                                            ShortLinkHelper.shorten(item.deepLink)
                                        }
                                        shortUrl = directUrl
                                        qrBitmap = QRCodeUtil.generate(directUrl)
                                    } finally {
                                        isCreating = false
                                    }
                                } else {
                                    // Real Mode without dedicated Trip Hub server: generate direct booking QR + TinyURL without mixing Demo links
                                    liveFallbackNotice = true
                                    val directUrl = withContext(Dispatchers.IO) {
                                        ShortLinkHelper.shorten(item.deepLink)
                                    }
                                    shortUrl = directUrl
                                    qrBitmap = QRCodeUtil.generate(directUrl)
                                    isCreating = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_trip_hub_button"),
                        enabled = !isCreating
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.btn_share_qr))
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
                        Text(
                            text = when {
                                !isRealMode -> stringResource(R.string.hub_mode_demo_tag)
                                liveFallbackNotice -> stringResource(R.string.hub_error_no_backend)
                                else -> stringResource(R.string.hub_mode_live_tag)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isRealMode && !liveFallbackNotice) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = stringResource(R.string.btn_share_qr),
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
                                            "TravelRoulette24: $destCity • $formattedPrice -> $url"
                                        )
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "TravelRoulette24"))
                                }
                            ) {
                                Text(stringResource(R.string.hub_dialog_share_link))
                            }
                        }

                        // Live RSVP Section when Trip Hub backend is connected
                        if (isRealMode && !hubId.isNullOrBlank() && tripHubBaseUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(12.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = stringResource(R.string.hub_live_responses_title, responses.size, requiredCount),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (responses.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            responses.forEach { resp ->
                                                val badgeEmoji = if (resp.answer.equals("yes", ignoreCase = true)) "✅" else "❌"
                                                SuggestionChip(
                                                    onClick = {},
                                                    label = { Text("$badgeEmoji ${resp.nickname}") }
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = rsvpNickname,
                                        onValueChange = { rsvpNickname = it; rsvpSent = false },
                                        label = { Text(stringResource(R.string.hub_rsvp_nickname_label)) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val id = hubId ?: return@Button
                                                val nick = rsvpNickname.trim().ifBlank { "Traveler" }
                                                coroutineScope.launch {
                                                    isSendingRsvp = true
                                                    try {
                                                        TripHubHelper.sendReply(tripHubBaseUrl, id, nick, "yes")
                                                        rsvpSent = true
                                                    } catch (_: Exception) {
                                                    } finally {
                                                        isSendingRsvp = false
                                                    }
                                                }
                                            },
                                            enabled = !isSendingRsvp,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(stringResource(R.string.hub_rsvp_yes_btn))
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                val id = hubId ?: return@OutlinedButton
                                                val nick = rsvpNickname.trim().ifBlank { "Traveler" }
                                                coroutineScope.launch {
                                                    isSendingRsvp = true
                                                    try {
                                                        TripHubHelper.sendReply(tripHubBaseUrl, id, nick, "no")
                                                        rsvpSent = true
                                                    } catch (_: Exception) {
                                                    } finally {
                                                        isSendingRsvp = false
                                                    }
                                                }
                                            },
                                            enabled = !isSendingRsvp,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(stringResource(R.string.hub_rsvp_no_btn))
                                        }
                                    }
                                    if (rsvpSent) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = stringResource(R.string.hub_rsvp_sent),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_close))
            }
        }
    )
}
