package com.phuongnn14.tuithantai.capture

enum class AmountRole {
    REMAINING_DUE,
    FINAL_PAYABLE,
    GRAND_TOTAL,
    SUBTOTAL,
    DISCOUNT,
    PAID,
    CASH_RECEIVED,
    CHANGE,
    PHONE,
    ID,
    ITEM_PRICE
}

/** Resolves payable amounts from receipt rows before any semantic model is consulted. */
object ReceiptTableInterpreter {
    private val moneyCellPattern = Regex(
        """(?<![A-Za-z0-9])[-−]?\s*(?:0(?=\s*(?:VND|VNĐ|vnđ|đ|d)\b)|[0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]{1,3}(?:\s[0-9]{3})+|[0-9]{4,12}|[0-9]{1,3}\s*[kK])\s*(?:VND|VNĐ|vnđ|đ|d)?(?![A-Za-z0-9])"""
    )
    private val dateOrTimePattern = Regex(
        """\b(?:\d{1,2}:\d{2}(?::\d{2})?|\d{1,2}[/.-]\d{1,2}[/.-]\d{2,4})\b"""
    )
    private val servicePhonePattern = Regex(
        """(?i)(?:^|\D)(?:1800|1900)[\s.:-]*\d{3,4}(?:\D|$)"""
    )
    private val genericPhonePattern = Regex(
        """(?<!\d)(?:\+?84|0(?:2\d|[35789]))(?:[\s.()-]*\d){7,9}(?!\d)"""
    )

    private val phoneLabels = listOf(
        "tong dai", "hotline", "lien he", "bao hanh", "gop y", "dien thoai", "sdt", "phone", "tel"
    )
    private val discountLabels = listOf(
        "giam gia", "chiet khau", "khuyen mai", "discount", "折扣", "优惠", "優惠",
        "値引", "割引", "할인", "छूट", "descuento", "remise", "rabatt", "desconto", "diskon", "ส่วนลด"
    )
    private val changeLabels = listOf(
        "tien thua", "tra lai", "change returned", "change", "找零", "お釣り", "거스름돈", "cambio", "monnaie", "ruckgeld", "troco", "เงินทอน"
    )
    private val cashReceivedLabels = listOf(
        "khach dua", "tien khach dua", "cash received", "cash tendered", "tendered"
    )
    private val paidLabels = listOf(
        "da thanh toan", "da tra", "tien pmh", "prepaid", "deposit", "paid amount", "amount paid"
    )
    private val idLabels = listOf(
        "ma don", "ma don hang", "ma hoa don", "ma hd", "so hoa don", "so hd", "mst",
        "ma so thue", "invoice no", "invoice number", "order id", "serial", "barcode", "qr"
    )
    private val remainingDueLabels = listOf(
        "con lai phai thu", "con phai thu", "con lai phai tra", "con phai tra", "balance due",
        "remaining due", "outstanding amount", "so tien con lai"
    )
    private val finalPayableLabels = listOf(
        "tien can thanh toan", "tong thanh toan", "so tien thanh toan", "so tien phai tra",
        "nguoi mua con phai tra", "tien thu nguoi nhan", "thuc tra", "thanh toan", "phai tra",
        "amount payable", "amount due", "total due", "应付金额", "應付金額", "实付金额", "實付金額",
        "お支払金額", "支払金額", "請求額", "お会計", "총 결제금액", "결제금액", "청구금액",
        "कुल देय", "देय राशि", "भुगतान राशि", "total a pagar", "net a payer", "montant du",
        "zu zahlen", "rechnungsbetrag", "totale da pagare", "valor total", "jumlah bayar",
        "jumlah perlu dibayar", "ยอดชำระ", "ยอดสุทธิ"
    )
    private val grandTotalLabels = listOf(
        "tong cong", "tong so tien", "tong tien", "grand total", "total", "总计", "總計", "合计", "合計",
        "総合計", "총액", "합계", "कुल राशि", "importe total", "monto total", "gesamtbetrag",
        "importo totale", "total pembayaran", "รวมทั้งสิ้น"
    )
    private val subtotalLabels = listOf(
        "tong tien hang", "thanh tien", "subtotal", "item total", "merchandise total"
    )
    private val feeLabels = listOf(
        "vat", "thue", "thu khac", "phi dich vu", "service charge", "tax"
    )
    private val documentHeaders = listOf(
        "hoa don thanh toan", "tax invoice", "invoice", "receipt", "发票", "發票",
        "領収書", "レシート", "영수증", "रसीद", "factura", "recu", "rechnung", "ใบเสร็จ"
    )

    fun resolveFinalAmount(rawText: String): AmountExtractionResult? =
        resolveRows(rawText.lineSequence().map(String::trim).filter(String::isNotBlank).toList())

