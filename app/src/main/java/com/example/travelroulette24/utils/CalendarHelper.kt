package com.example.travelroulette24.utils

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import com.example.travelroulette24.ui.HubCoordinates
import java.time.Instant

object CalendarHelper {
    /**
     * Dispatches an ACTION_INSERT intent to the device's native calendar application,
     * pre-filling the title, departure time, origin terminal, and booking link.
     */
    fun addTravelEvent(context: Context, item: TravelIntelligenceEngine.ResultItem) {
        val destCity = HubCoordinates.getCity(item.destinationHub)
        val originCity = HubCoordinates.getCity(item.originHub)
        val startMillis = try {
            Instant.parse(item.departureTime).toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis() + 4 * 3600 * 1000L
        }
        val endMillis = startMillis + 2 * 3600 * 1000L

        val modeLabel = when (item.mode.lowercase()) {
            "train" -> "Tren"
            "ferry" -> "Ferry"
            else -> "Vuelo"
        }

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            putExtra(CalendarContract.Events.TITLE, "✈️ Salida a $destCity (${item.destinationHub})")
            putExtra(
                CalendarContract.Events.DESCRIPTION,
                "Reserva TravelRoulette24: $modeLabel desde $originCity (${item.originHub}) a $destCity (${item.destinationHub}).\nTarifa: €${item.price.toInt()}\nEnlace de reserva: ${item.deepLink}"
            )
            putExtra(CalendarContract.Events.EVENT_LOCATION, "${item.originHub} Terminal")
            putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
        }

        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "No se encontró aplicación de calendario", Toast.LENGTH_SHORT).show()
        }
    }
}
