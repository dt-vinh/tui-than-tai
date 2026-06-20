package com.phuongnn14.tuithantai.capture

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

class OcrService {
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun recognizeBitmap(bitmap: Bitmap): String {
        val image = InputImage.fromBitmap(bitmap, 0)
        return recognizer.process(image).await().text
    }

    suspend fun recognizeUri(context: Context, uri: Uri): String {
        val image = InputImage.fromFilePath(context, uri)
        return recognizer.process(image).await().text
    }

    @OptIn(ExperimentalGetImage::class)
    suspend fun recognizeImageProxy(imageProxy: ImageProxy): String {
        val mediaImage = imageProxy.image ?: return ""
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        return recognizer.process(image).await().text
    }
}
