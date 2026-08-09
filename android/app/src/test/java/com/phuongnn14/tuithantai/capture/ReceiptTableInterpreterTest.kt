package com.phuongnn14.tuithantai.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptTableInterpreterTest {
    private val winMartReceipt = """
        WinMart
        VINAMILK STTT co duong -7,800 81,600
        MILO Sua lua mach Nestle -4,500 57,500
        NABATI TPBS Banh k.xop Ri -13,100 26,900
        TONG TIEN HANG -32,000 666,100
        Tien can thanh toan 666,100
    """.trimIndent()

    @Test
    fun selectsLastPositiveCellFromTotalRow() {
        val result = ReceiptTableInterpreter.resolveFinalAmount(winMartReceipt)

        assertEquals(666_100L, result?.amount)
    }

    @Test
    fun splitTotalRowStillSelectsPayableInsteadOfDiscount() {
        val text = """
            TONG TIEN HANG
            -32.000 666.100
        """.trimIndent()

        assertEquals(666_100L, ReceiptTableInterpreter.resolveFinalAmount(text)?.amount)
    }

    @Test
    fun amountExtractorUsesSemanticTableBeforeLargestItemValue() {
        assertEquals(666_100L, AmountExtractor.extract(winMartReceipt)?.amount)
    }

    @Test
    fun markdownPreservesRowsAndMoneyCellsForLlm() {
        val markdown = ReceiptTableInterpreter.toMarkdown(winMartReceipt)

        assertTrue(markdown.contains("| tong tien hang |"))
        assertTrue(markdown.contains("-32,000, 666,100"))
    }
}
