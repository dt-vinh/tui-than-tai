package com.phuongnn14.tuithantai.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReceiptAmountResolverRegressionTest {
    @Test
    fun coffeeHouseUsesPaymentAfterUnsignedDiscount() {
        val receipt = """
            THE COFFEE HOUSE
            Tổng số lượng 12
            Thành tiền: 774 000
            + Giảm giá: 246 400
            Thanh Toán: 527 600
            Tiền khách đưa: 527 600
            Tiền thừa: 0
            Mọi thắc mắc xin liên hệ 0267 1057 088
        """.trimIndent()

        assertEquals(527_600L, ReceiptTableInterpreter.resolveFinalAmount(receipt)?.amount)
        assertEquals(527_600L, AmountExtractor.extract(receipt)?.amount)
    }

    @Test
    fun dienMayXanhUsesRemainingDueAndRejectsSwitchboards() {
        val receipt = """
            Điện máy XANH
            Tổng tiền: 600,000
            Đã thanh toán: 90,000
            Tiền PMH: 90,000
            Còn lại phải thu: 510,000
            Tổng đài liên hệ góp ý: 1800 1063
            Tổng đài bảo hành: 1900.232.465
        """.trimIndent()

        assertEquals(510_000L, ReceiptTableInterpreter.resolveFinalAmount(receipt)?.amount)
        assertEquals(510_000L, AmountExtractor.extract(receipt)?.amount)
    }

    @Test
    fun derivesFinalAmountWhenPaymentRowIsMissing() {
        val receipt = """
            Thành tiền 774.000
            Giảm giá 246.400
        """.trimIndent()

        assertEquals(527_600L, ReceiptTableInterpreter.resolveFinalAmount(receipt)?.amount)
    }

    @Test
    fun readsValueSplitFromRemainingDueLabel() {
        val receipt = """
            Tổng tiền 600.000
            Đã thanh toán 90.000
            Còn lại phải thụ:
            510.000
        """.trimIndent()

        assertEquals(510_000L, ReceiptTableInterpreter.resolveFinalAmount(receipt)?.amount)
    }

    @Test
    fun documentContainingOnlyPhoneNumbersHasNoAmount() {
        val receipt = """
            Tổng đài góp ý 1800 1063
            Tổng đài bảo hành 1900.232.465
            Liên hệ 0388 351 604
        """.trimIndent()

        assertNull(ReceiptTableInterpreter.resolveFinalAmount(receipt))
        assertNull(AmountExtractor.extract(receipt))
    }
}
