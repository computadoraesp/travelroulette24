package com.example.travelroulette24.utils

import kotlin.math.roundToInt

/**
 * Handles currency symbols, live/fallback conversion rates from EUR base fares,
 * and consistent price formatting across the application.
 */
object CurrencyHelper {

    const val EUR = "EUR"
    const val USD = "USD"
    const val GBP = "GBP"

    fun symbolFor(currencyCode: String): String = when (currencyCode.uppercase()) {
        USD -> "$"
        GBP -> "£"
        else -> "€"
    }

    fun fallbackRateFromEur(currencyCode: String): Double = when (currencyCode.uppercase()) {
        USD -> 1.09
        GBP -> 0.85
        else -> 1.0
    }

    fun convertAmount(baseEurAmount: Double, rateFromEur: Double): Int {
        return (baseEurAmount * rateFromEur).roundToInt().coerceAtLeast(1)
    }

    fun toEur(displayAmount: Double, rateFromEur: Double): Double {
        if (rateFromEur <= 0.0) return displayAmount
        return displayAmount / rateFromEur
    }

    fun kmPerDisplayCurrency(kmPerEur: Double, rateFromEur: Double): Int {
        val ratio = if (rateFromEur > 0.0) kmPerEur / rateFromEur else kmPerEur
        return ratio.roundToInt().coerceAtLeast(1)
    }

    fun formatPrice(
        baseEurAmount: Double,
        currencyCode: String,
        rateFromEur: Double = fallbackRateFromEur(currencyCode)
    ): String {
        val symbol = symbolFor(currencyCode)
        val converted = convertAmount(baseEurAmount, rateFromEur)
        return "$symbol$converted"
    }
}
