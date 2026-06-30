package com.phuongnn14.tuithantai.capture

object MoneyPresenceDetector {
    private const val AUTO_FILL_AMOUNT_THRESHOLD = 0.75f

    fun detect(rawOcrText: String, sourceImageUri: String? = null): ExpenseCaptureResult? {
        VietnameseBanknoteDetector.detect(rawOcrText)?.let { banknote ->
            return ExpenseCaptureResult(
                mode = CaptureMode.MONEY_SCAN,
                transactionType = TransactionType.EXPENSE,
                amount = banknote.amount,
                productNote = banknote.note,
                merchantName = null,
                categoryName = "Khác",
                confidence = banknote.confidence,
                rawOcrText = rawOcrText,
                sourceImageUri = sourceImageUri,
                needsReview = true
            )
        }

        val amountResult = AmountExtractor.extract(rawOcrText) ?: return null
        val merchant = MerchantExtractor.extract(rawOcrText)
        val productNote = ProductNoteExtractor.extract(rawOcrText) ?: merchant
        val category = CategoryResolver.resolve(rawOcrText, productNote)
        val confidence = amountResult.confidence
        val safeAmount = amountResult.amount.takeIf { confidence >= AUTO_FILL_AMOUNT_THRESHOLD }

        return ExpenseCaptureResult(
            mode = CaptureMode.MONEY_SCAN,
            transactionType = TransactionType.EXPENSE,
            amount = safeAmount,
            productNote = productNote,
            merchantName = merchant,
            categoryName = category,
            confidence = confidence,
            rawOcrText = rawOcrText,
            sourceImageUri = sourceImageUri,
            needsReview = true
        )
    }

    fun uncertainDraft(rawOcrText: String, sourceImageUri: String? = null): ExpenseCaptureResult =
        VietnameseBanknoteDetector.detect(rawOcrText)?.let { banknote ->
            ExpenseCaptureResult(
                mode = CaptureMode.MONEY_SCAN,
                transactionType = TransactionType.EXPENSE,
                amount = banknote.amount,
                productNote = banknote.note,
                merchantName = null,
                categoryName = "Khác",
                confidence = banknote.confidence,
                rawOcrText = rawOcrText,
                sourceImageUri = sourceImageUri,
                needsReview = true
            )
        } ?:
        ProductNoteExtractor.extract(rawOcrText).let { productNote ->
        ExpenseCaptureResult(
            mode = CaptureMode.MONEY_SCAN,
            transactionType = TransactionType.EXPENSE,
            amount = null,
            productNote = productNote ?: MerchantExtractor.extract(rawOcrText),
            merchantName = MerchantExtractor.extract(rawOcrText),
            categoryName = CategoryResolver.resolve(rawOcrText, productNote),
            confidence = 0f,
            rawOcrText = rawOcrText,
            sourceImageUri = sourceImageUri,
            needsReview = true
        )
    }
}
