package com.phuongnn14.tuithantai.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.phuongnn14.tuithantai.BuildConfig
import com.phuongnn14.tuithantai.ocr.OcrThresholds
import com.phuongnn14.tuithantai.ocr.engine.GeminiReceiptAnalyzer

/**
 * Điều phối OCR theo hướng **local-first**:
 *
 * 1. Chạy MLKit local ([MoneyPresenceDetector]) trước — nhanh, offline, miễn phí.
 * 2. Nếu local đọc đủ tốt (có tổng tiền + độ tin cậy >= AUTO_FILL) → dùng luôn.
 * 3. Nếu local đọc kém (thiếu tổng tiền hoặc độ tin cậy thấp) → gọi Gemini Vision
 *    để "cứu", vốn đọc chính xác tên món + tổng tiền trên hoá đơn font khó.
 * 4. Nếu Gemini cũng thất bại (mất mạng / lỗi) → trả kết quả local tốt nhất.
 */
object CaptureOcrOrchestrator {
    private const val TAG = "CaptureOcr"
    private val gemini by lazy { GeminiReceiptAnalyzer(BuildConfig.GEMINI_API_KEY) }

    suspend fun analyze(context: Context, uri: Uri, rawText: String): ExpenseCaptureResult {
        val local = MoneyPresenceDetector.detect(rawText, uri.toString())
        val localGoodEnough = local?.amount != null &&
            local.amount > 0L &&
            local.confidence >= OcrThresholds.AUTO_FILL
        if (localGoodEnough) {
            Log.d(TAG, "Local đủ tốt (conf=${local!!.confidence}) — bỏ qua Gemini")
            return local
        }

        // Local kém → thử Gemini cứu
        if (BuildConfig.GEMINI_API_KEY.isNotBlank()) {
            val bitmap = loadBitmap(context, uri)
            if (bitmap != null) {
                val g = runCatching { gemini.analyze(bitmap) }
                    .onFailure { Log.w(TAG, "Gemini cứu thất bại: ${it.message}") }
                    .getOrNull()
                if (g?.totalAmount != null && g.totalAmount > 0) {
                    Log.d(TAG, "Gemini cứu OK: total=${g.totalAmount} merchant=${g.merchantName}")
                    val note = g.merchantName?.takeIf { it.isNotBlank() }
                        ?: g.items.firstOrNull { it.name.isNotBlank() }?.name
                    return ExpenseCaptureResult(
                        mode = CaptureMode.MONEY_SCAN,
                        transactionType = TransactionType.EXPENSE,
                        amount = g.totalAmount.toLong(),
                        productNote = note,
                        merchantName = g.merchantName,
                        categoryName = mapCategory(g.categoryId),
                        confidence = g.confidence.toFloat(),
                        rawOcrText = rawText,
                        sourceImageUri = uri.toString(),
                        needsReview = g.needsUserReview
                    )
                }
            }
        }

        // Cả hai đều kém → trả local tốt nhất (hoặc bản nháp không chắc chắn)
        return local ?: MoneyPresenceDetector.uncertainDraft(rawText, uri.toString())
    }

    private fun loadBitmap(context: Context, uri: Uri): Bitmap? =
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()

    private fun mapCategory(categoryId: String): String = when (categoryId) {
        "food_and_drink", "coffee" -> "Ăn uống"
        "transport", "travel" -> "Di chuyển"
        "shopping" -> "Mua sắm"
        "bills", "utilities" -> "Nhà ở"
        "health" -> "Y tế"
        "entertainment" -> "Giải trí"
        else -> "Khác"
    }
}
