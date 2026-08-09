package com.phuongnn14.tuithantai.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.phuongnn14.tuithantai.BuildConfig
import com.phuongnn14.tuithantai.capture.AmountExtractor
import com.phuongnn14.tuithantai.capture.ReceiptTableInterpreter
import com.phuongnn14.tuithantai.data.CategoryEntity
import com.phuongnn14.tuithantai.ocr.DocumentType
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

/**
 * Two-stage OCR pipeline:
 *
 * STAGE 1 — ML Kit OCR + Gemini semantic reconstruction
 *   • ML Kit preserves the visible OCR text for review and builds receipt rows
 *   • Gemini receives both image pixels and a Markdown table of those rows
 *   • Final amount is selected by total-row meaning, not a confidence threshold
 *
 * STAGE 2 — ML Kit + semantic row resolver (offline fallback)
 *   • Runs when Gemini is unavailable (no network / API error / blank key)
 *   • Language-agnostic: currency-symbol detection + positional scoring
 *     + frequency-penalty (unit prices repeat, total doesn't)
 *   • Always returns a numeric amount; 0 means no monetary value was found
 */
class ExpenseAnalyzer(private val engineSelector: OcrEngineSelector? = null) {

    companion object {
        private const val TAG = "ExpenseAnalyzer"
        /** Max image side for Gemini — keeps token cost low */
        private const val GEMINI_MAX_SIDE = 768
    }

    private val ocrAnalyzer = OcrAnalyzer()
    private val imageLabelAnalyzer by lazy { ImageLabelAnalyzer() }
    private val gemini by lazy { GeminiReceiptAnalyzer(BuildConfig.GEMINI_API_KEY) }

