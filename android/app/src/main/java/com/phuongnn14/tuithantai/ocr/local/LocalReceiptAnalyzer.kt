package com.phuongnn14.tuithantai.ocr.local

import ai.djl.huggingface.tokenizers.Encoding
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.phuongnn14.tuithantai.capture.AmountExtractor
import com.phuongnn14.tuithantai.capture.MerchantExtractor
import com.phuongnn14.tuithantai.capture.MerchantNameValidator
import com.phuongnn14.tuithantai.capture.ProductNoteExtractor
import com.phuongnn14.tuithantai.capture.ReceiptDocument
import com.phuongnn14.tuithantai.capture.ReceiptTableInterpreter
import com.phuongnn14.tuithantai.ocr.OcrAnalyzer
import com.phuongnn14.tuithantai.ocr.OcrResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.max
import kotlin.math.sqrt

/** Uses a bundled multilingual transformer to understand OCR lines without a network call. */
class LocalReceiptAnalyzer(private val context: Context) {
    suspend fun analyze(rawOcrText: String, receiptDocument: ReceiptDocument? = null): OcrResult? {
        if (rawOcrText.isBlank()) return null
        val candidates = LocalAmountCandidates.extract(rawOcrText)
        if (candidates.isEmpty()) return null

        return withContext(Dispatchers.Default) {
            runCatching {
                if (!LocalSemanticModel.isReady(context)) return@runCatching null
                val base = OcrAnalyzer().analyze(rawOcrText)
                val merchant = MerchantNameValidator.clean(base.merchantName)
                    ?: MerchantNameValidator.clean(MerchantExtractor.extract(rawOcrText))
                val product = ProductNoteExtractor.extract(rawOcrText)
                val parsedItems = base.items.asSequence()
                    .map { it.name.trim() }
                    .filter { it.length in 2..96 }
                    .distinct()
                    .take(4)
                    .toList()
                val classificationText = buildString {
                    merchant?.let { append(it).append(". ") }
                    when {
                        product != null -> append(product)
                        parsedItems.isNotEmpty() -> append(parsedItems.joinToString(", "))
                        merchant == null -> append(compactOcr(rawOcrText))
                    }
                }
                val semantic = LocalMiniLmRuntime.analyze(
                    context.applicationContext,
                    classificationText
                )
                val structuredAmount = receiptDocument
                    ?.let(ReceiptTableInterpreter::resolveFinalAmount)
                    ?: ReceiptTableInterpreter.resolveFinalAmount(rawOcrText)
                    ?: AmountExtractor.extract(rawOcrText)
                val selectedAmount = structuredAmount?.amount?.toDouble() ?: 0.0
                val needsReview = selectedAmount <= 0.0
                base.copy(
                    merchantName = merchant,
                    categoryId = semantic.categoryId,
                    totalAmount = selectedAmount,
                    confidence = structuredAmount?.confidence?.toDouble()
                        ?: base.confidence.coerceIn(0.35, 0.90),
                    needsUserReview = needsReview,
                    reviewFields = if (needsReview) listOf("totalAmount") else emptyList(),
                    reason = structuredAmount?.reason
                        ?: "No payable row survived deterministic receipt guards"
                )
            }.onFailure { Log.w(TAG, "Local MiniLM analysis failed", it) }
                .getOrNull()
        }
    }

    companion object {
        private const val TAG = "LocalReceiptAnalyzer"
        private const val MAX_OCR_CHARS = 2_400

        internal fun compactOcr(rawOcrText: String): String {
            val lines = rawOcrText.lineSequence().map(String::trim).filter(String::isNotBlank).toList()
            return if (rawOcrText.length <= MAX_OCR_CHARS) rawOcrText.trim() else {
                (lines.take(14) + listOf("...") + lines.takeLast(38)).joinToString("\n")
                    .take(MAX_OCR_CHARS)
            }
        }
    }
}

internal data class LocalAmountCandidate(
    val amount: Double,
    val rawAmount: String,
    val sourceLine: String,
    val semanticText: String = sourceLine
)

internal object LocalAmountCandidates {
    private val token = Regex(
        """(?i)(?<![a-z0-9])(?:USD|VND|VNĐ|EUR|GBP|THB|CNY|RMB|JPY|KRW|INR)?\s*""" +
            """(?:[\$€£¥₹₩฿₫])?\s*\d{1,12}(?:(?:[.,]\d{1,3})+|(?:\s\d{3})+)?""" +
            """\s*(?:USD|VND|VNĐ|EUR|GBP|THB|CNY|RMB|JPY|KRW|INR|đ|d)?(?![a-z0-9])"""
    )
    private val dateOrTime = Regex(
        """\b(?:\d{1,2}:\d{2}(?::\d{2})?|\d{1,2}[/.-]\d{1,2}[/.-]\d{2,4})\b"""
    )
    private val measurementUnit = Regex(
        """(?i)(?:^|\s)\d[\d.,\s]*\s*(?:mg|g|kg|ml|l|cm|mm|m|km|oz|lb|%)\b"""
    )

