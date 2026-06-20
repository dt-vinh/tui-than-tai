package com.phuongnn14.tuithantai.capture

import android.graphics.Bitmap
import com.phuongnn14.tuithantai.ml.ImageLabelAnalyzer

interface ObjectClassifier {
    suspend fun classify(bitmap: Bitmap): ObjectClassificationResult?
}

class MlKitObjectClassifier(
    private val imageLabelAnalyzer: ImageLabelAnalyzer = ImageLabelAnalyzer()
) : ObjectClassifier {

    override suspend fun classify(bitmap: Bitmap): ObjectClassificationResult? {
        val labels = imageLabelAnalyzer.label(bitmap)
        if (labels.isEmpty()) return null

        val labelText = labels.joinToString(" ") { it.text }
        val normalized = AmountExtractor.normalize(labelText)
        val category = CategoryResolver.resolve(rawOcrText = null, objectHint = labelText)
        val note = when {
            normalized.contains("watch") || normalized.contains("clock") -> "Đồng hồ"
            normalized.contains("medicine") || normalized.contains("medical") ||
                normalized.contains("pharmacy") || normalized.contains("bandage") ->
                "Thuốc / vật tư y tế"
            labels.first().text.isNotBlank() -> labels.first().text
            else -> return null
        }

        val confidence = labels.maxOfOrNull { it.confidence } ?: 0f
        return ObjectClassificationResult(
            productNote = note,
            categoryName = category,
            confidence = confidence
        )
    }
}
