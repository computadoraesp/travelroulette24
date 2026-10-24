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
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.travelroulette24.R
import com.example.travelroulette24.budget.BudgetGuardian
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import java.time.Instant
import java.time.ZoneId
import androidx.compose.ui.platform.LocalContext
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import com.example.travelroulette24.utils.CalendarHelper

/**
 * Animated Winning Recommendation Card (Tarjeta Ganadora de la Ruleta).
 */
@Composable
fun WinningRecommendationCard(
    item: TravelIntelligenceEngine.ResultItem,
    budget: BudgetGuardian.Budget? = null,
    isSaved: Boolean = false,
    onToggleSave: (() -> Unit)? = null,
    onBook: (deepLink: String) -> Unit,
    onChainTravel: ((destinationHub: String) -> Unit)? = null,
    onInviteFriends: (item: TravelIntelligenceEngine.ResultItem) -> Unit,
    onSpinAgain: () -> Unit,
    onOpenLuggageChecklist: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val originCity = HubCoordinates.getCity(item.originHub)
    val destCity = HubCoordinates.getCity(item.destinationHub)
    val distanceKm = HubCoordinates.calculateDistanceKm(item.originHub, item.destinationHub)

    val modeIcon = when (item.mode.lowercase()) {
        "train" -> "🚆"
        "ferry" -> "⛴️"
        else -> "✈️"
    }

    val modeLabel = when (item.mode.lowercase()) {
        "train" -> stringResource(R.string.transport_train_express)
        "ferry" -> stringResource(R.string.transport_ferry_fast)
        else -> stringResource(R.string.transport_flight_direct)
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
                                    text = stringResource(R.string.badge_winner_title),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Immediate departure live ticking countdown badge
                            LiveDepartureCountdownBadge(
                                departureTimeIso = currentItem.departureTime,
                                modifier = Modifier.testTag("winning_live_countdown")
                            )

                            if (onToggleSave != null) {
                                FilledTonalIconButton(
                                    onClick = onToggleSave,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("toggle_save_winning_card_button"),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = if (isSaved) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (isSaved) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Text(if (isSaved) "❤️" else "🤍", fontSize = 16.sp)
                                }
                            }
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
                                text = stringResource(R.string.badge_winner_desc),
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

                            Spacer(modifier = Modifier.height(6.dp))

                            // Destination Weather Widget
                            DestinationWeatherBadge(destinationHub = currentItem.destinationHub)
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
                                        text = if (currentItem.returnTime != null) stringResource(R.string.mode_round_trip)
                                        else stringResource(R.string.mode_one_way),
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
                                    text = stringResource(R.string.distance_prefix) + " $distanceKm km",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.departure_prefix) + " $departureFormatted",
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
                                        text = stringResource(R.string.return_prefix) + " $returnFormatted",
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
                                label = { Text(stringResource(R.string.badge_opportunity), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
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
                                    label = { Text("🛡️ " + stringResource(R.string.budget_friendly), fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                )
                            }
                        }

                        if (currentItem.checkInOnline) {
                            Text(
                                text = stringResource(R.string.checkin_online_available),
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
                                text = stringResource(R.string.btn_book_winning_trip, currentItem.price.toInt()),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Express Travel Toolkit: Maleta Express + Añadir a Calendario
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { onOpenLuggageChecklist?.invoke() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_luggage_checklist_button")
                        ) {
                            Text(stringResource(R.string.btn_open_luggage), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = { CalendarHelper.addTravelEvent(context, currentItem) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_to_calendar_button")
                        ) {
                            Text(stringResource(R.string.btn_add_calendar), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Secondary Actions: Share QR, Chaining, and Spin Again
                    if (currentItem.returnTime == null && onChainTravel != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { onChainTravel(currentItem.destinationHub) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chain_winning_trip_button")
                            ) {
                                Text(stringResource(R.string.btn_chain_short), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { onInviteFriends(currentItem) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("share_winning_trip_button")
                            ) {
                                Text(stringResource(R.string.btn_share_qr), fontSize = 13.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = onSpinAgain,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("spin_again_button")
                        ) {
                            Text(stringResource(R.string.btn_try_another_luck), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onInviteFriends(currentItem) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("share_winning_trip_button")
                            ) {
                                Text(stringResource(R.string.btn_share_qr), fontSize = 13.sp)
                            }
                            OutlinedButton(
                                onClick = onSpinAgain,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("spin_again_button")
                            ) {
                                Text(stringResource(R.string.btn_spin_again), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
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
