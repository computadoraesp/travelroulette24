package com.example.travelroulette24.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelroulette24.R
import com.example.travelroulette24.budget.BudgetGuardian
import com.example.travelroulette24.utils.CurrencyHelper
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MainBudgetGuardianControls(
    budget: BudgetGuardian.Budget?,
    currencySymbol: String,
    currencyRate: Double,
    onBudgetChange: (BudgetGuardian.Budget?) -> Unit
) {
    val budgetActive = budget?.enabled == true
    val budgetRange = remember(currencyRate) { CurrencyHelper.scaledBudgetRange(currencyRate) }
    val budgetPresets = remember(currencyRate) { CurrencyHelper.scaledBudgetPresets(currencyRate) }

    val canonicalDisplay = CurrencyHelper.convertAmount(budget?.maxPerTrip ?: 100.0, currencyRate)
        .toFloat()
        .coerceIn(budgetRange.start, budgetRange.endInclusive)

    var sliderDisplay by remember { mutableFloatStateOf(canonicalDisplay) }
    var budgetInputText by remember { mutableStateOf(canonicalDisplay.toInt().toString()) }

    LaunchedEffect(budget?.enabled, budget?.maxPerTrip, currencyRate) {
        val synced = CurrencyHelper.convertAmount(budget?.maxPerTrip ?: 100.0, currencyRate)
            .toFloat()
            .coerceIn(budgetRange.start, budgetRange.endInclusive)
        sliderDisplay = synced
        budgetInputText = synced.toInt().toString()
    }

    fun buildBudgetFromDisplay(displayAmount: Float): BudgetGuardian.Budget {
        val eurAmount = CurrencyHelper.toEur(displayAmount.toDouble(), currencyRate)
            .coerceAtLeast(5.0)
        return BudgetGuardian.Budget(
            maxPerTrip = eurAmount,
            maxPerRoundTrip = eurAmount * 1.8,
            maxChainStep = eurAmount,
            enabled = true
        )
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    Spacer(modifier = Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = stringResource(R.string.budget_guardian_label),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (budgetActive) {
                    stringResource(R.string.budget_active_currency_format, currencySymbol, sliderDisplay.toInt())
                } else {
                    stringResource(R.string.budget_free_mode)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (budgetActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (budgetActive) FontWeight.SemiBold else FontWeight.Normal
            )
        }
        Switch(
            checked = budgetActive,
            onCheckedChange = { checked ->
                if (checked) {
                    onBudgetChange(buildBudgetFromDisplay(sliderDisplay))
                } else {
                    onBudgetChange(null)
                }
            },
            modifier = Modifier.testTag("budget_guardian_switch")
        )
    }

    if (budgetActive) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = budgetInputText,
                onValueChange = { input ->
                    val digits = input.filter { it.isDigit() }.take(9)
                    budgetInputText = digits
                    val parsed = digits.toFloatOrNull()?.coerceIn(budgetRange.start, budgetRange.endInclusive)
                    if (parsed != null) {
                        sliderDisplay = parsed
                        onBudgetChange(buildBudgetFromDisplay(parsed))
                    }
                },
                label = { Text(stringResource(R.string.budget_input_field_label)) },
                prefix = { Text("$currencySymbol ", fontWeight = FontWeight.Bold) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("budget_amount_input")
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$currencySymbol${sliderDisplay.toInt()}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${budgetRange.start.toInt()}$currencySymbol - ${budgetRange.endInclusive.toInt()}$currencySymbol",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Slider(
            value = sliderDisplay.coerceIn(budgetRange.start, budgetRange.endInclusive),
            onValueChange = { newVal ->
                sliderDisplay = newVal
                budgetInputText = newVal.toInt().toString()
            },
            onValueChangeFinished = {
                onBudgetChange(buildBudgetFromDisplay(sliderDisplay))
            },
            valueRange = budgetRange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("budget_slider")
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${budgetRange.start.toInt()} $currencySymbol",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${budgetRange.endInclusive.toInt()} $currencySymbol",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            budgetPresets.forEach { limit ->
                FilterChip(
                    selected = sliderDisplay.toInt() == limit.toInt(),
                    onClick = {
                        sliderDisplay = limit
                        budgetInputText = limit.toInt().toString()
                        onBudgetChange(buildBudgetFromDisplay(limit))
                    },
                    label = { Text("$currencySymbol${limit.toInt()}") },
                    modifier = Modifier.testTag("budget_preset_${limit.toInt()}")
                )
            }
        }
    }
}

@Composable
internal fun MainDistanceVetoControls(
    enabled: Boolean,
    minDistanceKm: Int,
    hasHistoricalCoverage: Boolean,
    onChange: (Boolean, Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = stringResource(R.string.min_distance_veto_title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = when {
                    !enabled -> stringResource(R.string.min_distance_veto_off_desc)
                    hasHistoricalCoverage -> stringResource(R.string.min_distance_veto_active_format, minDistanceKm)
                    else -> stringResource(R.string.min_distance_veto_no_history_format, minDistanceKm)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled && !hasHistoricalCoverage) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = { onChange(it, minDistanceKm) },
            modifier = Modifier.testTag("min_distance_veto_switch")
        )
    }

    if (enabled) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.min_distance_veto_slider_label, minDistanceKm),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Slider(
            value = minDistanceKm.toFloat().coerceIn(100f, 3000f),
            onValueChange = { onChange(true, (it / 50f).roundToInt() * 50) },
            valueRange = 100f..3000f,
            modifier = Modifier.testTag("min_distance_veto_slider")
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(200, 500, 1000, 2000).forEach { presetKm ->
                FilterChip(
                    selected = minDistanceKm == presetKm,
                    onClick = { onChange(true, presetKm) },
                    label = { Text("${presetKm}km", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f).testTag("min_distance_veto_preset_$presetKm")
                )
            }
        }
    }
}
