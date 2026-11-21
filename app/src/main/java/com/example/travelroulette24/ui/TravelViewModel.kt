package com.example.travelroulette24.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelroulette24.PreferenceHelper
import com.example.travelroulette24.budget.BudgetGuardian
import com.example.travelroulette24.data.DemoTravelDataSource
import com.example.travelroulette24.data.TravelRepository
import com.example.travelroulette24.data.byok.ByokTravelService
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import com.example.travelroulette24.utils.CurrencyHelper
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.Instant

/**
 * MVVM ViewModel exposing app state to the Compose UI.
 *
 * Strictly distinguishes Demo simulation state from Real live state, manages
 * currency conversion, live weather, OpenTripMap attractions, and Gemini 24h itineraries.
 */
@Suppress("unused")
class TravelViewModel(
    private val repository: TravelRepository,
    private val budgetController: BudgetGuardian.BudgetController,
    private val byokService: ByokTravelService? = null
) : ViewModel() {

    companion object {
        private val jsonFormat = Json { ignoreUnknownKeys = true }
        const val MODE_ROUND_TRIP = "round_trip"
        const val MODE_ONE_WAY = "one_way"
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
        val sortOption: SortOption = SortOption.CHEAPEST,
        val currency: String = CurrencyHelper.EUR,
        val currencyRate: Double = 1.0,
        val selectedTransportFilter: String = "all",
        val winningLiveWeather: DestinationWeather? = null,
        val winningAttractions: List<String> = emptyList(),
        val winningItinerary: String? = null,
        val isLoadingItinerary: Boolean = false,
        val winningPhoto: Bitmap? = null,
        val winningFlightRadar: ByokTravelService.LiveFlightRadarStatus? = null
    ) {
        val displayResults: List<TravelIntelligenceEngine.ResultItem>
            get() = when (selectedTransportFilter.lowercase()) {
                "flight" -> results.filter { it.mode.equals("flight", ignoreCase = true) }
                "train" -> results.filter { it.mode.equals("train", ignoreCase = true) }
                "ferry" -> results.filter { it.mode.equals("ferry", ignoreCase = true) || it.mode.equals("boat", ignoreCase = true) }
                else -> results
            }.ifEmpty { results }

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

    private val initialCurrency = byokService?.credentials?.currency ?: CurrencyHelper.EUR
    private val initialSortOption = runCatching {
        SortOption.valueOf(PreferenceHelper.getSortOptionName(SortOption.BEST_VALUE.name))
    }.getOrDefault(SortOption.BEST_VALUE)
    private val _uiState = MutableStateFlow(
        UiState(
            budget = budgetController.getBudget(),
            isRealMode = byokService?.credentials?.isRealMode ?: repository.isCurrentlyRealMode(),
            sortOption = initialSortOption,
            currency = initialCurrency,
            currencyRate = CurrencyHelper.fallbackRateFromEur(initialCurrency)
        )
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun setTransportFilter(filter: String) {
        _uiState.update { it.copy(selectedTransportFilter = filter) }
    }

    fun setMode(newMode: String) {
        _uiState.update { it.copy(mode = newMode) }
    }

    fun setPassengers(count: Int) {
        _uiState.update { it.copy(passengers = count.coerceIn(1, 10)) }
    }

    fun setStayDuration(minHours: Int, maxHours: Int) {
        _uiState.update { it.copy(stayMinHours = minHours, stayMaxHours = maxHours) }
    }

    fun setSortOption(option: SortOption) {
        PreferenceHelper.setSortOptionName(option.name)
        _uiState.update { state ->
            val sorted = sortItems(state.results, option)
            state.copy(sortOption = option, results = sorted)
        }
    }

    fun setCurrency(currency: String) {
        _uiState.update {
            it.copy(
                currency = currency,
                currencyRate = CurrencyHelper.fallbackRateFromEur(currency)
            )
        }
        refreshCurrencyRate(currency)
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

    /**
     * Executes travel chaining: records current city to chain history and
     * immediately spins the roulette from the destination city.
     */
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
            departureWindowHours = _uiState.value.departureWindowHours
        )
    }

    /**
     * Spinnable, free roulette loader triggering coordinate lookup to satisfy no-typing spec rules.
     */
    fun loadTop20WithCoordinates(
        latitude: Double,
        longitude: Double,
        mode: String = _uiState.value.mode,
        passengers: Int = _uiState.value.passengers,
        stayMinHours: Int = _uiState.value.stayMinHours,
        stayMaxHours: Int = _uiState.value.stayMaxHours,
        departureWindowHours: Int = _uiState.value.departureWindowHours
    ) {
        viewModelScope.launch {
            val activeCurrency = byokService?.credentials?.currency ?: _uiState.value.currency
            val activeReal = byokService?.credentials?.isRealMode ?: repository.isCurrentlyRealMode()

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
                    winningItinerary = null,
                    winningAttractions = emptyList(),
                    winningPhoto = null,
                    winningFlightRadar = null
                )
            }
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
                    budget = budget
                )
                val outputJson = repository.generateTop20(input, Instant.now())
                val output = jsonFormat.decodeFromString(TravelIntelligenceEngine.EngineOutput.serializer(), outputJson)

                val rawResults = output.results
                val sortedResults = sortItems(rawResults, _uiState.value.sortOption)
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
                        budget = budget
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "") }
            }
        }
    }

    /**
     * Loads destination weather (live Open-Meteo in Real Mode, simulated in Demo Mode),
     * OpenTripMap attractions, Unsplash destination hero photo, and AviationStack flight radar.
     */
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

    /**
     * Generates a Gemini 24h express travel itinerary for the winning destination in Real Mode.
     */
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

    /** Updates and persists the budget; re-filters and sorts current results immediately. */
    fun setBudget(budget: BudgetGuardian.Budget?) {
        if (budget == null) budgetController.clearBudget() else budgetController.setBudget(budget)
        _uiState.update { state ->
            val filtered = BudgetGuardian.filterWithinBudget(
                budget,
                state.results,
                isRoundTrip = { it.returnTime != null },
                price = { it.price }
            )
            val sorted = sortItems(filtered, state.sortOption)
            state.copy(
                budget = budget,
                results = sorted
            )
        }
    }
}