package com.phuongnn14.tuithantai.ocr

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests using OCR text that simulates ML Kit output from the 4 real receipt images.
 * Images: Quán Ăn Đông Vui, Tiệm Cà Phê Gia Đình (×2), Payroll sheet.
 */
class RealReceiptTest {

    private val analyzer = OcrAnalyzer()

    // ── Image 1: Quán Ăn Đông Vui ──────────────────────────────────────────
    // Simple restaurant receipt. Total = 40,000 VND. Two items.

    @Test fun `dong vui - total 40000 not line items`() {
        val ocrText = """
            QUÁN ĂN ĐÔNG VUI
            Địa chỉ: 123 Đường Lê Lợi, Q.1, TP.HCM
            SỐ HĐ: 004567
            NGÀY: 20/10/2023
            GIỜ: 12:35
            SL  TÊN MÓN          ĐƠN GIÁ    TỔNG
            1   Cơm Tấm Sườn     35,000     35,000
            1   Trà Đá             5,000      5,000
            TỔNG CỘNG (TOTAL):              40,000 VND
            THANH TOÁN:  TIỀN MẶT
            Cảm ơn Quý khách!
        """.trimIndent()

        val result = analyzer.analyze(ocrText)

        assertEquals(40_000.0, result.totalAmount!!, 0.0)
        assertEquals("VND", result.currency)
        assertEquals("food", result.categoryId)
        assertEquals(TransactionType.EXPENSE, result.transactionType)
        assertEquals(DocumentType.POS_RECEIPT, result.documentType)
        assertNotNull(result.merchantName)
        assertTrue(result.merchantName!!.contains("ĐÔNG VUI", ignoreCase = true))
    }

    // ── Image 2: Tiệm Cà Phê Gia Đình — tiền thừa trap ─────────────────────
    // KEY TEST: Tiền nhận = 200,000 and Tiền thừa = 100,000 must NOT be total.
    // TỔNG CỘNG = 100,000 must be selected.

    @Test fun `cafe gia dinh - total 100000 not tien thua`() {
        val ocrText = """
            TIỆM CÀ PHÊ GIA ĐÌNH
            Địa chỉ: 45 Đường Hoàng Diệu, Diệu, Q. Bình Thạnh, TP.HCM
            SỐ HĐ: 112346
            NGÀY: 22/10/2023
            GIỜ: 10:30
            TÊN MÓN             SL  ĐƠN GIÁ   TỔNG
            Cà Phê Sữa Đá (L)    2   35,000   70,000
            Bánh Croissant        1   25,000   25,000
            Nước Suối (L)         1    5,000    5,000
            TỔNG CỘNG (TOTAL):              100,000 VND
            THANH TOÁN: TIỀN MẶT (CASH)
            Tiền nhận: 200,000
            Tiền thừa: 100,000
            Cảm ơn sự ủng hộ của Quý khách!
        """.trimIndent()

        val result = analyzer.analyze(ocrText)

        assertEquals(100_000.0, result.totalAmount!!, 0.0)
        assertNotEquals(200_000.0, result.totalAmount)
        assertNotEquals(100_000.0 * 2, result.totalAmount) // not tiền nhận
        assertEquals("VND", result.currency)
        assertEquals(TransactionType.EXPENSE, result.transactionType)
    }

    @Test fun `cafe gia dinh - category coffee not other`() {
        val ocrText = """
            TIỆM CÀ PHÊ GIA ĐÌNH
            Cà Phê Sữa Đá (L)    2   35,000   70,000
            Bánh Croissant        1   25,000   25,000
            TỔNG CỘNG:           100,000 VND
        """.trimIndent()
        val result = analyzer.analyze(ocrText)
        val validCategories = setOf("coffee", "food")
        assertTrue("Category should be coffee or food, got ${result.categoryId}", result.categoryId in validCategories)
    }

    // ── Image 3: Payroll / salary sheet ─────────────────────────────────────
    // "HĐ LƯƠNG/PAYROLL" — this is NOT a consumer receipt.
    // Must be flagged as needs_user_review=true.
    // Must NOT auto-select total_amount (it's aggregate salary, not the user's expense).

