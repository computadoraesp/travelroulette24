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
import com.example.travelroulette24.R
import com.example.travelroulette24.budget.BudgetGuardian
import com.example.travelroulette24.utils.CurrencyHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MainBudgetGuardianControls(
    budget: BudgetGuardian.Budget?,
    currencySymbol: String,
    currencyRate: Double,
    onBudgetChange: (BudgetGuardian.Budget?) -> Unit
) {
    val budgetActive = budget?.enabled == true
    val canonicalDisplay = CurrencyHelper.convertAmount(budget?.maxPerTrip ?: 100.0, currencyRate)
        .toFloat()
        .coerceIn(10f, 5000f)

    var sliderDisplay by remember { mutableFloatStateOf(canonicalDisplay) }
    var budgetInputText by remember { mutableStateOf(canonicalDisplay.toInt().toString()) }

    LaunchedEffect(budget?.enabled, budget?.maxPerTrip, currencyRate) {
        val synced = CurrencyHelper.convertAmount(budget?.maxPerTrip ?: 100.0, currencyRate)
            .toFloat()
            .coerceIn(10f, 5000f)
        sliderDisplay = synced
        budgetInputText = synced.toInt().toString()
    }

    fun buildBudgetFromDisplay(displayAmount: Float): BudgetGuardian.Budget {
        val eurAmount = CurrencyHelper.toEur(displayAmount.toDouble(), currencyRate)
            .coerceAtLeast(10.0)
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
                    val digits = input.filter { it.isDigit() }.take(5)
                    budgetInputText = digits
                    val parsed = digits.toFloatOrNull()?.coerceIn(10f, 5000f)
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
                    text = "10$currencySymbol - 5000$currencySymbol",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Slider(
            value = sliderDisplay.coerceIn(10f, 5000f),
            onValueChange = { newVal ->
                sliderDisplay = newVal
                budgetInputText = newVal.toInt().toString()
            },
            onValueChangeFinished = {
                onBudgetChange(buildBudgetFromDisplay(sliderDisplay))
            },
            valueRange = 10f..5000f,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("budget_slider")
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "10 $currencySymbol",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "5000 $currencySymbol",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(50f, 100f, 250f, 500f, 1000f).forEach { limit ->
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
