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
                "sui cao", "com", "mi", "tra", "cafe", "ca phe", "restaurant",
                "quan", "bun", "pho", "lau", "nha hang", "food", "coffee", "tea"
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
                "shopping", "sach", "book", "fashion accessory"
            )
        ),
        Rule(
            categoryName = "Hóa đơn & Dịch vụ",
            tokens = listOf("dien", "nuoc", "internet", "dien thoai", "cuoc", "bill", "utility")
        )
    )

    fun resolve(rawOcrText: String?, objectHint: String? = null): String {
        val ocr = AmountExtractor.normalize(rawOcrText.orEmpty())
        val objectText = AmountExtractor.normalize(objectHint.orEmpty())

        val ocrHit = bestMatch(ocr)
        if (ocrHit != null) return ocrHit

        return bestMatch(objectText) ?: "Khác"
    }

    private fun bestMatch(text: String): String? {
        if (text.isBlank()) return null

        var bestCategory: String? = null
        var bestScore = 0
        for (rule in rules) {
            val score = rule.tokens.count { token -> text.contains(token) }
            if (score > bestScore) {
                bestScore = score
                bestCategory = rule.categoryName
            }
        }
        return bestCategory.takeIf { bestScore > 0 }
    }
}
