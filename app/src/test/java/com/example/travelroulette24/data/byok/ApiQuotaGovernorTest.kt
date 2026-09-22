package com.example.travelroulette24.data.byok

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ApiQuotaGovernorTest {

    private val context: Context = mock()
    private val prefs: SharedPreferences = mock()
    private val editor: SharedPreferences.Editor = mock()
    private val inMemoryStore = mutableMapOf<String, Any>()

    @Before
    fun setUp() {
        inMemoryStore.clear()
        whenever(context.getSharedPreferences(eq("byok_quota_governor_prefs"), eq(Context.MODE_PRIVATE)))
            .thenReturn(prefs)
        whenever(prefs.edit()).thenReturn(editor)
        whenever(editor.putInt(any(), any())).thenAnswer { invocation ->
            val key = invocation.getArgument<String>(0)
            val value = invocation.getArgument<Int>(1)
            inMemoryStore[key] = value
            editor
        }
        whenever(prefs.getInt(any(), any())).thenAnswer { invocation ->
            val key = invocation.getArgument<String>(0)
            val defaultVal = invocation.getArgument<Int>(1)
            inMemoryStore[key] as? Int ?: defaultVal
        }
    }

    @Test
    fun testQuotaGovernor_freeTierSafeDailyBudget() {
        val governor = ApiQuotaGovernor(context)

        // 500 monthly quota should yield 13 daily safe queries (leaving safety reserve for spins)
        val canExecuteFirst = governor.canExecuteRequest("kiwi", isPaidTier = false, monthlyQuota = 500)
        assertTrue(canExecuteFirst)

        val status500 = governor.getQuotaStatus("kiwi", isPaidTier = false, monthlyQuota = 500)
        assertEquals(13, status500.dailyBudget)
        assertEquals(0, status500.requestsToday)
        assertFalse(status500.isDailyExhausted)

        // 2000 monthly quota should yield 60 daily safe queries (leaving safety reserve for spins)
        val status2000 = governor.getQuotaStatus("amadeus", isPaidTier = false, monthlyQuota = 2000)
        assertEquals(60, status2000.dailyBudget)
        assertEquals(0, status2000.requestsToday)
        assertFalse(status2000.isDailyExhausted)
    }

    @Test
    fun testQuotaGovernor_paidTierIsUnlimited() {
        val governor = ApiQuotaGovernor(context)

        // Paid tier should always allow execution regardless of high consumption
        val canExecutePaid = governor.canExecuteRequest("amadeus", isPaidTier = true, monthlyQuota = 2000)
        assertTrue(canExecutePaid)

        val status = governor.getQuotaStatus("amadeus", isPaidTier = true, monthlyQuota = 2000)
        assertEquals(ApiQuotaGovernor.TierMode.PAID_UNLIMITED, status.tierMode)
        assertFalse(status.isDailyExhausted)
        assertFalse(status.isMonthlyExhausted)
    }

    @Test
    fun testQuotaGovernor_cacheTtlOperations() {
        val governor = ApiQuotaGovernor(context)
        val dummyOffers = JsonArray(listOf(buildJsonObject { put("test", "data") }))

        assertNull(governor.getCachedOffers("key_paris_london"))

        governor.putCachedOffers("key_paris_london", dummyOffers)
        val cached = governor.getCachedOffers("key_paris_london")
        assertNotNull(cached)
        assertEquals(1, cached?.size)

        governor.clearCache()
        assertNull(governor.getCachedOffers("key_paris_london"))
    }
}
