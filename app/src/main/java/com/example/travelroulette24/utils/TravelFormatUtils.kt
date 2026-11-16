package com.example.travelroulette24.utils

import android.net.Uri
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Shared formatting and parsing utilities across cards and dialogs,
 * avoiding duplicate formatters and provider-parsing blocks.
 */
object TravelFormatUtils {

    private val cardDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm - dd MMM")

    private val compactDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM • HH:mm")

    fun formatIsoDate(iso: String, compact: Boolean = false): String {
        return try {
            val instant = Instant.parse(iso)
            val zonedDateTime = instant.atZone(ZoneId.systemDefault())
            val formatter = if (compact) compactDateFormatter else cardDateFormatter
            formatter.format(zonedDateTime)
        } catch (_: Exception) {
            iso
        }
    }

    fun modeEmoji(mode: String): String = when (mode.lowercase()) {
        "train" -> "🚆"
        "ferry", "boat" -> "⛴️"
        else -> "✈️"
    }

    fun extractCustomProviderName(deepLink: String): String? {
        return try {
            if (deepLink.contains("provider=")) {
                Uri.parse(deepLink).getQueryParameter("provider")
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
