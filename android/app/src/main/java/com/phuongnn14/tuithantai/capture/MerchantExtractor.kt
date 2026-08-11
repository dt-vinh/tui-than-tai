package com.phuongnn14.tuithantai.capture

import java.util.Locale

object MerchantExtractor {
    private val documentTokens = listOf(
        "hoa don", "invoice", "bill", "phieu tinh tien", "phieu tam tinh"
    )

    private val skipTokens = listOf(
        "hoa don", "invoice", "bill", "sdt", "phone", "mst", "ma so thue",
        "so:", "so hd", "ma hd", "gio vao", "gio in", "tai ban", "table",
        "dia chi", "address", "thanh cong", "vnd", "phieu tinh tien", "phieu tam tinh"
    )

    fun extract(rawText: String): String? {
        val lines = rawText.lineSequence()
            .map { it.trim() }
            .filter { it.length in 3..80 }
            .take(8)
            .toList()

        val beforeInvoice = lines.takeWhile {
            val norm = AmountExtractor.normalize(it)
            documentTokens.none { token -> norm.contains(token) }
        }

        val candidates = (beforeInvoice.ifEmpty { lines }).mapNotNull { line ->
            val norm = AmountExtractor.normalize(line)
            line.takeIf {
                skipTokens.none { token -> norm.contains(token) } &&
                line.count { it.isDigit() }.toFloat() / line.length < 0.35f &&
                line.any { it.isLetter() }
            }?.let(MerchantNameValidator::clean)
        }

        val best = candidates.maxByOrNull { line ->
            var score = 0
            if (line.count { it.isUpperCase() } >= line.count { it.isLowerCase() }) score += 3
            if (line.length >= 8) score += 2
            if (line.none { it.isDigit() }) score += 2
            score
        } ?: return null

        return toVietnameseTitleCase(best)
    }

    fun toVietnameseTitleCase(raw: String): String {
        return raw.lowercase(Locale("vi", "VN"))
            .split(Regex("""\s+"""))
            .joinToString(" ") { word ->
                word.replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(Locale("vi", "VN")) else ch.toString()
                }
            }
            .trim()
    }
}