    @Test fun `payroll sheet - not a consumer receipt, needs review`() {
        val ocrText = """
            TIỆM CÀ PHÊ GIA ĐÌNH
            Địa chỉ: 45 Đường Hoàng Diệu, Diệu, Q. Bình Thạnh, TP.HCM
            SỐ HĐ: 112345 (HĐ LƯƠNG/PAYROLL)
            NGÀY: 21/10/2023
            GIỜ: 09:15
            Chức Vụ       Lương Cơ   Phụ Cấp   Thưởng   Tăng Ca
            Quản Lý       8,500,000   300,000   500,000   250,000
            Pha Chế       6,500,000   300,000   300,000   150,000
            Phục Vụ       5,500,000   200,000   200,000   100,000
            Phục Vụ       5,500,000   200,000   300,000   100,000
            TỔNG LƯƠNG TRẢ (TOTAL PAID): 28,900,000 VND
            THANH TOÁN: CHUYỂN KHOẢN (BANK TRANSFER)
            Cảm ơn sự cống hiến của Quý Nhân viên!
        """.trimIndent()

        val result = analyzer.analyze(ocrText)

        // Must be flagged for review — payroll is not a simple consumer receipt
        assertTrue(
            "Payroll sheet must have needs_user_review=true",
            result.needsUserReview
        )
        // Must not create items from employee salary rows as if they were products
        result.items.forEach { item ->
            assertFalse(
                "Item name must not be a fake placeholder, got '${item.name}'",
                item.name in setOf("Không xác định", "Khác", "Hàng hóa", "Vật phẩm", "Receipt", "Bill")
            )
        }
    }

    @Test fun `payroll - if total parsed it should not be auto-saved without review`() {
        val ocrText = """
            HĐ LƯƠNG/PAYROLL
            TỔNG LƯƠNG TRẢ (TOTAL PAID): 28,900,000 VND
            THANH TOÁN: CHUYỂN KHOẢN (BANK TRANSFER)
        """.trimIndent()

        val result = analyzer.analyze(ocrText)
        // Even if total is parsed, user must review (payroll is not a normal expense)
        assertTrue(result.needsUserReview)
    }

    // ── Image 4: Tiệm Cà Phê — zero-price item (Nước Lọc = 0đ) ─────────────

    @Test fun `cafe gia dinh - total 40000 with zero price item`() {
        val ocrText = """
            TIỆM CÀ PHÊ GIA ĐÌNH
            Địa chỉ: 45 Đường Hoàng Diệu, Diệu, Q. Bình Thạnh, TP.HCM
            SỐ HĐ: 112345
            NGÀY: 21/10/2023
            GIỜ: 09:15
            SL  TÊN MÓN          ĐƠN GIÁ    TỔNG
            1   Cà Phê Đen Đá (L) 28,000    28,000
            1   Bánh Mì Kẹp        12,000    12,000
            2   Nước Lọc                0         0
            TỔNG CỘNG (TOTAL):              40,000 VND
            THANH TOÁN: TIỀN MẶT
            Cảm ơn và hẹn gặp lại Quý khách!
        """.trimIndent()

        val result = analyzer.analyze(ocrText)

        assertEquals(40_000.0, result.totalAmount!!, 0.0)
        assertEquals("VND", result.currency)
        assertEquals(TransactionType.EXPENSE, result.transactionType)
        assertNotEquals(28_000.0, result.totalAmount)
        assertNotEquals(12_000.0, result.totalAmount)
    }

    @Test fun `cafe gia dinh - zero price does not corrupt total selection`() {
        val ocrText = """
            Cà Phê Đen Đá (L)    1   28,000   28,000
            Bánh Mì Kẹp           1   12,000   12,000
            Nước Lọc              2        0        0
            TỔNG CỘNG:                       40,000 VND
        """.trimIndent()

        val result = analyzer.analyze(ocrText)
        assertEquals(40_000.0, result.totalAmount!!, 0.0)
    }
}
