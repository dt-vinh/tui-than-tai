package com.phuongnn14.tuithantai.ocr.engine

import android.graphics.Bitmap
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage

/**
 * OCR engine backed by Google ML Kit Text Recognition.
 * Works fully on-device with no internet requirement.
 * Latin/ASCII only — Vietnamese diacritics are read via latin model,
 * which handles most printed text acceptably.
 */
class MlKitOcrEngine : OcrEngine {

    override val name: String = "mlkit_multilingual"

    override suspend fun recognize(bitmap: Bitmap): OcrEngineResult {
        val start = System.currentTimeMillis()
        val image = InputImage.fromBitmap(bitmap, 0)
        val selected = MlKitMultilingualRecognizer.recognize(image)
        val result = selected.text

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
            engineName = "${name}_${selected.script}",
            rawText = result.text,
            lines = lines,
            confidence = avgConf,
            elapsedMs = elapsed
        )
    }
}
