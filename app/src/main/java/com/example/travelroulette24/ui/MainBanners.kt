package com.example.travelroulette24.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelroulette24.R
import com.example.travelroulette24.data.PriceAlertManager
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import com.example.travelroulette24.utils.CurrencyHelper

@Composable
fun getSortOptionLabel(option: TravelViewModel.SortOption): String {
    return when (option) {
        TravelViewModel.SortOption.BEST_VALUE -> stringResource(R.string.sort_option_best_value)
        TravelViewModel.SortOption.CHEAPEST -> stringResource(R.string.sort_option_cheapest)
        TravelViewModel.SortOption.FURTHEST_DISTANCE -> stringResource(R.string.sort_option_furthest)
        TravelViewModel.SortOption.EXTREME_ROULETTE -> stringResource(R.string.sort_option_extreme_roulette)
        TravelViewModel.SortOption.EARLIEST_DEPARTURE -> stringResource(R.string.sort_option_earliest)
    }
}

@Composable
fun getSortOptionDescription(option: TravelViewModel.SortOption): String {
    return when (option) {
        TravelViewModel.SortOption.BEST_VALUE -> stringResource(R.string.sort_desc_best_value)
        TravelViewModel.SortOption.CHEAPEST -> stringResource(R.string.sort_desc_cheapest)
        TravelViewModel.SortOption.FURTHEST_DISTANCE -> stringResource(R.string.sort_desc_furthest)
        TravelViewModel.SortOption.EXTREME_ROULETTE -> stringResource(R.string.sort_desc_extreme_roulette)
        TravelViewModel.SortOption.EARLIEST_DEPARTURE -> stringResource(R.string.sort_desc_earliest)
    }
}

@Composable
internal fun WishlistMatchBanner(
    wishlistDeals: List<TravelIntelligenceEngine.ResultItem>,
    currencyCode: String,
    currencyRate: Double,
    onClick: () -> Unit
) {
    val bestWishlistOffer = wishlistDeals.minByOrNull { it.price } ?: return
    val destCity = HubCoordinates.getCity(bestWishlistOffer.destinationHub)
    val bestFormatted = CurrencyHelper.formatPrice(bestWishlistOffer.totalPrice, currencyCode, currencyRate)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("wishlist_match_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("❤️", fontSize = 22.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.wishlist_notification_title, destCity),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = stringResource(
                        R.string.wishlist_notification_body,
                        wishlistDeals.size,
                        destCity,
                        bestFormatted
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
internal fun DealRadarBanner(
    alertConfig: PriceAlertManager.AlertConfig,
    matchingCount: Int,
    currencySymbol: String,
    currencyRate: Double,
    onlyDealsFilter: Boolean,
    onToggleOnlyDeals: () -> Unit
) {
    val maxPriceDisplay = CurrencyHelper.convertAmount(alertConfig.maxPriceEur.toDouble(), currencyRate)
    val kmPerDisplay = CurrencyHelper.kmPerDisplayCurrency(alertConfig.minKmPerEur.toDouble(), currencyRate)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔔", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.bargain_alert_banner_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = when (alertConfig.alertMode) {
                            PriceAlertManager.AlertMode.BEST_KM_PER_PRICE -> stringResource(
                                R.string.bargain_alert_km_desc,
                                matchingCount,
                                kmPerDisplay,
                                currencySymbol
                            )
                            PriceAlertManager.AlertMode.HISTORICAL_LOW -> stringResource(
                                R.string.bargain_alert_historical_desc,
                                matchingCount
                            )
                            PriceAlertManager.AlertMode.LOWEST_PRICE -> stringResource(
                                R.string.bargain_alert_currency_desc,
                                matchingCount,
                                currencySymbol,
                                maxPriceDisplay
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            FilterChip(
                selected = onlyDealsFilter,
                onClick = onToggleOnlyDeals,
                label = {
                    Text(
                        text = if (onlyDealsFilter) stringResource(R.string.bargain_filter_all)
                        else stringResource(R.string.bargain_filter_view_deals),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    }
}

@Composable
internal fun RealEmptyResultsCard(
    onOpenWizard: () -> Unit,
    onSwitchToDemo: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("real_mode_empty_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🛰️", fontSize = 40.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.real_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.real_empty_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onOpenWizard,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.real_empty_open_wizard), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onSwitchToDemo,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.real_empty_switch_demo))
            }
        }
    }
}

@Composable
internal fun FilteredTransportEmptyCard(
    activeIcon: String,
    onEnableAllTransports: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("filtered_transport_empty_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(activeIcon, fontSize = 38.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.empty_transport_mode_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.empty_transport_mode_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onEnableAllTransports,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("enable_all_transports_button")
            ) {
                Text(stringResource(R.string.btn_enable_all_transports), fontWeight = FontWeight.Bold)
            }
        }
    }
}
