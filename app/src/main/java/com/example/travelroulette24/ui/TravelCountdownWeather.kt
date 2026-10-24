package com.example.travelroulette24.ui

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
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

data class DestinationWeather(
    val tempC: Int,
    val icon: String,
    val condition: String
)

object WeatherDirectory {
    private val weatherMap = mapOf(
        "BCN" to DestinationWeather(22, "☀️", "Soleado y templado"),
        "MAD" to DestinationWeather(21, "🌤️", "Despejado con brisa"),
        "PMI" to DestinationWeather(24, "☀️", "Sol mediterráneo"),
        "IBZ" to DestinationWeather(25, "☀️", "Cálido y soleado"),
        "AGP" to DestinationWeather(23, "☀️", "Sol de la Costa"),
        "VLC" to DestinationWeather(22, "🌤️", "Agradable"),
        "SVQ" to DestinationWeather(26, "☀️", "Cálido y despejado"),
        "BIO" to DestinationWeather(18, "🌦️", "Nublado suave"),
        "ALC" to DestinationWeather(23, "☀️", "Despejado"),
        "SCQ" to DestinationWeather(17, "🌧️", "Lluvia fina"),
        "PAR" to DestinationWeather(16, "⛅", "Parcialmente nublado"),
        "CDG" to DestinationWeather(16, "⛅", "Parcialmente nublado"),
        "ORY" to DestinationWeather(16, "⛅", "Parcialmente nublado"),
        "LON" to DestinationWeather(15, "☁️", "Nublado típico"),
        "LHR" to DestinationWeather(15, "☁️", "Nublado"),
        "LGW" to DestinationWeather(15, "☁️", "Nublado"),
        "STN" to DestinationWeather(14, "☁️", "Fresco y nublado"),
        "ROM" to DestinationWeather(20, "☀️", "Soleado romano"),
        "FCO" to DestinationWeather(20, "☀️", "Soleado"),
        "BER" to DestinationWeather(14, "🌥️", "Fresco con claros"),
        "AMS" to DestinationWeather(15, "🌦️", "Brisa marina y nubes"),
        "LIS" to DestinationWeather(21, "☀️", "Sol atlántico"),
        "OPO" to DestinationWeather(19, "🌤️", "Templado con brisa"),
        "MIL" to DestinationWeather(19, "⛅", "Claro"),
        "MXP" to DestinationWeather(19, "⛅", "Claro"),
        "VCE" to DestinationWeather(18, "🌤️", "Agradable"),
        "ATH" to DestinationWeather(23, "☀️", "Cálido mediterráneo"),
        "DUB" to DestinationWeather(13, "🌧️", "Fresco y lluvioso"),
        "VIE" to DestinationWeather(16, "⛅", "Templado"),
        "PRG" to DestinationWeather(15, "🌥️", "Fresco y seco"),
        "BRU" to DestinationWeather(15, "☁️", "Nuboso"),
        "ZRH" to DestinationWeather(14, "🌤️", "Fresco alpino"),
        "GVA" to DestinationWeather(15, "🌤️", "Despejado"),
        "TFN" to DestinationWeather(24, "☀️", "Primavera eterna"),
        "TFS" to DestinationWeather(25, "☀️", "Cálido y soleado"),
        "LPA" to DestinationWeather(24, "☀️", "Agradable y soleado"),
        "MAH" to DestinationWeather(23, "☀️", "Soleado"),
        "FUE" to DestinationWeather(25, "💨", "Soleado con brisa")
    )

    fun getWeather(hubCode: String): DestinationWeather {
        return weatherMap[hubCode.uppercase()]
            ?: DestinationWeather(
                tempC = 20 + (hubCode.hashCode() % 6).coerceAtLeast(0),
                icon = "🌤️",
                condition = "Templado agradable"
            )
    }
}

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

    val hours = (remainingSeconds / 3600).coerceAtLeast(0)
    val minutes = ((remainingSeconds % 3600) / 60).coerceAtLeast(0)
    val seconds = (remainingSeconds % 60).coerceAtLeast(0)

    val displayText = when {
        isDeparted -> "✈️ En puerta / Despegando"
        compact -> String.format("⏳ en %02dh %02dm", hours, minutes)
        isImminent -> String.format("🔥 ¡Inminente! %02dh %02dm %02ds", hours, minutes, seconds)
        else -> String.format("⏳ Despegue en %02dh %02dm %02ds", hours, minutes, seconds)
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
 */
@Composable
fun DestinationWeatherBadge(
    destinationHub: String,
    modifier: Modifier = Modifier
) {
    val weather = WeatherDirectory.getWeather(destinationHub)

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
        Text(
            text = "• ${weather.condition}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
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
