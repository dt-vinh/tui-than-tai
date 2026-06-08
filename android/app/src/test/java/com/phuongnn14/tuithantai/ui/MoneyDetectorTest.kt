package com.phuongnn14.tuithantai.ui

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests cho MoneyDetector – bao phủ 25 case trong PRD section §15.
 *
 * Naming convention: MONEY_XXX khớp với test_matrix trong PRD.
 */
class MoneyDetectorTest {

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────
    private fun parseNorm(raw: String): ParsedMoney? =
        MoneyDetector.parseMoney(ReceiptOcrParser.normalize(raw))

    private fun parseNormCorrected(raw: String): ParsedMoney? {
        val corrected = MoneyDetector.correctOcrChars(raw)
        return MoneyDetector.parseMoney(ReceiptOcrParser.normalize(corrected))
    }

    private fun candidates(vararg lines: String) =
        MoneyDetector.detectMoneyCandidates(lines.toList())

    // ─────────────────────────────────────────────────────────────────────────
    // MONEY_001 – 10.000 VNĐ/ → 10,000 VND
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `MONEY_001 - 10000 VND voi ky hieu VND va slash`() {
        val result = parseNorm("10.000 VNĐ/")
        assertNotNull("Phải parse được", result)
        assertEquals("VND", result!!.currency)
        assertEquals(10_000.0, result.amount, 1.0)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MONEY_002 – 285k → 285,000
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `MONEY_002 - 285k = 285000`() {
        val result = parseNorm("285k")
        assertNotNull(result)
        assertEquals(285_000.0, result!!.amount, 1.0)
        assertEquals("VND", result.currency)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MONEY_003 – 100k → 100,000
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `MONEY_003 - 100k = 100000`() {
        val result = parseNorm("100k")
        assertNotNull(result)
        assertEquals(100_000.0, result!!.amount, 1.0)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MONEY_004 – 10.5k → 10,500 (dấu . là thập phân khi có k)
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `MONEY_004 - 10-5k = 10500`() {
        val result = parseNorm("10.5k")
        assertNotNull(result)
        assertEquals(10_500.0, result!!.amount, 1.0)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MONEY_005 – 10,5k → 10,500 (dấu , là thập phân khi có k)
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `MONEY_005 - 10comma5k = 10500`() {
        val result = parseNorm("10,5k")
        assertNotNull(result)
        assertEquals(10_500.0, result!!.amount, 1.0)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MONEY_006 – 1tr → 1,000,000
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `MONEY_006 - 1tr = 1000000`() {
        val result = parseNorm("1tr")
        assertNotNull(result)
        assertEquals(1_000_000.0, result!!.amount, 1.0)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MONEY_007 – 1tr2 → 1,200,000
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `MONEY_007 - 1tr2 = 1200000`() {
        val result = parseNorm("1tr2")
        assertNotNull(result)
        assertEquals(1_200_000.0, result!!.amount, 1.0)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MONEY_008 – 1.2tr → 1,200,000
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `MONEY_008 - 1-2tr = 1200000`() {
        val result = parseNorm("1.2tr")
        assertNotNull(result)
        assertEquals(1_200_000.0, result!!.amount, 1.0)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MONEY_009 – 2 củ 5 → 2,500,000
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `MONEY_009 - 2 cu 5 = 2500000`() {
        val result = parseNorm("2 củ 5")
        assertNotNull("Phải parse được '2 củ 5'", result)
        assertEquals(2_500_000.0, result!!.amount, 1.0)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MONEY_010 – 125.000đ → 125,000 VND
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `MONEY_010 - 125000d = 125000 VND`() {
        val result = parseNorm("125.000đ")
        assertNotNull(result)
        assertEquals("VND", result!!.currency)
        assertEquals(125_000.0, result.amount, 1.0)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MONEY_011 – 125,000đ → 125,000 VND
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `MONEY_011 - 125comma000d = 125000 VND`() {
        val result = parseNorm("125,000đ")
        assertNotNull(result)
        assertEquals(125_000.0, result!!.amount, 1.0)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MONEY_012 – ₫125.000 → 125,000 VND (prefix ký hiệu đồng)
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `MONEY_012 - dong prefix 125000`() {
        val result = MoneyDetector.parseMoney("₫125.000")  // ₫ không bị norm
        assertNotNull(result)
        assertEquals(125_000.0, result!!.amount, 1.0)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MONEY_013 – $12.50 → 12.50 USD
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `MONEY_013 - dollar 12-50 USD`() {
        val result = MoneyDetector.parseMoney("\$12.50")
        assertNotNull(result)
        assertEquals("USD", result!!.currency)
        assertEquals(12.50, result.amount, 0.01)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MONEY_014 – 12.50 USD → 12.50 USD
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `MONEY_014 - 12-50 USD suffix`() {
        val result = parseNorm("12.50 USD")
        assertNotNull(result)
        assertEquals("USD", result!!.currency)
        assertEquals(12.50, result.amount, 0.01)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MONEY_015 – 05/06/2026 → Không phải tiền (no candidate)
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `MONEY_015 - ngay thang nam khong phai tien`() {
        val cands = candidates("05/06/2026")
        // Không được parse ngày thành tiền
        val moneyAmounts = cands.map { it.amount }
        // Nếu có candidate, số không được là tổng hợp của 05, 06, 2026
        assertTrue(
            "05/06/2026 không được tạo candidate tổng tiền",
            cands.none { it.amount == 5_062_026.0 || it.amount == 5060.0 || it.amount == 2026.0 }
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MONEY_016 – 17:45 → Không phải tiền
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `MONEY_016 - gio phut khong phai tien`() {
        val spans = MoneyDetector.findMoneySpans("17:45")
        assertTrue("17:45 không tạo money span", spans.isEmpty())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MONEY_017 – Mã GD 827362991 → Không parse thành tiền
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `MONEY_017 - ma giao dich khong phai tien`() {
        val cands = candidates("Mã GD 827362991")
        // Dòng có "ma gd" → isCodeLine = true, không có currency symbol → bỏ qua
        assertTrue(
            "Mã GD không được là tổng tiền",
            cands.none { it.role == MoneyRole.TOTAL_CANDIDATE && it.amount == 827_362_991.0 }
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MONEY_018 – VAT 10% → Không phải tổng tiền
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `MONEY_018 - vat phan tram khong phai total`() {
        val spans = MoneyDetector.findMoneySpans(ReceiptOcrParser.normalize("VAT 10%"))
        // "10" alone (2 digits, no separator, no currency) → no span
        assertTrue("VAT 10% không tạo money span hợp lệ", spans.isEmpty())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MONEY_019 – Khách đưa 200.000 → role cash_received, không phải total
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `MONEY_019 - khach dua la cash_received khong phai total`() {
        val cands = candidates("Khách đưa: 200.000")
        val c = cands.firstOrNull { it.amount == 200_000.0 }
        assertNotNull("Phải tìm được 200.000", c)
        assertEquals("Role phải là cash_received", MoneyRole.CASH_RECEIVED, c!!.role)
        assertNotEquals("Không được là total_candidate", MoneyRole.TOTAL_CANDIDATE, c.role)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MONEY_020 – Tiền thừa 75.000 → role change, không phải total
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `MONEY_020 - tien thua la change khong phai total`() {
        val cands = candidates("Tiền thừa: 75.000")
        val c = cands.firstOrNull { it.amount == 75_000.0 }
        assertNotNull("Phải tìm được 75.000", c)
        assertEquals("Role phải là change", MoneyRole.CHANGE, c!!.role)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MONEY_021 – "Bia - 18,000 35 630,000" → phát hiện 18000 và 630000
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `MONEY_021 - dong item bia co unit price va line total`() {
        val lineNorm = ReceiptOcrParser.normalize("Bia chai Tuborg - 18,000 35 630,000")
        val spans = MoneyDetector.findMoneySpans(lineNorm)
        val amounts = spans.mapNotNull { MoneyDetector.parseMoney(it)?.amount }

        assertTrue("Phải có 18,000 (unit price)", amounts.any { it == 18_000.0 })
        assertTrue("Phải có 630,000 (line total)", amounts.any { it == 630_000.0 })
        assertFalse("35 (SL) không được là tiền", amounts.any { it == 35.0 })
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MONEY_022 – Khô cá lóc chiên 50k/100g → 50,000
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `MONEY_022 - 50k tren 100g chi lay 50k`() {
        val lineNorm = ReceiptOcrParser.normalize("Khô cá lóc chiên 50k/100g")
        val spans = MoneyDetector.findMoneySpans(lineNorm)
        val amounts = spans.mapNotNull { MoneyDetector.parseMoney(it)?.amount }

        assertTrue("Phải có 50,000", amounts.any { it == 50_000.0 })
        assertFalse("100 (gram) không phải tiền", amounts.any { it == 100.0 })
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MONEY_023 – Lạp xưởng 500g 170k → 170,000
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `MONEY_023 - 170k cuoi dong lap xuong`() {
        val lineNorm = ReceiptOcrParser.normalize("Lạp xưởng tươi bò 500g 170k")
        val spans = MoneyDetector.findMoneySpans(lineNorm)
        val amounts = spans.mapNotNull { MoneyDetector.parseMoney(it)?.amount }

        assertTrue("Phải có 170,000", amounts.any { it == 170_000.0 })
        assertFalse("500 (gram) không phải tiền", amounts.any { it == 500.0 })
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MONEY_024 – 6,85O,OOO đ → OCR correction → 6,850,000 + needsReview
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `MONEY_024 - ocr correction O=0 → 6850000 needsReview`() {
        val rawLine = "6,85O,OOO đ"
        val cands = MoneyDetector.detectMoneyCandidates(listOf(rawLine))
        val c = cands.firstOrNull { it.amount == 6_850_000.0 }
        assertNotNull("Phải parse được 6,850,000 sau OCR correction", c)
        assertTrue("needsReview = true vì có OCR correction", c!!.needsReview)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MONEY_025 – S0,000đ → OCR correction S→5 → 50,000 + needsReview
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `MONEY_025 - ocr correction S=5 → 50000 needsReview`() {
        val rawLine = "S0,000đ"
        val cands = MoneyDetector.detectMoneyCandidates(listOf(rawLine))
        val c = cands.firstOrNull { it.amount == 50_000.0 }
        assertNotNull("Phải parse được 50,000 sau OCR correction S→5", c)
        assertTrue("needsReview = true vì có OCR correction", c!!.needsReview)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bonus: selectBestTotal không chọn cash_received / change
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `selectBestTotal bo qua cash_received va change`() {
        val cands = MoneyDetector.detectMoneyCandidates(listOf(
            "TỔNG THANH TOÁN: 125.000",
            "Khách đưa: 200.000",
            "Tiền thừa: 75.000"
        ))
        val best = MoneyDetector.selectBestTotal(cands)
        assertNotNull("Phải tìm được total", best)
        assertEquals("Best total phải là 125,000", 125_000.0, best!!.amount, 1.0)
        assertNotEquals(200_000.0, best.amount, 1.0)
        assertNotEquals(75_000.0, best.amount, 1.0)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bonus: 1tr thay vì 1.000.000 trực tiếp
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `bonus - 1trieu ky tu co dau = 1000000`() {
        val result = parseNorm("1 triệu")
        assertNotNull(result)
        assertEquals(1_000_000.0, result!!.amount, 1.0)
    }

    @Test
    fun `bonus - 2cu = 2000000`() {
        val result = parseNorm("2 củ")
        assertNotNull(result)
        assertEquals(2_000_000.0, result!!.amount, 1.0)
    }

    @Test
    fun `bonus - 285 nghin = 285000`() {
        val result = parseNorm("285 nghìn")
        assertNotNull(result)
        assertEquals(285_000.0, result!!.amount, 1.0)
    }

    @Test
    fun `bonus - correctOcrChars chi sua trong context so`() {
        assertEquals("6,850,000", MoneyDetector.correctOcrChars("6,85O,OOO"))
        assertEquals("50,000",    MoneyDetector.correctOcrChars("S0,000"))
        // "OPEN" không sửa vì O không kề chữ số
        assertEquals("OPEN",      MoneyDetector.correctOcrChars("OPEN"))
    }
}
