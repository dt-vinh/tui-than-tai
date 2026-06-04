package com.phuongnn14.tuithantai.ml

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.phuongnn14.tuithantai.data.CategoryEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.text.Normalizer

data class ExpenseSuggestion(
    val title: String = "",
    val amount: Long = 0,
    val categoryId: String = "other",
    val ocrText: String = "",
    val labels: List<String> = emptyList()
)

class ExpenseAnalyzer {
    suspend fun analyze(
        context: Context,
        imageUri: Uri,
        categories: List<CategoryEntity>
    ): ExpenseSuggestion = coroutineScope {
        val image = InputImage.fromFilePath(context, imageUri)
        val textJob = async {
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(image)
                .await()
                .text
        }
        val labelJob = async {
            ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
                .process(image)
                .await()
                .map { it.text }
        }
        val ocrText = runCatching { textJob.await() }.getOrDefault("")
        val labels = runCatching { labelJob.await() }.getOrDefault(emptyList())
        val amount = extractAmount(ocrText)
        val category = inferCategory(ocrText, labels, categories)
        ExpenseSuggestion(
            title = inferTitle(ocrText, labels),
            amount = amount,
            categoryId = category,
            ocrText = ocrText,
            labels = labels
        )
    }

    fun extractAmount(text: String): Long {
        val normalized = text.replace(',', '.')
        val candidates = Regex("""(?i)(?:total|tong|thanh\s*toan|paid|cong|vnd|đ|d)?\s*([0-9]{1,3}(?:[.\s][0-9]{3})+|[0-9]{4,9})\s*(?:vnd|đ|d)?""")
            .findAll(normalized)
            .mapNotNull { match ->
                match.groupValues.getOrNull(1)
                    ?.replace(Regex("""[.\s]"""), "")
                    ?.toLongOrNull()
            }
            .filter { it in 1_000..99_999_999 }
            .toList()
        return candidates.maxOrNull() ?: 0
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
            val score = category.keywords
                .split(',')
                .map { normalize(it.trim().lowercase()) }
                .count { it.isNotBlank() && haystack.contains(it) }
            if (score > bestScore) {
                bestScore = score
                bestId = category.id
            }
        }
        return bestId
    }

    private fun normalize(value: String): String {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace('đ', 'd')
            .replace('Đ', 'D')
    }
}
