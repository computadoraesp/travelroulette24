package com.example.travelroulette24.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import com.example.travelroulette24.utils.CurrencyHelper
import com.example.travelroulette24.utils.TravelFormatUtils

/**
 * Optional product module: toggleable Top Value Comparison (km / €) card.
 * Lets users opt in/out of comparing the top destinations side-by-side by Travel Value Index.
 */
@Composable
fun ValueComparisonSection(
    items: List<TravelIntelligenceEngine.ResultItem>,
    selectedItem: TravelIntelligenceEngine.ResultItem?,
    enabled: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    currencyCode: String,
    currencyRate: Double,
    onSelectItem: (TravelIntelligenceEngine.ResultItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val topValueItems = remember(items) {
        items.sortedWith(
            compareBy(
                { it.isEstimatedPrice },
                {
                    val dist = HubCoordinates.calculateDistanceKm(it.originHub, it.destinationHub)
                    -TravelIntelligenceEngine.computeTravelValueIndex(dist, it.price)
                },
                { it.price }
            )
        ).take(4)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("value_comparison_section_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = stringResource(R.string.value_comparison_toggle_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.value_comparison_toggle_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggleEnabled,
                    modifier = Modifier.testTag("toggle_value_comparison_switch")
                )
            }

            AnimatedVisibility(visible = enabled && topValueItems.isNotEmpty()) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        text = stringResource(R.string.value_comparison_tap_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    topValueItems.forEachIndexed { index, item ->
                        val destCity = HubCoordinates.getCity(item.destinationHub)
                        val distKm = HubCoordinates.calculateDistanceKm(item.originHub, item.destinationHub)
                        val valueIdx = TravelIntelligenceEngine.computeTravelValueIndex(distKm, item.price)
                        val formattedPrice = CurrencyHelper.formatPrice(item.price, currencyCode, currencyRate)
                        val isSelected = selectedItem == item

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                )
                                .clickable { onSelectItem(item) }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("value_comparison_row_$index"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "#${index + 1} ${TravelFormatUtils.modeEmoji(item.mode)} $destCity",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (index == 0) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.primary)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.badge_best_value),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "$distKm km • ${stringResource(R.string.value_index_format, valueIdx)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = formattedPrice,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (item.isEstimatedPrice) {
                                    Text(
                                        text = stringResource(R.string.badge_estimated_price),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        if (index < topValueItems.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}
