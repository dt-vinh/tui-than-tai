package com.phuongnn14.tuithantai.ml

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.tasks.await

data class ImageLabelSignal(
    val text: String,
    val confidence: Float
)

/**
 * On-device image labels for photos that do not contain useful OCR text.
 * This keeps object-photo category suggestions available without network.
 */
class ImageLabelAnalyzer {
    private val labeler by lazy {
        ImageLabeling.getClient(
            ImageLabelerOptions.Builder()
                .setConfidenceThreshold(MIN_CONFIDENCE)
                .build()
        )
    }

    suspend fun label(bitmap: Bitmap): List<ImageLabelSignal> {
        val image = InputImage.fromBitmap(bitmap, 0)
        return runCatching {
            labeler.process(image).await()
                .map { ImageLabelSignal(text = it.text, confidence = it.confidence) }
                .sortedByDescending { it.confidence }
                .take(MAX_LABELS)
        }.onFailure {
            Log.w(TAG, "Image labeling failed: ${it.message}")
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val TAG = "ImageLabelAnalyzer"
        const val MIN_CONFIDENCE = 0.55f
        const val MAX_LABELS = 8
    }
}