    suspend fun analyze(
        context: Context,
        imageUri: Uri,
        @Suppress("UNUSED_PARAMETER") categories: List<CategoryEntity>
    ): ExpenseSuggestion {
        val bitmap = loadBitmap(context, imageUri)
            ?: return ExpenseSuggestion(amount = 0L, needsReview = false)

        // Read on-device OCR first so Gemini receives both image pixels and a row table.
        val selector = engineSelector ?: OcrEngineSelector(context)
        val t1 = System.currentTimeMillis()
        val engineAttempt = runCatching { selector.recognize(bitmap) }
            .onFailure { Log.e(TAG, "MLKit OCR failed: ${it.message}") }
        val engineResult = engineAttempt.getOrNull()
        val rawOcrText = engineResult?.rawText.orEmpty()

        // ── STAGE 1: Gemini Vision + reconstructed OCR table ─────────────────
        if (BuildConfig.GEMINI_API_KEY.isNotBlank()) {
            val t0 = System.currentTimeMillis()
            val geminiResult = runCatching {
                gemini.analyze(scaleBitmap(bitmap, GEMINI_MAX_SIDE), rawOcrText)
            }
                .onFailure { Log.w(TAG, "Gemini primary exception: ${it.message}") }
                .getOrNull()

            if (geminiResult != null) {
                val elapsed = System.currentTimeMillis() - t0
                Log.d(TAG, "Gemini primary OK in ${elapsed}ms: " +
                    "total=${geminiResult.totalAmount} conf=${geminiResult.confidence} " +
                    "cat=${geminiResult.categoryId} needsReview=${geminiResult.needsUserReview}")
                return ExpenseSuggestion(
                    title        = geminiResult.merchantName?.takeIf { it.isNotBlank() } ?: "",
                    amount       = ReceiptTableInterpreter.resolveFinalAmount(rawOcrText)?.amount
                        ?: geminiResult.totalAmount?.toLong()
                        ?: AmountExtractor.extract(rawOcrText)?.amount
                        ?: 0L,
                    categoryId   = geminiResult.categoryId,
                    ocrText      = rawOcrText,
                    labels       = emptyList(),
                    needsReview  = false,
                    reviewFields = emptyList(),
                    ocrEngine    = "gemini-primary",
                    ocrConfidence = geminiResult.confidence.toFloat(),
                    ocrElapsedMs  = elapsed
                )
            }
            Log.w(TAG, "Gemini primary returned null — falling back to MLKit+SmartResolver")
        } else {
            Log.d(TAG, "Gemini API key not set — using MLKit+SmartResolver directly")
        }

        // ── STAGE 2: MLKit OCR + SmartTotalResolver ───────────────────────────
        if (engineResult == null) {
            val labels = imageLabelAnalyzer.label(scaleBitmap(bitmap, GEMINI_MAX_SIDE))
            val objectSuggestion = ObjectCategoryClassifier.classify(labels)
            return ExpenseSuggestion(
                title = objectSuggestion.title,
                amount = 0L,
                categoryId = objectSuggestion.categoryId,
                ocrText = "",
                labels = labels.map { it.text },
                needsReview = false,
                reviewFields = emptyList(),
                ocrEngine = "image_labeling",
                ocrConfidence = objectSuggestion.confidence,
                ocrElapsedMs = System.currentTimeMillis() - t1
            )
        }
        val elapsed = System.currentTimeMillis() - t1
        Log.d(TAG, "MLKit OCR in ${elapsed}ms: engine=${engineResult.engineName} " +
            "lines=${engineResult.lines.size} textLen=${engineResult.rawText.length}")

        // Debug: print raw OCR lines
        engineResult.rawText.lines().forEachIndexed { i, line ->
            Log.d("OcrRawText", "[$i] $line")
        }

        val result = ocrAnalyzer.analyze(engineResult.rawText)
        Log.d(TAG, "SmartResolver: total=${result.totalAmount} conf=${result.confidence} " +
            "cat=${result.categoryId} needsReview=${result.needsUserReview}")

        val shouldLabelImage = result.categoryId == "other" ||
            result.totalAmount == null ||
            result.documentType == DocumentType.NOT_RECEIPT
        val labels = if (shouldLabelImage) {
            imageLabelAnalyzer.label(scaleBitmap(bitmap, GEMINI_MAX_SIDE))
        } else {
            emptyList()
        }
        val objectSuggestion = ObjectCategoryClassifier.classify(labels)
        val categoryId = when {
            objectSuggestion.categoryId != "other" &&
                (result.categoryId == "other" || result.totalAmount == null) ->
                objectSuggestion.categoryId
            else -> result.categoryId
        }
        val semanticAmount = ReceiptTableInterpreter.resolveFinalAmount(engineResult.rawText)?.amount
            ?: result.totalAmount?.toLong()
            ?: AmountExtractor.extract(engineResult.rawText)?.amount
            ?: 0L

        return ExpenseSuggestion(
            title        = result.merchantName
                ?: inferTitle(engineResult.rawText, labels.map { it.text })
                ?: objectSuggestion.title,
            amount       = semanticAmount,
            categoryId   = categoryId,
            ocrText      = engineResult.rawText,
            labels       = labels.map { it.text },
            needsReview  = false,
            reviewFields = emptyList(),
            ocrEngine    = engineResult.engineName,
            ocrConfidence = result.confidence.toFloat(),
            ocrElapsedMs  = elapsed
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun loadBitmap(context: Context, uri: Uri): Bitmap? =
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()

    private fun scaleBitmap(bitmap: Bitmap, maxSide: Int): Bitmap {
        val max = maxOf(bitmap.width, bitmap.height)
        if (max <= maxSide) return bitmap
        val scale = maxSide.toFloat() / max
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt(),
            (bitmap.height * scale).toInt(),
            true
        )
    }

    fun inferTitle(text: String, labels: List<String> = emptyList()): String? =
        text.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.length in 3..48 && !it.any(Char::isDigit) }
            ?: labels.firstOrNull { it.length in 3..48 }

    // ── Legacy helpers kept for unit-test compatibility ───────────────────────

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

    private fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace('đ', 'd').replace('Đ', 'D')
}
