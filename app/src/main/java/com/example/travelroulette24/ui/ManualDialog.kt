package com.example.travelroulette24.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.travelroulette24.R

/**
 * Interactive User Manual Dialog for TravelRoulette24.
 * Completely localized with zero hardcoded strings.
 */
@Composable
fun ManualDialog(
    onDismiss: () -> Unit,
    onOpenWizard: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        Pair(stringResource(R.string.manual_tab_roulette), 0),
        Pair(stringResource(R.string.manual_tab_modes), 1),
        Pair(stringResource(R.string.manual_tab_favorites), 2),
        Pair(stringResource(R.string.manual_tab_radar), 3),
        Pair(stringResource(R.string.manual_tab_luggage), 4),
        Pair(stringResource(R.string.manual_tab_chain), 5),
        Pair(stringResource(R.string.manual_tab_appearance), 6)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📖", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.manual_dialog_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.manual_dialog_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    FilledTonalIconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("manual_dialog_close_button")
                    ) {
                        Text(stringResource(R.string.btn_close), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Horizontally Scrollable Tabs Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabs.forEach { (title, index) ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                )
                                .clickable { selectedTab = index }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("manual_tab_$index"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Content Area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            ManualSectionCard(
                                icon = "🎲",
                                title = stringResource(R.string.manual_roulette_title),
                                body = stringResource(R.string.manual_roulette_body)
                            )
                        }
                        1 -> {
                            ManualSectionCard(
                                icon = "🟢",
                                title = stringResource(R.string.manual_modes_title),
                                body = stringResource(R.string.manual_modes_body)
                            )
                        }
                        2 -> {
                            ManualSectionCard(
                                icon = "⭐",
                                title = stringResource(R.string.manual_favorites_title),
                                body = stringResource(R.string.manual_favorites_body)
                            )
                        }
                        3 -> {
                            ManualSectionCard(
                                icon = "🔔",
                                title = stringResource(R.string.manual_radar_title),
                                body = stringResource(R.string.manual_radar_body)
                            )
                        }
                        4 -> {
                            ManualSectionCard(
                                icon = "🎒",
                                title = stringResource(R.string.manual_luggage_title),
                                body = stringResource(R.string.manual_luggage_body)
                            )
                        }
                        5 -> {
                            ManualSectionCard(
                                icon = "🔗",
                                title = stringResource(R.string.manual_chain_title),
                                body = stringResource(R.string.manual_chain_body)
                            )
                        }
                        6 -> {
                            ManualSectionCard(
                                icon = "🎨",
                                title = stringResource(R.string.manual_appearance_title),
                                body = stringResource(R.string.manual_appearance_body)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Footer with Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (onOpenWizard != null) {
                        Button(
                            onClick = {
                                onDismiss()
                                onOpenWizard()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_open_wizard_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("🧙 " + stringResource(R.string.wizard_title), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("manual_dialog_understood_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(stringResource(R.string.btn_accept), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualSectionCard(
    icon: String,
    title: String,
    body: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                Text(text = icon, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )
        }
    }
}
