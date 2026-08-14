package com.phuongnn14.tuithantai.ocr.local

import android.content.Context
import android.os.Build
import java.io.File
import java.security.MessageDigest

/** Bundled quantized multilingual MiniLM model and tokenizer. */
object LocalSemanticModel {
    const val MODEL_FILE_NAME = "multilingual-minilm-l12-v2-int8.onnx"
    const val MODEL_ASSET_PATH = "models/$MODEL_FILE_NAME"
    const val TOKENIZER_FILE_NAME = "multilingual-minilm-tokenizer.json"
    const val TOKENIZER_ASSET_PATH = "models/$TOKENIZER_FILE_NAME"
    const val EXPECTED_MODEL_BYTES = 118_412_398L
    const val MODEL_SHA256 = "783fea82d71a58179b830a4dbd2d58447e640609e98eedf9ffa12622d375a672"
    const val EXPECTED_TOKENIZER_BYTES = 9_081_518L
    const val TOKENIZER_SHA256 = "2c3387be76557bd40970cec13153b3bbf80407865484b209e655e5e4729076b8"

    fun isReady(context: Context): Boolean = runCatching {
        val modelSize = context.assets.openFd(MODEL_ASSET_PATH).use { it.length }
        val tokenizerSize = context.assets.open(TOKENIZER_ASSET_PATH).use { it.available().toLong() }
        modelSize == EXPECTED_MODEL_BYTES && tokenizerSize == EXPECTED_TOKENIZER_BYTES
    }.getOrDefault(false)

    internal fun supportsCurrentDevice(): Boolean = Build.SUPPORTED_ABIS.any {
        it == "arm64-v8a" || it == "armeabi-v7a" || it == "x86_64" || it == "x86"
    }

    internal fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
