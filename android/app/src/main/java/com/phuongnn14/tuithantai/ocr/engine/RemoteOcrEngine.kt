package com.phuongnn14.tuithantai.ocr.engine

import android.graphics.Bitmap

/**
 * Backend PC fallback OCR engine.
 *
 * Called only when local OCR confidence is below threshold (< 0.5)
 * or total_amount is null after local parsing.
 *
 * Backend endpoint: POST /ocr/receipt
 * Payload:         multipart/form-data  { image: JPEG bytes }
 * Response:        OcrEngineResult JSON
 *
 * This engine must NEVER block local save or manual entry.
 * If the backend is offline or times out, the app proceeds with
 * local result and sets needs_user_review=true.
 *
 * TODO (next sprint):
 * - Implement HTTP client call using BackendClient pattern
 * - Add backend endpoint in tui-than-tai/backend/
 * - Run PaddleOCR-v4 or Surya on backend for better Vietnamese accuracy
 */
interface RemoteOcrEngine : OcrEngine {
    /** True if the backend is reachable and the user is authenticated. */
    suspend fun isAvailable(): Boolean
}

/** Stub used when no backend is configured. Always unavailable. */
object NoOpRemoteOcrEngine : RemoteOcrEngine {
    override val name = "remote_noop"
    override suspend fun isAvailable() = false
    override suspend fun recognize(bitmap: Bitmap): OcrEngineResult {
        throw OcrEngineUnavailableException("Remote OCR not configured")
    }
}
