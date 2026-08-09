package com.phuongnn14.tuithantai.ocr.engine

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.phuongnn14.tuithantai.capture.ReceiptTableInterpreter
import kotlinx.coroutines.tasks.await

/** Runs the bundled ML Kit script models after capture and keeps the richest result. */
object MlKitMultilingualRecognizer {
    data class Result(val script: String, val text: Text)

    private data class ScriptRecognizer(
        val name: String,
        val recognizer: TextRecognizer,
        val nativeCharacter: (Char) -> Boolean
    )

    private val recognizers by lazy {
        listOf(
            ScriptRecognizer("latin", TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS), ::isLatin),
            ScriptRecognizer("chinese", TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()), ::isCjk),
            ScriptRecognizer("japanese", TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build()), ::isJapanese),
            ScriptRecognizer("korean", TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build()), ::isKorean),
            ScriptRecognizer("devanagari", TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build()), ::isDevanagari)
        )
    }

    suspend fun recognize(image: InputImage): Result {
        val results = recognizers.mapNotNull { model ->
            runCatching { Result(model.name, model.recognizer.process(image).await()) }.getOrNull()
        }
        return results.maxByOrNull { result ->
            val model = recognizers.first { it.name == result.script }
            qualityScore(result.text, model.nativeCharacter)
        } ?: error("No ML Kit text recognizer completed successfully")
    }

    private fun qualityScore(text: Text, nativeCharacter: (Char) -> Boolean): Int {
        val raw = text.text
        val readable = raw.count { it.isLetterOrDigit() }
        val native = raw.count(nativeCharacter)
        val lines = text.textBlocks.sumOf { it.lines.size }
        val semanticTotal = if (ReceiptTableInterpreter.resolveFinalAmount(raw) != null) 250 else 0
        return readable + native * 3 + lines * 8 + semanticTotal - raw.count { it == '\uFFFD' } * 20
    }

    private fun isLatin(char: Char): Boolean =
        char.code in 0x0041..0x024F || char.code in 0x1E00..0x1EFF

    private fun isCjk(char: Char): Boolean =
        char.code in 0x3400..0x4DBF || char.code in 0x4E00..0x9FFF

    private fun isJapanese(char: Char): Boolean =
        isCjk(char) || char.code in 0x3040..0x30FF

    private fun isKorean(char: Char): Boolean =
        char.code in 0x1100..0x11FF || char.code in 0x3130..0x318F || char.code in 0xAC00..0xD7AF

    private fun isDevanagari(char: Char): Boolean = char.code in 0x0900..0x097F
}
