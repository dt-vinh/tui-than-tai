package com.phuongnn14.tuithantai.ocr.engine

import android.content.Context
import android.graphics.Bitmap
import android.util.Log

/**
 * Picks and runs the best available OCR engine.
 *
 * Priority:
 *   1. rapid_onnx  — if model files present (experimental)
 *   2. mlkit_latin — always available on-device (baseline)
 *   3. remote      — fallback when local confidence < threshold
 *
 * Engine can be overridden via BuildConfig.OCR_ENGINE or app settings.
 */
class OcrEngineSelector(
    private val context: Context,
    private val remoteEngine: RemoteOcrEngine = NoOpRemoteOcrEngine,
    private val forcedEngine: String? = null   // null = auto
) {
    private val mlKit = MlKitOcrEngine()
    private val rapidOcr by lazy { RapidOcrEngine(context) }

    suspend fun recognize(bitmap: Bitmap): OcrEngineResult {
        val primary = choosePrimary()
        val result = runCatching { primary.recognize(scaledBitmap(bitmap)) }
            .getOrElse { e ->
                Log.w("OcrEngineSelector", "${primary.name} failed: ${e.message}; falling back to mlkit")
                mlKit.recognize(scaledBitmap(bitmap))
            }

        // Remote fallback if local confidence is very low or no text was found
        if (result.confidence < REMOTE_FALLBACK_THRESHOLD || result.rawText.isBlank()) {
            if (remoteEngine.isAvailable()) {
                return runCatching { remoteEngine.recognize(bitmap) }
                    .getOrDefault(result)
                    .also { Log.d("OcrEngineSelector", "Used remote fallback") }
            }
        }

        return result
    }

    private fun choosePrimary(): OcrEngine = when (forcedEngine) {
        "rapid_onnx" -> rapidOcr
        "mlkit" -> mlKit
        else -> if (RapidOcrEngine.isAvailable(context)) rapidOcr else mlKit
    }

    /** Downscale if larger than MAX_SIDE to avoid OOM and slow inference. */
    private fun scaledBitmap(src: Bitmap): Bitmap {
        val max = maxOf(src.width, src.height)
        if (max <= MAX_SIDE) return src
        val scale = MAX_SIDE.toFloat() / max
        return Bitmap.createScaledBitmap(src, (src.width * scale).toInt(), (src.height * scale).toInt(), true)
    }

    companion object {
        private const val MAX_SIDE = 1600
        private const val REMOTE_FALLBACK_THRESHOLD = 0.4f
    }
}
