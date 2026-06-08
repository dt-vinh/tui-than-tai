package com.phuongnn14.tuithantai.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Covers OCR-MONEY-001 through OCR-MONEY-010 from the casebook. */
class MoneyParserTest {

    @Test fun `OCR-MONEY-001 plain integer VND`() {
        val result = MoneyParser.parse("125000")
        assertEquals(125_000.0, result!!.amount, 0.0)
        assertEquals(MoneyParser.Currency.VND, result.currency)
    }

    @Test fun `OCR-MONEY-002 dot thousands VND`() {
        val result = MoneyParser.parse("125.000đ")
        assertEquals(125_000.0, result!!.amount, 0.0)
    }

    @Test fun `OCR-MONEY-003 comma thousands VND`() {
        val result = MoneyParser.parse("125,000 đ")
        assertEquals(125_000.0, result!!.amount, 0.0)
    }

    @Test fun `OCR-MONEY-004 space thousands VND`() {
        val result = MoneyParser.parse("1 250 000 VND")
        assertEquals(1_250_000.0, result!!.amount, 0.0)
    }

    @Test fun `OCR-MONEY-005 K suffix VND`() {
        val result = MoneyParser.parse("125K")
        assertEquals(125_000.0, result!!.amount, 0.0)
    }

    @Test fun `OCR-MONEY-006 OCR O to 0 substitution`() {
        val result = MoneyParser.parse("6,85O,OOO đ")
        assertEquals(6_850_000.0, result!!.amount, 0.0)
    }

    @Test fun `OCR-MONEY-007 dollar sign USD`() {
        val result = MoneyParser.parse("\$12.50")
        assertEquals(12.50, result!!.amount, 0.001)
        assertEquals(MoneyParser.Currency.USD, result.currency)
    }

    @Test fun `OCR-MONEY-008 Vietnamese decimal comma USD`() {
        val result = MoneyParser.parse("12,50 USD")
        assertEquals(12.50, result!!.amount, 0.001)
        assertEquals(MoneyParser.Currency.USD, result.currency)
    }

    @Test fun `OCR-MONEY-009 time string must return null`() {
        assertNull(MoneyParser.parse("17:45"))
    }

    @Test fun `OCR-MONEY-010 date string must return null`() {
        assertNull(MoneyParser.parse("05/06/2026"))
    }

    @Test fun `fixOcrDigits replaces O in numeric context`() {
        assertEquals("6,850,000", MoneyParser.fixOcrDigits("6,85O,OOO"))
    }

    @Test fun `fixOcrDigits does not replace O in text`() {
        assertEquals("CAFE", MoneyParser.fixOcrDigits("CAFE"))
    }

    @Test fun `blank input returns null`() {
        assertNull(MoneyParser.parse(""))
        assertNull(MoneyParser.parse("   "))
    }

    @Test fun `large VND multiple dots`() {
        val result = MoneyParser.parse("6.850.000đ")
        assertEquals(6_850_000.0, result!!.amount, 0.0)
    }
}
