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
    fun splitThanhToanKeywordAndAmountStillAutoFills() {
        val text = """
            Tiền hàng (7)
            415,000
            THANH TOÁN
            415,000đ
        """.trimIndent()

        val result = MoneyPresenceDetector.detect(text)

        assertEquals(415_000L, result?.amount)
        assertTrue((result?.confidence ?: 0f) >= 0.75f)
    }

    @Test
    fun invoiceTitleThanhToanDoesNotBoostInvoiceNumber() {
        val text = """
            SỦI CẢO ĐỆ NHẤT ĐÔNG BẮC
            HÓA ĐƠN THANH TOÁN
            Số: 100097932
            Tiền hàng (7)
            415,000
            THANH TOÁN
            415,000đ
        """.trimIndent()

        val result = MoneyPresenceDetector.detect(text)

        assertEquals(415_000L, result?.amount)
        assertTrue(result?.productNote?.contains("Sủi Cảo", ignoreCase = true) == true)
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
    fun weightLineIsNotMoneyAmount() {
        val text = """
            Tiền thu Người nhận:
            0 VND
            Khối lượng tối đa: 30,000 g
        """.trimIndent()

        val result = AmountExtractor.extract(text)

        assertNull(result)
    }

    @Test
    fun splitWeightLineIsNotMoneyAmount() {
        val text = """
            Tiền thu Người nhận:
            0 VND
            Khối lượng tối đa:
            30,000
            g
        """.trimIndent()

        val result = AmountExtractor.extract(text)

        assertNull(result)
    }

    @Test
    fun shopeeShippingLabelExtractsProductAndShoppingCategory() {
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

        val draft = MoneyPresenceDetector.uncertainDraft(text)

        assertNull(AmountExtractor.extract(text))
        assertEquals(null, draft.amount)
        assertTrue(draft.productNote?.contains("iPhone 8 Plus 64 GB") == true)
        assertEquals("Mua sắm", draft.categoryName)
    }

    @Test
    fun kiemTraDoesNotResolveFoodCategory() {
        val result = CategoryResolver.resolve("Kiểm tra tên sản phẩm trước khi nhận hàng")

        assertEquals("Khác", result)
    }

    @Test
    fun vietnameseBanknoteTenThousandIsDetected() {
        val text = """
            CỘNG HÒA XÃ HỘI CHỦNGHĨA
            VIỆTNAM
            MƯỜI NGHÌN
            ĐỒNG
            10000
            D000
            CX 23374345
        """.trimIndent()

        val result = MoneyPresenceDetector.detect(text)

        assertEquals(10_000L, result?.amount)
        assertEquals("Tiền Việt Nam 10.000 ₫", result?.productNote)
        assertEquals("Khác", result?.categoryName)
        assertTrue((result?.confidence ?: 0f) >= 0.85f)
    }

    @Test
    fun allVietnameseBanknoteDenominationsAreDetected() {
        val cases = listOf(
            500L to "NĂM TRĂM ĐỒNG 500",
            1_000L to "MỘT NGHÌN ĐỒNG 1000",
            2_000L to "HAI NGHÌN ĐỒNG 2000",
            5_000L to "NĂM NGHÌN ĐỒNG 5000",
            10_000L to "MƯỜI NGHÌN ĐỒNG 10000",
            20_000L to "HAI MƯƠI NGHÌN ĐỒNG 20000",
            50_000L to "NĂM MƯƠI NGHÌN ĐỒNG 50000",
            100_000L to "MỘT TRĂM NGHÌN ĐỒNG 100000",
            200_000L to "HAI TRĂM NGHÌN ĐỒNG 200000",
            500_000L to "NĂM TRĂM NGHÌN ĐỒNG 500000"
        )

        cases.forEach { (amount, denomText) ->
            val text = """
                CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM
                $denomText
                AB 12345678
            """.trimIndent()

            assertEquals(amount, VietnameseBanknoteDetector.detect(text)?.amount)
        }
    }

    @Test
    fun twoWordVisibleBanknotesAreSummedConservatively() {
        val text = """
            CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM
            HAI NGHÌN ĐỒNG
            CD 12345678
            CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM
            HAI NGHÌN ĐỒNG
            EF 87654321
        """.trimIndent()

        assertEquals(4_000L, VietnameseBanknoteDetector.detect(text)?.amount)
    }
}
