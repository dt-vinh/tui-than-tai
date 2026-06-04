package com.phuongnn14.tuithantai.ml

import com.phuongnn14.tuithantai.data.defaultCategories
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpenseAnalyzerTest {
    private val analyzer = ExpenseAnalyzer()

    @Test
    fun extractAmountPicksLikelyLargestVndTotal() {
        val text = """
            CUA HANG BANH MI
            Banh mi 20.000 d
            Cafe 15.000 d
            Tong cong 35.000 VND
        """.trimIndent()

        assertEquals(35_000L, analyzer.extractAmount(text))
    }

    @Test
    fun extractAmountHandlesPlainLongNumber() {
        val text = "Thanh toán 385250 VND"

        assertEquals(385_250L, analyzer.extractAmount(text))
    }

    @Test
    fun extractAmountReturnsZeroWhenNoMoneyCandidate() {
        val text = "Cảm ơn quý khách"

        assertEquals(0L, analyzer.extractAmount(text))
    }

    @Test
    fun inferCategoryMapsFoodTextToFood() {
        val result = analyzer.inferCategory("Banh mi pho com", emptyList(), defaultCategories)

        assertEquals("food", result)
    }

    @Test
    fun inferCategoryMapsCoffeeLabelToCoffee() {
        val result = analyzer.inferCategory("", listOf("coffee", "drink"), defaultCategories)

        assertEquals("coffee", result)
    }

    @Test
    fun inferCategoryFallsBackToOther() {
        val result = analyzer.inferCategory("unknown words", listOf("random"), defaultCategories)

        assertEquals("other", result)
    }

    @Test
    fun inferTitleUsesFirstReadableNonNumericLine() {
        val title = analyzer.inferTitle(
            """
            123456
            Mini Mart
            Total 50.000
            """.trimIndent(),
            listOf("receipt")
        )

        assertEquals("Mini Mart", title)
    }
}