    fun extract(text: String, limit: Int = 36): List<LocalAmountCandidate> {
        val lines = text.lineSequence().map(String::trim).filter(String::isNotBlank).toList()
        val found = mutableListOf<LocalAmountCandidate>()
        lines.forEachIndexed { lineIndex, line ->
            if (ReceiptTableInterpreter.isHardExcludedLine(line)) return@forEachIndexed
            if (lineNeedsContext(line) &&
                lines.getOrNull(lineIndex - 1)?.let(ReceiptTableInterpreter::isHardExcludedLine) == true
            ) return@forEachIndexed
            val excludedRanges = dateOrTime.findAll(line).map { it.range }.toList()
            token.findAll(line).forEach tokenLoop@{ match ->
                val raw = match.value.trim()
                val prefix = line.substring(0, match.range.first).trimEnd()
                if (prefix.endsWith('-') || prefix.endsWith('−')) return@tokenLoop
                if (excludedRanges.any { range -> match.range.first in range }) return@tokenLoop
                if (measurementUnit.containsMatchIn(line)) return@tokenLoop
                if (!looksMonetary(raw)) return@tokenLoop
                val amount = parseAmount(raw) ?: return@tokenLoop
                val semanticText = listOfNotNull(
                    lines.getOrNull(lineIndex - 1)?.takeIf { lineNeedsContext(line) },
                    line
                ).joinToString(" | ")
                found += LocalAmountCandidate(amount, raw, line, semanticText)
                if (found.size >= limit) return found
            }
        }
        return found
    }

    private fun lineNeedsContext(line: String): Boolean {
        val letters = line.count(Char::isLetter)
        return letters < 3
    }

    private fun looksMonetary(raw: String): Boolean {
        val hasCurrency = raw.contains(
            Regex("""(?i)(USD|VND|VNĐ|EUR|GBP|THB|CNY|RMB|JPY|KRW|INR|[\$€£¥₹₩฿₫đ])""")
        )
        val digits = raw.count(Char::isDigit)
        val groupedOrDecimal = raw.contains('.') || raw.contains(',') ||
            raw.contains(Regex("""\d\s\d{3}"""))
        if (!hasCurrency && !groupedOrDecimal && digits >= 9) return false
        return hasCurrency || groupedOrDecimal || digits >= 4
    }

    internal fun parseAmount(raw: String): Double? {
        val numeric = raw.filter { it.isDigit() || it == '.' || it == ',' || it == ' ' }
            .replace(" ", "")
        if (numeric.isBlank()) return null
        val separators = numeric.withIndex().filter { it.value == '.' || it.value == ',' }
        val normalized = when {
            separators.isEmpty() -> numeric
            separators.size > 1 && separators.map { it.value }.distinct().size == 1 ->
                numeric.replace(separators.first().value.toString(), "")
            separators.size > 1 -> {
                val decimalAt = separators.last().index
                val fraction = numeric.length - decimalAt - 1
                if (fraction in 1..2) {
                    numeric.substring(0, decimalAt).replace(".", "").replace(",", "") +
                        "." + numeric.substring(decimalAt + 1)
                } else numeric.replace(".", "").replace(",", "")
            }
            else -> {
                val separator = separators.single()
                val fraction = numeric.length - separator.index - 1
                if (fraction == 3) numeric.replace(separator.value.toString(), "")
                else numeric.replace(separator.value, '.')
            }
        }
        return normalized.toDoubleOrNull()?.takeIf { it > 0.0 }
    }
}

private data class SemanticSelection(
    val categoryId: String
)

private object LocalMiniLmRuntime {
    private const val EMBEDDING_SIZE = 384
    private const val PAD_TOKEN_ID = 1L
    private const val RELEASE_AFTER_MS = 30_000L
    private val categoryPrompts = linkedMapOf(
        "food" to listOf(
            "Nhà hàng, quán ăn, quán cafe; menu món được chế biến và phục vụ để dùng ngay",
            "Món ăn tại bàn: cơm, mì, phở, sủi cảo, thịt, trà và đồ uống"
        ),
        "transport" to listOf("Taxi, xe công nghệ, xăng dầu, gửi xe, vé xe và phương tiện giao thông"),
        "shopping" to listOf(
            "Siêu thị, cửa hàng bán lẻ, thực phẩm đóng gói, sữa hộp và hàng tiêu dùng",
            "Điện thoại, máy tính, đồ điện tử, quần áo, giày dép và phụ kiện"
        ),
        "bills" to listOf("Tiền điện, nước, internet, cước điện thoại và dịch vụ gia đình định kỳ"),
        "health" to listOf("Nhà thuốc, bệnh viện, phòng khám, thuốc và vật tư y tế"),
        "travel" to listOf("Khách sạn, vé máy bay, tour du lịch và chi phí chuyến đi"),
        "entertainment" to listOf("Rạp phim, karaoke, trò chơi, phòng gym và hoạt động thể thao giải trí"),
        "other" to listOf("Một khoản chi tiêu khác không thuộc các nhóm cụ thể")
    )
    private val lock = Any()
    private val handler = Handler(Looper.getMainLooper())
    private var state: RuntimeState? = null
    private val release = Runnable {
        synchronized(lock) {
            state?.close()
            state = null
        }
    }

