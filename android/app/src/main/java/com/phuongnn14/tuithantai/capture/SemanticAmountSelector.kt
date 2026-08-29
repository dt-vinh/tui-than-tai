package com.phuongnn14.tuithantai.capture

/** Keeps exact receipt structure authoritative; semantic models never override payable rows. */
object SemanticAmountSelector {
    fun select(
        @Suppress("UNUSED_PARAMETER")
        semanticModelAmount: Long?,
        reconstructedOcrText: String,
        rawOcrText: String = reconstructedOcrText,
        receiptDocument: ReceiptDocument? = null
    ): Long = receiptDocument?.let(ReceiptTableInterpreter::resolveFinalAmount)?.amount
        ?: ReceiptTableInterpreter.resolveFinalAmount(reconstructedOcrText)?.amount
        ?: ReceiptTableInterpreter.resolveFinalAmount(rawOcrText)?.amount
        ?: AmountExtractor.extract(reconstructedOcrText)?.amount
        ?: AmountExtractor.extract(rawOcrText)?.amount
        ?: 0L
}
