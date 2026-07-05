package com.phuongnn14.tuithantai.capture

import com.phuongnn14.tuithantai.ocr.OcrThresholds

object MoneyPresenceDetector {
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
                needsReview = banknote.needsReview
            )
        }

        val amountResult = AmountExtractor.extract(rawOcrText) ?: return null
        val merchant = MerchantExtractor.extract(rawOcrText)
        val productNote = ProductNoteExtractor.extract(rawOcrText) ?: merchant
        val category = CategoryResolver.resolve(rawOcrText, productNote)
        val confidence = amountResult.confidence

        return ExpenseCaptureResult(
            mode = CaptureMode.MONEY_SCAN,
            transactionType = TransactionType.EXPENSE,
            amount = amountResult.amount,
            productNote = productNote,
            merchantName = merchant,
            categoryName = category,
            confidence = confidence,
            rawOcrText = rawOcrText,
            sourceImageUri = sourceImageUri,
            needsReview = confidence < OcrThresholds.AUTO_FILL
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
                needsReview = banknote.needsReview
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
