package com.phuongnn14.tuithantai.ui

/**
 * MoneyCandidate – một ứng viên số tiền được tìm thấy trong OCR text.
 *
 * @param rawText     Chuỗi gốc khớp pattern (ví dụ "6,850,000 đ", "285k").
 * @param amount      Giá trị số đã chuẩn hóa (ví dụ 6850000.0, 285000.0).
 * @param currency    "VND" hoặc "USD".
 * @param sourceLine  Dòng OCR gốc chứa số tiền này.
 * @param lineIndex   Chỉ số dòng trong danh sách OCR lines.
 * @param role        Xem [MoneyRole].
 * @param confidence  Độ tin cậy 0.0–1.0.
 * @param needsReview true nếu có OCR correction hoặc confidence thấp.
 * @param reason      Danh sách lý do (dùng cho debug/UI).
 */
data class MoneyCandidate(
    val rawText: String,
    val amount: Double,
    val currency: String,
    val sourceLine: String,
    val lineIndex: Int,
    val role: String,
    val confidence: Double,
    val needsReview: Boolean,
    val reason: List<String> = emptyList()
)

/** Các vai trò (role) của số tiền trong hóa đơn. */
object MoneyRole {
    const val TOTAL_CANDIDATE = "total_candidate"  // ứng viên tổng tiền cuối cùng
    const val LINE_TOTAL      = "line_total"        // thành tiền từng dòng sản phẩm
    const val UNIT_PRICE      = "unit_price"        // đơn giá
    const val DISCOUNT        = "discount"          // giảm giá / chiết khấu
    const val TAX             = "tax"               // thuế / VAT
    const val CASH_RECEIVED   = "cash_received"     // khách đưa / tiền nhận
    const val CHANGE          = "change"            // tiền thừa / trả lại
    const val BALANCE         = "balance"           // số dư
    const val TRANSFER_AMOUNT = "transfer_amount"   // số tiền chuyển khoản
    const val FEE             = "fee"               // phí giao dịch / phí dịch vụ
    const val UNKNOWN_MONEY   = "unknown_money"     // có vẻ là tiền nhưng chưa rõ vai trò
}

/**
 * Kết quả parse của [MoneyDetector.parseMoney].
 * Dùng nội bộ để truyền thông tin parse + OCR correction flag.
 */
data class ParsedMoney(
    val amount: Double,
    val currency: String,
    val raw: String,
    val needsReview: Boolean = false
)

/**
 * Module phát hiện và chuẩn hóa số tiền từ OCR text.
 *
 * ## Workflow:
 * 1. Sửa OCR char (O→0, l→1, S→5, B→8) trong context số.
 * 2. Normalize text (bỏ dấu tiếng Việt → ASCII).
 * 3. Tìm các span tiền theo pattern (k, tr, củ, đ, $, VND, grouped...).
 * 4. Parse từng span → [ParsedMoney].
 * 5. Gán role + confidence → [MoneyCandidate].
 *
 * ## Loại bỏ false positive:
 * - Ngày (05/06/2026), giờ (17:45), phần trăm (10%), mã GD, số điện thoại.
 * - Các role riêng: khách đưa, tiền thừa, VAT, giảm giá, số dư → không chọn làm tổng.
 */
object MoneyDetector {

    // ─────────────────────────────────────────────────────────────────────────
    // OCR character correction – chỉ sửa khi kề với ký tự số/dấu
    // ─────────────────────────────────────────────────────────────────────────
    internal fun correctOcrChars(s: String): String {
        if (s.none { it == 'O' || it == 'l' || it == 'I' || it == 'S' || it == 'B' }) return s
        // Dùng StringBuilder mutable: sau khi sửa O→0 tại vị trí i,
        // vị trí i+1 sẽ thấy sb[i]='0' (digit) → cascade đúng cho chuỗi OOO → 000.
        val sb = StringBuilder(s)
        for (i in sb.indices) {
            val c = sb[i]
            val prevDig = i > 0 && (sb[i - 1].isDigit() || sb[i - 1] == ',' || sb[i - 1] == '.')
            val nextDig = i < sb.length - 1 && (sb[i + 1].isDigit() || sb[i + 1] == ',' || sb[i + 1] == '.')
            sb[i] = when {
                (c == 'O') && (prevDig || nextDig) -> '0'
                (c == 'l' || c == 'I') && (prevDig || nextDig) -> '1'
                c == 'S' && (prevDig || nextDig) -> '5'
                c == 'B' && (prevDig || nextDig) -> '8'
                else -> c
            }
        }
        return sb.toString()
    }

    private fun norm(s: String) = ReceiptOcrParser.normalize(s)

