package com.example.travelroulette24.utils

import java.util.Currency
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Universal ISO-4217 currency catalog, symbol/name resolution, live/fallback
 * EUR conversion rates, and dynamic UI scaling for world currencies.
 */
object CurrencyHelper {

    const val EUR = "EUR"
    const val USD = "USD"
    const val GBP = "GBP"

    data class CurrencyOption(
        val code: String,
        val symbol: String,
        val displayName: String
    )

    private val fallbackRatesFromEur: Map<String, Double> = mapOf(
        "EUR" to 1.0,
        "USD" to 1.09,
        "GBP" to 0.85,
        "MXN" to 21.5,
        "COP" to 4450.0,
        "ARS" to 1120.0,
        "CLP" to 1040.0,
        "PEN" to 4.08,
        "BRL" to 6.25,
        "UYU" to 45.5,
        "BOB" to 7.52,
        "PYG" to 8350.0,
        "CRC" to 558.0,
        "DOP" to 65.5,
        "GTQ" to 8.40,
        "HNL" to 27.3,
        "NIO" to 40.1,
        "PAB" to 1.09,
        "CAD" to 1.51,
        "CHF" to 0.94,
        "JPY" to 165.0,
        "KRW" to 1500.0,
        "CNY" to 7.85,
        "AUD" to 1.66,
        "NZD" to 1.81,
        "SEK" to 11.4,
        "NOK" to 11.7,
        "DKK" to 7.46,
        "PLN" to 4.30,
        "CZK" to 25.2,
        "HUF" to 405.0,
        "TRY" to 37.5,
        "MAD" to 10.8,
        "ZAR" to 19.6,
        "AED" to 4.00,
        "INR" to 92.0
    )

    private val distinctSymbols: Map<String, String> = mapOf(
        "EUR" to "€",
        "USD" to "$",
        "GBP" to "£",
        "MXN" to "MX$",
        "COP" to "COP$",
        "ARS" to "AR$",
        "CLP" to "CLP$",
        "PEN" to "S/",
        "BRL" to "R$",
        "UYU" to "\$U",
        "CAD" to "CA$",
        "AUD" to "A$",
        "NZD" to "NZ$",
        "JPY" to "¥",
        "CNY" to "¥",
        "KRW" to "₩",
        "INR" to "₹",
        "TRY" to "₺",
        "CHF" to "CHF "
    )

    fun defaultCurrencyForLocale(locale: Locale = Locale.getDefault()): String {
        return try {
            Currency.getInstance(locale)?.currencyCode?.uppercase(Locale.US) ?: USD
        } catch (_: Exception) {
            USD
        }
    }

    fun getAvailableWorldCurrencies(locale: Locale = Locale.getDefault()): List<CurrencyOption> {
        val priorityCodes = listOf(
            defaultCurrencyForLocale(locale),
            "USD", "EUR", "MXN", "COP", "ARS", "CLP", "PEN", "BRL", "UYU", "GBP", "CAD", "CHF", "JPY"
        ).distinct()

        val all = try {
            Currency.getAvailableCurrencies()
                .asSequence()
                .filter { it.currencyCode.length == 3 && !it.currencyCode.startsWith("X") }
                .map { cur ->
                    val code = cur.currencyCode.uppercase(Locale.US)
                    CurrencyOption(
                        code = code,
                        symbol = symbolFor(code, locale),
                        displayName = runCatching { cur.getDisplayName(locale) }.getOrDefault(code)
                            .replaceFirstChar { ch -> if (ch.isLowerCase()) ch.titlecase(locale) else ch.toString() }
                    )
                }
                .toList()
        } catch (_: Exception) {
            fallbackRatesFromEur.keys.map { code ->
                CurrencyOption(code = code, symbol = symbolFor(code, locale), displayName = code)
            }
        }

        val byCode = all.associateBy { it.code }
        val prioritySet = priorityCodes.toSet()
        val prioritized = priorityCodes.mapNotNull { byCode[it] }
        val rest = all.filter { it.code !in prioritySet }.sortedBy { it.code }
        return prioritized + rest
    }

