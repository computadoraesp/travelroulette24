package com.example.travelroulette24.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelroulette24.R
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import com.example.travelroulette24.utils.CalendarHelper
import com.example.travelroulette24.utils.CurrencyHelper
import com.example.travelroulette24.utils.TravelFormatUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OpportunityCard(
    item: TravelIntelligenceEngine.ResultItem,
    currencyCode: String = CurrencyHelper.EUR,
    currencyRate: Double = 1.0,
    isSaved: Boolean = false,
    onToggleSave: (() -> Unit)? = null,
    isBargainDeal: Boolean = false,
    onChainTravel: ((destinationHub: String) -> Unit)? = null,
    onInviteFriends: (item: TravelIntelligenceEngine.ResultItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val originCity = HubCoordinates.getCity(item.originHub)
    val destCity = HubCoordinates.getCity(item.destinationHub)

    val modeIcon = TravelFormatUtils.modeEmoji(item.mode)

    val modeLabel = when (item.mode.lowercase()) {
        "train" -> stringResource(R.string.transport_train)
        "ferry", "boat" -> stringResource(R.string.transport_ferry)
        else -> stringResource(R.string.transport_flight)
    }

    val departureFormatted = TravelFormatUtils.formatIsoDate(item.departureTime)
    val returnFormatted = item.returnTime?.let { TravelFormatUtils.formatIsoDate(it) }
    val formattedPrice = CurrencyHelper.formatPrice(item.price, currencyCode, currencyRate)

    val distanceKm = HubCoordinates.calculateDistanceKm(item.originHub, item.destinationHub)

    val customProviderName = TravelFormatUtils.extractCustomProviderName(item.deepLink)

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
            // Header: Responsive Mode & Badges + Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
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

                    if (isBargainDeal) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.badge_bargain),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    if (customProviderName != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                                .padding(horizontal = 6.dp, vertical = 4.dp)
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

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formattedPrice,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )

                    if (onToggleSave != null) {
                        FilledTonalIconButton(
                            onClick = onToggleSave,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("toggle_save_opportunity_${item.originHub}_${item.destinationHub}"),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (isSaved) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSaved) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(
                                text = if (isSaved) "❤️" else "🤍",
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Route destination & distance without redundant origin repetition
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "➔ $destCity (${item.destinationHub})",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.from_label) + " $originCity (${item.originHub}) • 📏 $distanceKm km",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Timings & Live Departure Countdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.departure_prefix) + " $departureFormatted",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                LiveDepartureCountdownBadge(
                    departureTimeIso = item.departureTime,
                    compact = true
                )
            }

            if (returnFormatted != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.return_prefix) + " $returnFormatted",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (item.checkInOnline) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.checkin_online_available),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            // Badges row: highlight historical low prices
            if (item.isOpportunity) {
                Spacer(modifier = Modifier.height(8.dp))
                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            stringResource(R.string.badge_opportunity),
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

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Booking Button: Full-width, prominent, high contrast & ergonomic
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.deepLink))
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("book_opportunity_button_${item.originHub}_${item.destinationHub}"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(
                            R.string.book_ticket_formatted,
                            stringResource(R.string.btn_book_ticket),
                            formattedPrice
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Secondary Actions Row: Balanced utilities (Calendar, Chaining, and Invite)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = {
                        CalendarHelper.addTravelEvent(
                            context = context,
                            item = item,
                            currencyCode = currencyCode,
                            rateFromEur = currencyRate
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.btn_calendar_short),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                if (item.returnTime == null && onChainTravel != null) {
                    FilledTonalButton(
                        onClick = { onChainTravel(item.destinationHub) },
                        modifier = Modifier
                            .weight(1.1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.btn_chain_short),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                OutlinedButton(
                    onClick = { onInviteFriends(item) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.btn_invite_short),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
