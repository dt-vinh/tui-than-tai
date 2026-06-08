package com.phuongnn14.tuithantai.ocr

import java.text.Normalizer

/**
 * Selects the final payable total from a list of amount candidates.
 *
 * Priority (highest first): TỔNG CỘNG, TỔNG THANH TOÁN, TỔNG TẠM TÍNH, THANH TOÁN,
 * PHẢI TRẢ, GRAND TOTAL, TOTAL, AMOUNT DUE.
 *
 * Excluded when better options exist: Tiền hàng, Tạm tính, VAT, Thuế, Giảm giá,
 * Chiết khấu, Khách đưa, Tiền thừa, Change, Cash, Received, Balance.
 */
object TotalResolver {

    data class Candidate(
        val label: String,
        val amount: Double,
        val lineIndex: Int = 0
    )

    // Ordered highest priority first — index 0 = best match
    private val PRIORITY_LABELS = listOf(
        "tong cong",
        "tong thanh toan",
        "tong tien thanh toan",
        "tong tam tinh",
        "thanh toan",
        "phai tra",
        "grand total",
        "amount due",
        "total"
    )

    private val EXCLUDE_LABELS = setOf(
        "tien hang",
        "tam tinh",
        "vat",
        "thue",
        "giam gia",
        "chiet khau",
        "khach dua",
        "tien thua",
        "tien thua tra khach",
        "change",
        "cash",
        "received",
        "balance",
        "so du",
        "tien mat"
    )

    fun resolve(candidates: List<Candidate>): Double? {
        if (candidates.isEmpty()) return null

        data class Scored(val candidate: Candidate, val priority: Int, val excluded: Boolean, val hasVndTag: Boolean)

        // Detect explicit VND/đ/₫ currency marker in the original label
        val vndTagRe = Regex("""(?:vnd|vnđ|[đd]\b|₫)""", RegexOption.IGNORE_CASE)

        val scored = candidates.map { c ->
            val norm = normalize(c.label)
            val priority = PRIORITY_LABELS.indexOfFirst { norm.contains(it) }
                .let { if (it >= 0) PRIORITY_LABELS.size - it else 0 }
            val excluded = EXCLUDE_LABELS.any { norm.contains(it) }
            val hasVndTag = vndTagRe.containsMatchIn(c.label)
            Scored(c, priority, excluded, hasVndTag)
        }

        // STAGE 0: Currency-tagged + priority keyword (e.g. "TỔNG CỘNG 2,000,000đ")
        scored.filter { it.hasVndTag && it.priority > 0 && !it.excluded }
            .maxByOrNull { it.priority }
            ?.let { return it.candidate.amount }

        // STAGE 1: Currency-tagged + non-excluded (số có đ/VND marker > số không có)
        val vndCandidates = scored.filter { it.hasVndTag && !it.excluded }
        if (vndCandidates.isNotEmpty()) {
            // Among VND-tagged: prefer priority label, else largest
            vndCandidates.filter { it.priority > 0 }.maxByOrNull { it.priority }
                ?.let { return it.candidate.amount }
            vndCandidates.maxByOrNull { it.candidate.amount }
                ?.let { return it.candidate.amount }
        }

        // STAGE 2: high-priority label, non-excluded (no currency tag required)
        scored.filter { it.priority > 0 && !it.excluded }
            .maxByOrNull { it.priority }
            ?.let { return it.candidate.amount }

        // STAGE 3: non-excluded, largest amount (likely the total)
        scored.filter { !it.excluded }
            .maxByOrNull { it.candidate.amount }
            ?.let { return it.candidate.amount }

        // Last resort: largest overall
        return candidates.maxByOrNull { it.amount }?.amount
    }

    internal fun normalize(s: String): String = Normalizer
        .normalize(s, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace('đ', 'd').replace('Đ', 'D')
        .lowercase()
        .trim()
}
