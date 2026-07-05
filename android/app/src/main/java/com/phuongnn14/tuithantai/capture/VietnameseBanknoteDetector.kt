package com.phuongnn14.tuithantai.capture

import com.phuongnn14.tuithantai.ocr.OcrThresholds

object VietnameseBanknoteDetector {
    data class Detection(
        val amount: Long,
        val confidence: Float,
        val note: String,
        val needsReview: Boolean
    )

    private data class Denomination(
        val amount: Long,
        val wordTokens: List<String>,
        val numberTokens: List<String>
    )

    private val denominations = listOf(
        Denomination(500L, listOf("nam tram dong"), listOf("500")),
        Denomination(1_000L, listOf("mot nghin dong", "mot ngan dong"), listOf("1000", "1.000", "1,000")),
        Denomination(2_000L, listOf("hai nghin dong", "hai ngan dong"), listOf("2000", "2.000", "2,000")),
        Denomination(5_000L, listOf("nam nghin dong", "nam ngan dong"), listOf("5000", "5.000", "5,000")),
        Denomination(10_000L, listOf("muoi nghin dong", "muoi ngan dong"), listOf("10000", "10.000", "10,000")),
        Denomination(20_000L, listOf("hai muoi nghin dong", "hai muoi ngan dong"), listOf("20000", "20.000", "20,000")),
        Denomination(50_000L, listOf("nam muoi nghin dong", "nam muoi ngan dong"), listOf("50000", "50.000", "50,000")),
        Denomination(100_000L, listOf("mot tram nghin dong", "mot tram ngan dong"), listOf("100000", "100.000", "100,000")),
        Denomination(200_000L, listOf("hai tram nghin dong", "hai tram ngan dong"), listOf("200000", "200.000", "200,000")),
        Denomination(500_000L, listOf("nam tram nghin dong", "nam tram ngan dong"), listOf("500000", "500.000", "500,000"))
    )

    private val banknoteMarkers = listOf(
        "cong hoa xa hoi chu nghia",
        "cong hoa xa hoi",
        "ngan hang nha nuoc",
        "chu tich ho chi minh",
        "doc lap tu do hanh phuc",
        "viet nam",
        "vietnam"
    )

    fun detect(rawText: String): Detection? {
        val lines = rawText.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
        if (lines.isEmpty()) return null

        val normalizedLines = lines.map { AmountExtractor.normalize(it) }
        val normalizedFull = normalizedLines.joinToString("\n")
        val hasBanknoteContext =
            banknoteMarkers.count { normalizedFull.contains(it) } >= 1 &&
                (normalizedFull.contains("dong") || normalizedFull.contains("vnd"))
        if (!hasBanknoteContext) return null

        val wordSegments = buildWordSegments(normalizedLines)
        val consumedSegments = BooleanArray(wordSegments.size)
        val wordHitsByAmount = mutableMapOf<Long, Int>()
        denominations.sortedByDescending { it.amount }.forEach { denom ->
            var count = 0
            wordSegments.forEachIndexed { index, segment ->
                if (!consumedSegments[index] && denom.wordTokens.any { token -> containsToken(segment, token) }) {
                    consumedSegments[index] = true
                    count += 1
                }
            }
            if (count > 0) wordHitsByAmount[denom.amount] = count
        }

        val hits = denominations.mapNotNull { denom ->
            val wordHitCount = wordHitsByAmount[denom.amount] ?: 0
            val numericHit = normalizedLines.any { line ->
                denom.numberTokens.any { token -> lineContainsStandaloneNumberToken(line, token) }
            }
            when {
                wordHitCount > 0 -> denom.amount to wordHitCount
                numericHit -> denom.amount to 1
                else -> null
            }
        }

        if (hits.isEmpty()) return null
        val total = hits.sumOf { (amount, count) -> amount * count.coerceAtLeast(1) }
        val wordHitCount = wordHitsByAmount.values.sum()
        val confidence = when {
            wordHitCount >= 3 -> 0.92f
            wordHitCount == 2 -> 0.86f
            wordHitCount == 1 -> 0.65f
            else -> 0.40f
        }

        return Detection(
            amount = total,
            confidence = confidence,
            note = "Tiền Việt Nam ${formatVnd(total)}",
            needsReview = confidence < OcrThresholds.AUTO_FILL
        )
    }

    private fun lineContainsStandaloneNumberToken(line: String, token: String): Boolean {
        val compactLine = line.replace(" ", "")
        val compactToken = token.replace(".", "").replace(",", "")
        return Regex("""(^|[^0-9A-Za-z])${Regex.escape(token)}([^0-9A-Za-z]|$)""")
            .containsMatchIn(line) ||
            Regex("""(^|[^0-9A-Za-z])${Regex.escape(compactToken)}([^0-9A-Za-z]|$)""")
                .containsMatchIn(compactLine)
    }

    private fun buildWordSegments(lines: List<String>): List<String> {
        val segments = mutableListOf<String>()
        lines.forEachIndexed { index, line ->
            segments += line
            val next = lines.getOrNull(index + 1) ?: return@forEachIndexed
            val canJoinWithDongLine =
                Regex("""(^|[^a-z0-9])(tram|nghin|ngan|muoi)([^a-z0-9]|$)""").containsMatchIn(line) &&
                    containsToken(next, "dong")
            if (canJoinWithDongLine) segments += "$line $next"
        }
        return segments
    }

    private fun containsToken(text: String, token: String): Boolean =
        Regex("""(^|[^a-z0-9])${Regex.escape(token)}([^a-z0-9]|$)""")
            .containsMatchIn(text)

    private fun formatVnd(amount: Long): String =
        amount.toString()
            .reversed()
            .chunked(3)
            .joinToString(".")
            .reversed() + " ₫"
}
