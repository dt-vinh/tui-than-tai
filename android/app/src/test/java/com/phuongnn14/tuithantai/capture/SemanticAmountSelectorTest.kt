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
}
