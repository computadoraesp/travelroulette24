package com.example.travelroulette24.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun OpportunityCard(
    item: TravelIntelligenceEngine.ResultItem,
    onChainTravel: ((destinationHub: String) -> Unit)? = null,
    onInviteFriends: (item: TravelIntelligenceEngine.ResultItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val originCity = HubCoordinates.getCity(item.originHub)
    val destCity = HubCoordinates.getCity(item.destinationHub)

    val modeIcon = when (item.mode.lowercase()) {
        "train" -> "🚆"
        "ferry" -> "⛴️"
        else -> "✈️"
    }

    val modeLabel = when (item.mode.lowercase()) {
        "train" -> "Tren"
        "ferry" -> "Ferri"
        else -> "Vuelo"
    }

    val departureFormatted = formatIsoDate(item.departureTime)
    val returnFormatted = item.returnTime?.let { formatIsoDate(it) }

    val distanceKm = HubCoordinates.calculateDistanceKm(item.originHub, item.destinationHub)

    val customProviderName = try {
        if (item.deepLink.contains("provider=")) {
            Uri.parse(item.deepLink).getQueryParameter("provider")
        } else null
    } catch (_: Exception) { null }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("opportunity_card_${item.destinationHub}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Mode + Badges + Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$modeIcon $modeLabel",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (customProviderName != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "⚡ $customProviderName",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    text = "€${item.price.toInt()}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Route destination & distance
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$originCity (${item.originHub})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "➔ $destCity (${item.destinationHub})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "📏 $distanceKm km",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Timings
            Text(
                text = "Salida: $departureFormatted",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (returnFormatted != null) {
                Text(
                    text = "Vuelta: $returnFormatted",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (item.checkInOnline) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "✓ Check-in online disponible",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            // Badges row
            if (item.isOpportunity || item.isBudgetFriendly) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.isOpportunity) {
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    "⚡ Mínimo histórico",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                    if (item.isBudgetFriendly) {
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    "🛡️ En presupuesto",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Actions: Book (Deep Link), Chain (if one-way), Invite Friends
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.deepLink))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Reservar", fontWeight = FontWeight.SemiBold)
                }

                if (item.returnTime == null && onChainTravel != null) {
                    FilledTonalButton(
                        onClick = { onChainTravel(item.destinationHub) }
                    ) {
                        Text("Encadenar ➔")
                    }
                }

                OutlinedButton(
                    onClick = { onInviteFriends(item) }
                ) {
                    Text("Invitar")
                }
            }
        }
    }
}

private val formatter = DateTimeFormatter.ofPattern("HH:mm - dd MMM")

private fun formatIsoDate(iso: String): String {
    return try {
        val instant = Instant.parse(iso)
        val zonedDateTime = instant.atZone(ZoneId.systemDefault())
        formatter.format(zonedDateTime)
    } catch (_: Exception) {
        iso
    }
}