    fun resolveFinalAmount(document: ReceiptDocument): AmountExtractionResult? =
        resolveRows(
            document.rows.map { it.text }.filter(String::isNotBlank)
                .ifEmpty { document.fallbackText.lines().map(String::trim).filter(String::isNotBlank) }
        )

    private fun resolveRows(lines: List<String>): AmountExtractionResult? {
        if (lines.isEmpty()) return null
        val rows = lines.mapIndexed { index, line -> analyzeRow(index, line) }
        val candidates = rows.mapNotNull { row -> candidateFor(rows, row) }

        val explicit = candidates.filter { it.role != AmountRole.SUBTOTAL }.maxWithOrNull(
            compareBy<ResolvedCandidate> { priorityOf(it.role) }.thenBy { it.lineIndex }
        )
        if (explicit != null) return explicit.toResult(validatedByArithmetic = validates(explicit, rows))

        deriveFromAdjustments(rows)?.let { return it }

        return candidates.filter { it.role == AmountRole.SUBTOTAL }
            .maxByOrNull { it.lineIndex }
            ?.toResult(validatedByArithmetic = false)
    }

    fun toMarkdown(rawText: String): String = toMarkdown(
        ReceiptDocument(
            rows = rawText.lineSequence().map(String::trim).filter(String::isNotBlank).map { row ->
                ReceiptRow(listOf(ReceiptCell(row, 0f, 0f, 0f, 0f)))
            }.toList(),
            fallbackText = rawText
        )
    )

    fun toMarkdown(document: ReceiptDocument): String {
        val rows = document.rows.ifEmpty {
            document.fallbackText.lineSequence().map(String::trim).filter(String::isNotBlank).map { row ->
                ReceiptRow(listOf(ReceiptCell(row, 0f, 0f, 0f, 0f)))
            }.toList()
        }
        if (rows.isEmpty()) return "| # | role | OCR row | money cells |\n|---:|---|---|---|"
        return buildString {
            appendLine("| # | role | OCR row | money cells |")
            appendLine("|---:|---|---|---|")
            rows.forEachIndexed { index, row ->
                val analyzed = analyzeRow(index, row.text)
                val values = analyzed.moneyCells.joinToString(", ") { it.raw.trim() }.ifBlank { "-" }
                appendLine(
                    "| ${index + 1} | ${displayRole(analyzed.role)} | ${escape(row.text)} | ${escape(values)} |"
                )
            }
        }.trimEnd()
    }

    internal fun isHardExcludedLine(line: String): Boolean {
        val role = classifyRole(AmountExtractor.normalize(line), line)
        return role in setOf(
            AmountRole.DISCOUNT,
            AmountRole.PAID,
            AmountRole.CASH_RECEIVED,
            AmountRole.CHANGE,
            AmountRole.PHONE,
            AmountRole.ID
        )
    }

    private fun analyzeRow(index: Int, line: String): AnalyzedRow {
        val normalized = AmountExtractor.normalize(line)
        return AnalyzedRow(
            index = index,
            text = line,
            normalized = normalized,
            role = classifyRole(normalized, line),
            moneyCells = moneyCells(line)
        )
    }

    private fun classifyRole(normalized: String, rawLine: String): AmountRole {
        if (matchesAny(normalized, phoneLabels) ||
            servicePhonePattern.containsMatchIn(rawLine) ||
            genericPhonePattern.containsMatchIn(rawLine)
        ) return AmountRole.PHONE
        if (matchesAny(normalized, discountLabels)) return AmountRole.DISCOUNT
        if (matchesAny(normalized, changeLabels)) return AmountRole.CHANGE
        if (matchesAny(normalized, cashReceivedLabels)) return AmountRole.CASH_RECEIVED
        if (matchesAny(normalized, paidLabels)) return AmountRole.PAID
        if (matchesAny(normalized, idLabels) || dateOrTimePattern.containsMatchIn(rawLine)) return AmountRole.ID
        if (matchesAny(normalized, remainingDueLabels)) return AmountRole.REMAINING_DUE
        if (matchesAny(normalized, documentHeaders)) return AmountRole.ID
        if (matchesAny(normalized, subtotalLabels)) return AmountRole.SUBTOTAL
        if (matchesAny(normalized, finalPayableLabels)) return AmountRole.FINAL_PAYABLE
        if (matchesAny(normalized, grandTotalLabels)) return AmountRole.GRAND_TOTAL
        return AmountRole.ITEM_PRICE
    }

    private fun candidateFor(rows: List<AnalyzedRow>, row: AnalyzedRow): ResolvedCandidate? {
        if (priorityOf(row.role) <= 0) return null
        val direct = row.moneyCells.lastOrNull { !it.negative && it.amount > 0L }
        val contextualRow = rows.drop(row.index + 1).take(2)
            .takeWhile { it.role == AmountRole.ITEM_PRICE }
            .firstOrNull { it.moneyCells.isNotEmpty() }
        val value = direct
            ?: contextualRow?.moneyCells?.lastOrNull { !it.negative && it.amount > 0L }
            ?: return null
        return ResolvedCandidate(
            amount = value.amount,
            sourceLine = row.text,
            lineIndex = row.index,
            role = row.role
        )
    }

