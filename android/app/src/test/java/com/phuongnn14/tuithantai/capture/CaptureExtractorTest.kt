package com.phuongnn14.tuithantai.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureExtractorTest {
    @Test
    fun thanhToanLineReturnsTotalAmount() {
        val result = AmountExtractor.extract("THANH TOÁN 415,000đ")

        assertEquals(415_000L, result?.amount)
        assertTrue((result?.confidence ?: 0f) >= 0.75f)
    }

    @Test
    fun cashGivenAndChangeAreIgnored() {
        val text = """
            Tiền hàng 415,000
            Khách đưa 500,000
            Tiền thừa 85,000
            THANH TOÁN 415,000đ
        """.trimIndent()

        val result = AmountExtractor.extract(text)

        assertEquals(415_000L, result?.amount)
        assertTrue(result?.sourceLine?.contains("THANH", ignoreCase = true) == true)
    }

    @Test
    fun priceLineReturnsAmount() {
        val result = AmountExtractor.extract("Giá: 120.000đ")

        assertEquals(120_000L, result?.amount)
    }

    @Test
    fun phoneNumberIsIgnored() {
        val result = AmountExtractor.extract("SĐT: 0388351604")

        assertNull(result)
    }

    @Test
    fun timeAndDateLineIsIgnored() {
        val result = AmountExtractor.extract("Giờ vào: 12:24 12/06/2026")

        assertNull(result)
    }

    @Test
    fun merchantIsTitleCased() {
        val result = MerchantExtractor.extract(
            """
            SỦI CẢO ĐỆ NHẤT ĐÔNG BẮC
            102G22 Thành Công - Giảng Võ
            HÓA ĐƠN THANH TOÁN
            """.trimIndent()
        )

        assertEquals("Sủi Cảo Đệ Nhất Đông Bắc", result)
    }

    @Test
    fun foodKeywordsResolveFoodCategory() {
        val result = CategoryResolver.resolve("sủi cảo, mì, trà")

        assertEquals("Ăn uống", result)
    }

    @Test
    fun shopeeShippingLabelUsesCodZeroAndIgnoresWeight() {
        val text = """
            S Shopee
            be
            Hỏa Tốc Mã đơn hàng: 260629C13TP4BS
            Nội dung hàng (Tổng SL sản phẩm: 1)
            1. (NFC)Iphone 8 plus 64 GB,Iphone X mất face,SL: 1
            Tiền thu Người nhận:
            0 VND
            Khối lượng tối đa: 30,000 g
            Kiểm tra tên sản phẩm và đối chiếu Mã vận đơn trên ứng dụng Shopee trước khi nhận hàng
        """.trimIndent()

        val amount = AmountExtractor.extract(text)
        val result = MoneyPresenceDetector.detect(text)

        assertEquals(0L, amount?.amount)
        assertEquals(0L, result?.amount)
        assertTrue(result?.productNote?.contains("iPhone 8 Plus 64 GB") == true)
        assertEquals("Mua sắm", result?.categoryName)
    }

    @Test
    fun shopeeWeightWithoutCodIsNotMoneyAmount() {
        val text = """
            S Shopee
            Nội dung hàng (Tổng SL sản phẩm: 1)
            1. (NFC)Iphone 8 plus 64 GB,Iphone X mất face,SL: 1
            Khối lượng tối đa: 30,000 g
        """.trimIndent()

        assertNull(AmountExtractor.extract(text))
        assertEquals("Mua sắm", CategoryResolver.resolve(text, ProductNoteExtractor.extract(text)))
    }
}
