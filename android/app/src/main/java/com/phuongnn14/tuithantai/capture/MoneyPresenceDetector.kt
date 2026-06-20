package com.phuongnn14.tuithantai.capture

object MoneyPresenceDetector {
    fun detect(rawOcrText: String, sourceImageUri: String? = null): ExpenseCaptureResult? {
        val amountResult = AmountExtractor.extract(rawOcrText) ?: return null
        val merchant = MerchantExtractor.extract(rawOcrText)
        val category = CategoryResolver.resolve(rawOcrText)
        val confidence = amountResult.confidence

        return ExpenseCaptureResult(
            mode = CaptureMode.MONEY_SCAN,
            transactionType = TransactionType.EXPENSE,
            amount = amountResult.amount,
            productNote = merchant,
            merchantName = merchant,
            categoryName = category,
            confidence = confidence,
            rawOcrText = rawOcrText,
            sourceImageUri = sourceImageUri,
            needsReview = true
        )
    }

    fun uncertainDraft(rawOcrText: String, sourceImageUri: String? = null): ExpenseCaptureResult =
        ExpenseCaptureResult(
            mode = CaptureMode.MONEY_SCAN,
            transactionType = TransactionType.EXPENSE,
            amount = null,
            productNote = MerchantExtractor.extract(rawOcrText),
            merchantName = MerchantExtractor.extract(rawOcrText),
            categoryName = CategoryResolver.resolve(rawOcrText),
            confidence = 0f,
            rawOcrText = rawOcrText,
            sourceImageUri = sourceImageUri,
            needsReview = true
        )
}
