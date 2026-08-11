package com.phuongnn14.tuithantai.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Regression coverage for the four real receipts reported as incorrect. */
class ReportedReceiptSemanticRegressionTest {
    @Test
    fun sinceTeaReceiptUsesFinalAfterDiscountAndIgnoresGrabPaymentMethod() {
        val text = """
            HÓA ĐƠN THANH TOÁN
            Trà Xanh Jasmine Sữa Tươi 52,800 52,800
            Bánh Trứng Nướng 2 44,000 88,000
            Bánh Trứng Xoài 1 52,800 52,800
            Bánh Trứng Ổi Hồng 1 52,800 52,800
            Thành tiền: 246,400 đ
            Tiền chiết khấu: 35,280 đ
            Tổng tiền: 211,120 đ
            +Thanh toán (GRAB_ONLINE) 211,120 đ
        """.trimIndent()

        assertEquals(211_120L, AmountExtractor.extract(text)?.amount)
        assertEquals("Ăn uống", CategoryResolver.resolve(text))
    }

    @Test
    fun winMartReceiptUsesPayableColumnAndRetailCategory() {
        val text = """
            WinMart
            PHIẾU TÍNH TIỀN
            VINAMILK STTT có đường -7,800 81,600
            MILO Sữa lúa mạch Nestle -4,500 57,500
            NABATI TPBS Bánh k.xốp -13,100 26,900
            TỔNG TIỀN HÀNG -32,000 666,100
            Tiền cần thanh toán 666,100
        """.trimIndent()

        assertEquals(666_100L, AmountExtractor.extract(text)?.amount)
        assertEquals("Winmart", MerchantExtractor.extract(text))
        assertEquals("Mua sắm", CategoryResolver.resolve(text))
    }

    @Test
    fun coffeeHouseReceiptIgnoresCashReceivedAndChange() {
        val text = """
            THE COFFEE HOUSE
            Cà Phê Kem Chanh 1 69 000 69 000
            Matcha Latte Tây Bắc Trân Châu Hoàng Kim 1 65 000 65 000
            Thành tiền: 134 000
            Thanh Toán: 134 000
            Tiền khách đưa: 134 000
            Tiền thừa: 0
        """.trimIndent()

        assertEquals(134_000L, AmountExtractor.extract(text)?.amount)
        assertEquals("The Coffee House", MerchantExtractor.extract(text))
        assertEquals("Ăn uống", CategoryResolver.resolve(text))
    }

    @Test
    fun temporaryReceiptAddsVatAndDoesNotInventMerchantFromAddress() {
        val text = """
            145 Hoàng Cầu
            PHIẾU TẠM TÍNH
            Đậu thịt sốt cà chua 95,000
            Cơm trắng 30,000
            Sườn xào chua ngọt 125,000
            Canh ngao hến nấu chua 105,000
            Trà sen nhài 40,000
            Tổng tiền hàng: 395,000
            Thu khác: VAT (8%): 31,600
            Tổng cộng: 426,600
        """.trimIndent()

        assertEquals(426_600L, AmountExtractor.extract(text)?.amount)
        assertNull(MerchantExtractor.extract(text))
        assertEquals("Ăn uống", CategoryResolver.resolve(text))
    }
}
