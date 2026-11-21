package com.example.travelroulette24.ui

import android.graphics.Bitmap
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
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.travelroulette24.R
import com.example.travelroulette24.budget.BudgetGuardian
import com.example.travelroulette24.data.byok.ByokTravelService
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import androidx.compose.ui.platform.LocalContext
import kotlin.math.roundToInt
import kotlin.math.sin
import com.example.travelroulette24.utils.CalendarHelper
import com.example.travelroulette24.utils.CurrencyHelper
import com.example.travelroulette24.utils.TravelFormatUtils

/**
 * Animated Winning Recommendation Card (Tarjeta Ganadora de la Ruleta).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WinningRecommendationCard(
    item: TravelIntelligenceEngine.ResultItem,
    budget: BudgetGuardian.Budget? = null,
    currencyCode: String = CurrencyHelper.EUR,
    currencyRate: Double = 1.0,
    isRealMode: Boolean = false,
    liveWeather: DestinationWeather? = null,
    attractions: List<String> = emptyList(),
    aiItinerary: String? = null,
    isLoadingItinerary: Boolean = false,
    hasGeminiKey: Boolean = false,
    destinationPhoto: Bitmap? = null,
    liveFlightRadar: ByokTravelService.LiveFlightRadarStatus? = null,
    isBestValue: Boolean = false,
    onGenerateItinerary: (() -> Unit)? = null,
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

    val modeIcon = TravelFormatUtils.modeEmoji(item.mode)

    val modeLabel = when (item.mode.lowercase()) {
        "train" -> stringResource(R.string.transport_train_express)
        "ferry", "boat" -> stringResource(R.string.transport_ferry_fast)
        else -> stringResource(R.string.transport_flight_direct)
    }

    val departureFormatted = TravelFormatUtils.formatIsoDate(item.departureTime)
    val returnFormatted = item.returnTime?.let { TravelFormatUtils.formatIsoDate(it) }

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

                    // Unsplash Destination Hero Banner (Real Mode when key configured)
                    if (isRealMode && destinationPhoto != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .testTag("winning_destination_photo")
                        ) {
                            Image(
                                bitmap = destinationPhoto.asImageBitmap(),
                                contentDescription = destCity,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.unsplash_photo_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    val formattedPrice = CurrencyHelper.formatPrice(currentItem.price, currencyCode, currencyRate)

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
                            val customProviderName = TravelFormatUtils.extractCustomProviderName(currentItem.deepLink)

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

                            // Destination Weather Widget (live Open-Meteo in Real Mode, Demo in Demo Mode)
                            DestinationWeatherBadge(
                                destinationHub = currentItem.destinationHub,
                                liveWeather = liveWeather,
                                isRealMode = isRealMode
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
                                        text = formattedPrice,
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
                                    if (currentItem.isEstimatedPrice) {
                                        Text(
                                            text = stringResource(R.string.badge_estimated_price),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFF59D),
                                            modifier = Modifier.testTag("winning_estimated_price_badge")
                                        )
                                    }
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

                            // Distance, Travel Value Index (km/€) & Imminent Timing Bar
                            val valueIndex = TravelIntelligenceEngine.computeTravelValueIndex(distanceKm, currentItem.price)
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Opportunity, Best Value & Budget Highlights Row
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isBestValue) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text(stringResource(R.string.badge_best_value), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("winning_best_value_badge")
                            )
                        }

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

                    // Live Flight Radar Pill (AviationStack in Real Mode when configured)
                    if (isRealMode && liveFlightRadar != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("winning_flight_radar_banner")
                        ) {
                            val radarParts = buildList {
                                add(stringResource(R.string.radar_flight_prefix, liveFlightRadar.flightNumber))
                                liveFlightRadar.airline?.takeIf { it.isNotBlank() }?.let { add(it) }
                                add(liveFlightRadar.status.uppercase())
                                liveFlightRadar.terminal?.takeIf { it.isNotBlank() }?.let {
                                    add(stringResource(R.string.radar_terminal_format, it))
                                }
                                liveFlightRadar.gate?.takeIf { it.isNotBlank() }?.let {
                                    add(stringResource(R.string.radar_gate_format, it))
                                }
                            }
                            Text(
                                text = "🛰️ ${radarParts.joinToString(" • ")}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // OpenTripMap Top Sights Strip (Real Mode with key configured)
                    if (attractions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.attractions_top_title),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            attractions.forEach { sight ->
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(sight, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                )
                            }
                        }
                    }

                    // Gemini 24h Express Itinerary (Real Mode with Gemini Key configured)
                    if (isRealMode && hasGeminiKey && onGenerateItinerary != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        if (aiItinerary == null) {
                            OutlinedButton(
                                onClick = onGenerateItinerary,
                                enabled = !isLoadingItinerary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("generate_gemini_itinerary_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isLoadingItinerary) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.ai_itinerary_loading), fontSize = 13.sp)
                                } else {
                                    Text(stringResource(R.string.ai_itinerary_btn), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.ai_itinerary_title),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = aiItinerary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Action: BOOK THE WINNING TRIP!
                    val bookPriceText = CurrencyHelper.formatPrice(currentItem.price, currencyCode, currencyRate)
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
                                text = stringResource(R.string.btn_book_winning_formatted, bookPriceText),
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
                            Text(
                                text = stringResource(R.string.btn_open_luggage),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                CalendarHelper.addTravelEvent(
                                    context = context,
                                    item = currentItem,
                                    currencyCode = currencyCode,
                                    rateFromEur = currencyRate
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_to_calendar_button")
                        ) {
                            Text(
                                text = stringResource(R.string.btn_add_calendar),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
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
                                Text(
                                    text = stringResource(R.string.btn_chain_short),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            OutlinedButton(
                                onClick = { onInviteFriends(currentItem) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("share_winning_trip_button")
                            ) {
                                Text(
                                    text = stringResource(R.string.btn_share_qr),
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = onSpinAgain,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("spin_again_button")
                        ) {
                            Text(
                                text = stringResource(R.string.btn_try_another_luck),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
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
                                Text(
                                    text = stringResource(R.string.btn_share_qr),
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            OutlinedButton(
                                onClick = onSpinAgain,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("spin_again_button")
                            ) {
                                Text(
                                    text = stringResource(R.string.btn_spin_again),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
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
