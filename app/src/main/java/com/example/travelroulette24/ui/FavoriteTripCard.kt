package com.example.travelroulette24.ui

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelroulette24.R
import com.example.travelroulette24.data.FavoritePriceHistoryDb
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import com.example.travelroulette24.utils.CurrencyHelper
import com.example.travelroulette24.utils.TravelFormatUtils

@Composable
internal fun FavoriteResultCard(
    hub: String,
    bestOffer: TravelIntelligenceEngine.ResultItem?,
    record: FavoritePriceHistoryDb.HistoricalPriceRecord?,
    currencyCode: String,
    currencyRate: Double,
    tolerancePercent: Int,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    onBookOffer: (String) -> Unit
) {
    val city = HubCoordinates.getCity(hub)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("fav_result_card_$hub"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "❤️ $city ($hub)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (bestOffer != null) {
                        Text(
                            text = CurrencyHelper.formatPrice(bestOffer.price, currencyCode, currencyRate),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    TextButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("fav_edit_btn_$hub")
                    ) {
                        Text("✏️", fontSize = 14.sp)
                    }
                    TextButton(
                        onClick = onRemove,
                        modifier = Modifier.testTag("fav_delete_btn_$hub")
                    ) {
                        Text("🗑️", fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (record != null) {
                val minFormatted = CurrencyHelper.formatPrice(record.lowestPrice, currencyCode, currencyRate)
                Text(
                    text = stringResource(R.string.fav_trip_hist_low_format, minFormatted, record.observations),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (bestOffer != null) {
                val histLow = record?.lowestPrice ?: bestOffer.price
                val eval = FavoritePriceHistoryDb.evaluateAcceptance(
                    currentPrice = bestOffer.price,
                    lowestHistoricalPrice = histLow,
                    tolerancePercent = tolerancePercent
                )
                val targetFormatted = CurrencyHelper.formatPrice(eval.maxAcceptablePrice, currencyCode, currencyRate)
                val distKm = HubCoordinates.calculateDistanceKm(bestOffer.originHub, bestOffer.destinationHub)
                val valueIdx = TravelIntelligenceEngine.computeTravelValueIndex(distKm, bestOffer.price)

                Text(
                    text = "${TravelFormatUtils.modeEmoji(bestOffer.mode)} $distKm km • ${stringResource(R.string.value_index_format, valueIdx)} • ${stringResource(R.string.fav_trip_target_format, targetFormatted)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                val (badgeText, badgeContainer, badgeTextColor) = when {
                    eval.isNewRecordLow -> Triple(
                        stringResource(R.string.fav_trip_badge_record_low),
                        MaterialTheme.colorScheme.tertiaryContainer,
                        MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    eval.isWithinAcceptanceRange -> Triple(
                        stringResource(R.string.fav_trip_badge_accepted),
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    else -> Triple(
                        stringResource(R.string.fav_trip_badge_above, eval.deltaPercent.toInt()),
                        MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = { onBookOffer(bestOffer.deepLink) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("fav_book_btn_$hub")
                    ) {
                        Text(
                            text = "🎟️ ${stringResource(R.string.btn_book_short)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.fav_trip_no_offer_now),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
