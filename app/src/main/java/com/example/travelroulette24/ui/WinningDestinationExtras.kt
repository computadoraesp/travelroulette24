package com.example.travelroulette24.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelroulette24.R
import com.example.travelroulette24.data.byok.ByokTravelService
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import com.example.travelroulette24.utils.CalendarHelper
import com.example.travelroulette24.utils.CurrencyHelper

/**
 * Progressive-disclosure panel for secondary destination intelligence
 * (Live Flight Radar, OpenTripMap Attractions, and Gemini Express Itinerary)
 * keeping the primary Winning Card clean and focused above the fold.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WinningDestinationExtrasPanel(
    isRealMode: Boolean,
    liveFlightRadar: ByokTravelService.LiveFlightRadarStatus?,
    attractions: List<String>,
    aiItinerary: String?,
    isLoadingItinerary: Boolean,
    hasGeminiKey: Boolean,
    onGenerateItinerary: (() -> Unit)?
) {
    val hasRadar = isRealMode && liveFlightRadar != null
    val hasAttractions = attractions.isNotEmpty()
    val hasAiSection = isRealMode && hasGeminiKey && onGenerateItinerary != null
    if (!hasRadar && !hasAttractions && !hasAiSection) return

    var expanded by remember(liveFlightRadar, attractions.size, aiItinerary, isLoadingItinerary) {
        mutableStateOf(aiItinerary != null || isLoadingItinerary)
    }

    Spacer(modifier = Modifier.height(10.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { expanded = !expanded }
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .testTag("winning_extras_toggle"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.winner_extras_toggle_label),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = if (expanded) "▲" else "▼",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }

    AnimatedVisibility(visible = expanded) {
        Column(modifier = Modifier.padding(top = 8.dp)) {
            if (isRealMode && liveFlightRadar != null) {
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
                        if (liveFlightRadar.delayMinutes > 0) {
                            add("+${liveFlightRadar.delayMinutes}m")
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

            if (hasAttractions) {
                Spacer(modifier = Modifier.height(8.dp))
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
                            label = {
                                Text(
                                    text = sight,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }

            if (isRealMode && hasGeminiKey && onGenerateItinerary != null) {
                Spacer(modifier = Modifier.height(8.dp))
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
        }
    }
}

@Composable
internal fun WinningActionsSection(
    currentItem: TravelIntelligenceEngine.ResultItem,
    currencyCode: String,
    currencyRate: Double,
    onBook: (deepLink: String) -> Unit,
    onChainTravel: ((destinationHub: String) -> Unit)?,
    onInviteFriends: (item: TravelIntelligenceEngine.ResultItem) -> Unit,
    onSpinAgain: () -> Unit,
    onOpenLuggageChecklist: (() -> Unit)?
) {
    val context = LocalContext.current
    val bookPriceText = CurrencyHelper.formatPrice(currentItem.totalPrice, currencyCode, currencyRate)

    Button(
        onClick = { onBook(currentItem.deepLink) },
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("book_winning_trip_button"),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
