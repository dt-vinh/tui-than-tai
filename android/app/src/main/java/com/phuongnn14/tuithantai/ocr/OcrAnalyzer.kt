package com.phuongnn14.tuithantai.ocr

import java.text.Normalizer

/**
 * Pure logic layer: raw OCR text → OcrResult.
 * No Android dependencies — fully unit-testable.
 *
 * Philosophy (from casebook):
 *  - OCR is a suggestion, not an auto-save.
 *  - Never hallucinate: if unclear, leave blank and set needsUserReview.
 *  - Priority: total > currency > transaction_type > category > merchant > items.
 */
class OcrAnalyzer {

    fun analyze(ocrText: String): OcrResult {
        if (ocrText.isBlank()) return emptyResult("Empty OCR text")

        val lines = ocrText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val normFull = normalizeText(ocrText)

        val documentType = DocumentTypeDetector.detect(normFull)

        // Images with no monetary context (product photos, landscapes)
        if (documentType == DocumentType.NOT_RECEIPT && !hasMoney(ocrText)) {
            return emptyResult("No monetary content detected")
        }

        val currency = detectCurrency(ocrText)
        val defaultCurrency = if (currency == "USD") MoneyParser.Currency.USD else MoneyParser.Currency.VND
        val transactionType = detectTransactionType(normFull, documentType)

        // Build amount candidates from each line
        val resolverCandidates = buildCandidates(lines, defaultCurrency)
        val resolution = TotalResolver.resolveWithConfidence(resolverCandidates)
        val totalAmount = resolution.amount
        val resolverConfidence = resolution.confidence
        val candidates = resolverCandidates.map {
            AmountCandidate(amount = it.amount, label = it.label, reason = "", confidence = 0.8)
        }

        val items = when (documentType) {
            DocumentType.PAYMENT_CONFIRMATION, DocumentType.STATEMENT -> emptyList()
            else -> LineItemParser.parse(lines, currency)
        }

        val merchantName = extractMerchant(lines, normFull, documentType)
        val counterpartyName = if (documentType == DocumentType.PAYMENT_CONFIRMATION) {
            extractCounterparty(normFull)
        } else null

        val categoryId = CategoryClassifier.classify(
            items = items,
            merchantHint = merchantName,
            contextText = normFull
        )

        val reviewFields = mutableListOf<String>()
        if (totalAmount == null) reviewFields.add("total_amount")
        if (merchantName == null && documentType !in listOf(DocumentType.PAYMENT_CONFIRMATION, DocumentType.STATEMENT)) {
            reviewFields.add("merchant_name")
        }
        if (items.any { it.needsReview }) reviewFields.add("items")

        val confidence = computeConfidence(totalAmount, resolverCandidates, documentType, resolverConfidence)

        // Statements (including payroll sheets) always need user review —
        // they contain multiple records that cannot be auto-mapped to one transaction.
        val forceReview = documentType == DocumentType.STATEMENT || documentType == DocumentType.NOT_RECEIPT
        if (forceReview && "document_type" !in reviewFields) reviewFields.add("document_type")

        return OcrResult(
            documentType = documentType,
            transactionType = transactionType,
            merchantName = merchantName,
            counterpartyName = counterpartyName,
            categoryId = categoryId,
            currency = currency,
            totalAmount = totalAmount,
            amountCandidates = candidates,
            items = items,
            dateTime = extractDateTime(lines),
            paymentMethod = extractPaymentMethod(normFull),
            confidence = confidence,
            needsUserReview = forceReview ||
                confidence < OcrThresholds.NEEDS_REVIEW.toDouble() ||
                totalAmount == null ||
                reviewFields.isNotEmpty(),
            reviewFields = reviewFields,
            reason = buildReason(documentType, totalAmount, candidates)
        )
    }

    // Lines that contain reference/ID numbers, not monetary amounts
    private val ID_LINE_TOKENS = setOf(
        "ma giao dich", "transaction id", "so tham chieu", "reference",
        "ma don", "order id", "invoice number", "so hoa don",
        "bien so", "ma kh", "so dien thoai", "dien thoai", "di dong",
        "hotline", "phone", "tel", "may ban", "sdt",
        // Account / card numbers — never monetary amounts
        "tai khoan", "so tai khoan", "so the", "so tk",
        "account number", "card number",
        // Time / status lines
        "thoi gian", "trang thai", "ngan hang", "nguoi nhan", "nguoi chuyen",
        "noi dung", "tin nhan", "gio vao", "gio ra", "ngay:"
    )

