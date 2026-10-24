package com.example.travelroulette24.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelroulette24.R

data class LuggageCategory(
    val titleRes: Int,
    val icon: String,
    val itemResList: List<Int>
)

val EXPRESS_LUGGAGE_CATEGORIES_RES = listOf(
    LuggageCategory(
        titleRes = R.string.luggage_cat_docs,
        icon = "🛂",
        itemResList = listOf(
            R.string.luggage_item_dni,
            R.string.luggage_item_boarding,
            R.string.luggage_item_cards,
            R.string.luggage_item_health
        )
    ),
    LuggageCategory(
        titleRes = R.string.luggage_cat_tech,
        icon = "⚡",
        itemResList = listOf(
            R.string.luggage_item_charger,
            R.string.luggage_item_powerbank,
            R.string.luggage_item_headphones
        )
    ),
    LuggageCategory(
        titleRes = R.string.luggage_cat_liquids,
        icon = "🧴",
        itemResList = listOf(
            R.string.luggage_item_zipbag,
            R.string.luggage_item_toothbrush,
            R.string.luggage_item_deodorant,
            R.string.luggage_item_meds
        )
    ),
    LuggageCategory(
        titleRes = R.string.luggage_cat_clothes,
        icon = "👕",
        itemResList = listOf(
            R.string.luggage_item_underwear,
            R.string.luggage_item_shirt,
            R.string.luggage_item_jacket
        )
    ),
    LuggageCategory(
        titleRes = R.string.luggage_cat_tips,
        icon = "💡",
        itemResList = listOf(
            R.string.luggage_item_shoes,
            R.string.luggage_item_bottle,
            R.string.luggage_item_offline_maps
        )
    )
)

@Composable
fun ExpressLuggageDialog(
    destinationCity: String? = null,
    onDismiss: () -> Unit
) {
    val checkedMap = remember {
        mutableStateMapOf<Int, Boolean>().apply {
            EXPRESS_LUGGAGE_CATEGORIES_RES.flatMap { it.itemResList }.forEach { itemRes ->
                put(itemRes, false)
            }
        }
    }

    val totalItems = EXPRESS_LUGGAGE_CATEGORIES_RES.sumOf { it.itemResList.size }
    val checkedCount = checkedMap.values.count { it }
    val progress = if (totalItems > 0) checkedCount.toFloat() / totalItems.toFloat() else 0f
    val percent = (progress * 100).toInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎒", fontSize = 20.sp)
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.luggage_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.luggage_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.luggage_progress, checkedCount, totalItems, percent),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (checkedCount == totalItems) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EXPRESS_LUGGAGE_CATEGORIES_RES.forEach { category ->
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                        ) {
                            Text(category.icon, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(category.titleRes),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    items(category.itemResList) { itemRes ->
                        val isChecked = checkedMap[itemRes] ?: false
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    checkedMap[itemRes] = !isChecked
                                }
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checkedMap[itemRes] = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(itemRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isChecked) FontWeight.Normal else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("close_luggage_checklist_button")
            ) {
                Text(
                    text = if (checkedCount == totalItems) stringResource(R.string.luggage_ready_badge)
                    else stringResource(R.string.btn_save),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    val shouldCheck = checkedCount < totalItems
                    EXPRESS_LUGGAGE_CATEGORIES_RES.flatMap { it.itemResList }.forEach { itemRes ->
                        checkedMap[itemRes] = shouldCheck
                    }
                }
            ) {
                Text(stringResource(R.string.btn_accept))
            }
        }
    )
}
