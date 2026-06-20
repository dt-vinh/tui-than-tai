package com.phuongnn14.tuithantai.capture

enum class CaptureMode { MONEY_SCAN, OBJECT_CAPTURE }

enum class TransactionType { EXPENSE, INCOME }

data class ExpenseCaptureResult(
    val mode: CaptureMode,
    val transactionType: TransactionType,
    val amount: Long?,
    val currency: String = "VND",
    val productNote: String?,
    val merchantName: String?,
    val categoryName: String?,
    val confidence: Float,
    val rawOcrText: String? = null,
    val sourceImageUri: String? = null,
    val needsReview: Boolean = true
)

data class AmountExtractionResult(
    val amount: Long,
    val sourceLine: String,
    val confidence: Float,
    val reason: String
)

data class ObjectClassificationResult(
    val productNote: String,
    val categoryName: String,
    val confidence: Float
)
