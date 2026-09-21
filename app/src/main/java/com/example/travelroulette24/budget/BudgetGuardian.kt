package com.example.travelroulette24.budget

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Travel Budget Guardian for TravelRoulette24.
 *
 * Protects the user's budget across all travel modes:
 * - Budget is set per trip (one-way leg) and/or per round trip.
 * - Filters travel options so only budget-compliant ones are shown.
 * - Generates human-readable alerts when a budget-compliant opportunity appears.
 * - Supports "Budget Roulette": only budget-compliant options enter the wheel.
 * - Works per leg for multi-leg (travel-chained) journeys.
 * - No registration: persisted locally on device via [BudgetStorage].
 */
object BudgetGuardian {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    data class Budget(
        val maxPerTrip: Double? = null,      // one-way / single leg limit
        val maxPerRoundTrip: Double? = null, // round-trip limit
        val maxChainStep: Double? = null,    // per step limit in a chain
        val enabled: Boolean = false
    )

    /** Lightweight local persistence abstraction (e.g. SharedPreferences/DataStore-backed). */
    interface BudgetStorage {
        fun load(): String?
        fun save(json: String)
    }

    /**
     * Device-local budget controller. The budget never leaves the device —
     * no registration, no server.
     */
    class BudgetController(private val storage: BudgetStorage) {
        fun getBudget(): Budget? = storage.load()?.takeIf { it.isNotBlank() }?.let {
            try { json.decodeFromString<Budget>(it) } catch (_: Exception) { null }
        }

        fun setBudget(budget: Budget) = storage.save(json.encodeToString(budget))

        fun clearBudget() = storage.save("")
    }

    // ---------------- Filtering ----------------

    /**
     * Returns true when the option price fits the active budget limits.
     * Calculated even if the budget is not 'enabled', as it drives the 
     * 'isBudgetFriendly' flag in the engine.
     */
    fun fitsBudget(budget: Budget?, price: Double, isRoundTrip: Boolean): Boolean {
        if (budget == null) return true
        return if (isRoundTrip) {
            budget.maxPerRoundTrip?.let { price <= it } ?: budget.maxPerTrip?.let { price <= it } ?: true
        } else {
            val limit = budget.maxPerTrip ?: budget.maxChainStep
            limit?.let { price <= it } ?: true
        }
    }

    /**
     * Filters options (e.g. engine results) down to only those within budget.
     * Only filters if budget. Enabled is true.
     */
    fun <T> filterWithinBudget(
        budget: Budget?,
        options: List<T>,
        isRoundTrip: (T) -> Boolean,
        price: (T) -> Double
    ): List<T> {
        if (budget?.enabled != true) return options
        return options.filter { fitsBudget(budget, price(it), isRoundTrip(it)) }
    }

    // ---------------- Alerts ----------------

    /**
     * Generates an alert when a travel option is both within budget and flagged
     * as an opportunity (lowest historical price). Returns null otherwise.
     * Works per leg for multi-leg journeys: call with each leg's data.
     */
    fun maybeAlert(
        budget: Budget?,
        price: Double,
        currency: String,
        isOpportunity: Boolean,
        mode: String,
        destinationHub: String,
        isRoundTrip: Boolean
    ): String? {
        if (!fitsBudget(budget, price, isRoundTrip)) return null
        if (!isOpportunity) return null
        val cap = budget?.let { if (isRoundTrip) it.maxPerRoundTrip else it.maxPerTrip }
        return "There is a $mode to $destinationHub for ${formatPrice(price, currency)}, " +
            "which is within your ${formatPrice(cap ?: price, currency)} budget."
    }

    private fun formatPrice(amount: Double, currency: String): String =
        if (amount == amount.toLong().toDouble()) {
            "$currency${amount.toLong()}"
        } else {
            "$currency${"%.2f".format(amount)}"
        }
}