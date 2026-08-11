package com.phuongnn14.tuithantai.capture

object CategoryResolver {
    private data class Rule(
        val categoryName: String,
        val tokens: List<String>
    )

    private val rules = listOf(
        Rule(
            categoryName = "Ăn uống",
            tokens = listOf(
                "sui cao", "com", "mi", "tra sua", "tra dao", "tra chanh",
                "tra thao moc", "cafe", "ca phe", "restaurant", "bun", "pho",
                "lau", "nha hang", "food", "coffee", "tea", "banh", "sua tuoi",
                "do uong"
            )
        ),
        Rule(
            categoryName = "Y tế",
            tokens = listOf(
                "nha thuoc", "thuoc", "pharmacy", "bong bang", "paracetamol",
                "benh vien", "clinic", "medical", "medicine", "bandage"
            )
        ),
        Rule(
            categoryName = "Di chuyển",
            tokens = listOf("grab", "taxi", "xang", "bus", "ve xe", "parking", "transport")
        ),
        Rule(
            categoryName = "Mua sắm",
            tokens = listOf(
                "dong ho", "watch", "quan ao", "clothes", "shoes", "giay",
                "shopping", "sach", "book", "fashion accessory", "shopee",
                "lazada", "tiki", "iphone", "ipad", "macbook", "apple",
                "samsung", "oppo", "xiaomi", "dien thoai", "smartphone",
                "laptop", "may tinh", "dien tu", "phu kien", "winmart",
                "sieu thi", "supermarket", "grocery"
            )
        ),
        Rule(
            categoryName = "Hóa đơn & Dịch vụ",
            tokens = listOf("dien", "nuoc", "internet", "dien thoai", "cuoc", "bill", "utility")
        )
    )

    private val retailContextTokens = listOf(
        "winmart", "sieu thi", "supermarket", "grocery", "convenience store"
    )

    fun resolve(rawOcrText: String?, objectHint: String? = null): String {
        val ocr = normalizeCategoryText(rawOcrText.orEmpty())
        val objectText = AmountExtractor.normalize(objectHint.orEmpty())
        if (retailContextTokens.any { containsToken(ocr, it) }) return "Mua sắm"

        return bestMatch(
            listOf(
                WeightedText(objectText, weight = 3),
                WeightedText(ocr, weight = 1)
            )
        ) ?: "Khác"
    }

    private fun normalizeCategoryText(rawText: String): String = rawText.lineSequence()
        .filterNot { line ->
            val normalized = AmountExtractor.normalize(line)
            normalized.contains("grab_online") ||
                (normalized.contains("thanh toan") && normalized.contains("grab"))
        }
        .joinToString(" ") { AmountExtractor.normalize(it) }

    private fun bestMatch(parts: List<WeightedText>): String? {
        if (parts.all { it.text.isBlank() }) return null

        var bestCategory: String? = null
        var bestScore = 0
        for (rule in rules) {
            val score = parts.sumOf { part ->
                rule.tokens.count { token -> containsToken(part.text, token) } * part.weight
            }
            if (score > bestScore) {
                bestScore = score
                bestCategory = rule.categoryName
            }
        }
        return bestCategory.takeIf { bestScore > 0 }
    }

    private fun containsToken(text: String, token: String): Boolean {
        if (text.isBlank()) return false
        if (token.contains(" ")) return text.contains(token)
        return Regex("""(^|[^a-z0-9])${Regex.escape(token)}([^a-z0-9]|$)""")
            .containsMatchIn(text)
    }

    private data class WeightedText(val text: String, val weight: Int)
}