    private fun buildCandidates(
        lines: List<String>,
        currency: MoneyParser.Currency
    ): List<TotalResolver.Candidate> {
        val result = mutableListOf<TotalResolver.Candidate>()
        val totalLines = lines.size.coerceAtLeast(1)
        var pendingTotalLabel: String? = null
        var pendingTotalLabelIndex = -1
        for ((index, line) in lines.withIndex()) {
            val norm = normalizeText(line)
            // Skip lines that are reference IDs, not monetary amounts
            if (ID_LINE_TOKENS.any { norm.contains(it) }) {
                pendingTotalLabel = null
                continue
            }
            val amountStr = extractAmountStringFromLine(line)
            if (amountStr == null) {
                if (isStandaloneTotalLabel(norm)) {
                    pendingTotalLabel = line
                    pendingTotalLabelIndex = index
                } else if (norm.isNotBlank()) {
                    pendingTotalLabel = null
                }
                continue
            }
            val parsed = MoneyParser.parse(amountStr, currency) ?: continue
            // Skip numbers with 11+ consecutive digits → account numbers / transaction IDs
            val rawDigits = amountStr.filter { it.isDigit() }
            if (rawDigits.length > 10 || isLikelyPhoneNumber(line, rawDigits)) {
                pendingTotalLabel = null
                continue
            }
            val candidateLabel = if (
                pendingTotalLabel != null &&
                index - pendingTotalLabelIndex in 1..2 &&
                !hasPriorityTotalKeyword(norm)
            ) {
                "$pendingTotalLabel $line"
            } else {
                line
            }
            result.add(
                TotalResolver.Candidate(
                    label = candidateLabel,
                    amount = parsed.amount,
                    lineIndex = index,
                    totalLines = totalLines
                )
            )
            pendingTotalLabel = null
        }
        return result
    }

    private fun extractAmountStringFromLine(line: String): String? {
        // Match currency-bearing numbers; require no adjacent letter (excludes IDs like FT2606050001)
        val match = Regex(
            // [đĐdkK]: MLKit thường đọc 'đ' thành ASCII 'd' — phải include cả 'd' để "9.385.662d" match full
            """(?<![A-Za-z])([$]?\s*[0-9][0-9OoО,.\s]*[0-9]\s*(?:VND|vnđ|[đĐdkK])?|[0-9]+[Kk])(?![A-Za-z0-9])"""
        ).findAll(line).lastOrNull()
        return match?.value?.trim()
    }

    private fun isStandaloneTotalLabel(normLine: String): Boolean =
        hasPriorityTotalKeyword(normLine) &&
            !normLine.contains("don gia") &&
            !normLine.contains("ten mon") &&
            !normLine.contains("so luong") &&
            !normLine.contains("sl ") &&
            normLine.length <= 40

    private fun hasPriorityTotalKeyword(normLine: String): Boolean =
        normLine.contains("tong cong") ||
            normLine.contains("tong thanh toan") ||
            normLine.contains("tong tien") ||
            normLine.contains("thanh tien") ||
            normLine.contains("amount due") ||
            normLine.contains("grand total") ||
            normLine.contains("total")

    private fun isLikelyPhoneNumber(line: String, rawDigits: String): Boolean {
        val norm = normalizeText(line)
        if (ID_LINE_TOKENS.any { norm.contains(it) }) return true
        return rawDigits.length in 9..11 && rawDigits.startsWith("0") && !hasPriorityTotalKeyword(norm)
    }

    private fun detectCurrency(text: String): String {
        val norm = normalizeText(text)
        return when {
            text.contains("$") || norm.contains(Regex("""\bUSD\b""")) -> "USD"
            else -> "VND"
        }
    }

