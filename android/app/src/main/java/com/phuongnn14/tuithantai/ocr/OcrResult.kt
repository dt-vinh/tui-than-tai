package com.phuongnn14.tuithantai.ocr

data class OcrResult(
    val documentType: DocumentType,
    val transactionType: TransactionType,
    val merchantName: String?,
    val counterpartyName: String?,
    val categoryId: String,
    val currency: String,
    val totalAmount: Double?,
    val amountCandidates: List<AmountCandidate>,
    val items: List<LineItem>,
    val dateTime: String?,
    val paymentMethod: String?,
    val confidence: Double,
    val needsUserReview: Boolean,
    val reviewFields: List<String>,
    val reason: String
)

enum class DocumentType(val jsonValue: String) {
    POS_RECEIPT("pos_receipt"),
    TEMPORARY_RECEIPT("temporary_receipt"),
    E_INVOICE("e_invoice"),
    PAYMENT_CONFIRMATION("payment_confirmation"),
    UTILITY_BILL("utility_bill"),
    STATEMENT("statement"),
    NOT_RECEIPT("not_receipt")
}

enum class TransactionType(val jsonValue: String) {
    EXPENSE("expense"),
    INCOME("income"),
    UNKNOWN("unknown")
}

data class AmountCandidate(
    val amount: Double,
    val label: String,
    val reason: String,
    val confidence: Double
)

data class LineItem(
    val name: String,
    val quantity: Double?,
    val unitPrice: Double?,
    val lineTotal: Double?,
    val confidence: Double,
    val needsReview: Boolean,
    val rawText: String
)
