package com.phuongnn14.tuithantai.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitBillCalculatorTest {

    @Test
    fun splitSharesDistributeRemainderToFirstParticipant() {
        val shares = SplitBillCalculator.splitShares(1_000_000L, 3)

        assertEquals(listOf(333_334L, 333_333L, 333_333L), shares)
        assertEquals(1_000_000L, shares.sum())
    }

    @Test
    fun settlementSuggestionsUseWholeVndAndBalancedTotals() {
        val result = SplitBillCalculator.calculate("B, A, C", "A", 1_000_000.0)

        assertEquals(0.0, result.balances.values.sum(), 0.0)
        assertEquals(2, result.suggestions.size)
        assertEquals(333_334.0, result.suggestions.first { it.debtor == "B" }.amount, 0.0)
        assertEquals(333_333.0, result.suggestions.first { it.debtor == "C" }.amount, 0.0)
        assertTrue(result.suggestions.all { it.amount % 1.0 == 0.0 })
    }
}
