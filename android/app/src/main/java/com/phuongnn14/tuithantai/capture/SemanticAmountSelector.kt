package com.phuongnn14.tuithantai.capture

/** Gives the image-aware semantic model authority; deterministic OCR rules are fallback only. */
object SemanticAmountSelector {
    fun select(
        semanticModelAmount: Long?,
        reconstructedOcrText: String,
        rawOcrText: String = reconstructedOcrText
    ): Long = semanticModelAmount?.takeIf { it > 0L }
        ?: ReceiptTableInterpreter.resolveFinalAmount(reconstructedOcrText)?.amount
        ?: ReceiptTableInterpreter.resolveFinalAmount(rawOcrText)?.amount
        ?: AmountExtractor.extract(reconstructedOcrText)?.amount
        ?: AmountExtractor.extract(rawOcrText)?.amount
        ?: 0L
}
