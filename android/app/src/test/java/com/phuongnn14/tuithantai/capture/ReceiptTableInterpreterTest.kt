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

    @Test
    fun resolvesChineseJapaneseAndKoreanPayableRows() {
        assertEquals(
            88_000L,
            ReceiptTableInterpreter.resolveFinalAmount("优惠 -5,000\n应付金额 88,000")?.amount
        )
        assertEquals(
            3_500L,
            ReceiptTableInterpreter.resolveFinalAmount("割引 -500\nお支払金額 3,500")?.amount
        )
        assertEquals(
            29_000L,
            ReceiptTableInterpreter.resolveFinalAmount("할인 -1,000\n결제금액 29,000")?.amount
        )
    }

    @Test
    fun resolvesDevanagariAndThaiPayableRows() {
        assertEquals(
            9_500L,
            ReceiptTableInterpreter.resolveFinalAmount("छूट -500\nकुल देय 9,500")?.amount
        )
        assertEquals(
            120_000L,
            ReceiptTableInterpreter.resolveFinalAmount("ส่วนลด -10,000\nยอดชำระ 120,000")?.amount
        )
    }

    @Test
    fun resolvesEuropeanAndSoutheastAsianPayableRows() {
        val examples = mapOf(
            "Discount -2,000\nGrand Total 40,000" to 40_000L,
            "Descuento -2,000\nTotal a pagar 40,000" to 40_000L,
            "Remise -2,000\nNet à payer 40,000" to 40_000L,
            "Rabatt -2,000\nZu zahlen 40,000" to 40_000L,
            "Desconto -2,000\nValor total 40,000" to 40_000L,
            "Diskon -2,000\nJumlah bayar 40,000" to 40_000L
        )

        examples.forEach { (text, expected) ->
            assertEquals(expected, ReceiptTableInterpreter.resolveFinalAmount(text)?.amount)
        }
    }

    @Test
    fun multilingualDocumentHeaderIsNotMistakenForPayableRow() {
        val text = """
            TAX INVOICE 20260001
            Item 12,000
            Amount payable 10,000
        """.trimIndent()

        assertEquals(10_000L, ReceiptTableInterpreter.resolveFinalAmount(text)?.amount)
    }
}
