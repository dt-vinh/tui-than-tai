package com.phuongnn14.tuithantai.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TotalResolverTest {

    private fun c(label: String, amount: Double) = TotalResolver.Candidate(label, amount)

    @Test fun `picks TONG CONG over subtotal`() {
        val candidates = listOf(
            c("Tiền hàng", 100_000.0),
            c("VAT 10%", 10_000.0),
            c("TỔNG CỘNG", 110_000.0)
        )
        assertEquals(110_000.0, TotalResolver.resolve(candidates)!!, 0.0)
    }

    @Test fun `does not pick Khach dua`() {
        val candidates = listOf(
            c("TỔNG THANH TOÁN", 35_000.0),
            c("Khách đưa", 50_000.0),
            c("Tiền thừa", 15_000.0)
        )
        assertEquals(35_000.0, TotalResolver.resolve(candidates)!!, 0.0)
    }

    @Test fun `does not pick Tien thua`() {
        val candidates = listOf(
            c("TOTAL", 385_000.0),
            c("Tiền thừa trả khách", 15_000.0)
        )
        assertEquals(385_000.0, TotalResolver.resolve(candidates)!!, 0.0)
    }

    @Test fun `does not pick VAT as total`() {
        val candidates = listOf(
            c("Tạm tính", 200_000.0),
            c("VAT", 20_000.0),
            c("THANH TOÁN", 220_000.0)
        )
        assertEquals(220_000.0, TotalResolver.resolve(candidates)!!, 0.0)
    }

    @Test fun `TONG THANH TOAN beats TONG TAM TINH`() {
        val candidates = listOf(
            c("TỔNG TẠM TÍNH", 300_000.0),
            c("TỔNG THANH TOÁN", 315_000.0)
        )
        assertEquals(315_000.0, TotalResolver.resolve(candidates)!!, 0.0)
    }

    @Test fun `returns null for empty candidates`() {
        assertNull(TotalResolver.resolve(emptyList()))
    }

    @Test fun `falls back to largest non-excluded when no priority label`() {
        val candidates = listOf(
            c("Item A", 30_000.0),
            c("Item B", 70_000.0),
            c("Item C", 50_000.0)
        )
        assertEquals(70_000.0, TotalResolver.resolve(candidates)!!, 0.0)
    }

    @Test fun `to ky bill - 6850000 not khach dua`() {
        val candidates = listOf(
            c("Kem đồ uống 250ml x255", 5_100_000.0),
            c("Bia chai Tuborg x35", 630_000.0),
            c("TỔNG TẠM TÍNH", 5_730_000.0),
            c("Phí dịch vụ 20%", 1_120_000.0),
            c("TỔNG CỘNG", 6_850_000.0),
            c("Khách đưa", 7_000_000.0),
            c("Tiền thừa", 150_000.0)
        )
        assertEquals(6_850_000.0, TotalResolver.resolve(candidates)!!, 0.0)
    }
}
