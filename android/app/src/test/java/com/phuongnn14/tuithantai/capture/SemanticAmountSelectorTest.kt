package com.phuongnn14.tuithantai.capture

import org.junit.Assert.assertEquals
import org.junit.Test

class SemanticAmountSelectorTest {
    @Test
    fun semanticModelWinsWhenLocalOcrMistakesSubtotalForFinalAmount() {
        val localOcr = "Tổng tiền hàng 395,000\nVAT 31,600"

        assertEquals(
            426_600L,
            SemanticAmountSelector.select(426_600L, localOcr)
        )
    }

    @Test
    fun localSemanticRowsRemainAvailableOffline() {
        val localOcr = "Tổng tiền hàng 395,000\nVAT 31,600\nTổng cộng 426,600"

        assertEquals(
            426_600L,
            SemanticAmountSelector.select(null, localOcr)
        )
    }

    @Test
    fun semanticModelCannotOverrideStructuredCoffeeHouseTotal() {
        val receipt = """
            Thành tiền 774.000
            Giảm giá 246.400
            Thanh toán 527.600
        """.trimIndent()

        assertEquals(527_600L, SemanticAmountSelector.select(246_400L, receipt))
    }

    @Test
    fun phoneOnlyDocumentReturnsZeroEvenIfModelSuggestsPhoneNumber() {
        val receipt = "Tổng đài bảo hành 1900.232.465"

        assertEquals(0L, SemanticAmountSelector.select(1_900_232_465L, receipt))
    }
}
