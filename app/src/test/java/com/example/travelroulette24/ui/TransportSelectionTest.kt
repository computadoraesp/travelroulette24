package com.example.travelroulette24.ui

import com.example.travelroulette24.budget.BudgetGuardian
import com.example.travelroulette24.data.TravelRepository
import com.example.travelroulette24.engine.TravelIntelligenceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransportSelectionTest {

    private val testDispatcher = StandardTestDispatcher()

    private class InMemoryBudgetStorage : BudgetGuardian.BudgetStorage {
        private var stored: String? = null
        override fun load(): String? = stored
        override fun save(json: String) {
            stored = json
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun resolveDynamicTransportIcon_returnsSpecificIconOrCloverForCombinations() {
        assertEquals("✈️", TravelViewModel.resolveDynamicTransportIcon(setOf("flight")))
        assertEquals("🚆", TravelViewModel.resolveDynamicTransportIcon(setOf("train")))
        assertEquals("⛴️", TravelViewModel.resolveDynamicTransportIcon(setOf("ferry")))
        assertEquals("⛴️", TravelViewModel.resolveDynamicTransportIcon(setOf("boat")))

        assertEquals("🍀", TravelViewModel.resolveDynamicTransportIcon(setOf("flight", "train")))
        assertEquals("🍀", TravelViewModel.resolveDynamicTransportIcon(setOf("flight", "ferry")))
        assertEquals("🍀", TravelViewModel.resolveDynamicTransportIcon(setOf("train", "ferry")))
        assertEquals("🍀", TravelViewModel.resolveDynamicTransportIcon(setOf("flight", "train", "ferry")))
        assertEquals("🍀", TravelViewModel.resolveDynamicTransportIcon(emptySet()))
    }

    @Test
    fun uiStateDisplayResults_filtersCombinableTransportModesWithoutFallback() {
        val sample = listOf(
            TravelIntelligenceEngine.ResultItem(
                mode = "flight",
                originHub = "MAD",
                destinationHub = "BCN",
                price = 25.0,
                departureTime = "2026-10-10T08:00:00Z",
                returnTime = null,
                checkInOnline = true,
                isOpportunity = false,
                isBudgetFriendly = true,
                deepLink = "https://example.com/1"
            ),
            TravelIntelligenceEngine.ResultItem(
                mode = "train",
                originHub = "Atocha",
                destinationHub = "Sants",
                price = 32.0,
                departureTime = "2026-10-10T09:00:00Z",
                returnTime = null,
                checkInOnline = true,
                isOpportunity = true,
                isBudgetFriendly = true,
                deepLink = "https://example.com/2"
            ),
            TravelIntelligenceEngine.ResultItem(
                mode = "boat",
                originHub = "BCN-Port",
                destinationHub = "PMI-Port",
                price = 40.0,
                departureTime = "2026-10-10T10:00:00Z",
                returnTime = null,
                checkInOnline = false,
                isOpportunity = false,
                isBudgetFriendly = true,
                deepLink = "https://example.com/3"
            )
        )

        val trainAndFerry = TravelViewModel.UiState(
            results = sample,
            selectedTransportModes = setOf("train", "ferry")
        )
        assertEquals("🍀", trainAndFerry.activeTransportIcon)
        assertEquals(listOf("train", "boat"), trainAndFerry.displayResults.map { it.mode })

        val onlyTrain = trainAndFerry.copy(selectedTransportModes = setOf("train"))
        assertEquals("🚆", onlyTrain.activeTransportIcon)
        assertEquals(listOf("Sants"), onlyTrain.displayResults.map { it.destinationHub })

        val onlyFlightWhenMissing = TravelViewModel.UiState(
            results = sample.filter { it.mode != "flight" },
            selectedTransportModes = setOf("flight")
        )
        assertEquals("✈️", onlyFlightWhenMissing.activeTransportIcon)
        assertTrue(onlyFlightWhenMissing.displayResults.isEmpty())
    }

    @Test
    fun viewModelToggleTransportMode_preventsTurningOffLastMode() = runTest {
        val repo = TravelRepository(isRealModeProvider = { false })
        val vm = TravelViewModel(
            repository = repo,
            budgetController = BudgetGuardian.BudgetController(InMemoryBudgetStorage())
        )
        advanceUntilIdle()

        assertEquals(setOf("flight", "train", "ferry"), vm.uiState.value.selectedTransportModes)
        assertEquals("🍀", vm.uiState.value.activeTransportIcon)

        vm.toggleTransportMode("flight")
        vm.toggleTransportMode("train")
        assertEquals(setOf("ferry"), vm.uiState.value.selectedTransportModes)
        assertEquals("⛴️", vm.uiState.value.activeTransportIcon)

        // Turning off the last active mode must keep it ON
        vm.toggleTransportMode("ferry")
        assertEquals(setOf("ferry"), vm.uiState.value.selectedTransportModes)
        assertEquals("⛴️", vm.uiState.value.activeTransportIcon)

        // Adding another switches back to clover
        vm.toggleTransportMode("flight")
        assertEquals(setOf("ferry", "flight"), vm.uiState.value.selectedTransportModes)
        assertEquals("🍀", vm.uiState.value.activeTransportIcon)
    }

    @Test
    fun repositoryDemoInput_respectsAllowedModesFilter() = runTest {
        val repo = TravelRepository(isRealModeProvider = { false })
        val json = Json { ignoreUnknownKeys = true }
        val inputJson = repo.buildEngineInputFromCoordinates(
            latitude = 48.8566,
            longitude = 2.3522,
            mode = TravelViewModel.MODE_ONE_WAY,
            passengers = 1,
            stayMinHours = 24,
            stayMaxHours = 72,
            departureWindowHours = 24,
            allowedModes = setOf("train", "ferry")
        )
        val parsed = json.decodeFromString(TravelIntelligenceEngine.EngineInput.serializer(), inputJson)
        assertTrue(parsed.realTimeOptions.isNotEmpty())
        assertTrue(parsed.realTimeOptions.all { it.mode == "train" || it.mode == "ferry" })
    }

    @Test
    fun canInstantSpin_returnsTrueOnlyWhenLoadedSignatureMatches() = runTest {
        val repo = TravelRepository(isRealModeProvider = { false })
        val budgetController = BudgetGuardian.BudgetController(InMemoryBudgetStorage())
        val vm = TravelViewModel(
            repository = repo,
            budgetController = budgetController
        )
        advanceUntilIdle()

        // Before initial load completes, cannot instant spin
        assertFalse(vm.canInstantSpin(48.8566, 2.3522, 24, 72))

        vm.loadTop20WithCoordinates(48.8566, 2.3522, stayMinHours = 24, stayMaxHours = 72)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.displayResults.isNotEmpty())
        assertTrue(vm.canInstantSpin(48.8566, 2.3522, 24, 72))

        // Different coordinates invalidate instant spin
        assertFalse(vm.canInstantSpin(40.4168, -3.7038, 24, 72))

        // Different stay window invalidates instant spin
        assertFalse(vm.canInstantSpin(48.8566, 2.3522, 48, 72))

        // Toggling a transport mode invalidates instant spin until reloaded
        vm.toggleTransportMode("ferry")
        assertFalse(vm.canInstantSpin(48.8566, 2.3522, 24, 72))
    }

    @Test
    fun setDepartureWindowHours_normalizesAndInvalidatesInstantSpinUntilReloaded() = runTest {
        val repo = TravelRepository(isRealModeProvider = { false })
        val vm = TravelViewModel(
            repository = repo,
            budgetController = BudgetGuardian.BudgetController(InMemoryBudgetStorage())
        )
        advanceUntilIdle()

        vm.loadTop20WithCoordinates(48.8566, 2.3522, stayMinHours = 24, stayMaxHours = 72, departureWindowHours = 24)
        advanceUntilIdle()
        assertEquals(24, vm.uiState.value.departureWindowHours)
        assertTrue(vm.canInstantSpin(48.8566, 2.3522, 24, 72))

        vm.setDepartureWindowHours(72)
        assertEquals(72, vm.uiState.value.departureWindowHours)
        assertFalse(vm.canInstantSpin(48.8566, 2.3522, 24, 72))

        // Invalid values normalize back to 24h
        vm.setDepartureWindowHours(99)
        assertEquals(24, vm.uiState.value.departureWindowHours)
    }
}
