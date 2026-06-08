package com.phuongnn14.tuithantai.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.phuongnn14.tuithantai.BuildConfig
import com.phuongnn14.tuithantai.data.CategoryEntity
import com.phuongnn14.tuithantai.ocr.MoneyParser
import com.phuongnn14.tuithantai.ocr.OcrAnalyzer
import com.phuongnn14.tuithantai.ocr.OcrResult
import com.phuongnn14.tuithantai.ocr.engine.GeminiReceiptAnalyzer
import com.phuongnn14.tuithantai.ocr.engine.OcrEngineSelector
import java.text.Normalizer

data class ExpenseSuggestion(
    val title: String = "",
    val amount: Long = 0,
    val categoryId: String = "other",
    val ocrText: String = "",
    val labels: List<String> = emptyList(),
    val needsReview: Boolean = false,
    val reviewFields: List<String> = emptyList(),
    val ocrEngine: String = "",
    val ocrConfidence: Float = 0f,
    val ocrElapsedMs: Long = 0L
)

class ExpenseAnalyzer(private val engineSelector: OcrEngineSelector? = null) {
    companion object {
        // Below this local parser confidence → trigger Gemini fallback
        private const val GEMINI_FALLBACK_THRESHOLD = 0.55
    }
    private val ocrAnalyzer = OcrAnalyzer()
    private val gemini by lazy { GeminiReceiptAnalyzer(BuildConfig.GEMINI_API_KEY) }

    suspend fun analyze(
        context: Context,
        imageUri: Uri,
        @Suppress("UNUSED_PARAMETER") categories: List<CategoryEntity>
    ): ExpenseSuggestion {
        val bitmap = context.contentResolver.openInputStream(imageUri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return ExpenseSuggestion(needsReview = true, reviewFields = listOf("total_amount"))

        val selector = engineSelector ?: OcrEngineSelector(context)
        val engineResult = runCatching { selector.recognize(bitmap) }
            .getOrElse { e ->
                Log.e("ExpenseAnalyzer", "OCR failed: ${e.message}")
                return ExpenseSuggestion(needsReview = true, reviewFields = listOf("total_amount"))
            }

        Log.d(
            "OcrBenchmark",
            "engine=${engineResult.engineName} " +
            "elapsed=${engineResult.elapsedMs}ms " +
            "imgSize=${bitmap.width}x${bitmap.height} " +
            "textLen=${engineResult.rawText.length} " +
            "lines=${engineResult.lines.size} " +
            "engineConf=${engineResult.confidence}"
        )
        // Log raw OCR text để debug amount parsing
        engineResult.rawText.lines().forEachIndexed { i, line ->
            Log.d("OcrRawText", "[$i] $line")
        }

        val result = ocrAnalyzer.analyze(engineResult.rawText)

        Log.d(
            "OcrBenchmark",
            "total=${result.totalAmount} " +
            "category=${result.categoryId} " +
            "parserConf=${result.confidence} " +
            "needsReview=${result.needsUserReview} " +
            "reviewFields=${result.reviewFields}"
        )

        // Gemini fallback: when local OCR/parser is uncertain
        val finalResult = if (shouldUseGeminiFallback(result)) {
            Log.d("OcrBenchmark", "local confidence low — trying Gemini fallback")
            gemini.analyze(bitmap)?.also {
                Log.d("OcrBenchmark", "Gemini result: total=${it.totalAmount} conf=${it.confidence}")
            } ?: result
        } else result

        return ExpenseSuggestion(
            title = finalResult.merchantName ?: inferTitle(engineResult.rawText, emptyList()),
            amount = finalResult.totalAmount?.toLong() ?: 0L,
            categoryId = finalResult.categoryId,
            ocrText = engineResult.rawText,
            labels = emptyList(),
            needsReview = finalResult.needsUserReview,
            reviewFields = finalResult.reviewFields,
            ocrEngine = engineResult.engineName + if (finalResult !== result) "+gemini" else "",
            ocrConfidence = engineResult.confidence,
            ocrElapsedMs = engineResult.elapsedMs
        )
    }

    // Kept for unit-test compatibility
    fun extractAmount(text: String): Long {
        val lines = text.lines()
        var best = 0L
        for (line in lines) {
            for (token in line.split(Regex("""\s+"""))) {
                val parsed = MoneyParser.parse(token) ?: continue
                val v = parsed.amount.toLong()
                if (v > best) best = v
            }
        }
        return best
    }

    fun inferTitle(text: String, labels: List<String>): String {
        val firstLine = text.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.length in 3..48 && !it.any(Char::isDigit) }
        return firstLine ?: labels.firstOrNull() ?: ""
    }

    fun inferCategory(
        ocrText: String,
        labels: List<String>,
        categories: List<CategoryEntity>
    ): String {
        val haystack = normalize((ocrText + " " + labels.joinToString(" ")).lowercase())
        var bestId = "other"
        var bestScore = 0
        for (category in categories) {
            val keyword = normalize(category.name.trim().lowercase())
            val score = if (keyword.isNotBlank() && haystack.contains(keyword)) 1 else 0
            if (score > bestScore) {
                bestScore = score
                bestId = category.name
            }
        }
        return bestId
    }

    /**
     * Use Gemini fallback when local parser is uncertain:
     * - total_amount not found, OR
     * - parser confidence is low, OR
     * - document type is not_receipt but image likely has text (may be unusual doc)
     */
    private fun shouldUseGeminiFallback(result: OcrResult): Boolean {
        if (BuildConfig.GEMINI_API_KEY.isBlank()) return false
        return result.totalAmount == null || result.confidence < GEMINI_FALLBACK_THRESHOLD
    }

    private fun normalize(value: String): String {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace('đ', 'd')
            .replace('Đ', 'D')
    }
}
