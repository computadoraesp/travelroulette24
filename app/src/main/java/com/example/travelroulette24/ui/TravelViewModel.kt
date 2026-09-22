package com.example.travelroulette24.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelroulette24.budget.BudgetGuardian
import com.example.travelroulette24.data.TravelRepository
import com.example.travelroulette24.engine.TravelIntelligenceEngine
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
 * State-driven pipeline: load Top 20 -> apply budget filter -> expose
 * results + alerts via StateFlow. Supports travel chaining: after arriving,
 * call [loadTop20WithCoordinates] again with the destination city as origin.
 */
@Suppress("unused")
class TravelViewModel(
    private val repository: TravelRepository,
    private val budgetController: BudgetGuardian.BudgetController
) : ViewModel() {

    companion object {
        private val jsonFormat = Json { ignoreUnknownKeys = true }
        const val MODE_ROUND_TRIP = "round_trip"
        const val MODE_ONE_WAY = "one_way"
    }

    data class UiState(
        val isLoading: Boolean = false,
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
        val chainHistory: List<String> = emptyList()
    )

    private val _uiState = MutableStateFlow(UiState(budget = budgetController.getBudget()))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun setMode(newMode: String) {
        _uiState.update { it.copy(mode = newMode) }
    }

    fun setPassengers(count: Int) {
        _uiState.update { it.copy(passengers = count.coerceIn(1, 10)) }
    }

    fun setStayDuration(minHours: Int, maxHours: Int) {
        _uiState.update { it.copy(stayMinHours = minHours, stayMaxHours = maxHours) }
    }

    /**
     * Executes travel chaining: records current city to chain history and
     * immediately spins the roulette from the destination city.
     */
    fun chainTravel(destinationCity: String, destinationLat: Double, destinationLon: Double) {
        val currentOrigin = _uiState.value.originCity ?: "Origin"
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
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    mode = mode,
                    passengers = passengers,
                    stayMinHours = stayMinHours,
                    stayMaxHours = stayMaxHours,
                    departureWindowHours = departureWindowHours
                )
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
                
                val results = output.results
                val alerts = results.mapNotNull { repository.budgetAlertFor(it, budget) }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        originCity = output.originCity,
                        results = results,
                        alerts = alerts,
                        budget = budget
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Unknown error") }
            }
        }
    }

    /** Updates and persists the budget; re-filters current results immediately. */
    fun setBudget(budget: BudgetGuardian.Budget?) {
        if (budget == null) budgetController.clearBudget() else budgetController.setBudget(budget)
        _uiState.update { state ->
            state.copy(
                budget = budget,
                results = BudgetGuardian.filterWithinBudget(
                    budget,
                    state.results,
                    isRoundTrip = { it.returnTime != null },
                    price = { it.price }
                )
            )
        }
    }
}