    // ─────────────────────────────────────────────────────────────────────────
    // Pattern tìm span tiền (áp dụng trên normalized text)
    // ─────────────────────────────────────────────────────────────────────────
    // Thứ tự alternation quan trọng: 1tr2 phải trước 1tr, cu pattern phải cụ thể.
    private val moneySpanPattern = Regex(
        // ── k / nghin / ngan suffix ──────────────────────────────────────────
        """[0-9]+(?:[.,][0-9]+)?\s*k\b""" +
        """|[0-9]+(?:[.,][0-9]+)?\s*(?:nghin|ngan)\b""" +
        // ── tr / trieu (1tr2 trước, rồi 1.2tr) ──────────────────────────────
        """|[0-9]+\s*tr\s*[0-9]+\b""" +
        """|[0-9]+(?:[.,][0-9]+)?\s*tr(?:ieu)?\b""" +
        // ── cu (củ) ──────────────────────────────────────────────────────────
        """|[0-9]+\s*cu\b(?:\s*[0-9]+)?""" +
        // ── VND explicit: trailing d/₫/vnd ───────────────────────────────────
        """|[0-9][0-9.,]*\s*vnd\b""" +
        """|[0-9][0-9.,]*d\b""" +
        // ── VND explicit: leading ₫ ───────────────────────────────────────────
        """|₫[0-9][0-9.,]*""" +
        // ── USD ───────────────────────────────────────────────────────────────
        """|\$[0-9][0-9.,]*""" +
        """|[0-9][0-9.,]*\s*usd\b""" +
        // ── Grouped plain (nghìn separator): 125.000 / 6,850,000 ─────────────
        """|[0-9]{1,3}(?:[.,][0-9]{3})+"""
    )

    /**
     * Tìm các chuỗi nghi là tiền trong một dòng đã normalize.
     * Input phải là kết quả của [norm] + [correctOcrChars].
     */
    fun findMoneySpans(normalizedLine: String): List<String> =
        moneySpanPattern.findAll(normalizedLine).map { it.value.trim() }.toList()

