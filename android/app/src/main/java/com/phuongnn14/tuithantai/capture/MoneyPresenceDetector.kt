package com.phuongnn14.tuithantai.capture

object MoneyPresenceDetector {
    private const val AUTO_FILL_AMOUNT_THRESHOLD = 0.75f

    fun detect(rawOcrText: String, sourceImageUri: String? = null): ExpenseCaptureResult? {
        val amountResult = AmountExtractor.extract(rawOcrText)
        if (hasReceiptContext(rawOcrText) && amountResult != null) {
            return amountResult.toCaptureResult(rawOcrText, sourceImageUri)
        }

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

        return amountResult?.toCaptureResult(rawOcrText, sourceImageUri)
    }

    private fun AmountExtractionResult.toCaptureResult(
        rawOcrText: String,
        sourceImageUri: String?
    ): ExpenseCaptureResult {
        val merchant = MerchantExtractor.extract(rawOcrText)
        val productNote = ProductNoteExtractor.extract(rawOcrText) ?: merchant
        val category = CategoryResolver.resolve(rawOcrText, productNote)
        val safeAmount = amount.takeIf { confidence >= AUTO_FILL_AMOUNT_THRESHOLD }

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

    private fun hasReceiptContext(rawOcrText: String): Boolean {
        val normalized = AmountExtractor.normalize(rawOcrText)
        return listOf(
            "hoa don", "thanh toan", "tong tien", "tong cong", "thanh tien",
            "don gia", "tien chiet khau", "invoice", "receipt"
        ).any { normalized.contains(it) }
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
