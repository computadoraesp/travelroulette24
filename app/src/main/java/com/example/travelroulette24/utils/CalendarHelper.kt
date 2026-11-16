package com.example.travelroulette24.utils

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import com.example.travelroulette24.R
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import com.example.travelroulette24.ui.HubCoordinates
import java.time.Instant

object CalendarHelper {
    /**
     * Dispatches an ACTION_INSERT intent to the device's native calendar application,
     * pre-filling the title, departure time, origin terminal, and booking link.
     */
    fun addTravelEvent(
        context: Context,
        item: TravelIntelligenceEngine.ResultItem,
        currencyCode: String = CurrencyHelper.EUR,
        rateFromEur: Double = 1.0
    ) {
        val destCity = HubCoordinates.getCity(item.destinationHub)
        val originCity = HubCoordinates.getCity(item.originHub)
        val startMillis = try {
            Instant.parse(item.departureTime).toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis() + 4 * 3600 * 1000L
        }
        val endMillis = startMillis + 2 * 3600 * 1000L

        val modeLabel = when (item.mode.lowercase()) {
            "train" -> context.getString(R.string.calendar_mode_train)
            "ferry", "boat" -> context.getString(R.string.calendar_mode_ferry)
            else -> context.getString(R.string.calendar_mode_flight)
        }
        val formattedPrice = CurrencyHelper.formatPrice(item.price, currencyCode, rateFromEur)

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            putExtra(
                CalendarContract.Events.TITLE,
                context.getString(R.string.calendar_event_title, destCity, item.destinationHub)
            )
            putExtra(
                CalendarContract.Events.DESCRIPTION,
                context.getString(
                    R.string.calendar_event_desc,
                    modeLabel,
                    originCity,
                    item.originHub,
                    destCity,
                    item.destinationHub,
                    formattedPrice,
                    item.deepLink
                )
            )
            putExtra(
                CalendarContract.Events.EVENT_LOCATION,
                context.getString(R.string.calendar_event_location, item.originHub)
            )
            putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
        }

        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, context.getString(R.string.calendar_not_found), Toast.LENGTH_SHORT).show()
        }
    }
}