    // ─────────────────────────────────────────────────────────────────────────
    // Parse một span → ParsedMoney
    // Input: normalized span (lowercase, no diacritics, OCR corrected)
    // ─────────────────────────────────────────────────────────────────────────
    fun parseMoney(span: String, defaultCurrency: String = "VND"): ParsedMoney? {
        val n = span.trim()
        if (n.isEmpty()) return null

        // ── USD ──────────────────────────────────────────────────────────────
        if (n.contains('$') || n.contains("usd")) {
            val digits = n.replace(",", "").replace(Regex("""[^0-9.]"""), "")
            val v = digits.toDoubleOrNull() ?: return null
            return if (v > 0) ParsedMoney(v, "USD", span) else null
        }

        // ── 1tr2 format: "1tr2" → 1,200,000 ─────────────────────────────────
        val tr2 = Regex("""([0-9]+)\s*tr\s*([0-9]+)\b""").find(n)
        if (tr2 != null) {
            val main = tr2.groupValues[1].toLongOrNull() ?: return null
            val frac = tr2.groupValues[2]
            val fracVal = when (frac.length) {
                1    -> frac.toLong() * 100_000L
                2    -> frac.toLong() * 10_000L
                else -> frac.toLongOrNull() ?: 0L
            }
            val v = (main * 1_000_000L + fracVal).toDouble()
            return if (v > 0) ParsedMoney(v, "VND", span) else null
        }

        // ── tr / trieu: "1tr", "1.2tr", "1trieu" → × 1,000,000 ──────────────
        val tr = Regex("""([0-9]+(?:[.,][0-9]+)?)\s*tr(?:ieu)?\b""").find(n)
        if (tr != null) {
            val raw = tr.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return null
            val v = raw * 1_000_000.0
            return if (v > 0) ParsedMoney(v, "VND", span) else null
        }

        // ── củ/cu: "2cu", "2cu5", "2 cu 5" → × 1,000,000 ───────────────────
        val cu = Regex("""([0-9]+)\s*cu\b(?:\s*([0-9]+))?""").find(n)
        if (cu != null) {
            val main = cu.groupValues[1].toLongOrNull() ?: return null
            val frac = cu.groupValues[2].toLongOrNull() ?: 0L
            val v = (main * 1_000_000L + frac * 100_000L).toDouble()
            return if (v > 0) ParsedMoney(v, "VND", span) else null
        }

        // ── k / nghin / ngan: "285k", "10.5k", "285 nghin" → × 1,000 ────────
        val k = Regex("""([0-9]+(?:[.,][0-9]+)?)\s*(?:k|nghin|ngan)\b""").find(n)
        if (k != null) {
            val raw = k.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return null
            val v = raw * 1_000.0
            return if (v > 0) ParsedMoney(v, "VND", span) else null
        }

        // ── VND thông thường / grouped: chỉ lấy digits ───────────────────────
        val digits = n.replace(Regex("""[^0-9]"""), "")
        val v = digits.toDoubleOrNull() ?: return null
        return if (v > 0) ParsedMoney(v, defaultCurrency, span) else null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Kiểm tra false positive
    // ─────────────────────────────────────────────────────────────────────────

    /** Trả true nếu chuỗi trông như ngày (05/06/2026, 2026-06-05...). */
    fun isDateLike(s: String): Boolean =
        Regex("""\b\d{1,2}[/.\-]\d{1,2}[/.\-]\d{2,4}\b""").containsMatchIn(s)
            || Regex("""\b\d{4}[/.\-]\d{1,2}[/.\-]\d{1,2}\b""").containsMatchIn(s)

    /** Trả true nếu chuỗi chỉ là giờ (17:45, 08:30:22). */
    fun isTimeLike(s: String): Boolean =
        Regex("""\b\d{1,2}:\d{2}(?::\d{2})?\b""").containsMatchIn(s)

    /** Keyword trên dòng có mã/ID → không parse số là tiền (trừ khi có currency symbol). */
    private val codeKeywordsNorm = listOf(
        "mst", "ma so thue", "so hoa don", "ky hieu", "ma gd", "ma giao dich",
        "transaction id", "ref no", "reference", "so dien thoai", "dien thoai",
        "hotline", "tel:", "order id", "ma don hang", "so ban", "table",
        "bill no", "invoice no", "so the", "so tai khoan", "so tk",
        "id:", "no:", "sdt"
    )

    fun isCodeLine(lineNorm: String): Boolean =
        codeKeywordsNorm.any { lineNorm.contains(it) }

    // ─────────────────────────────────────────────────────────────────────────
    // Gán role theo keyword trên dòng (normalized)
    // ─────────────────────────────────────────────────────────────────────────
    private val cashReceivedNorm = listOf(
        "khach dua", "tien khach", "cash tendered", "tien nhan", "cash received", "received"
    )
    private val changeNorm = listOf(
        "tien thua", "tien tra lai", "tra lai", "change", "tien du"
    )
    private val taxNorm    = listOf("vat ", " vat", "thue gtgt", "thue ")
    private val discountNorm = listOf("giam gia", "chiet khau", "discount ", " discount", "khuyen mai")
    private val balanceNorm  = listOf("so du", "balance", "du cuoi ngay")
    private val feeNorm      = listOf("phi giao dich", "phi dich vu", "phi chuyen", "service charge", "fee")
    private val totalKeywordsNorm = listOf(
        "tong cong", "tong tien thanh toan", "grand total", "total amount",
        "tong thanh toan", "tong don hang", "phai tra", "can thanh toan",
        "amount due", "payment amount", "so tien chuyen", "so tien giao dich",
        "so tien thanh toan", "tong", "total"
    )

    fun inferRole(lineNorm: String): String = when {
        cashReceivedNorm.any { lineNorm.contains(it) } -> MoneyRole.CASH_RECEIVED
        changeNorm.any      { lineNorm.contains(it) } -> MoneyRole.CHANGE
        taxNorm.any         { lineNorm.contains(it) } -> MoneyRole.TAX
        discountNorm.any    { lineNorm.contains(it) } -> MoneyRole.DISCOUNT
        balanceNorm.any     { lineNorm.contains(it) } -> MoneyRole.BALANCE
        feeNorm.any         { lineNorm.contains(it) } -> MoneyRole.FEE
        totalKeywordsNorm.any { lineNorm.contains(it) } -> MoneyRole.TOTAL_CANDIDATE
        else -> MoneyRole.UNKNOWN_MONEY
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tính confidence dựa trên role + vị trí + keyword
    // ─────────────────────────────────────────────────────────────────────────
    fun scoreCandidate(role: String, lineNorm: String, lineIndex: Int, totalLines: Int): Double {
        var score = when (role) {
            MoneyRole.TOTAL_CANDIDATE -> 0.50
            MoneyRole.CASH_RECEIVED,
            MoneyRole.CHANGE,
            MoneyRole.TAX,
            MoneyRole.DISCOUNT,
            MoneyRole.BALANCE,
            MoneyRole.FEE            -> 0.40   // rõ vai trò, không phải total
            else                     -> 0.25
        }
        if (role == MoneyRole.TOTAL_CANDIDATE) {
            // Strong total keywords boost
            val strong = listOf(
                "tong cong", "tong tien thanh toan", "grand total",
                "phai tra", "can thanh toan", "amount due", "tong thanh toan"
            )
            if (strong.any { lineNorm.contains(it) }) score += 0.35
            // Near end of receipt
            if (totalLines > 0 && lineIndex.toDouble() / totalLines > 0.70) score += 0.15
        }
        return score.coerceIn(0.0, 1.0)
    }

    private fun buildReason(role: String, lineNorm: String, lineIndex: Int, totalLines: Int): List<String> {
        val r = mutableListOf<String>()
        when (role) {
            MoneyRole.TOTAL_CANDIDATE -> r.add("Gần keyword tổng tiền")
            MoneyRole.CASH_RECEIVED   -> r.add("Tiền khách đưa – không phải tổng cuối")
            MoneyRole.CHANGE          -> r.add("Tiền thừa/trả lại – không phải tổng cuối")
            MoneyRole.TAX             -> r.add("Thuế/VAT – không phải tổng cuối")
            MoneyRole.DISCOUNT        -> r.add("Giảm giá – không phải tổng cuối")
            MoneyRole.BALANCE         -> r.add("Số dư – không phải tổng giao dịch")
            MoneyRole.FEE             -> r.add("Phí giao dịch")
        }
        if (totalLines > 0 && lineIndex.toDouble() / totalLines > 0.75)
            r.add("Nằm gần cuối hóa đơn")
        return r
    }

    // ─────────────────────────────────────────────────────────────────────────
    // API chính: phát hiện tất cả MoneyCandidate từ danh sách OCR lines
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Phát hiện các ứng viên số tiền từ danh sách OCR lines.
     *
     * @param lines           Danh sách dòng text từ OCR (raw, chưa normalize).
     * @param defaultCurrency "VND" hoặc "USD".
     * @return Danh sách [MoneyCandidate] với role, confidence và needsReview.
     */
    fun detectMoneyCandidates(
        lines: List<String>,
        defaultCurrency: String = "VND"
    ): List<MoneyCandidate> {
        val candidates = mutableListOf<MoneyCandidate>()

        for ((idx, rawLine) in lines.withIndex()) {
            // 1. OCR correction
            val correctedLine = correctOcrChars(rawLine)
            val wasOcrCorrected = correctedLine != rawLine

            // 2. Normalize để find spans (chỉ match pattern trên normalized)
            val normLine = norm(correctedLine)

            // 3. Skip dòng chỉ là ngày/giờ
            if (isDateLike(rawLine) && rawLine.none { it.isLetter() && it.lowercaseChar() !in "thntdnr" }) continue

            // 4. Tìm money spans
            val spans = findMoneySpans(normLine)
            if (spans.isEmpty()) continue

            val lineNorm = normLine  // alias

            for (span in spans) {
                // Skip span là phần của ngày/giờ
                if (isDateLike(span) || isTimeLike(span)) continue

                // Skip nếu ngay sau span là "%"
                val afterIdx = normLine.indexOf(span) + span.length
                if (afterIdx < normLine.length && normLine[afterIdx] == '%') continue

                // Skip nếu dòng là mã/ID và span không có currency symbol
                val hasCurrencyMark = span.contains(Regex("""d\b|₫|vnd|usd|\$"""))
                if (isCodeLine(lineNorm) && !hasCurrencyMark) continue

                val parsed = parseMoney(span, defaultCurrency) ?: continue
                if (parsed.amount <= 0.0) continue

                val role       = inferRole(lineNorm)
                val confidence = scoreCandidate(role, lineNorm, idx, lines.size)
                val review     = wasOcrCorrected || parsed.needsReview || confidence < 0.65

                candidates.add(
                    MoneyCandidate(
                        rawText    = span,
                        amount     = parsed.amount,
                        currency   = parsed.currency,
                        sourceLine = rawLine,
                        lineIndex  = idx,
                        role       = role,
                        confidence = confidence,
                        needsReview = review,
                        reason     = buildReason(role, lineNorm, idx, lines.size)
                    )
                )
            }
        }
        return candidates
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Chọn tổng tiền tốt nhất
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Chọn [MoneyCandidate] có khả năng là tổng tiền cuối cùng.
     * Bỏ qua các role: cash_received, change, tax, discount, balance.
     */
    fun selectBestTotal(candidates: List<MoneyCandidate>): MoneyCandidate? {
        val excluded = setOf(
            MoneyRole.CASH_RECEIVED, MoneyRole.CHANGE,
            MoneyRole.TAX, MoneyRole.DISCOUNT, MoneyRole.BALANCE
        )
        return candidates
            .filter { it.role !in excluded }
            .maxByOrNull { it.confidence }
    }
}
