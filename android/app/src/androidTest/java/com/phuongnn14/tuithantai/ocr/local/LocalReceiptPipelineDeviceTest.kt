package com.phuongnn14.tuithantai.ocr.local

import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalReceiptPipelineDeviceTest {
    @Test
    fun vietnameseReceiptRunsFullyOfflineWithinFiveSecondsWarm() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        assertTrue("Bundled multilingual MiniLM model is missing", LocalSemanticModel.isReady(context))
        val receipt = """
            HÓA ĐƠN THANH TOÁN
            Trà Phong Lan Đá 65.000 VND
            Thành tiền 65.000 VND
            Giảm giá 27.000 VND
            Sau ưu đãi, người mua còn phải trả 38.000 VND
            Khách đưa 100.000 VND
            Tiền thừa 62.000 VND
        """.trimIndent()

        assertNotNull(LocalReceiptAnalyzer(context).analyze(receipt))
        val started = SystemClock.elapsedRealtime()
        val result = LocalReceiptAnalyzer(context).analyze(receipt)
        val elapsedMs = SystemClock.elapsedRealtime() - started

        Log.i(TAG, "Vietnamese local pipeline elapsedMs=$elapsedMs result=$result")
        assertEquals(38_000L, result?.totalAmount?.toLong())
        assertTrue("Warm local receipt pipeline took ${elapsedMs}ms", elapsedMs < 5_000L)
        releaseLocalReceiptModel()
    }

    @Test
    fun multilingualSemanticBatchSelectsTotalAndShoppingCategory() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val receipt = """
            WINMART PHIẾU TÍNH TIỀN
            VINAMILK STT có đường -7.800 81.600
            TỔNG TIỀN HÀNG -32.000 666.100
            Tiền cần thanh toán 666.100
        """.trimIndent()

        val result = LocalReceiptAnalyzer(context).analyze(receipt)

        assertEquals(666_100L, result?.totalAmount?.toLong())
        assertEquals("shopping", result?.categoryId)
        releaseLocalReceiptModel()
    }

    @Test
    fun semanticCategoriesUnderstandProductsRatherThanMatchingKeywords() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val cases = listOf(
            """
                SHOPEE
                Nội dung hàng: iPhone 8 Plus 64 GB, iPhone X mất face
                Tiền thu người nhận: 8.500.000 VND
            """.trimIndent() to "shopping",
            """
                SỦI CẢO ĐỆ NHẤT ĐÔNG BẮC
                Sủi cảo tôm 70.000 VND
                Mì bò 135.000 VND
                Trà thảo mộc 50.000 VND
                Người mua còn phải trả 255.000 VND
            """.trimIndent() to "food",
            """
                NHÀ THUỐC LONG CHÂU
                Paracetamol và bông băng y tế 85.000 VND
                Số tiền khách thực trả 85.000 VND
            """.trimIndent() to "health"
        )

        cases.forEach { (receipt, expectedCategory) ->
            val result = LocalReceiptAnalyzer(context).analyze(receipt)
            assertEquals(expectedCategory, result?.categoryId)
        }
        releaseLocalReceiptModel()
    }

    companion object {
        private const val TAG = "LocalReceiptPipelineTest"
    }
}
