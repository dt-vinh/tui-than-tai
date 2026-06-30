package com.phuongnn14.tuithantai.ocr

import java.text.Normalizer

/**
 * Selects the final payable total from a list of amount candidates.
 *
 * Strategy (language-agnostic, works for VI/EN/and more):
 *  1. Currency-symbol detection  (đ ₫ $ € £ ¥ ₩ …) — universal signal
 *  2. Multi-language total keyword scoring
 *  3. Positional scoring — totals appear near the bottom of receipts
 *  4. Frequency penalty — unit prices repeat; totals usually appear once
 *  5. Exclusion list (change, discount, tax, …)
 *
 * Returns both the resolved amount AND a confidence score (0..1) so callers
 * can decide whether to trust the result or escalate to Gemini / ask user.
 */
object TotalResolver {

    data class Candidate(
        val label: String,       // raw OCR line (kept for currency-symbol check)
        val amount: Double,
        val lineIndex: Int = 0,
        val totalLines: Int = 1  // needed for positional scoring
    )

    data class Resolution(val amount: Double?, val confidence: Double)

    // ── Multi-language total keywords (weight 1-10) ───────────────────────────
    private val TOTAL_KEYWORDS = listOf(
        // Vietnamese
        "tong cong"             to 10,
        "tong thanh toan"       to 10,
        "tong tien thanh toan"  to 10,
        "tong tien"             to  8,
        "tong tam tinh"         to  7,
        "thanh tien"            to  9,
        "thanh toan"            to  8,
        "tien phai tra"         to  9,
        "phai tra"              to  8,
        // English
        "grand total"           to 10,
        "amount due"            to  9,
        "total amount"          to  9,
        "total due"             to  9,
        "net total"             to  8,
        "total"                 to  7,
        // Malay / Indonesian
        "jumlah bayar"          to  9,
        "jumlah"                to  7,
        "bayar"                 to  6,
        // Thai
        "รวม"                   to  7,
        "ยอดชำระ"               to  9,
        // Japanese
        "合計"                   to  9,
        "お会計"                 to  8,
        "ご請求"                 to  8,
        // Korean
        "합계"                   to  9,
        "결제금액"               to  9,
        "총액"                   to  8,
        // Chinese (Simplified + Traditional)
        "合计"                   to  9,
        "总计"                   to  9,
        "实付"                   to  8,
        "應付"                   to  9,
        "總計"                   to  9
    )

    // ── Exclusion keywords — skip these lines when better options exist ───────
    private val EXCLUDE_KEYWORDS = setOf(
        // Vietnamese
        "tien hang", "tam tinh", "vat", "thue", "giam gia", "chiet khau",
        "khach dua", "tien thua", "tien thua tra khach", "so du", "tien mat",
        // English
        "change", "cash", "discount", "tax", "received",
        "balance", "subtotal", "unit price", "price each",
        // Common
        "tip", "gratuity", "service charge",
        "so dien thoai", "dien thoai", "di dong", "hotline", "phone", "tel",
        // Shipping labels / measurement values — never transaction amounts
        "khoi luong", "trong luong", "can nang", "kich thuoc", "tong sl",
        "sl san pham", "maximum weight", "weight", "gram", "grams"
    )

    // ── Universal currency symbols / codes ────────────────────────────────────
    private val CURRENCY_RE = Regex(
        """[đ₫$€£¥₩₹฿]|(?:VND|USD|EUR|GBP|JPY|KRW|THB|MYR|SGD|AUD|CAD|CNY|TWD|HKD)\b""",
        RegexOption.IGNORE_CASE
    )

    // ─────────────────────────────────────────────────────────────────────────

    /** Convenience: resolve amount only (for callers that don't need confidence). */
    fun resolve(candidates: List<Candidate>): Double? =
        resolveWithConfidence(candidates).amount

    /**
     * Main resolver — returns amount + confidence.
     *
     * Confidence interpretation:
     *   >= 0.85  → high, safe to use without review
     *   0.65–0.84 → medium, show to user for quick confirm
     *   < 0.65   → low, ask user or escalate to Gemini
     */
    fun resolveWithConfidence(candidates: List<Candidate>): Resolution {
        if (candidates.isEmpty()) return Resolution(null, 0.0)

        // How many times does each amount appear?  Repeated amounts = unit prices.
        val amountFreq = candidates.groupBy { it.amount }.mapValues { it.value.size }

        data class Scored(
            val candidate: Candidate,
            val keywordScore: Int,
            val hasCurrency: Boolean,
            val excluded: Boolean,
            val positionScore: Double,   // 0.0 – 3.0
            val frequencyPenalty: Int    // >0 when amount repeats
        ) {
            // Combined score (exclusion sets to –∞ effectively)
            val total: Double get() =
                (if (excluded) -1000.0 else 0.0) +
                (if (hasCurrency) 3.0 else 0.0) +
                keywordScore +
                positionScore -
                frequencyPenalty
        }

        val scored = candidates.map { c ->
            val n = norm(c.label)
            val keywordScore = TOTAL_KEYWORDS
                .filter { (kw, _) -> n.contains(kw) }
                .maxOfOrNull { (_, w) -> w } ?: 0
            val excluded    = EXCLUDE_KEYWORDS.any { n.contains(it) }
            val hasCurrency = CURRENCY_RE.containsMatchIn(c.label)
            // Lines toward the bottom of the document score higher (receipts: total is last)
            val positionScore = (c.lineIndex.toDouble() / c.totalLines.coerceAtLeast(1)) * 3.0
            val freqPenalty   = if ((amountFreq[c.amount] ?: 1) > 1) 2 else 0
            Scored(c, keywordScore, hasCurrency, excluded, positionScore, freqPenalty)
        }

        val validScored = scored.filter { !it.excluded }
        val best = (if (validScored.isNotEmpty()) validScored else scored)
            .maxWithOrNull(
                compareBy<Scored> { it.total }
                    .thenBy { it.candidate.amount }
                    .thenBy { it.candidate.lineIndex }
            )
            ?: return Resolution(null, 0.0)

        // ── Confidence ───────────────────────────────────────────────────────
        val currencyCount = validScored.count { it.hasCurrency }
        val isUnique = (amountFreq[best.candidate.amount] ?: 1) == 1
        val isLargest = best.candidate.amount == validScored.maxOfOrNull { it.candidate.amount }

        val baseConfidence = when {
            best.excluded                                -> 0.15
            best.hasCurrency && best.keywordScore >= 8  -> 0.93  // currency + strong keyword
            best.hasCurrency && best.keywordScore >= 5  -> 0.85  // currency + medium keyword
            best.hasCurrency && currencyCount == 1      -> 0.80  // only 1 currency-marked amount
            best.hasCurrency && isUnique                -> 0.72  // multiple amounts but unique
            best.hasCurrency                            -> 0.62  // multiple currency amounts
            best.keywordScore >= 8                      -> 0.70  // strong keyword, no currency
            best.keywordScore >= 5                      -> 0.58  // medium keyword
            isUnique && isLargest                       -> 0.45  // largest unique, no signals
            else                                        -> 0.35  // pure guess
        }

        // Small boost when it's clearly the largest non-repeating amount
        val confidence = if (isUnique && isLargest && !best.excluded)
            (baseConfidence + 0.04).coerceAtMost(0.95)
        else
            baseConfidence

        return Resolution(best.candidate.amount, confidence)
    }

    internal fun norm(s: String): String = Normalizer
        .normalize(s, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace('đ', 'd').replace('Đ', 'D')
        .lowercase().trim()

    // Kept for backward-compat callers that still use the old name
    internal fun normalize(s: String): String = norm(s)
}
