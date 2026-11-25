package com.example.travelroulette24.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    isDesired: Boolean = false,
    onToggleDesired: (() -> Unit)? = null,
    isBargainDeal: Boolean = false,
    onChainTravel: ((destinationHub: String) -> Unit)? = null,
    onInviteFriends: (item: TravelIntelligenceEngine.ResultItem) -> Unit,
    showValueIndex: Boolean = false,
    isBestValue: Boolean = false,
    isSelectedForWinner: Boolean = false,
    onInspectOnWinner: (() -> Unit)? = null,
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
    val formattedPrice = CurrencyHelper.formatPrice(item.totalPrice, currencyCode, currencyRate)
    val formattedPerPaxPrice = CurrencyHelper.formatPrice(item.price, currencyCode, currencyRate)

    val distanceKm = HubCoordinates.calculateDistanceKm(item.originHub, item.destinationHub)
    val valueIdx = TravelIntelligenceEngine.computeTravelValueIndex(distanceKm, item.price)

    val customProviderName = TravelFormatUtils.extractCustomProviderName(item.deepLink)

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (onInspectOnWinner != null) {
                    Modifier.clickable { onInspectOnWinner() }
                } else Modifier
            )
            .testTag("opportunity_card_${item.destinationHub}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelectedForWinner) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
            } else {
                MaterialTheme.colorScheme.surface
            }
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

                    if (isBestValue) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                                .testTag("opportunity_best_value_badge_${item.destinationHub}")
                        ) {
                            Text(
                                text = "💎 ${stringResource(R.string.badge_best_value)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
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

                    if (isDesired) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                                .testTag("opportunity_wishlist_badge_${item.destinationHub}")
                        ) {
                            Text(
                                text = stringResource(R.string.badge_wishlist_dest),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
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
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formattedPrice,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (item.passengers > 1) {
                            Text(
                                text = "${item.passengers} pax · $formattedPerPaxPrice",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (onToggleDesired != null) {
                        val wishlistCd = stringResource(R.string.cd_toggle_wishlist)
                        FilledTonalIconButton(
                            onClick = onToggleDesired,
                            modifier = Modifier
                                .size(38.dp)
                                .semantics { contentDescription = wishlistCd }
                                .testTag("toggle_desired_opportunity_${item.originHub}_${item.destinationHub}"),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (isDesired) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isDesired) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(
                                text = if (isDesired) "❤️" else "🤍",
                                fontSize = 15.sp
                            )
                        }
                    }

                    if (onToggleSave != null) {
                        val favorite24hCd = stringResource(R.string.cd_toggle_favorite_24h)
                        FilledTonalIconButton(
                            onClick = onToggleSave,
                            modifier = Modifier
                                .size(38.dp)
                                .semantics { contentDescription = favorite24hCd }
                                .testTag("toggle_save_opportunity_${item.originHub}_${item.destinationHub}"),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (isSaved) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSaved) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(
                                text = if (isSaved) "⭐" else "☆",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
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
                    val valueSuffix = if (showValueIndex && valueIdx > 0.0) {
                        " • 💎 ${stringResource(R.string.value_index_format, valueIdx)}"
                    } else ""
                    Text(
                        text = stringResource(R.string.from_label) + " $originCity (${item.originHub}) • 📏 $distanceKm km$valueSuffix",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (showValueIndex && valueIdx > 0.0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (showValueIndex && valueIdx > 0.0) FontWeight.SemiBold else FontWeight.Normal
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
