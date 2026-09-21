package com.example.travelroulette24.budget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetGuardianTest {

    private class FakeStorage : BudgetGuardian.BudgetStorage {
        var data: String? = null
        override fun load(): String? = data
        override fun save(json: String) { data = json }
    }

    @Test
    fun `budget persists locally without registration`() {
        val storage = FakeStorage()
        val controller = BudgetGuardian.BudgetController(storage)
        controller.setBudget(BudgetGuardian.Budget(maxPerTrip = 25.0, maxPerRoundTrip = 60.0))
        val loaded = BudgetGuardian.BudgetController(storage).getBudget()
        assertEquals(25.0, loaded!!.maxPerTrip!!, 0.001)
        assertEquals(60.0, loaded.maxPerRoundTrip!!, 0.001)
    }

    @Test
    fun `clear budget removes limit`() {
        val storage = FakeStorage()
        val controller = BudgetGuardian.BudgetController(storage)
        controller.setBudget(BudgetGuardian.Budget(maxPerTrip = 25.0))
        controller.clearBudget()
        assertNull(controller.getBudget())
    }

    @Test
    fun `corrupted stored budget is treated as none`() {
        val storage = FakeStorage()
        storage.data = "garbage{{{"
        assertNull(BudgetGuardian.BudgetController(storage).getBudget())
    }

    @Test
    fun `no budget set means everything fits`() {
        assertTrue(BudgetGuardian.fitsBudget(null, 1000.0, isRoundTrip = false))
        assertTrue(BudgetGuardian.fitsBudget(BudgetGuardian.Budget(), 1000.0, isRoundTrip = false))
    }

    @Test
    fun `per trip limit filters one way options`() {
        val budget = BudgetGuardian.Budget(maxPerTrip = 25.0)
        assertTrue(BudgetGuardian.fitsBudget(budget, 18.0, isRoundTrip = false))
        assertFalse(BudgetGuardian.fitsBudget(budget, 30.0, isRoundTrip = false))
    }

    @Test
    fun `round trip limit applies to round trips`() {
        val budget = BudgetGuardian.Budget(maxPerTrip = 25.0, maxPerRoundTrip = 60.0)
        assertTrue(BudgetGuardian.fitsBudget(budget, 55.0, isRoundTrip = true))
        assertFalse(BudgetGuardian.fitsBudget(budget, 70.0, isRoundTrip = true))
    }

    @Test
    fun `round trip without round trip limit falls back to per trip`() {
        val budget = BudgetGuardian.Budget(maxPerTrip = 25.0)
        assertTrue(BudgetGuardian.fitsBudget(budget, 20.0, isRoundTrip = true))
        assertFalse(BudgetGuardian.fitsBudget(budget, 40.0, isRoundTrip = true))
    }

    @Test
    fun `filterWithinBudget keeps only compliant options`() {
        val budget = BudgetGuardian.Budget(maxPerTrip = 25.0, enabled = true)
        val prices = listOf(18.0, 30.0, 25.0, 99.0)
        val filtered = BudgetGuardian.filterWithinBudget(budget, prices, isRoundTrip = { false }, price = { it })
        assertEquals(listOf(18.0, 25.0), filtered)
    }

    @Test
    fun `alert generated for budget compliant opportunity`() {
        val alert = BudgetGuardian.maybeAlert(
            budget = BudgetGuardian.Budget(maxPerTrip = 25.0),
            price = 18.0,
            currency = "€",
            isOpportunity = true,
            mode = "flight",
            destinationHub = "BCN",
            isRoundTrip = false
        )
        assertEquals(
            "There is a flight to BCN for €18, which is within your €25 budget.",
            alert
        )
    }

    @Test
    fun `no alert when price exceeds budget`() {
        assertNull(
            BudgetGuardian.maybeAlert(
                budget = BudgetGuardian.Budget(maxPerTrip = 25.0),
                price = 40.0,
                currency = "€",
                isOpportunity = true,
                mode = "ferry",
                destinationHub = "FNC",
                isRoundTrip = false
            )
        )
    }

    @Test
    fun `no alert when not an opportunity`() {
        assertNull(
            BudgetGuardian.maybeAlert(
                budget = BudgetGuardian.Budget(maxPerTrip = 25.0),
                price = 18.0,
                currency = "€",
                isOpportunity = false,
                mode = "train",
                destinationHub = "LIS",
                isRoundTrip = false
            )
        )
    }

    @Test
    fun `multi leg journey checked per leg`() {
        val budget = BudgetGuardian.Budget(maxPerTrip = 25.0)
        // Leg 1: arrived in Lisbon; leg 2: ferry to Madeira at €22 — within limit.
        assertTrue(BudgetGuardian.fitsBudget(budget, 22.0, isRoundTrip = false))
        // A pricier next leg is blocked.
        assertFalse(BudgetGuardian.fitsBudget(budget, 80.0, isRoundTrip = false))
    }

    @Test
    fun `decimal prices are formatted with two decimals`() {
        val alert = BudgetGuardian.maybeAlert(
            budget = BudgetGuardian.Budget(maxPerTrip = 25.5),
            price = 18.75,
            currency = "€",
            isOpportunity = true,
            mode = "flight",
            destinationHub = "MAD",
            isRoundTrip = false
        )
        assertEquals(
            "There is a flight to MAD for €18.75, which is within your €25.50 budget.",
            alert
        )
    }
}