    fun displayNameFor(currencyCode: String, locale: Locale = Locale.getDefault()): String {
        val normalized = currencyCode.trim().uppercase(Locale.US)
        return try {
            Currency.getInstance(normalized)
                .getDisplayName(locale)
                .replaceFirstChar { ch -> if (ch.isLowerCase()) ch.titlecase(locale) else ch.toString() }
        } catch (_: Exception) {
            normalized
        }
    }

    fun symbolFor(currencyCode: String, locale: Locale = Locale.getDefault()): String {
        val normalized = currencyCode.trim().uppercase(Locale.US)
        distinctSymbols[normalized]?.let { return it }
        return try {
            val sym = Currency.getInstance(normalized).getSymbol(locale)
            if (sym.isBlank() || sym == normalized) "$normalized " else sym
        } catch (_: Exception) {
            "€"
        }
    }

    fun fallbackRateFromEur(currencyCode: String): Double {
        val normalized = currencyCode.trim().uppercase(Locale.US)
        return fallbackRatesFromEur[normalized] ?: 1.0
    }

    fun convertAmount(baseEurAmount: Double, rateFromEur: Double): Int {
        val safeRate = if (rateFromEur > 0.0) rateFromEur else 1.0
        return (baseEurAmount * safeRate).roundToInt().coerceAtLeast(1)
    }

    fun toEur(displayAmount: Double, rateFromEur: Double): Double {
        if (rateFromEur <= 0.0) return displayAmount
        return displayAmount / rateFromEur
    }

    fun kmPerDisplayCurrency(kmPerEur: Double, rateFromEur: Double): Int {
        val safeRate = if (rateFromEur > 0.0) rateFromEur else 1.0
        return (kmPerEur / safeRate).roundToInt().coerceAtLeast(1)
    }

    /** Rounds scaled values to clean human-friendly numbers regardless of currency magnitude. */
    fun niceRound(value: Double): Float {
        if (value <= 1.0) return 1f
        val magnitude = 10.0.pow(kotlin.math.floor(kotlin.math.log10(value)))
        val normalized = value / magnitude
        val snapped = when {
            normalized < 1.5 -> 1.0
            normalized < 3.5 -> 2.0
            normalized < 7.5 -> 5.0
            else -> 10.0
        }
        return (snapped * magnitude).toFloat().coerceAtLeast(1f)
    }

    fun scaledBudgetRange(rateFromEur: Double): ClosedFloatingPointRange<Float> {
        val safeRate = if (rateFromEur > 0.0) rateFromEur else 1.0
        val minVal = niceRound(10.0 * safeRate).coerceAtLeast(1f)
        val maxVal = niceRound(5000.0 * safeRate).coerceAtLeast(minVal + 50f)
        return minVal..maxVal
    }

    fun scaledBudgetPresets(rateFromEur: Double): List<Float> {
        val safeRate = if (rateFromEur > 0.0) rateFromEur else 1.0
        return listOf(50.0, 100.0, 250.0, 500.0, 1000.0)
            .map { niceRound(it * safeRate) }
            .distinct()
    }

    fun scaledAlertPriceRange(rateFromEur: Double): ClosedFloatingPointRange<Float> {
        val safeRate = if (rateFromEur > 0.0) rateFromEur else 1.0
        val minVal = niceRound(15.0 * safeRate).coerceAtLeast(1f)
        val maxVal = niceRound(350.0 * safeRate).coerceAtLeast(minVal + 20f)
        return minVal..maxVal
    }

    fun scaledAlertPricePresets(rateFromEur: Double): List<Float> {
        val safeRate = if (rateFromEur > 0.0) rateFromEur else 1.0
        return listOf(25.0, 50.0, 80.0, 120.0, 200.0)
            .map { niceRound(it * safeRate) }
            .distinct()
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
