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
    fun resolveEffectiveOriginHubs_onlyFiltersAmbiguousCategoriesAndKeepsMinimumOne() {
        val rawHubs = mapOf(
            "airports" to listOf("LGA", "JFK", "EWR"),
            "trainStations" to listOf("PennStation"),
            "ports" to listOf("NYC-Port")
        )
        // Single-hub categories cannot be filtered out; ambiguous airports keep only non-excluded
        val filtered = com.example.travelroulette24.location.LocalLocationHubResolver.resolveEffectiveOriginHubs(
            hubsMap = rawHubs,
            excludedHubs = setOf("LGA", "PennStation")
        )
        assertEquals(listOf("JFK", "EWR"), filtered["airports"])
        assertEquals(listOf("PennStation"), filtered["trainStations"])
        assertEquals(listOf("NYC-Port"), filtered["ports"])

        // Attempting to exclude ALL ambiguous hubs restores full list rather than leaving 0 hubs
        val safetyFallback = com.example.travelroulette24.location.LocalLocationHubResolver.resolveEffectiveOriginHubs(
            hubsMap = rawHubs,
            excludedHubs = setOf("LGA", "JFK", "EWR")
        )
        assertEquals(listOf("LGA", "JFK", "EWR"), safetyFallback["airports"])
    }

    @Test
    fun toggleAmbiguousOriginHub_requiresMultipleTerminalsAndPreservesAtLeastOne() = runTest {
        val repo = TravelRepository(isRealModeProvider = { false })
        val vm = TravelViewModel(
            repository = repo,
            budgetController = BudgetGuardian.BudgetController(InMemoryBudgetStorage())
        )
        advanceUntilIdle()

        // Single-terminal category must return false and ignore toggle
        assertFalse(vm.toggleAmbiguousOriginHub("MAD", listOf("MAD")))

        // Ambiguous category (2+ terminals) allows disabling one while blocking the last remaining one
        assertTrue(vm.toggleAmbiguousOriginHub("AEP", listOf("AEP", "EZE")))
        assertTrue("AEP" in vm.uiState.value.excludedOriginHubs)
        assertFalse(vm.toggleAmbiguousOriginHub("EZE", listOf("AEP", "EZE")))

        // Re-enabling succeeds
        assertTrue(vm.toggleAmbiguousOriginHub("AEP", listOf("AEP", "EZE")))
        assertFalse("AEP" in vm.uiState.value.excludedOriginHubs)
    }

    @Test
    fun distanceVetoHelper_appliesOnlyWhenHistoricalPriceCoversMinDistance() {
        val shortTrip = TravelIntelligenceEngine.ResultItem(
            mode = "flight",
            originHub = "MAD",
            destinationHub = "BCN", // ~505 km
            price = 39.0,
            departureTime = "2026-10-10T08:00:00Z",
            returnTime = null,
            checkInOnline = true,
            isOpportunity = false,
            isBudgetFriendly = true,
            deepLink = "https://example.com/bcn"
        )
        val longOpportunityTrip = TravelIntelligenceEngine.ResultItem(
            mode = "flight",
            originHub = "MAD",
            destinationHub = "JFK", // > 5700 km
            price = 210.0,
            departureTime = "2026-10-10T10:00:00Z",
            returnTime = null,
            checkInOnline = true,
            isOpportunity = true,
            isBudgetFriendly = true,
            deepLink = "https://example.com/jfk"
        )

        // Disabled -> keeps all items
        val disabled = DistanceVetoHelper.evaluate(
            items = listOf(shortTrip, longOpportunityTrip),
            originLat = 40.4168,
            originLon = -3.7038,
            historicalLowByHub = emptyMap(),
            enabled = false,
            minDistanceKm = 1000
        )
        assertEquals(2, disabled.filteredResults.size)

        // Enabled for 1000km with opportunity at >1000km -> filters out shortTrip (~505km)
        val withCoverage = DistanceVetoHelper.evaluate(
            items = listOf(shortTrip, longOpportunityTrip),
            originLat = 40.4168,
            originLon = -3.7038,
            historicalLowByHub = emptyMap(),
            enabled = true,
            minDistanceKm = 1000
        )
        assertTrue(withCoverage.hasHistoricalCoverage)
        assertEquals(listOf("JFK"), withCoverage.filteredResults.map { it.destinationHub })

        // Enabled for 1000km when NO historical record covers >= 1000km -> keeps list cleanly
        val noCoverage = DistanceVetoHelper.evaluate(
            items = listOf(shortTrip),
            originLat = 40.4168,
            originLon = -3.7038,
            historicalLowByHub = mapOf("BCN" to 35.0),
            enabled = true,
            minDistanceKm = 1000
        )
        assertFalse(noCoverage.hasHistoricalCoverage)
        assertEquals(1, noCoverage.filteredResults.size)
    }

    @Test
    fun worldCurrencies_andDemoAntarcticaAmericasRoutes_behaveGlobally() {
        val worldCurrencies = com.example.travelroulette24.utils.CurrencyHelper.getAvailableWorldCurrencies()
        assertTrue(worldCurrencies.size > 30)
        assertTrue(worldCurrencies.any { it.code == "COP" })
        assertTrue(worldCurrencies.any { it.code == "MXN" })
        assertTrue(worldCurrencies.any { it.code == "ARS" })

        val copRate = com.example.travelroulette24.utils.CurrencyHelper.fallbackRateFromEur("COP")
        val copPresets = com.example.travelroulette24.utils.CurrencyHelper.scaledAlertPricePresets(copRate)
        assertTrue(copPresets.first() > 50_000f)

        val ushuaiaOptions = com.example.travelroulette24.data.DemoTravelDataSource.generateDemoOptions(
            originHubs = listOf("USH", "Ushuaia-Port"),
            mode = TravelViewModel.MODE_ONE_WAY,
            stayMinHours = 24,
            stayMaxHours = 72,
            departureWindowHours = 24
        )
        assertTrue(ushuaiaOptions.isNotEmpty())

        val bogotaOptions = com.example.travelroulette24.data.DemoTravelDataSource.generateDemoOptions(
            originHubs = listOf("BOG"),
            mode = TravelViewModel.MODE_ONE_WAY,
            stayMinHours = 24,
            stayMaxHours = 72,
            departureWindowHours = 24
        )
        assertTrue(bogotaOptions.isNotEmpty())
        assertTrue(
            bogotaOptions.any {
                it.toString().contains("\"flight\"")
            }
        )
    }

    @Test
    fun loadTop20WithCoordinates_clearsStaleResultsImmediatelyOnNewSpin() = runTest {
        val repo = TravelRepository(isRealModeProvider = { false })
        val vm = TravelViewModel(
            repository = repo,
            budgetController = BudgetGuardian.BudgetController(InMemoryBudgetStorage())
        )
        vm.loadTop20WithCoordinates(-34.6037, -58.3816, forceFresh = true)
        assertTrue(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.results.isEmpty())

        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.displayResults.isNotEmpty())

        // Subsequent fresh spin immediately clears previous results while loading
        vm.loadTop20WithCoordinates(-34.6037, -58.3816, forceFresh = true)
        assertTrue(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.results.isEmpty())
    }
}
