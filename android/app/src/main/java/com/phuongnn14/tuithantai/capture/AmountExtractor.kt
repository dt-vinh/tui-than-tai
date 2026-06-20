package com.phuongnn14.tuithantai.capture

import java.text.Normalizer

object AmountExtractor {
    private val priorityKeywords = listOf(
        "tong thanh toan", "thanh toan", "tong cong", "phai tra", "thanh tien",
        "tong tien", "total", "grand total", "amount due", "gia"
    )

    private val strongKeywords = listOf(
        "tong thanh toan", "thanh toan", "tong cong", "phai tra", "thanh tien",
        "tong tien", "total", "grand total", "amount due"
    )

    private val excludeKeywords = listOf(
        "khach dua", "tien thua", "tra lai", "giam gia", "discount", "vat",
        "thue", "mst", "ma so thue", "so hoa don", "so hd", "ma hd", "sdt",
        "dien thoai", "phone", "gio vao", "gio in", "gio ra", "table",
        "qty", "sl/tl", "so luong"
    )

    private val amountPattern = Regex(
        """(?<![A-Za-z0-9])(?:[0-9]{1,3}(?:[.,\s][0-9]{3})+|[0-9]{4,9}|[0-9]{1,3}\s*[kK])\s*(?:VND|VNĐ|vnđ|đ|d)?(?![A-Za-z0-9])"""
    )

    fun extract(rawText: String): AmountExtractionResult? {
        val lines = rawText.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
        if (lines.isEmpty()) return null

        val candidates = mutableListOf<ScoredCandidate>()
        lines.forEachIndexed { index, line ->
            val normLine = normalize(line)
            val searchText = segmentAfterPriorityKeyword(line, normLine) ?: line
            amountPattern.findAll(searchText).forEach { match ->
                val amount = parseVndAmount(match.value) ?: return@forEach
                if (amount <= 0L) return@forEach
                if (looksLikeNoise(line, match.value, amount, normLine)) return@forEach
                candidates += ScoredCandidate(
                    amount = amount,
                    sourceLine = line,
                    lineIndex = index,
                    totalLines = lines.size,
                    normLine = normLine
                )
            }
        }

        if (candidates.isEmpty()) return null
        val largest = candidates.maxOf { it.amount }
        val scored = candidates.map { it.score(largest) }
        val best = scored.maxWithOrNull(
            compareBy<ScoredCandidate> { it.score }
                .thenBy { it.amount }
                .thenBy { it.lineIndex }
        ) ?: return null

        val confidence = (best.score / 100f).coerceIn(0.35f, 0.98f)
        return AmountExtractionResult(
            amount = best.amount,
            sourceLine = best.sourceLine,
            confidence = confidence,
            reason = best.reason
        )
    }

    fun parseVndAmount(raw: String): Long? {
        var value = normalizeMoneyChars(raw)
            .replace(Regex("""(?i)\b(VND|VND|VNĐ)\b"""), "")
            .replace(Regex("""[đd]$"""), "")
            .trim()

        val isK = value.endsWith("k", ignoreCase = true)
        if (isK) value = value.dropLast(1).trim()

        value = value.replace(" ", "")
        val numeric = when {
            value.contains('.') && value.substringAfterLast('.').length == 3 -> value.replace(".", "")
            value.contains(',') && value.substringAfterLast(',').length == 3 -> value.replace(",", "")
            else -> value.replace(".", "").replace(",", "")
        }

        val parsed = numeric.toLongOrNull() ?: return null
        val amount = if (isK) parsed * 1_000L else parsed
        return amount.takeIf { it in 1..999_999_999L }
    }

    private fun segmentAfterPriorityKeyword(line: String, normLine: String): String? {
        if (normLine.contains("hoa don")) return null
        val keyword = priorityKeywords.firstOrNull { normLine.contains(it) } ?: return null
        val keywordEnd = normLine.indexOf(keyword) + keyword.length
        return line.drop(keywordEnd.coerceAtMost(line.length))
    }

    private fun looksLikeNoise(line: String, rawAmount: String, amount: Long, normLine: String): Boolean {
        if (amount < 1_000L) return true
        if (excludeKeywords.any { normLine.contains(it) }) return true
        if (isTableOrSeatLine(normLine)) return true
        if (Regex("""\d{1,2}:\d{2}""").containsMatchIn(line)) return true
        if (Regex("""\d{1,2}[/.-]\d{1,2}[/.-]\d{2,4}""").containsMatchIn(line)) return true
        if (Regex("""\d+\s*%""").containsMatchIn(line)) return true

        val digits = rawAmount.filter { it.isDigit() }
        if (digits.length >= 10 && !priorityKeywords.any { normLine.contains(it) }) return true
        return false
    }

    private fun isTableOrSeatLine(normLine: String): Boolean =
        Regex("""(^|\s)(tai\s+ban|so\s+ban|ban\s*[:#-]|\bban\b)""").containsMatchIn(normLine)

    private fun ScoredCandidate.score(largest: Long): ScoredCandidate {
        var total = 0
        val reasons = mutableListOf<String>()
        if (strongKeywords.any { normLine.contains(it) }) {
            total += 60
            reasons += "priority keyword"
        } else if (normLine.contains("gia")) {
            total += 45
            reasons += "price keyword"
        }
        val positionScore = ((lineIndex + 1).toFloat() / totalLines.coerceAtLeast(1) * 25f).toInt()
        total += positionScore
        reasons += "bottom +$positionScore"
        if (amount == largest) {
            total += 15
            reasons += "largest"
        }
        if (excludeKeywords.any { normLine.contains(it) }) {
            total -= 80
            reasons += "excluded line"
        }
        if (Regex("""\d{1,2}:\d{2}|[/.-]\d{1,2}[/.-]""").containsMatchIn(sourceLine) ||
            Regex("""\d{10,}""").containsMatchIn(sourceLine)
        ) {
            total -= 60
            reasons += "id/date/time-like"
        }
        return copy(score = total, reason = reasons.joinToString(", "))
    }

    private fun normalizeMoneyChars(raw: String): String =
        raw.replace('O', '0').replace('o', '0').trim()

    internal fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace('đ', 'd')

    private data class ScoredCandidate(
        val amount: Long,
        val sourceLine: String,
        val lineIndex: Int,
        val totalLines: Int,
        val normLine: String,
        val score: Int = 0,
        val reason: String = ""
    )
}
