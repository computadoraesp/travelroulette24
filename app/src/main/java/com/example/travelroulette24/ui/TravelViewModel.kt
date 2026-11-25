package com.example.travelroulette24.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelroulette24.PreferenceHelper
import com.example.travelroulette24.budget.BudgetGuardian
import com.example.travelroulette24.data.DemoTravelDataSource
import com.example.travelroulette24.data.FavoritePriceHistoryDb
import com.example.travelroulette24.data.TravelRepository
import com.example.travelroulette24.data.byok.ByokTravelService
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import com.example.travelroulette24.utils.CurrencyHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.Instant

/**
 * MVVM ViewModel exposing app state to the Compose UI.
 *
 * Strictly distinguishes Demo simulation state from Real live state, supports
 * ambiguous local terminal toggling, world currency rates, optional historical-backed
 * minimum-distance veto, and fresh quota-governed roulette queries.
 */
class TravelViewModel(
    private val repository: TravelRepository,
    private val budgetController: BudgetGuardian.BudgetController,
    private val byokService: ByokTravelService? = null,
    private val priceHistoryDb: FavoritePriceHistoryDb? = null
) : ViewModel() {

    companion object {
        private val jsonFormat = Json { ignoreUnknownKeys = true }
        const val MODE_ROUND_TRIP = "round_trip"
        const val MODE_ONE_WAY = "one_way"
        const val TRANSPORT_FLIGHT = "flight"
        const val TRANSPORT_TRAIN = "train"
        const val TRANSPORT_FERRY = "ferry"
        val ALL_TRANSPORT_MODES: Set<String> = setOf(TRANSPORT_FLIGHT, TRANSPORT_TRAIN, TRANSPORT_FERRY)

        fun normalizeTransportModes(modes: Set<String>): Set<String> =
            modes.map { it.trim().lowercase() }
                .map { if (it == "boat") TRANSPORT_FERRY else it }
                .filter { it in ALL_TRANSPORT_MODES }
                .toSet()
                .ifEmpty { ALL_TRANSPORT_MODES }

        fun resolveDynamicTransportIcon(modes: Set<String>): String {
            return when (normalizeTransportModes(modes)) {
                setOf(TRANSPORT_FLIGHT) -> "✈️"
                setOf(TRANSPORT_TRAIN) -> "🚆"
                setOf(TRANSPORT_FERRY) -> "⛴️"
                else -> "🍀"
            }
        }
    }

    enum class SortOption(val icon: String) {
        BEST_VALUE("💎"),
        CHEAPEST("💰"),
        FURTHEST_DISTANCE("🚀"),
        EXTREME_ROULETTE("🎲"),
        EARLIEST_DEPARTURE("⏱️")
    }

    data class UiState(
        val isLoading: Boolean = false,
        val isRealMode: Boolean = false,
        val dataOrigin: TravelRepository.DataSourceOrigin = TravelRepository.DataSourceOrigin.DEMO_SIMULATION,
        val originCity: String? = null,
        val results: List<TravelIntelligenceEngine.ResultItem> = emptyList(),
        val alerts: List<String> = emptyList(),
        val error: String? = null,
        val budget: BudgetGuardian.Budget? = null,
        val mode: String = MODE_ONE_WAY,
        val passengers: Int = 1,
        val stayMinHours: Int = 24,
        val stayMaxHours: Int = 72,
        val departureWindowHours: Int = 24,
        val chainHistory: List<String> = emptyList(),
        val sortOption: SortOption = SortOption.BEST_VALUE,
        val currency: String = CurrencyHelper.EUR,
        val currencyRate: Double = 1.0,
        val selectedTransportModes: Set<String> = ALL_TRANSPORT_MODES,
        val excludedOriginHubs: Set<String> = emptySet(),
        val minDistanceVetoEnabled: Boolean = false,
        val minDistanceKm: Int = 300,
        val hasHistoricalCoverageForMinKm: Boolean = true,
        val winningLiveWeather: DestinationWeather? = null,
        val winningAttractions: List<String> = emptyList(),
        val winningItinerary: String? = null,
        val isLoadingItinerary: Boolean = false,
        val winningPhoto: Bitmap? = null,
        val winningFlightRadar: ByokTravelService.LiveFlightRadarStatus? = null,
        val historicalLowByHub: Map<String, Double> = emptyMap()
    ) {
        val activeTransportIcon: String
            get() = resolveDynamicTransportIcon(selectedTransportModes)

        val displayResults: List<TravelIntelligenceEngine.ResultItem>
            get() {
                val allowed = normalizeTransportModes(selectedTransportModes)
                return results.filter { item ->
                    val mode = if (item.mode.equals("boat", ignoreCase = true)) TRANSPORT_FERRY else item.mode.lowercase()
                    mode in allowed
                }
            }

        /** Highest Travel Value Index (km / €) option in the current filtered set, preferring verified prices. */
        val bestValueItem: TravelIntelligenceEngine.ResultItem?
            get() {
                val pool = displayResults
                val verified = pool.filter { !it.isEstimatedPrice }.ifEmpty { pool }
                return verified.maxByOrNull { item ->
                    val dist = HubCoordinates.calculateDistanceKm(item.originHub, item.destinationHub)
                    TravelIntelligenceEngine.computeTravelValueIndex(dist, item.price)
                }
            }
    }

    private var loadJob: Job? = null
    private var rawUnfilteredResults: List<TravelIntelligenceEngine.ResultItem> = emptyList()
    private var lastOriginLat: Double? = null
    private var lastOriginLon: Double? = null

    private val initialCurrency = byokService?.credentials?.currency ?: CurrencyHelper.EUR
    private val initialSortOption = runCatching {
        SortOption.valueOf(PreferenceHelper.getSortOptionName(SortOption.BEST_VALUE.name))
    }.getOrDefault(SortOption.BEST_VALUE)

    private val _uiState = MutableStateFlow(
        UiState(
            budget = budgetController.getBudget(),
            mode = PreferenceHelper.getTravelMode(),
            passengers = PreferenceHelper.getPassengers(),
            departureWindowHours = PreferenceHelper.getDepartureWindowHours(),
            isRealMode = byokService?.credentials?.isRealMode ?: repository.isCurrentlyRealMode(),
            sortOption = initialSortOption,
            currency = initialCurrency,
            currencyRate = CurrencyHelper.fallbackRateFromEur(initialCurrency),
            selectedTransportModes = normalizeTransportModes(PreferenceHelper.getSelectedTransportModes()),
            excludedOriginHubs = PreferenceHelper.getExcludedOriginHubs(),
            minDistanceVetoEnabled = PreferenceHelper.isMinDistanceVetoEnabled(),
            minDistanceKm = PreferenceHelper.getMinDistanceKm()
        )
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun toggleTransportMode(mode: String): Set<String> {
        val normalizedTarget = if (mode.equals("boat", ignoreCase = true)) TRANSPORT_FERRY else mode.lowercase()
        if (normalizedTarget !in ALL_TRANSPORT_MODES) return _uiState.value.selectedTransportModes
        val current = normalizeTransportModes(_uiState.value.selectedTransportModes)
        val next = if (normalizedTarget in current) {
            if (current.size <= 1) return current
            current - normalizedTarget
        } else {
            current + normalizedTarget
        }
        PreferenceHelper.setSelectedTransportModes(next)
        _uiState.update { it.copy(selectedTransportModes = next) }
        return next
    }

    fun setTransportModes(modes: Set<String>) {
        val normalized = normalizeTransportModes(modes)
        PreferenceHelper.setSelectedTransportModes(normalized)
        _uiState.update { it.copy(selectedTransportModes = normalized) }
    }

    /**
     * Toggles a single origin terminal ONLY when ambiguity exists ([categoryHubs.size] >= 2),
     * ensuring at least one terminal remains active in that category.
     */
    fun toggleAmbiguousOriginHub(hubCode: String, categoryHubs: List<String>): Boolean {
        if (categoryHubs.size < 2) return false
        val currentExcluded = _uiState.value.excludedOriginHubs
        val isCurrentlyExcluded = currentExcluded.any { it.equals(hubCode, ignoreCase = true) }

        val nextExcluded = if (isCurrentlyExcluded) {
            currentExcluded.filterNot { it.equals(hubCode, ignoreCase = true) }.toSet()
        } else {
            val activeCount = categoryHubs.count { code ->
                currentExcluded.none { it.equals(code, ignoreCase = true) }
            }
            if (activeCount <= 1) return false
            currentExcluded + hubCode
        }

        PreferenceHelper.setExcludedOriginHubs(nextExcluded)
        _uiState.update { it.copy(excludedOriginHubs = nextExcluded) }
        return true
    }

    fun setMinDistanceVeto(enabled: Boolean, km: Int = _uiState.value.minDistanceKm) {
        val safeKm = km.coerceIn(100, 5000)
        PreferenceHelper.setMinDistanceVetoEnabled(enabled)
        PreferenceHelper.setMinDistanceKm(safeKm)
        _uiState.update { state ->
            val budgetFiltered = BudgetGuardian.filterWithinBudget(
                state.budget,
                rawUnfilteredResults.ifEmpty { state.results },
                isRoundTrip = { it.returnTime != null },
                price = { it.totalPrice }
            )
            val eval = DistanceVetoHelper.evaluate(
                items = budgetFiltered,
                originLat = lastOriginLat,
                originLon = lastOriginLon,
                historicalLowByHub = state.historicalLowByHub,
                enabled = enabled,
                minDistanceKm = safeKm
            )
            val sorted = sortItems(eval.filteredResults, state.sortOption)
            state.copy(
                minDistanceVetoEnabled = enabled,
                minDistanceKm = safeKm,
                hasHistoricalCoverageForMinKm = eval.hasHistoricalCoverage,
                results = sorted
            )
        }
    }

    fun setMode(newMode: String) {
        PreferenceHelper.setTravelMode(newMode)
        _uiState.update { it.copy(mode = newMode) }
    }

    fun setPassengers(count: Int) {
        val safeCount = count.coerceIn(1, 10)
        PreferenceHelper.setPassengers(safeCount)
        _uiState.update { it.copy(passengers = safeCount) }
    }

    fun setStayDuration(minHours: Int, maxHours: Int) {
        _uiState.update { it.copy(stayMinHours = minHours, stayMaxHours = maxHours) }
    }

    fun setDepartureWindowHours(hours: Int) {
        val normalized = if (hours in setOf(24, 48, 72)) hours else 24
        PreferenceHelper.setDepartureWindowHours(normalized)
        _uiState.update { it.copy(departureWindowHours = normalized) }
    }

    fun setSortOption(option: SortOption) {
        PreferenceHelper.setSortOptionName(option.name)
        _uiState.update { state ->
            val sorted = sortItems(state.results, option)
            state.copy(sortOption = option, results = sorted)
        }
    }

    fun syncPreferencesFromCredentials() {
        val creds = byokService?.credentials ?: return
        val activeCurrency = creds.currency
        val activeReal = creds.isRealMode
        _uiState.update {
            it.copy(
                isRealMode = activeReal,
                currency = activeCurrency,
                currencyRate = CurrencyHelper.fallbackRateFromEur(activeCurrency)
            )
        }
        if (activeReal && activeCurrency != CurrencyHelper.EUR) {
            refreshCurrencyRate(activeCurrency)
        }
    }

    private fun refreshCurrencyRate(targetCurrency: String) {
        val service = byokService ?: return
        if (!service.credentials.isRealMode || targetCurrency == CurrencyHelper.EUR) return
        viewModelScope.launch {
            val rate = service.fetchCurrencyRate(targetCurrency)
            _uiState.update { it.copy(currencyRate = rate) }
        }
    }

    private fun sortItems(
        items: List<TravelIntelligenceEngine.ResultItem>,
        option: SortOption
    ): List<TravelIntelligenceEngine.ResultItem> {
        return when (option) {
            SortOption.BEST_VALUE -> items.sortedWith(
                compareBy(
                    { it.isEstimatedPrice },
                    {
                        val dist = HubCoordinates.calculateDistanceKm(it.originHub, it.destinationHub)
                        -TravelIntelligenceEngine.computeTravelValueIndex(dist, it.price)
                    },
                    { it.price }
                )
            )
            SortOption.CHEAPEST -> items.sortedWith(
                compareBy({ it.price }, { -HubCoordinates.calculateDistanceKm(it.originHub, it.destinationHub) })
            )
            SortOption.FURTHEST_DISTANCE -> items.sortedWith(
                compareBy({ -HubCoordinates.calculateDistanceKm(it.originHub, it.destinationHub) }, { it.price })
            )
            SortOption.EXTREME_ROULETTE -> items.sortedWith(
                compareBy(
                    {
                        val dist = HubCoordinates.calculateDistanceKm(it.originHub, it.destinationHub)
                        -TravelIntelligenceEngine.computeExtremeRouletteScore(
                            distanceKm = dist,
                            price = it.price,
                            isOpportunity = it.isOpportunity,
                            isEstimatedPrice = it.isEstimatedPrice
                        )
                    },
                    { it.price }
                )
            )
            SortOption.EARLIEST_DEPARTURE -> items.sortedWith(
                compareBy({ it.departureTime }, { it.price })
            )
        }
    }

    fun chainTravel(
        destinationCity: String,
        destinationLat: Double,
        destinationLon: Double,
        fallbackOrigin: String = "Origin"
    ) {
        val currentOrigin = _uiState.value.originCity ?: fallbackOrigin
        val updatedHistory = _uiState.value.chainHistory + currentOrigin
        _uiState.update { it.copy(chainHistory = updatedHistory) }
        loadTop20WithCoordinates(
            latitude = destinationLat,
            longitude = destinationLon,
            mode = MODE_ONE_WAY,
            passengers = _uiState.value.passengers,
            stayMinHours = _uiState.value.stayMinHours,
            stayMaxHours = _uiState.value.stayMaxHours,
            departureWindowHours = _uiState.value.departureWindowHours,
            forceFresh = true
        )
    }

    /**
     * Always clears stale winner data immediately and queries fresh offers protected by [ApiQuotaGovernor].
     */
    fun loadTop20WithCoordinates(
        latitude: Double,
        longitude: Double,
        mode: String = _uiState.value.mode,
        passengers: Int = _uiState.value.passengers,
        stayMinHours: Int = _uiState.value.stayMinHours,
        stayMaxHours: Int = _uiState.value.stayMaxHours,
        departureWindowHours: Int = _uiState.value.departureWindowHours,
        forceFresh: Boolean = true
    ) {
        loadJob?.cancel()
        lastOriginLat = latitude
        lastOriginLon = longitude
        val activeCurrency = byokService?.credentials?.currency ?: _uiState.value.currency
        val activeReal = byokService?.credentials?.isRealMode ?: repository.isCurrentlyRealMode()
        val allowedModes = normalizeTransportModes(_uiState.value.selectedTransportModes)
        val excludedHubs = _uiState.value.excludedOriginHubs

        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                isRealMode = activeReal,
                currency = activeCurrency,
                mode = mode,
                passengers = passengers,
                stayMinHours = stayMinHours,
                stayMaxHours = stayMaxHours,
                departureWindowHours = departureWindowHours,
                results = emptyList(),
                alerts = emptyList(),
                winningLiveWeather = null,
                winningAttractions = emptyList(),
                winningItinerary = null,
                isLoadingItinerary = false,
                winningPhoto = null,
                winningFlightRadar = null
            )
        }
        loadJob = viewModelScope.launch {
            if (activeReal && activeCurrency != CurrencyHelper.EUR) {
                refreshCurrencyRate(activeCurrency)
            }
            try {
                val budget = budgetController.getBudget()
                val input = repository.buildEngineInputFromCoordinates(
                    latitude = latitude,
                    longitude = longitude,
                    mode = mode,
                    passengers = passengers,
                    stayMinHours = stayMinHours,
                    stayMaxHours = stayMaxHours,
                    departureWindowHours = departureWindowHours,
                    budget = budget,
                    allowedModes = allowedModes,
                    excludedOriginHubs = excludedHubs,
                    forceFresh = forceFresh
                )
                val outputJson = repository.generateTop20(input, Instant.now())
                val output = jsonFormat.decodeFromString(TravelIntelligenceEngine.EngineOutput.serializer(), outputJson)

                val rawResults = output.results
                rawUnfilteredResults = rawResults

                val (priorHistory, updatedHistory) = if (priceHistoryDb != null) {
                    withContext(Dispatchers.IO) {
                        val before = priceHistoryDb.getAllHistory().mapValues { it.value.lowestPrice }
                        val after = if (rawResults.isNotEmpty()) {
                            priceHistoryDb.recordObservedOffers(rawResults.map { it.destinationHub to it.price })
                                .mapValues { it.value.lowestPrice }
                        } else {
                            before
                        }
                        before to after
                    }
                } else {
                    _uiState.value.historicalLowByHub to _uiState.value.historicalLowByHub
                }

                val vetoEval = DistanceVetoHelper.evaluate(
                    items = rawResults,
                    originLat = latitude,
                    originLon = longitude,
                    historicalLowByHub = priorHistory,
                    enabled = _uiState.value.minDistanceVetoEnabled,
                    minDistanceKm = _uiState.value.minDistanceKm
                )
                val sortedResults = sortItems(vetoEval.filteredResults, _uiState.value.sortOption)
                val currencySymbol = CurrencyHelper.symbolFor(activeCurrency)
                val alerts = sortedResults.mapNotNull { repository.budgetAlertFor(it, budget, currencySymbol) }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRealMode = activeReal,
                        dataOrigin = repository.lastOrigin,
                        originCity = output.originCity,
                        results = sortedResults,
                        alerts = alerts,
                        budget = budget,
                        hasHistoricalCoverageForMinKm = vetoEval.hasHistoricalCoverage,
                        historicalLowByHub = updatedHistory
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "") }
            }
        }
    }

    fun loadWinnerIntelligence(item: TravelIntelligenceEngine.ResultItem?) {
        if (item == null) return
        val realMode = _uiState.value.isRealMode
        if (!realMode) {
            _uiState.update {
                it.copy(
                    winningLiveWeather = DemoTravelDataSource.getSimulatedWeather(item.destinationHub),
                    winningAttractions = emptyList(),
                    winningItinerary = null,
                    winningPhoto = null,
                    winningFlightRadar = null
                )
            }
            return
        }

        val service = byokService ?: return
        val (lat, lon) = HubCoordinates.getCoordinates(item.destinationHub)
        val cityName = HubCoordinates.getCity(item.destinationHub)

        viewModelScope.launch {
            val weatherJob = async { service.fetchLiveWeather(lat, lon) }
            val attractionsJob = async {
                if (service.credentials.hasOpenTripMap()) service.fetchTopAttractions(cityName)
                else emptyList()
            }
            val photoJob = async {
                if (service.credentials.hasUnsplash()) service.fetchDestinationPhoto(cityName)
                else null
            }
            val radarJob = async {
                if (item.mode.equals("flight", ignoreCase = true) && service.credentials.hasAviationStack()) {
                    service.fetchLiveFlightStatus(item.originHub, item.destinationHub)
                } else null
            }

            _uiState.update {
                it.copy(
                    winningLiveWeather = weatherJob.await(),
                    winningAttractions = attractionsJob.await(),
                    winningPhoto = photoJob.await(),
                    winningFlightRadar = radarJob.await()
                )
            }
        }
    }

    fun generateAiItinerary(item: TravelIntelligenceEngine.ResultItem) {
        val service = byokService ?: return
        if (!service.credentials.isRealMode || !service.credentials.hasGemini()) return

        val cityName = HubCoordinates.getCity(item.destinationHub)
        val formattedPrice = CurrencyHelper.formatPrice(
            baseEurAmount = item.price,
            currencyCode = _uiState.value.currency,
            rateFromEur = _uiState.value.currencyRate
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingItinerary = true) }
            val itinerary = service.fetchGeminiExpressItinerary(
                cityName = cityName,
                mode = item.mode,
                stayHours = _uiState.value.stayMinHours,
                budgetFormatted = formattedPrice
            )
            _uiState.update {
                it.copy(
                    isLoadingItinerary = false,
                    winningItinerary = itinerary
                )
            }
        }
    }

    fun setBudget(budget: BudgetGuardian.Budget?) {
        if (budget == null) budgetController.clearBudget() else budgetController.setBudget(budget)
        _uiState.update { state ->
            val pool = rawUnfilteredResults.ifEmpty { state.results }
            val budgetFiltered = BudgetGuardian.filterWithinBudget(
                budget,
                pool,
                isRoundTrip = { it.returnTime != null },
                price = { it.totalPrice }
            )
            val eval = DistanceVetoHelper.evaluate(
                items = budgetFiltered,
                originLat = lastOriginLat,
                originLon = lastOriginLon,
                historicalLowByHub = state.historicalLowByHub,
                enabled = state.minDistanceVetoEnabled,
                minDistanceKm = state.minDistanceKm
            )
            val sorted = sortItems(eval.filteredResults, state.sortOption)
            state.copy(
                budget = budget,
                hasHistoricalCoverageForMinKm = eval.hasHistoricalCoverage,
                results = sorted
            )
        }
    }
}