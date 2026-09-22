package com.example.travelroulette24.data.byok

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Intelligent Token & Quota Governor for BYOK and Custom APIs.
 *
 * Prevents exhausting monthly quotas in a few days by:
 * 1. Enforcing daily safe budgets (Monthly Quota / 30).
 * 2. Caching flight and transit sweeps with configurable TTL (default 2 hours).
 * 3. Distinguishing Free Tiers (protected rate-limits) from Paid Tiers (unlimited/deep sweeps).
 * 4. Gracefully falling back to Free/Open tokens (Open HAFAS, deep links, synthetic heuristics)
 *    when daily quotas are reached.
 */
class ApiQuotaGovernor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    // Memory cache for active session with timestamps
    private val memoryOfferCache = mutableMapOf<String, Pair<Long, JsonArray>>()

    enum class TierMode {
        FREE_PROTECTED, // Strictly caps daily consumption to avoid burning monthly quota
        PAID_UNLIMITED  // Allows unrestricted live deep sweeps
    }

    data class QuotaStatus(
        val provider: String,
        val tierMode: TierMode,
        val requestsToday: Int,
        val dailyBudget: Int,
        val requestsThisMonth: Int,
        val monthlyLimit: Int,
        val isDailyExhausted: Boolean,
        val isMonthlyExhausted: Boolean,
        val remainingDailyTokens: Int
    )

    private val todayString: String
        get() = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    private val currentMonthString: String
        get() = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))

    /**
     * Calculates the safe daily budget leaving an extra reserve margin for when
     * the user spins the roulette multiple times without exhausting the monthly quota.
     * Specifically:
     * - ~13 req/day for 500/month plans (saving ~110 requests as safety reserve)
     * - ~60 req/day for 2,000/month plans (saving ~200 requests as safety reserve)
     */
    fun calculateSafeDailyBudget(monthlyQuota: Int, customDailyLimit: Int? = null): Int {
        if (customDailyLimit != null && customDailyLimit > 0) return customDailyLimit
        return when {
            monthlyQuota <= 500 -> 13
            monthlyQuota <= 1000 -> 28
            monthlyQuota <= 2000 -> 60
            else -> ((monthlyQuota * 0.90) / 30).toInt().coerceAtLeast(10)
        }
    }

    /**
     * Checks if a request is permitted under the provider's plan and quota.
     */
    fun canExecuteRequest(
        provider: String,
        isPaidTier: Boolean,
        monthlyQuota: Int = 500,
        customDailyLimit: Int? = null
    ): Boolean {
        if (isPaidTier) return true // Paid tier has no artificial daily cap

        val safeDailyBudget = calculateSafeDailyBudget(monthlyQuota, customDailyLimit)
        val todayCount = getTodayCount(provider)
        val monthCount = getMonthCount(provider)

        return todayCount < safeDailyBudget && monthCount < monthlyQuota
    }

    /**
     * Records a consumption of 1 or more API tokens/requests.
     */
    fun recordRequest(provider: String) {
        val todayKey = "quota_day_${provider}_$todayString"
        val monthKey = "quota_month_${provider}_$currentMonthString"

        val currentDay = prefs.getInt(todayKey, 0) + 1
        val currentMonth = prefs.getInt(monthKey, 0) + 1

        prefs.edit()
            .putInt(todayKey, currentDay)
            .putInt(monthKey, currentMonth)
            .apply()
    }

    /**
     * Retrieves current quota metrics for UI reporting.
     */
    fun getQuotaStatus(
        provider: String,
        isPaidTier: Boolean,
        monthlyQuota: Int,
        customDailyLimit: Int? = null
    ): QuotaStatus {
        val tier = if (isPaidTier) TierMode.PAID_UNLIMITED else TierMode.FREE_PROTECTED
        val dailyBudget = if (isPaidTier) 9999 else calculateSafeDailyBudget(monthlyQuota, customDailyLimit)
        val todayCount = getTodayCount(provider)
        val monthCount = getMonthCount(provider)

        val dailyExhausted = !isPaidTier && todayCount >= dailyBudget
        val monthlyExhausted = !isPaidTier && monthCount >= monthlyQuota
        val remainingDaily = if (isPaidTier) 9999 else (dailyBudget - todayCount).coerceAtLeast(0)

        return QuotaStatus(
            provider = provider,
            tierMode = tier,
            requestsToday = todayCount,
            dailyBudget = dailyBudget,
            requestsThisMonth = monthCount,
            monthlyLimit = monthlyQuota,
            isDailyExhausted = dailyExhausted,
            isMonthlyExhausted = monthlyExhausted,
            remainingDailyTokens = remainingDaily
        )
    }

    /**
     * Fetches cached sweep offers if still valid within TTL (in minutes).
     */
    fun getCachedOffers(cacheKey: String, ttlMinutes: Long = 120): JsonArray? {
        val entry = memoryOfferCache[cacheKey] ?: return null
        val now = System.currentTimeMillis()
        val ageMinutes = (now - entry.first) / (1000 * 60)
        return if (ageMinutes <= ttlMinutes) {
            entry.second
        } else {
            memoryOfferCache.remove(cacheKey)
            null
        }
    }

    /**
     * Saves sweep offers into cache with current timestamp.
     */
    fun putCachedOffers(cacheKey: String, offers: JsonArray) {
        memoryOfferCache[cacheKey] = Pair(System.currentTimeMillis(), offers)
    }

    fun clearCache() {
        memoryOfferCache.clear()
    }

    fun resetStats(provider: String) {
        val todayKey = "quota_day_${provider}_$todayString"
        val monthKey = "quota_month_${provider}_$currentMonthString"
        prefs.edit().remove(todayKey).remove(monthKey).apply()
    }

    private fun getTodayCount(provider: String): Int {
        val todayKey = "quota_day_${provider}_$todayString"
        return prefs.getInt(todayKey, 0)
    }

    private fun getMonthCount(provider: String): Int {
        val monthKey = "quota_month_${provider}_$currentMonthString"
        return prefs.getInt(monthKey, 0)
    }

    companion object {
        private const val PREFS_NAME = "byok_quota_governor_prefs"
    }
}
