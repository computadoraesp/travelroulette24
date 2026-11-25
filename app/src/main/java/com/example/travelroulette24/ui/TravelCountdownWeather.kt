package com.example.travelroulette24.ui

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.travelroulette24.R
import com.example.travelroulette24.data.DemoTravelDataSource
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

data class DestinationWeather(
    val tempC: Int,
    val icon: String,
    val condition: String,
    val isLive: Boolean = false,
    @StringRes val conditionRes: Int? = null
)

/**
 * Live departure countdown widget. Ticks every second and visually changes color
 * when the flight/transport departure is imminent (<2 hours).
 */
@Composable
fun LiveDepartureCountdownBadge(
    departureTimeIso: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    var remainingSeconds by remember(departureTimeIso) {
        mutableStateOf(calculateRemainingSeconds(departureTimeIso))
    }

    LaunchedEffect(departureTimeIso) {
        while (true) {
            remainingSeconds = calculateRemainingSeconds(departureTimeIso)
            delay(1000L)
        }
    }

    val isImminent = remainingSeconds in 1..(2 * 3600)
    val isDeparted = remainingSeconds <= 0

    val hours = (remainingSeconds / 3600).coerceAtLeast(0).toInt()
    val minutes = ((remainingSeconds % 3600) / 60).coerceAtLeast(0).toInt()
    val seconds = (remainingSeconds % 60).coerceAtLeast(0).toInt()

    val displayText = when {
        isDeparted -> stringResource(R.string.countdown_boarding)
        compact -> stringResource(R.string.countdown_compact_format, hours, minutes)
        isImminent -> stringResource(R.string.countdown_imminent_format, hours, minutes, seconds)
        else -> stringResource(R.string.countdown_regular_format, hours, minutes, seconds)
    }

    val containerColor by animateColorAsState(
        targetValue = when {
            isDeparted -> MaterialTheme.colorScheme.surfaceVariant
            isImminent -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f)
        },
        label = "countdown_bg"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isDeparted -> MaterialTheme.colorScheme.onSurfaceVariant
            isImminent -> MaterialTheme.colorScheme.onErrorContainer
            else -> MaterialTheme.colorScheme.onTertiaryContainer
        },
        label = "countdown_fg"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayText,
            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

/**
 * Compact destination weather widget displaying temperature and weather condition.
 * Strictly respects Demo vs Real separation: in Real Mode only shows live Open-Meteo weather.
 */
@Composable
fun DestinationWeatherBadge(
    destinationHub: String,
    modifier: Modifier = Modifier,
    liveWeather: DestinationWeather? = null,
    isRealMode: Boolean = false
) {
    val weather = if (isRealMode) {
        liveWeather ?: return
    } else {
        liveWeather ?: DemoTravelDataSource.getSimulatedWeather(destinationHub)
    }

    val tagText = if (weather.isLive) {
        stringResource(R.string.weather_live_tag)
    } else {
        stringResource(R.string.weather_demo_tag)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text = weather.icon, fontSize = 13.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "${weather.tempC}°C",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        val conditionLabel = weather.conditionRes?.let { stringResource(it) } ?: weather.condition
        Text(
            text = "• $conditionLabel",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (weather.isLive) Color(0xFF2E7D32).copy(alpha = 0.18f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
                )
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = tagText,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (weather.isLive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun calculateRemainingSeconds(departureTimeIso: String): Long {
    return try {
        val departureInstant = Instant.parse(departureTimeIso)
        val now = Instant.now()
        val duration = Duration.between(now, departureInstant)
        duration.seconds
    } catch (_: Exception) {
        // Fallback default: 5 hours 30 mins from now
        5 * 3600 + 30 * 60
    }
}
