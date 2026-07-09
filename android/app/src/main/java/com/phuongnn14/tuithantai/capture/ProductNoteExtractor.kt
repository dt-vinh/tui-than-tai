package com.phuongnn14.tuithantai.capture

object ProductNoteExtractor {
    private val itemSectionKeywords = listOf(
        "noi dung hang", "mat hang", "san pham", "ten hang", "ten mon",
        "hang hoa", "item", "product", "description"
    )

    private val stopKeywords = listOf(
        "tien thu", "tong tien", "thanh toan", "khach dua", "tien thua",
        "khoi luong", "trong luong", "chu ky", "xac nhan", "chi dan",
        "dia chi", "sdt", "ma don", "hoa toc", "barcode", "qr",
        "kiem tra", "cam on"
    )

    private val productSignals = listOf(
        "iphone", "ipad", "macbook", "apple", "samsung", "oppo", "xiaomi",
        "dien thoai", "smartphone", "laptop", "may tinh", "dong ho",
        "watch", "quan ao", "giay", "sach", "book"
    )

    fun extract(rawText: String): String? {
        val lines = rawText.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
        if (lines.isEmpty()) return null

        lines.forEachIndexed { index, line ->
            val norm = AmountExtractor.normalize(line)
            if (itemSectionKeywords.any { norm.contains(it) }) {
                cleanLine(line.substringAfter(":", "").substringAfter(")"))?.let { return it }
                lines.drop(index + 1).take(4).forEach { next ->
                    cleanLine(next)?.let { return it }
                }
            }
        }

        return lines.firstNotNullOfOrNull { line ->
            val norm = AmountExtractor.normalize(line)
            if (productSignals.any { norm.contains(it) }) cleanLine(line) else null
        }
    }

    private fun cleanLine(raw: String): String? {
        var value = raw.trim()
            .replace(Regex("""^\d+[\).:\-]\s*"""), "")
            .replace(Regex("""\([^)]{1,16}\)"""), "")
            .replace(Regex("""(?i)\bSL\s*[:=/\-]?\s*\d+.*$"""), "")
            .replace(Regex("""(?i)\bQTY\s*[:=/\-]?\s*\d+.*$"""), "")
            .replace(Regex("""\s*,\s*"""), ", ")
            .replace(Regex("""\s+"""), " ")
            .trim(' ', ',', '-', ':')

        if (value.isBlank()) return null
        val norm = AmountExtractor.normalize(value)
        if (stopKeywords.any { norm.contains(it) }) return null
        if (!value.any { it.isLetter() }) return null
        if (value.length !in 3..96) return null
        if (value.count { it.isDigit() }.toFloat() / value.length > 0.55f) return null

        value = MerchantExtractor.toVietnameseTitleCase(value)
            .replace(Regex("""\bIphone\b"""), "iPhone")
            .replace(Regex("""\bIpad\b"""), "iPad")
            .replace(Regex("""\bMacbook\b"""), "MacBook")
            .replace(Regex("""\bGb\b"""), "GB")
            .replace(Regex("""\bUsb\b"""), "USB")

        return value.takeIf { it.isNotBlank() }
    }
}
