package com.example.travelroulette24.ui

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelroulette24.budget.BudgetGuardian
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Animated Winning Recommendation Card (Tarjeta Ganadora de la Ruleta).
 *
 * Featured prominently when the travel roulette selects a winner, featuring:
 * - Dynamic casino celebration glow and animated sweep gradient border
 * - Confetti and sparkle floating particle effects
 * - Pulsing "🏆 ¡RECOMENDACIÓN GANADORA!" trophy badge
 * - Animated flight / transport vehicle moving along the journey path
 * - Imminent departure countdown (< 24h, ¡Salimos mañana!)
 * - Direct booking, chaining, QR sharing, and re-spin actions
 */
@Composable
fun WinningRecommendationCard(
    item: TravelIntelligenceEngine.ResultItem,
    budget: BudgetGuardian.Budget? = null,
    onBook: (deepLink: String) -> Unit,
    onChainTravel: ((destinationHub: String) -> Unit)? = null,
    onInviteFriends: (item: TravelIntelligenceEngine.ResultItem) -> Unit,
    onSpinAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val originCity = HubCoordinates.getCity(item.originHub)
    val destCity = HubCoordinates.getCity(item.destinationHub)
    val distanceKm = HubCoordinates.calculateDistanceKm(item.originHub, item.destinationHub)

    val modeIcon = when (item.mode.lowercase()) {
        "train" -> "🚆"
        "ferry" -> "⛴️"
        else -> "✈️"
    }

    val modeLabel = when (item.mode.lowercase()) {
        "train" -> "Tren Expreso"
        "ferry" -> "Ferry Rápido"
        else -> "Vuelo Directo"
    }

    val departureFormatted = formatIsoDate(item.departureTime)
    val returnFormatted = item.returnTime?.let { formatIsoDate(it) }

    // Infinite transitions for celebration animations
    val infiniteTransition = rememberInfiniteTransition(label = "winning_card_fx")

    // 1. Rotating rainbow/golden halo border angle
    val borderRotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "border_rotation"
    )

    // 2. Pulsing Winner Badge Scale
    val badgeScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badge_scale"
    )

    // 3. Shimmer shimmer sweep alpha
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    // 4. Vehicle movement offset along route
    val vehicleOffsetRatio by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vehicle_offset"
    )

    // Animated content transition keyed on winner coordinates and price
    AnimatedContent(
        targetState = item,
        transitionSpec = {
            (scaleIn(
                initialScale = 0.85f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(tween(350))) togetherWith (scaleOut(targetScale = 0.9f) + fadeOut(tween(200)))
        },
        label = "winning_card_animated_content"
    ) { currentItem ->
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .testTag("winning_recommendation_card_container")
        ) {
            // Background Canvas: Confetti and celebratory sparkles
            CelebrationConfettiCanvas(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(24.dp))
            )

            // Outer Glowing Animated Border Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(24.dp))
                    .border(
                        width = 3.dp,
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFFFFD700), // Gold
                                Color(0xFFFF6D00), // Amber-orange
                                Color(0xFFFF1744), // Crimson
                                Color(0xFF00E5FF), // Cyan
                                Color(0xFF76FF03), // Lime
                                Color(0xFFFFD700)  // Gold
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Top Winner Banner & Pulsing Trophy
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .scale(badgeScale)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFFFFB300),
                                            Color(0xFFFF8F00)
                                        )
                                    )
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🏆", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "RECOMENDACIÓN GANADORA",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        // Immediate departure badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "⚡ Salimos mañana",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Big Destination Highlight & Route
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "¡Prepara las maletas para...!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = destCity,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val customProviderName = try {
                                if (currentItem.deepLink.contains("provider=")) {
                                    Uri.parse(currentItem.deepLink).getQueryParameter("provider")
                                } else null
                            } catch (_: Exception) { null }

                            Text(
                                text = buildString {
                                    append("${currentItem.destinationHub} • $modeLabel")
                                    if (customProviderName != null) {
                                        append(" • ⚡ $customProviderName")
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        // Giant Price Tag with Pulsing Container
                        Column(horizontalAlignment = Alignment.End) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.tertiary
                                            )
                                        )
                                    )
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "€${currentItem.price.toInt()}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (currentItem.returnTime != null) "Ida y Vuelta" else "Solo Ida",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Animated Route Visualization (Origin -> Animated Vehicle -> Destination)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = originCity,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = currentItem.originHub,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Interactive Animated Vehicle along Route Path
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Dotted route line
                                    Canvas(modifier = Modifier.fillMaxWidth().height(2.dp)) {
                                        drawLine(
                                            color = Color.Gray.copy(alpha = 0.5f),
                                            start = Offset(0f, size.height / 2f),
                                            end = Offset(size.width, size.height / 2f),
                                            strokeWidth = 2.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                        )
                                    }

                                    // Moving Vehicle Icon
                                    val travelXOffset = (vehicleOffsetRatio * 100).dp - 50.dp
                                    Box(
                                        modifier = Modifier.offset { IntOffset(travelXOffset.roundToPx(), 0) }
                                    ) {
                                        Text(text = modeIcon, fontSize = 20.sp)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = destCity,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = currentItem.destinationHub,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Distance & Imminent Timing Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📏 Distancia: $distanceKm km",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "🛫 Salida: $departureFormatted",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (returnFormatted != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "🛬 Regreso: $returnFormatted",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Opportunity & Budget Highlights Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentItem.isOpportunity) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("⚡ Mínimo histórico", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )
                        }

                        budget?.let { b ->
                            if (currentItem.price <= (b.maxPerTrip ?: Double.MAX_VALUE)) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("🛡️ En tu presupuesto", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                )
                            }
                        }

                        if (currentItem.checkInOnline) {
                            Text(
                                text = "✓ Check-in online",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Action: BOOK THE WINNING TRIP!
                    Button(
                        onClick = { onBook(currentItem.deepLink) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("book_winning_trip_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("🎟️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "¡RESERVAR VIAJE GANADOR (€${currentItem.price.toInt()})!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Secondary Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Travel Chaining (for one-way trips)
                        if (currentItem.returnTime == null && onChainTravel != null) {
                            FilledTonalButton(
                                onClick = { onChainTravel(currentItem.destinationHub) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chain_winning_trip_button")
                            ) {
                                Text("Encadenar ➔", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Invite Friends / QR Code
                        OutlinedButton(
                            onClick = { onInviteFriends(currentItem) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("share_winning_trip_button")
                        ) {
                            Text("👥 Compartir QR", fontSize = 12.sp)
                        }

                        // Re-spin Wheel Action
                        OutlinedButton(
                            onClick = onSpinAgain,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("spin_again_button")
                        ) {
                            Text("🎲 Re-Girar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Animated Celebration Confetti & Sparkles canvas drawn around/within the card.
 */
@Composable
private fun CelebrationConfettiCanvas(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti_anim")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_time"
    )

    val confettiColors = remember {
        listOf(
            Color(0xFFFFD700), // Gold
            Color(0xFFFF4081), // Pink/Magenta
            Color(0xFF00E5FF), // Cyan
            Color(0xFF76FF03), // Lime green
            Color(0xFFFF9100)  // Orange
        )
    }

    // Static seeded particle data for consistent layout
    val particles = remember {
        List(24) { i ->
            ConfettiParticle(
                xRatio = ((i * 37) % 100) / 100f,
                speed = 0.5f + ((i * 13) % 10) / 20f,
                size = 4f + (i % 5) * 2f,
                colorIndex = i % confettiColors.size,
                isStar = i % 3 == 0
            )
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        particles.forEach { p ->
            val currentY = ((time * p.speed + p.xRatio) % 1f) * h
            val currentX = (p.xRatio * w) + (sin((time * 2 * Math.PI + p.speed).toFloat()) * 16.dp.toPx())
            val color = confettiColors[p.colorIndex].copy(alpha = 0.7f)

            if (p.isStar) {
                // Draw celebratory sparkle dot
                drawCircle(
                    color = color,
                    radius = p.size.dp.toPx() / 2f,
                    center = Offset(currentX, currentY)
                )
            } else {
                // Draw rotating confetti ribbon
                drawRoundRect(
                    color = color,
                    topLeft = Offset(currentX, currentY),
                    size = Size(p.size.dp.toPx(), (p.size * 1.8f).dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
        }
    }
}

private data class ConfettiParticle(
    val xRatio: Float,
    val speed: Float,
    val size: Float,
    val colorIndex: Int,
    val isStar: Boolean
)

private val winningDateFormatter = DateTimeFormatter.ofPattern("HH:mm - dd MMM")

private fun formatIsoDate(iso: String): String {
    return try {
        val instant = Instant.parse(iso)
        val zonedDateTime = instant.atZone(ZoneId.systemDefault())
        winningDateFormatter.format(zonedDateTime)
    } catch (_: Exception) {
        iso
    }
}
