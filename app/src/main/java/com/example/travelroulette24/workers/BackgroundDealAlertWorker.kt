package com.example.travelroulette24.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.travelroulette24.PreferenceHelper
import com.example.travelroulette24.budget.BudgetGuardian
import com.example.travelroulette24.data.FavoritePriceHistoryDb
import com.example.travelroulette24.data.PriceAlertManager
import com.example.travelroulette24.data.TravelRepository
import com.example.travelroulette24.data.byok.ApiCredentialsManager
import com.example.travelroulette24.data.byok.ApiQuotaGovernor
import com.example.travelroulette24.data.byok.ByokTravelService
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import com.example.travelroulette24.location.LocalLocationHubResolver
import com.example.travelroulette24.utils.CurrencyHelper
import com.example.travelroulette24.utils.DealNotificationDispatcher
import kotlinx.serialization.json.Json
import java.time.Instant

/**
 * Periodic background worker checking latent ❤️ Wishlist offers and 🔔 Deal Radar rules
 * when the app is closed, recording observed fares in SQLite and triggering vibration alerts.
 */
class BackgroundDealAlertWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private class WorkerBudgetStorage(context: Context) : BudgetGuardian.BudgetStorage {
        private val prefs = context.getSharedPreferences("travel_budget_prefs", Context.MODE_PRIVATE)
        override fun load(): String? = prefs.getString("budget_json", null)
        override fun save(json: String) {
            prefs.edit().putString("budget_json", json).apply()
        }
    }

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        PreferenceHelper.init(ctx)

        val favorites = PreferenceHelper.getFavoriteDestinations()
        val priceAlertManager = PriceAlertManager.getInstance(ctx)
        val alertConfig = priceAlertManager.configFlow.value
        if (favorites.isEmpty() && !alertConfig.isEnabled) {
            return Result.success()
        }

        val credentialsManager = ApiCredentialsManager(ctx)
        val quotaGovernor = ApiQuotaGovernor(ctx)
        val byokService = ByokTravelService(credentialsManager, quotaGovernor)
        val priceHistoryDb = FavoritePriceHistoryDb(ctx)
        val locationResolver = LocalLocationHubResolver(ctx)
        val budgetController = BudgetGuardian.BudgetController(WorkerBudgetStorage(ctx))

        if (credentialsManager.isRealMode && !hasLiveSourceConfigured(credentialsManager)) {
            return Result.success()
        }

        return try {
            val (_, lat, lon) = PreferenceHelper.getLastKnownOrigin()
                ?: locationResolver.resolveSystemDefaultOrigin()

            val repository = TravelRepository(
                api = null,
                apiKey = "",
                localResolver = locationResolver,
                byokService = byokService,
                priceHistoryDb = priceHistoryDb
            )

            val budget = budgetController.getBudget()
            val inputJson = repository.buildEngineInputFromCoordinates(
                latitude = lat,
                longitude = lon,
                mode = PreferenceHelper.getTravelMode(),
                passengers = PreferenceHelper.getPassengers(),
                stayMinHours = 24,
                stayMaxHours = 72,
                departureWindowHours = PreferenceHelper.getDepartureWindowHours(),
                budget = budget,
                allowedModes = PreferenceHelper.getSelectedTransportModes()
            )

            val outputJson = repository.generateTop20(inputJson, Instant.now())
            val output = WORKER_JSON
                .decodeFromString(TravelIntelligenceEngine.EngineOutput.serializer(), outputJson)

            val results = output.results
            val updatedHistory = if (results.isNotEmpty()) {
                priceHistoryDb.recordObservedOffers(results.map { it.destinationHub to it.price })
            } else {
                priceHistoryDb.getAllHistory()
            }
            val historicalLowByHub = updatedHistory.mapValues { it.value.lowestPrice }

            val wishlistDeals = DealNotificationDispatcher.findLatentWishlistDeals(
                results = results,
                favoriteDestinations = favorites,
                budget = budget,
                historicalLowByHub = historicalLowByHub,
                tolerancePercent = PreferenceHelper.getFavoriteTolerancePercent()
            )
            val radarDeals = if (alertConfig.isEnabled) {
                priceAlertManager.getMatchingDeals(
                    results = results,
                    config = alertConfig,
                    favoriteDestinations = favorites,
                    historicalLowByHub = historicalLowByHub
                )
            } else {
                emptyList()
            }

            if (wishlistDeals.isNotEmpty() || radarDeals.isNotEmpty()) {
                val currency = credentialsManager.currency
                val rate = if (credentialsManager.isRealMode && currency != CurrencyHelper.EUR) {
                    byokService.fetchCurrencyRate(currency)
                } else {
                    CurrencyHelper.fallbackRateFromEur(currency)
                }
                DealNotificationDispatcher.notifyIfNeeded(
                    context = ctx,
                    wishlistDeals = wishlistDeals,
                    radarDeals = radarDeals,
                    currencyCode = currency,
                    currencyRate = rate
                )
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun hasLiveSourceConfigured(creds: ApiCredentialsManager): Boolean {
        return (creds.amadeusApiKey.isNotBlank() && creds.amadeusApiSecret.isNotBlank()) ||
            creds.kiwiApiKey.isNotBlank() ||
            creds.travelPayoutsToken.isNotBlank() ||
            creds.rapidApiKey.isNotBlank() ||
            creds.openHafasEnabled ||
            creds.navitiaToken.isNotBlank() ||
            (creds.customFlightApiEnabled && creds.customFlightApiUrl.isNotBlank()) ||
            (creds.customTrainApiEnabled && creds.customTrainApiUrl.isNotBlank()) ||
            (creds.customBoatApiEnabled && creds.customBoatApiUrl.isNotBlank())
    }

    companion object {
        private val WORKER_JSON = Json { ignoreUnknownKeys = true }
    }
}