    private fun detectTransactionType(normText: String, docType: DocumentType): TransactionType {
        if (docType == DocumentType.PAYMENT_CONFIRMATION) {
            return when {
                normText.containsAny(
                    "nhan tien thanh cong", "tien vao", "+", "da nhan", "incoming"
                ) -> TransactionType.INCOME
                else -> TransactionType.EXPENSE
            }
        }
        return when {
            normText.containsAny("nhan tien", "thu tien", "income", "tien vao") -> TransactionType.INCOME
            else -> TransactionType.EXPENSE
        }
    }

    private fun extractMerchant(
        lines: List<String>,
        normFull: String,
        docType: DocumentType
    ): String? {
        if (docType == DocumentType.PAYMENT_CONFIRMATION) return null
        // Heuristic: first 1–3 lines that are mostly text (not digits) and long enough
        for (line in lines.take(4)) {
            val trimmed = line.trim()
            if (trimmed.length < 3) continue
            val digitRatio = trimmed.count { it.isDigit() }.toDouble() / trimmed.length
            if (digitRatio < 0.3 && trimmed.length in 3..80) return trimmed
        }
        return null
    }

    private fun extractCounterparty(normText: String): String? {
        val patterns = listOf(
            Regex("""(?:ben nhan|nguoi nhan|to)\s*[:\-]?\s*([^\n]{3,40})"""),
            Regex("""(?:ben chuyen|nguoi gui|from)\s*[:\-]?\s*([^\n]{3,40})""")
        )
        return patterns.firstNotNullOfOrNull { it.find(normText)?.groupValues?.getOrNull(1)?.trim() }
    }

    private fun extractDateTime(lines: List<String>): String? {
        val dateTimeRe = Regex(
            """(\d{1,2}[/\-.]\d{1,2}[/\-.]\d{2,4})\s*(\d{1,2}:\d{2}(:\d{2})?)?"""
        )
        for (line in lines) {
            val m = dateTimeRe.find(line)
            if (m != null) return m.value.trim()
        }
        return null
    }

    private fun extractPaymentMethod(normText: String): String? = when {
        normText.containsAny("tien mat", "cash") -> "cash"
        normText.containsAny("the tin dung", "credit card") -> "credit_card"
        normText.containsAny("the ghi no", "debit") -> "debit_card"
        normText.containsAny("momo", "zalopay", "vnpay", "vi dien tu") -> "e_wallet"
        normText.containsAny("chuyen khoan", "bank transfer") -> "bank_transfer"
        else -> null
    }

    private fun hasMoney(text: String): Boolean =
        Regex("""[0-9]{3,}""").containsMatchIn(text)

    private fun computeConfidence(
        totalAmount: Double?,
        candidates: List<TotalResolver.Candidate>,
        docType: DocumentType,
        resolverConfidence: Double = 0.5
    ): Double {
        if (totalAmount == null) return 0.0
        // Start from TotalResolver's signal-based confidence
        var score = resolverConfidence
        // Penalise ambiguous doc types
        if (docType == DocumentType.NOT_RECEIPT) score -= 0.15
        if (docType == DocumentType.STATEMENT)   score -= 0.20
        return score.coerceIn(0.0, 0.95)
    }

    private fun buildReason(
        docType: DocumentType,
        totalAmount: Double?,
        candidates: List<AmountCandidate>
    ): String {
        if (totalAmount == null) return "Total amount could not be determined from ${candidates.size} candidate(s)"
        return "Resolved total from ${candidates.size} candidate(s); document type: ${docType.jsonValue}"
    }

    private fun normalizeText(s: String): String = Normalizer
        .normalize(s, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace('đ', 'd').replace('Đ', 'D')
        .lowercase()

    private fun String.containsAny(vararg tokens: String): Boolean =
        tokens.any { this.contains(it) }

    private fun emptyResult(reason: String) = OcrResult(
        documentType = DocumentType.NOT_RECEIPT,
        transactionType = TransactionType.UNKNOWN,
        merchantName = null,
        counterpartyName = null,
        categoryId = "other",
        currency = "VND",
        totalAmount = null,
        amountCandidates = emptyList(),
        items = emptyList(),
        dateTime = null,
        paymentMethod = null,
        confidence = 0.0,
        needsUserReview = true,
        reviewFields = listOf("total_amount", "merchant_name"),
        reason = reason
    )
}
