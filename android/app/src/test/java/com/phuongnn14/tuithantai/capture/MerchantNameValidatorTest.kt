package com.phuongnn14.tuithantai.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MerchantNameValidatorTest {
    @Test
    fun rejectsAddressAndDocumentTitle() {
        assertNull(MerchantNameValidator.clean("145 Hoàng Cầu"))
        assertNull(MerchantNameValidator.clean("102G22 Thành Công - Giảng Võ"))
        assertNull(MerchantNameValidator.clean("PHIẾU TẠM TÍNH"))
    }

    @Test
    fun keepsRealMerchantNamesIncludingNumericBrands() {
        assertEquals("THE COFFEE HOUSE", MerchantNameValidator.clean("THE COFFEE HOUSE"))
        assertEquals("WinMart", MerchantNameValidator.clean("WinMart"))
        assertEquals("7 Eleven", MerchantNameValidator.clean("7 Eleven"))
    }
}
