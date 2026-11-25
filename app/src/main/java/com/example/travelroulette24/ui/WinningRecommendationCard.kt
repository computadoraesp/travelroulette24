package com.example.travelroulette24.ui

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelroulette24.R
import com.example.travelroulette24.budget.BudgetGuardian
import com.example.travelroulette24.data.byok.ByokTravelService
import com.example.travelroulette24.engine.TravelIntelligenceEngine
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
    historicalLowEur: Double? = null,
    isBestValue: Boolean = false,
    onGenerateItinerary: (() -> Unit)? = null,
    isSaved: Boolean = false,
    onToggleSave: (() -> Unit)? = null,
    isDesired: Boolean = false,
    onToggleDesired: (() -> Unit)? = null,
    onBook: (deepLink: String) -> Unit,
    onChainTravel: ((destinationHub: String) -> Unit)? = null,
    onInviteFriends: (item: TravelIntelligenceEngine.ResultItem) -> Unit,
    onSpinAgain: () -> Unit,
    onOpenLuggageChecklist: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
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

    val infiniteTransition = rememberInfiniteTransition(label = "winning_card_fx")
    val badgeScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badge_scale"
    )
    val vehicleOffsetRatio by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vehicle_offset"
    )

    val accentStart = MaterialTheme.colorScheme.primary
    val accentMid = MaterialTheme.colorScheme.tertiary
    val accentEnd = MaterialTheme.colorScheme.secondary

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
            CelebrationConfettiCanvas(
                celebrationKey = "${currentItem.originHub}-${currentItem.destinationHub}-${currentItem.departureTime}-${currentItem.mode}",
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(24.dp))
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 10.dp, shape = RoundedCornerShape(24.dp))
                    .border(
                        width = 2.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(accentStart, accentMid, accentEnd)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    WinningHeaderBar(
                        badgeScale = badgeScale,
                        departureTimeIso = currentItem.departureTime,
                        isDesired = isDesired,
                        onToggleDesired = onToggleDesired,
                        isSaved = isSaved,
                        onToggleSave = onToggleSave
                    )

                    Spacer(modifier = Modifier.height(14.dp))

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

                    val formattedPrice = CurrencyHelper.formatPrice(currentItem.totalPrice, currencyCode, currencyRate)
                    val formattedPerPax = CurrencyHelper.formatPrice(currentItem.price, currencyCode, currencyRate)

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
                            DestinationWeatherBadge(
                                destinationHub = currentItem.destinationHub,
                                liveWeather = liveWeather,
                                isRealMode = isRealMode
                            )
                        }

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
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Text(
                                        text = if (currentItem.returnTime != null) stringResource(R.string.mode_round_trip)
                                        else stringResource(R.string.mode_one_way),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                    )
                                    if (currentItem.passengers > 1) {
                                        Text(
                                            text = "${currentItem.passengers} pax · $formattedPerPax",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f)
                                        )
                                    }
                                    if (currentItem.isEstimatedPrice) {
                                        Text(
                                            text = stringResource(R.string.badge_estimated_price),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            modifier = Modifier.testTag("winning_estimated_price_badge")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    WinningRouteVisualizer(
                        originCity = originCity,
                        originHub = currentItem.originHub,
                        destCity = destCity,
                        destinationHub = currentItem.destinationHub,
                        modeIcon = modeIcon,
                        vehicleOffsetRatio = vehicleOffsetRatio,
                        distanceKm = distanceKm,
                        unitPriceEur = currentItem.price,
                        departureFormatted = departureFormatted,
                        returnFormatted = returnFormatted
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    HistoricalPriceComparisonBar(
                        currentUnitPriceEur = currentItem.price,
                        historicalLowEur = historicalLowEur,
                        isEngineOpportunity = currentItem.isOpportunity,
                        currencyCode = currencyCode,
                        currencyRate = currencyRate
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isDesired) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text(stringResource(R.string.badge_wishlist_dest), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    labelColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                modifier = Modifier.testTag("winning_wishlist_badge")
                            )
                        }
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
                            if (currentItem.totalPrice <= (b.maxPerTrip ?: Double.MAX_VALUE)) {
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

                    WinningDestinationExtrasPanel(
                        isRealMode = isRealMode,
                        liveFlightRadar = liveFlightRadar,
                        attractions = attractions,
                        aiItinerary = aiItinerary,
                        isLoadingItinerary = isLoadingItinerary,
                        hasGeminiKey = hasGeminiKey,
                        onGenerateItinerary = onGenerateItinerary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    WinningActionsSection(
                        currentItem = currentItem,
                        currencyCode = currencyCode,
                        currencyRate = currencyRate,
                        onBook = onBook,
                        onChainTravel = onChainTravel,
                        onInviteFriends = onInviteFriends,
                        onSpinAgain = onSpinAgain,
                        onOpenLuggageChecklist = onOpenLuggageChecklist
                    )
                }
            }
        }
    }
}
