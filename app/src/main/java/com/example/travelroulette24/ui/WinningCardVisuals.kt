package com.example.travelroulette24.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelroulette24.R
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import com.example.travelroulette24.ui.theme.LocalIsColorblindMode
import com.example.travelroulette24.ui.theme.OkabeBluishGreen
import com.example.travelroulette24.ui.theme.OkabeOrange
import com.example.travelroulette24.ui.theme.OkabeVermillion
import com.example.travelroulette24.utils.CurrencyHelper
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
internal fun WinningHeaderBar(
    badgeScale: Float,
    departureTimeIso: String,
    isDesired: Boolean,
    onToggleDesired: (() -> Unit)?,
    isSaved: Boolean,
    onToggleSave: (() -> Unit)?
) {
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
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer
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
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LiveDepartureCountdownBadge(
                departureTimeIso = departureTimeIso,
                modifier = Modifier.testTag("winning_live_countdown")
            )

            if (onToggleDesired != null) {
                val wishlistCd = stringResource(R.string.cd_toggle_wishlist)
                FilledTonalIconButton(
                    onClick = onToggleDesired,
                    modifier = Modifier
                        .size(40.dp)
                        .semantics { contentDescription = wishlistCd }
                        .testTag("toggle_desired_winning_card_button"),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (isDesired) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isDesired) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(if (isDesired) "❤️" else "🤍", fontSize = 15.sp)
                }
            }

            if (onToggleSave != null) {
                val favorite24hCd = stringResource(R.string.cd_toggle_favorite_24h)
                FilledTonalIconButton(
                    onClick = onToggleSave,
                    modifier = Modifier
                        .size(40.dp)
                        .semantics { contentDescription = favorite24hCd }
                        .testTag("toggle_save_winning_card_button"),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (isSaved) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isSaved) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        text = if (isSaved) "⭐" else "☆",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
internal fun WinningRouteVisualizer(
    originCity: String,
    originHub: String,
    destCity: String,
    destinationHub: String,
    modeIcon: String,
    vehicleOffsetRatio: Float,
    distanceKm: Int,
    unitPriceEur: Double,
    departureFormatted: String,
    returnFormatted: String?
) {
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
                        text = originHub,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(2.dp)) {
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.5f),
                            start = Offset(0f, size.height / 2f),
                            end = Offset(size.width, size.height / 2f),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }

                    val travelXOffset = (vehicleOffsetRatio * 100).dp - 50.dp
                    Box(modifier = Modifier.offset { IntOffset(travelXOffset.roundToPx(), 0) }) {
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
                        text = destinationHub,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val valueIndex = TravelIntelligenceEngine.computeTravelValueIndex(distanceKm, unitPriceEur)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildString {
                        append(stringResource(R.string.distance_prefix))
                        append(" $distanceKm km")
                        if (valueIndex > 0.0) {
                            append(" • ")
                            append(stringResource(R.string.value_index_format, valueIndex))
                        }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .testTag("winning_value_index_text")
                )
                Text(
                    text = stringResource(R.string.departure_prefix) + " $departureFormatted",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
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
}

@Composable
internal fun HistoricalPriceComparisonBar(
    currentUnitPriceEur: Double,
    historicalLowEur: Double?,
    isEngineOpportunity: Boolean,
    currencyCode: String,
    currencyRate: Double
) {
    val effectiveLow = historicalLowEur?.takeIf { it > 0.0 }
        ?: if (isEngineOpportunity) currentUnitPriceEur else return
    if (currentUnitPriceEur <= 0.0) return

    val isColorblind = LocalIsColorblindMode.current
    val isAtRecordLow = isEngineOpportunity || currentUnitPriceEur <= effectiveLow * 1.02
    val pctAboveLow = (((currentUnitPriceEur - effectiveLow) / effectiveLow) * 100.0)
        .coerceAtLeast(0.0)
        .roundToInt()

    val barColor = when {
        isAtRecordLow -> if (isColorblind) OkabeBluishGreen else MaterialTheme.colorScheme.tertiary
        pctAboveLow <= 25 -> if (isColorblind) OkabeOrange else MaterialTheme.colorScheme.primary
        else -> if (isColorblind) OkabeVermillion else MaterialTheme.colorScheme.secondary
    }

    val scaleMax = maxOf(currentUnitPriceEur * 1.15, effectiveLow * 1.6)
    val markerRatio = ((currentUnitPriceEur - effectiveLow) / (scaleMax - effectiveLow).coerceAtLeast(1.0))
        .toFloat()
        .coerceIn(0.06f, 0.96f)

    val formattedLow = CurrencyHelper.formatPrice(effectiveLow, currencyCode, currencyRate)
    val formattedCurrent = CurrencyHelper.formatPrice(currentUnitPriceEur, currencyCode, currencyRate)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("winning_historical_comparison_bar")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.winner_history_title),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(barColor.copy(alpha = 0.16f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isAtRecordLow) {
                        stringResource(R.string.winner_history_at_record)
                    } else {
                        stringResource(R.string.winner_history_above_pct, pctAboveLow)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = barColor
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
        ) {
            val h = size.height
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(0f, h * 0.25f),
                size = Size(size.width, h * 0.5f),
                cornerRadius = CornerRadius(h / 2f, h / 2f)
            )
            drawRoundRect(
                color = barColor,
                topLeft = Offset(0f, h * 0.25f),
                size = Size(size.width * markerRatio, h * 0.5f),
                cornerRadius = CornerRadius(h / 2f, h / 2f)
            )
            drawCircle(
                color = barColor,
                radius = h * 0.48f,
                center = Offset(size.width * markerRatio, h / 2f)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.winner_history_low_label, formattedLow),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.winner_history_now_label, formattedCurrent),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = barColor
            )
        }
    }
}

/**
 * One-shot Celebration Confetti & Sparkles burst drawn softly when a winning destination is revealed.
 */
@Composable
internal fun CelebrationConfettiCanvas(
    celebrationKey: String = "",
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(celebrationKey) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2200, easing = LinearEasing)
        )
    }

    val isColorblind = LocalIsColorblindMode.current
    val confettiColors = remember(isColorblind) {
        if (isColorblind) {
            listOf(
                Color(0xFF0072B2),
                Color(0xFFE69F00),
                Color(0xFF009E73),
                Color(0xFFD55E00),
                Color(0xFF56B4E9)
            )
        } else {
            listOf(
                Color(0xFFFFD700),
                Color(0xFFFF4081),
                Color(0xFF00E5FF),
                Color(0xFF76FF03),
                Color(0xFFFF9100)
            )
        }
    }

    val particles = remember {
        List(22) { i ->
            ConfettiParticle(
                xRatio = ((i * 37) % 100) / 100f,
                speed = 0.65f + ((i * 13) % 10) / 18f,
                size = 4f + (i % 4) * 1.8f,
                colorIndex = i % confettiColors.size,
                isStar = i % 3 == 0
            )
        }
    }

    val t = progress.value
    if (t >= 1f) return
    val fade = if (t < 0.7f) 0.55f else ((1f - t) / 0.3f) * 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        particles.forEach { p ->
            val currentY = ((t * p.speed + p.xRatio * 0.35f) % 1f) * h
            val currentX = (p.xRatio * w) + (sin((t * 2.5 * Math.PI + p.speed).toFloat()) * 14.dp.toPx())
            val color = confettiColors[p.colorIndex].copy(alpha = fade)

            if (p.isStar) {
                drawCircle(
                    color = color,
                    radius = p.size.dp.toPx() / 2f,
                    center = Offset(currentX, currentY)
                )
            } else {
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