    fun releaseNow() {
        handler.removeCallbacks(release)
        release.run()
    }

    fun analyze(
        context: Context,
        classificationText: String
    ): SemanticSelection = synchronized(lock) {
        handler.removeCallbacks(release)
        val ready = state ?: RuntimeState.create(context).also { state = it }
        try {
            val flatCategoryPrompts = categoryPrompts.flatMap { (category, prompts) ->
                prompts.map { category to it }
            }
            val texts = flatCategoryPrompts.map { it.second } + classificationText
            val vectors = ready.embed(texts)
            val receiptVector = vectors.last()
            val categoryScores = flatCategoryPrompts.mapIndexed { index, (category, _) ->
                category to dot(receiptVector, vectors[index])
            }.groupBy({ it.first }, { it.second })
                .mapValues { (_, scores) -> scores.max() }
            val category = categoryScores.maxBy { it.value }.key
            SemanticSelection(categoryId = category)
        } finally {
            handler.postDelayed(release, RELEASE_AFTER_MS)
        }
    }

    private fun dot(left: FloatArray, right: FloatArray): Float =
        left.indices.sumOf { (left[it] * right[it]).toDouble() }.toFloat()

    private fun normalize(vector: FloatArray): FloatArray {
        val length = sqrt(vector.sumOf { (it * it).toDouble() }).toFloat().coerceAtLeast(1e-12f)
        vector.indices.forEach { vector[it] /= length }
        return vector
    }

    private class RuntimeState(
        private val tokenizer: HuggingFaceTokenizer,
        private val sessionOptions: OrtSession.SessionOptions,
        private val session: OrtSession,
        @Suppress("unused") private val modelBuffer: MappedByteBuffer
    ) : AutoCloseable {
        private val environment = OrtEnvironment.getEnvironment()

        fun embed(texts: List<String>): List<FloatArray> {
            val encodings = tokenizer.batchEncode(texts.toTypedArray())
            val width = encodings.maxOf { it.ids.size }
            val inputIds = padded(encodings, width, PAD_TOKEN_ID) { it.ids }
            val attention = padded(encodings, width, 0L) { it.attentionMask }
            val tokenTypes = padded(encodings, width, 0L) { it.typeIds }
            OnnxTensor.createTensor(environment, inputIds).use { idsTensor ->
                OnnxTensor.createTensor(environment, attention).use { attentionTensor ->
                    OnnxTensor.createTensor(environment, tokenTypes).use { typeTensor ->
                        session.run(
                            mapOf(
                                "input_ids" to idsTensor,
                                "attention_mask" to attentionTensor,
                                "token_type_ids" to typeTensor
                            )
                        ).use { output ->
                            val hidden = output[0] as OnnxTensor
                            val values = hidden.floatBuffer
                            return encodings.indices.map { row ->
                                val pooled = FloatArray(EMBEDDING_SIZE)
                                var tokenCount = 0
                                for (tokenIndex in 0 until width) {
                                    if (attention[row][tokenIndex] == 0L) continue
                                    tokenCount++
                                    val offset = (row * width + tokenIndex) * EMBEDDING_SIZE
                                    for (dimension in 0 until EMBEDDING_SIZE) {
                                        pooled[dimension] += values.get(offset + dimension)
                                    }
                                }
                                val divisor = max(1, tokenCount).toFloat()
                                pooled.indices.forEach { pooled[it] /= divisor }
                                normalize(pooled)
                            }
                        }
                    }
                }
            }
        }

        override fun close() {
            session.close()
            sessionOptions.close()
            tokenizer.close()
        }

        companion object {
            fun create(context: Context): RuntimeState {
                val tokenizer = context.assets.open(LocalSemanticModel.TOKENIZER_ASSET_PATH).use {
                    HuggingFaceTokenizer.newInstance(it, emptyMap())
                }
                val descriptor = context.assets.openFd(LocalSemanticModel.MODEL_ASSET_PATH)
                val modelBuffer = descriptor.use { asset ->
                    FileInputStream(asset.fileDescriptor).channel.use { channel ->
                        channel.map(
                            FileChannel.MapMode.READ_ONLY,
                            asset.startOffset,
                            asset.declaredLength
                        )
                    }
                }
                val options = OrtSession.SessionOptions().apply {
                    setIntraOpNumThreads(4)
                    setInterOpNumThreads(1)
                }
                val session = OrtEnvironment.getEnvironment().createSession(modelBuffer, options)
                return RuntimeState(tokenizer, options, session, modelBuffer)
            }

            private fun padded(
                encodings: Array<Encoding>,
                width: Int,
                padValue: Long,
                values: (Encoding) -> LongArray
            ): Array<LongArray> = Array(encodings.size) { row ->
                LongArray(width) { padValue }.also { output ->
                    values(encodings[row]).copyInto(output)
                }
            }
        }
    }
}

internal fun releaseLocalReceiptModel() = LocalMiniLmRuntime.releaseNow()
