package com.phuongnn14.tuithantai.ocr.engine

import android.graphics.Bitmap
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/**
 * OCR engine backed by Google ML Kit Text Recognition.
 * Works fully on-device with no internet requirement.
 * Latin/ASCII only — Vietnamese diacritics are read via latin model,
 * which handles most printed text acceptably.
 */
class MlKitOcrEngine : OcrEngine {

    override val name: String = "mlkit_latin"

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    override suspend fun recognize(bitmap: Bitmap): OcrEngineResult {
        val start = System.currentTimeMillis()
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = recognizer.process(image).await()

        val lines = mutableListOf<OcrLine>()
        for (block in result.textBlocks) {
            for (line in block.lines) {
                val box = line.boundingBox?.let { b ->
                    RectF(b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat())
                }
                val conf = line.elements.mapNotNull { it.confidence }.average()
                    .takeIf { it.isFinite() }?.toFloat()
                lines.add(OcrLine(text = line.text, boundingBox = box, confidence = conf))
            }
        }

        val elapsed = System.currentTimeMillis() - start
        val avgConf = lines.mapNotNull { it.confidence }.average()
            .takeIf { it.isFinite() }?.toFloat() ?: 0.8f

        return OcrEngineResult(
            engineName = name,
            rawText = result.text,
            lines = lines,
            confidence = avgConf,
            elapsedMs = elapsed
        )
    }
}