    private fun deriveFromAdjustments(rows: List<AnalyzedRow>): AmountExtractionResult? {
        val subtotal = rows.mapNotNull { row ->
            if (row.role == AmountRole.SUBTOTAL) candidateFor(rows, row) else null
        }.maxByOrNull { it.lineIndex } ?: return null
        val discounts = adjustmentAmounts(rows, AmountRole.DISCOUNT)
        val fees = rows.filter { matchesAny(it.normalized, feeLabels) }
            .mapNotNull(::lastMeaningfulAmount)
            .distinct()
        if (discounts.isEmpty() && fees.isEmpty()) return null
        val derived = subtotal.amount - discounts.sum() + fees.sum()
        if (derived <= 0L) return null
        return AmountExtractionResult(
            amount = derived,
            sourceLine = subtotal.sourceLine,
            confidence = 0.94f,
            reason = "receipt arithmetic: subtotal ${subtotal.amount} - discounts ${discounts.sum()} + fees ${fees.sum()}"
        )
    }

    private fun validates(candidate: ResolvedCandidate, rows: List<AnalyzedRow>): Boolean {
        val subtotal = rows.mapNotNull { row ->
            if (row.role == AmountRole.SUBTOTAL || row.role == AmountRole.GRAND_TOTAL) {
                candidateFor(rows, row)
            } else null
        }.firstOrNull() ?: return false
        val discounts = adjustmentAmounts(rows, AmountRole.DISCOUNT)
        val paid = adjustmentAmounts(rows, AmountRole.PAID)
        val expected = when (candidate.role) {
            AmountRole.REMAINING_DUE -> subtotal.amount - paid.sum()
            else -> subtotal.amount - discounts.sum() + rows.filter { matchesAny(it.normalized, feeLabels) }
                .mapNotNull(::lastMeaningfulAmount).distinct().sum()
        }
        return expected > 0L && expected == candidate.amount
    }

    private fun adjustmentAmounts(rows: List<AnalyzedRow>, role: AmountRole): List<Long> =
        rows.filter { it.role == role }.mapNotNull(::lastMeaningfulAmount).distinct()

    private fun lastMeaningfulAmount(row: AnalyzedRow): Long? = row.moneyCells
        .lastOrNull { it.amount >= 1_000L }
        ?.amount

    private fun moneyCells(line: String): List<MoneyCell> = moneyCellPattern.findAll(line).mapNotNull { match ->
        val raw = match.value.trim()
        val negative = raw.startsWith('-') || raw.startsWith('−')
        val amount = AmountExtractor.parseVndAmount(raw.removePrefix("-").removePrefix("−").trim())
            ?: raw.filter(Char::isDigit).takeIf { digits ->
                digits.isNotEmpty() && digits.all { it == '0' }
            }?.let { 0L }
            ?: return@mapNotNull null
        MoneyCell(raw, amount, negative)
    }.toList()

    private fun priorityOf(role: AmountRole): Int = when (role) {
        AmountRole.REMAINING_DUE -> 500
        AmountRole.FINAL_PAYABLE -> 400
        AmountRole.GRAND_TOTAL -> 300
        AmountRole.SUBTOTAL -> 100
        else -> 0
    }

    private fun ResolvedCandidate.toResult(validatedByArithmetic: Boolean): AmountExtractionResult =
        AmountExtractionResult(
            amount = amount,
            sourceLine = sourceLine,
            confidence = when (role) {
                AmountRole.REMAINING_DUE, AmountRole.FINAL_PAYABLE -> 0.98f
                AmountRole.GRAND_TOTAL -> 0.96f
                AmountRole.SUBTOTAL -> 0.75f
                else -> 0.70f
            },
            reason = "semantic receipt role: $role" +
                if (validatedByArithmetic) "; arithmetic validated" else ""
        )

    private fun matchesAny(normalizedLine: String, labels: List<String>): Boolean =
        labels.any { label -> normalizedLine.contains(AmountExtractor.normalize(label)) }

    private fun displayRole(role: AmountRole): String = when (role) {
        AmountRole.SUBTOTAL -> "tong tien hang"
        AmountRole.ITEM_PRICE -> "item_or_value"
        else -> role.name.lowercase()
    }

    private fun escape(value: String): String = value.replace("|", "\\|").replace("\n", " ")

    private data class MoneyCell(val raw: String, val amount: Long, val negative: Boolean)

    private data class AnalyzedRow(
        val index: Int,
        val text: String,
        val normalized: String,
        val role: AmountRole,
        val moneyCells: List<MoneyCell>
    )

    private data class ResolvedCandidate(
        val amount: Long,
        val sourceLine: String,
        val lineIndex: Int,
        val role: AmountRole
    )
}
