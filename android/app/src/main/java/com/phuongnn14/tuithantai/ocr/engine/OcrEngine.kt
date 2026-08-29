package com.phuongnn14.tuithantai.ocr.engine

import android.graphics.Bitmap
import android.graphics.RectF

/**
 * Abstraction over any OCR backend (ML Kit, RapidOCR-ONNX, remote, etc.).
 * The rest of the app consumes OcrEngineResult — never ML Kit classes directly.
 */
interface OcrEngine {
    val name: String

    /** Run OCR on a bitmap. Must be called from a background coroutine. */
    suspend fun recognize(bitmap: Bitmap): OcrEngineResult
}

data class OcrEngineResult(
    val engineName: String,
    val rawText: String,
    val lines: List<OcrLine>,
    /** 0.0–1.0 overall confidence from the OCR model itself */
    val confidence: Float,
    val elapsedMs: Long
)

data class OcrLine(
    val text: String,
    val boundingBox: RectF?,
    val confidence: Float?,
    val elements: List<OcrElement> = emptyList()
)

data class OcrElement(
    val text: String,
    val boundingBox: RectF?,
    val confidence: Float?
)

/** Describes pre-scan image quality hints passed to the engine. */
data class OcrImageQuality(
    val widthPx: Int,
    val heightPx: Int,
    val estimatedSkewDeg: Float = 0f,
    val estimatedBrightness: Float = 1f   // 0=dark, 1=normal
)
