package com.phuongnn14.tuithantai.ocr.engine

import android.content.Context
import android.graphics.Bitmap

/**
 * Experimental OCR engine using RapidOCR-ONNX via ONNX Runtime Mobile.
 *
 * STATUS: Stub — model files not yet bundled.
 *
 * To enable this engine:
 * 1. Run scripts/download-ocr-models.ps1 to download model files.
 * 2. Place models under android/app/src/main/assets/ocr/rapidocr/:
 *      ch_PP-OCRv4_det_infer.onnx   (~2.3 MB)
 *      ch_PP-OCRv4_cls_infer.onnx   (~1.4 MB)
 *      ch_PP-OCRv4_rec_infer.onnx   (~10 MB, or Vietnamese fine-tune)
 *      ppocr_keys_v1.txt            (character dictionary)
 * 3. Add to build.gradle.kts:
 *      implementation("com.microsoft.onnxruntime:onnxruntime-android:1.17.3")
 * 4. Set OCR_ENGINE = "rapid_onnx" in BuildConfig or app settings.
 *
 * Why RapidOCR over vanilla PaddleOCR on Android:
 * - Pure ONNX Runtime — no Paddle Lite dependency.
 * - Smaller binary size.
 * - Supports ch_PP-OCRv4 models which handle Vietnamese better than v2/v3.
 *
 * Vietnamese diacritic quality notes:
 * - The base ch_PP-OCRv4 rec model reads Vietnamese partially (shares Latin chars).
 * - For best results, use a Vietnamese fine-tuned rec model.
 *   See docs/ocr/OPEN_SOURCE_OCR_MODEL_PLAN.md for details.
 *
 * Fallback: if model files are absent or inference fails, this engine
 * throws [OcrEngineUnavailableException] and the caller should fall back
 * to [MlKitOcrEngine].
 */
class RapidOcrEngine(private val context: Context) : OcrEngine {

    override val name: String = "rapid_onnx"

    override suspend fun recognize(bitmap: Bitmap): OcrEngineResult {
        // TODO: implement ONNX Runtime inference
        // Steps:
        //   1. Preprocess: resize max-side to 1280, normalize
        //   2. Run det model → bounding boxes
        //   3. Crop each box, run cls model → orientation correction
        //   4. Run rec model on each crop → text + confidence
        //   5. Sort lines top-to-bottom, left-to-right
        //   6. Build OcrEngineResult

        throw OcrEngineUnavailableException(
            "$name: model files not found. " +
            "Run scripts/download-ocr-models.ps1 and place files under assets/ocr/rapidocr/. " +
            "See docs/ocr/OPEN_SOURCE_OCR_MODEL_PLAN.md."
        )
    }

    companion object {
        private const val ASSETS_PATH = "ocr/rapidocr"
        private const val DET_MODEL = "$ASSETS_PATH/ch_PP-OCRv4_det_infer.onnx"
        private const val CLS_MODEL = "$ASSETS_PATH/ch_PP-OCRv4_cls_infer.onnx"
        private const val REC_MODEL = "$ASSETS_PATH/ch_PP-OCRv4_rec_infer.onnx"

        fun isAvailable(context: Context): Boolean = runCatching {
            context.assets.open(DET_MODEL).close()
            context.assets.open(REC_MODEL).close()
            true
        }.getOrDefault(false)
    }
}

class OcrEngineUnavailableException(message: String) : Exception(message)
