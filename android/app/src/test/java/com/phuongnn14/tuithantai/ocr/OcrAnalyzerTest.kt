package com.phuongnn14.tuithantai.ocr

import org.junit.Assert.*
import org.junit.Test

class OcrAnalyzerTest {

    private val analyzer = OcrAnalyzer()

    // ── No-hallucination ────────────────────────────────────────────────────

    @Test fun `blank image returns not_receipt with null total`() {
        val result = analyzer.analyze("")
        assertEquals(DocumentType.NOT_RECEIPT, result.documentType)
        assertNull(result.totalAmount)
        assertEquals("", result.merchantName ?: "")
        assertTrue(result.needsUserReview)
    }

    @Test fun `unrelated image text returns not_receipt`() {
        val result = analyzer.analyze("Cảnh đẹp buổi sáng\nBầu trời xanh trong")
        assertEquals(DocumentType.NOT_RECEIPT, result.documentType)
        assertNull(result.totalAmount)
        assertTrue(result.needsUserReview)
    }

    @Test fun `no hallucinated item names for unclear text`() {
        val result = analyzer.analyze("... ... 35000")
        val forbidden = setOf(
            "Không xác định", "Khác", "Hàng hóa",
            "Vật phẩm", "Sản phẩm", "Receipt", "Bill"
        )
        result.items.forEach { item ->
            assertFalse(
                "Item name must not be a placeholder, got '${item.name}'",
                item.name in forbidden
            )
        }
    }

    // ── Golden fixture: Tô Ký badminton court ──────────────────────────────

    @Test fun `to ky fixture - total 6850000 and category food`() {
        val ocrText = """
            SÂN CẦU LÔNG & PICKLEBALL TÔ KÝ
            Ngày: 05/06/2026  19:30
            Bàn: B2
            -----------------------------------------------
            Kem/đồ uống 250ml   255   20.000   5.100.000
            Bia chai Tuborg chai xanh  35  18.000  630.000
            -----------------------------------------------
            TỔNG TẠM TÍNH        5.730.000
            Phí dịch vụ 20%      1.120.000
            TỔNG CỘNG            6.850.000
            Khách đưa            7.000.000
            Tiền thừa              150.000
        """.trimIndent()

        val result = analyzer.analyze(ocrText)

        assertEquals(6_850_000.0, result.totalAmount!!, 0.0)
        assertEquals("VND", result.currency)
        assertEquals("food", result.categoryId)
        assertEquals(TransactionType.EXPENSE, result.transactionType)
        assertNotNull(result.merchantName)
        assertTrue(result.merchantName!!.contains("TÔ KÝ", ignoreCase = true))
    }

    // ── Bank transfer out ───────────────────────────────────────────────────

    @Test fun `bank transfer out - expense, no line items`() {
        val ocrText = """
            CHUYỂN KHOẢN THÀNH CÔNG
            Số tiền: 500.000 VND
            Người nhận: NGUYEN VAN A
            Ngân hàng: VietcomBank
            Nội dung: tien thang 6
            Mã giao dịch: FT2606050001
            Thời gian: 05/06/2026 10:23
        """.trimIndent()

        val result = analyzer.analyze(ocrText)

        assertEquals(DocumentType.PAYMENT_CONFIRMATION, result.documentType)
        assertEquals(TransactionType.EXPENSE, result.transactionType)
        assertEquals(500_000.0, result.totalAmount!!, 0.0)
        assertTrue("Payment confirmation must have no line items", result.items.isEmpty())
    }

    // ── Bank transfer in ────────────────────────────────────────────────────

    @Test fun `bank transfer in - income`() {
        val ocrText = """
            NHẬN TIỀN THÀNH CÔNG
            +2.000.000 VND
            Người gửi: TRAN THI B
            Nội dung: luong thang 6
            Thời gian: 05/06/2026 08:00
        """.trimIndent()

        val result = analyzer.analyze(ocrText)

        assertEquals(DocumentType.PAYMENT_CONFIRMATION, result.documentType)
        assertEquals(TransactionType.INCOME, result.transactionType)
    }

    // ── Supermarket: must not pick khách đưa ───────────────────────────────

    @Test fun `supermarket - total not khach dua`() {
        val ocrText = """
            VINMART+
            Banh mi sandwich     25.000
            Sua tuoi vinamilk    35.000
            Coca cola 330ml      15.000
            TONG CONG            75.000
            Khach dua           100.000
            Tien thua tra khach  25.000
        """.trimIndent()

        val result = analyzer.analyze(ocrText)
        assertEquals(75_000.0, result.totalAmount!!, 0.0)
    }

    // ── Product photo / not receipt ─────────────────────────────────────────

    @Test fun `product photo - merchant name not a forbidden placeholder`() {
        val result = analyzer.analyze(
            "iPhone 16 Pro Max\nMàu: Titan tự nhiên\nGiá niêm yết: 33.990.000đ"
        )
        val forbidden = listOf("Không xác định", "Khác", "Hàng hóa", "Vật phẩm")
        assertFalse(result.merchantName in forbidden)
    }

    // ── Time/date not misread as amount ─────────────────────────────────────

    @Test fun `time 17h45 must not become amount`() {
        val ocrText = "Giờ vào: 17:45\nTOTAL: 50.000đ"
        val result = analyzer.analyze(ocrText)
        assertEquals(50_000.0, result.totalAmount!!, 0.0)
        assertNotEquals(1745.0, result.totalAmount)
        assertNotEquals(17_450.0, result.totalAmount)
    }
}
