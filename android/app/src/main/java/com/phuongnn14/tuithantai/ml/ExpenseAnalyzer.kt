package com.phuongnn14.tuithantai.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.phuongnn14.tuithantai.capture.ReceiptLayoutReconstructor
import com.phuongnn14.tuithantai.capture.SemanticAmountSelector
import com.phuongnn14.tuithantai.data.CategoryEntity
import com.phuongnn14.tuithantai.ocr.DocumentType
import com.phuongnn14.tuithantai.ocr.MoneyParser
import com.phuongnn14.tuithantai.ocr.OcrAnalyzer
import com.phuongnn14.tuithantai.ocr.engine.OcrEngineSelector
import com.phuongnn14.tuithantai.ocr.local.LocalReceiptAnalyzer
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
 * Two-stage offline OCR pipeline:
 *
 * STAGE 1 - ML Kit OCR + deterministic row/column amount resolution
 * STAGE 2 - bundled multilingual MiniLM product/category classification.
 */
class ExpenseAnalyzer(private val engineSelector: OcrEngineSelector? = null) {

    companion object {
        private const val TAG = "ExpenseAnalyzer"
        private const val LABEL_MAX_SIDE = 768
    }

    private val ocrAnalyzer = OcrAnalyzer()
    private val imageLabelAnalyzer by lazy { ImageLabelAnalyzer() }

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
        val receiptDocument = engineResult?.let {
            ReceiptLayoutReconstructor.reconstructDocument(it.lines, it.rawText)
        }
        val reconstructedOcrText = receiptDocument?.asText().orEmpty()

        // Bundled multilingual MiniLM is primary and requires no network or translation pack.
        if (reconstructedOcrText.isNotBlank()) {
            val localStart = System.currentTimeMillis()
            val localResult = runCatching {
                LocalReceiptAnalyzer(context.applicationContext).analyze(
                    reconstructedOcrText,
                    receiptDocument
                )
            }.onFailure { Log.w(TAG, "Local MiniLM exception: ${it.message}") }.getOrNull()
            if (localResult != null) {
                val semanticElapsed = System.currentTimeMillis() - localStart
                val totalElapsed = System.currentTimeMillis() - t1
                Log.d(
                    TAG,
                    "Local MiniLM OK: ocr=${engineResult?.elapsedMs ?: 0}ms " +
                        "semantic=${semanticElapsed}ms total=${totalElapsed}ms " +
                        "amount=${localResult.totalAmount} cat=${localResult.categoryId}"
                )
                return ExpenseSuggestion(
                    title = localResult.merchantName.orEmpty(),
                    amount = SemanticAmountSelector.select(
                        semanticModelAmount = localResult.totalAmount?.toLong(),
                        reconstructedOcrText = reconstructedOcrText,
                        rawOcrText = rawOcrText,
                        receiptDocument = receiptDocument
                    ),
                    categoryId = localResult.categoryId,
                    ocrText = rawOcrText,
                    labels = emptyList(),
                    needsReview = localResult.needsUserReview,
                    reviewFields = localResult.reviewFields,
                    ocrEngine = "mlkit+minilm-multilingual-local",
                    ocrConfidence = localResult.confidence.toFloat(),
                    ocrElapsedMs = totalElapsed
                )
            }
        }

        // Final offline fallback: deterministic semantic-row resolver.
        if (engineResult == null) {
            val labels = imageLabelAnalyzer.label(scaleBitmap(bitmap, LABEL_MAX_SIDE))
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
            imageLabelAnalyzer.label(scaleBitmap(bitmap, LABEL_MAX_SIDE))
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
        val semanticAmount = SemanticAmountSelector.select(
            semanticModelAmount = null,
            reconstructedOcrText = reconstructedOcrText,
            rawOcrText = engineResult.rawText,
            receiptDocument = receiptDocument
        )

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
