package com.example.travelroulette24.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelroulette24.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainTopBar(
    activeTransportIcon: String,
    selectedTransportModes: Set<String>,
    onToggleTransportMode: (String) -> Unit,
    isRealMode: Boolean,
    onToggleRealMode: () -> Unit,
    wishlistMatchCount: Int,
    wishlistTotalCount: Int,
    onOpenWishlist: () -> Unit,
    savedTripsCount: Int,
    onOpenSavedTrips: () -> Unit,
    isRadarEnabled: Boolean,
    radarMatchCount: Int,
    onOpenPriceAlert: () -> Unit,
    onOpenLuggage: () -> Unit,
    onOpenApiWizard: () -> Unit,
    onOpenManual: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var showOverflowMenu by remember { mutableStateOf(false) }
    val wishlistCd = stringResource(R.string.cd_open_wishlist)
    val fav24hCd = stringResource(R.string.cd_open_saved_24h)
    val moreOptionsCd = stringResource(R.string.menu_more_options)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .testTag("active_transport_emblem")
                        ) {
                            Text(activeTransportIcon, fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isRealMode) MaterialTheme.colorScheme.tertiaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { onToggleRealMode() }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("toggle_real_mode_chip"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRealMode) stringResource(R.string.mode_real_badge)
                            else stringResource(R.string.mode_demo_badge),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isRealMode) MaterialTheme.colorScheme.onTertiaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // ❤️ Latent Wishlist Destinations (Top 3) Button
                    Box {
                        IconButton(
                            onClick = onOpenWishlist,
                            modifier = Modifier
                                .semantics { contentDescription = wishlistCd }
                                .testTag("open_favorite_trip_button")
                        ) {
                            Text("❤️", fontSize = 20.sp)
                        }
                        val wishlistBadgeCount = if (wishlistMatchCount > 0) wishlistMatchCount else wishlistTotalCount
                        if (wishlistBadgeCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 4.dp, end = 4.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (wishlistMatchCount > 0) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.secondary
                                    )
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "$wishlistBadgeCount",
                                    color = if (wishlistMatchCount > 0) MaterialTheme.colorScheme.onError
                                    else MaterialTheme.colorScheme.onSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // ⭐ Saved 24h Favorites Button
                    Box {
                        IconButton(
                            onClick = onOpenSavedTrips,
                            modifier = Modifier
                                .semantics { contentDescription = fav24hCd }
                                .testTag("open_saved_trips_button")
                        ) {
                            Text("⭐", fontSize = 20.sp)
                        }
                        if (savedTripsCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 4.dp, end = 4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "$savedTripsCount",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // ⋮ Overflow Menu
                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier
                                .semantics { contentDescription = moreOptionsCd }
                                .testTag("open_overflow_menu_button")
                        ) {
                            Text("⋮", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                        if (isRadarEnabled && radarMatchCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 6.dp, end = 6.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🔔", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(stringResource(R.string.menu_item_price_alerts))
                                        if (isRadarEnabled && radarMatchCount > 0) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.error)
                                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = "$radarMatchCount",
                                                    color = MaterialTheme.colorScheme.onError,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onOpenPriceAlert()
                                },
                                modifier = Modifier.testTag("open_price_alert_button")
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🎒", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(stringResource(R.string.menu_item_luggage))
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onOpenLuggage()
                                },
                                modifier = Modifier.testTag("open_luggage_checklist_topbar_button")
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🧙", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(stringResource(R.string.menu_item_api_wizard))
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onOpenApiWizard()
                                },
                                modifier = Modifier.testTag("open_api_wizard_header_button")
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("📖", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(stringResource(R.string.menu_item_user_manual))
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onOpenManual()
                                },
                                modifier = Modifier.testTag("open_user_manual_topbar_button")
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("⚙️", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(stringResource(R.string.menu_item_settings))
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onOpenSettings()
                                },
                                modifier = Modifier.testTag("open_settings_button")
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            TransportModeSelectorBar(
                selectedTransportModes = selectedTransportModes,
                onToggleTransportMode = onToggleTransportMode
            )
        }
    }
}

@Composable
private fun TransportModeSelectorBar(
    selectedTransportModes: Set<String>,
    onToggleTransportMode: (String) -> Unit
) {
    val options = listOf(
        Triple(TravelViewModel.TRANSPORT_FLIGHT, "✈️", stringResource(R.string.transport_flight)),
        Triple(TravelViewModel.TRANSPORT_TRAIN, "🚆", stringResource(R.string.transport_train)),
        Triple(TravelViewModel.TRANSPORT_FERRY, "⛴️", stringResource(R.string.transport_ferry))
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { (modeKey, icon, label) ->
            val isSelected = modeKey in selectedTransportModes
            val containerColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                },
                label = "transport_switch_bg_$modeKey"
            )
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                },
                label = "transport_switch_border_$modeKey"
            )
            val indicatorColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                },
                label = "transport_switch_dot_$modeKey"
            )

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onToggleTransportMode(modeKey) }
                    .semantics {
                        selected = isSelected
                        contentDescription = label
                    }
                    .testTag("transport_switch_$modeKey"),
                shape = RoundedCornerShape(14.dp),
                color = containerColor,
                border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(indicatorColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = icon,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
