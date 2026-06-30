package com.phuongnn14.tuithantai.ocr

import java.text.Normalizer

/**
 * Classifies expense category using weighted signals:
 *   line items  70%
 *   merchant    20%
 *   context     10%
 *
 * Key rule: if merchant is a sports venue but items are food/drinks,
 * return food_and_drink (mapped to "food" in app DB).
 */
object CategoryClassifier {

    // Maps OCR category token → app DB category ID
    private data class Rule(val tokens: List<String>, val categoryId: String)

    private val RULES = listOf(
        Rule(
            listOf(
                "bia", "ruou", "com", "pho", "bun", "banh mi", "banh", "nuoc uong",
                "nuoc ngot", "tra sua", "cafe", "ca phe", "do an", "do uong",
                "do nhac", "kho ca", "trung", "kem", "mut", "snack", "mon an",
                "thit", "ca", "rau", "pho mai", "mi", "bap", "thit ga", "ga ran"
            ),
            "food"
        ),
        Rule(listOf("cafe", "ca phe", "tra sua", "nuoc uong", "coffee"), "coffee"),
        Rule(
            listOf(
                "taxi", "grab", "be ", "gojek", "xang", "nhien lieu", "fuel",
                "gui xe", "do xe", "parking", "bus", "xe buyt", "metro", "xe om",
                "cuoc xe", "phi cau duong", "ve tau", "ve xe"
            ),
            "transport"
        ),
        Rule(
            listOf(
                "thue san", "gio san", "phi san", "pickleball", "cau long",
                "bong da", "boi loi", "the duc", "gym", "yoga", "tennis",
                "san bong", "san the thao"
            ),
            "entertainment"
        ),
        Rule(
            listOf(
                "sieu thi", "tien ich", "quan ao", "giay dep", "tui xach",
                "dien tu", "dien thoai", "laptop", "may tinh", "do gia dung",
                "sach", "van phong pham", "do choi", "iphone", "ipad",
                "macbook", "apple", "samsung", "oppo", "xiaomi", "shopee",
                "lazada", "tiki", "smartphone", "phu kien"
            ),
            "shopping"
        ),
        Rule(
            listOf(
                "dien ", "nuoc ", "internet", "truyen hinh", "cap quang",
                "dien thoai", "phi dich vu", "ky cuoc", "tien dien", "tien nuoc"
            ),
            "bills"
        ),
        Rule(
            listOf(
                "thuoc", "duoc", "phong kham", "benh vien", "kham benh",
                "xet nghiem", "tiem thuoc", "nha thuoc", "y te"
            ),
            "health"
        ),
        Rule(
            listOf("khach san", "hotel", "phong ", "ve may bay", "du lich", "tour"),
            "travel"
        ),
        Rule(
            listOf("karaoke", "phim", "xem phim", "game", "bar ", "vu truong", "giai tri"),
            "entertainment"
        )
    )

    fun classify(
        items: List<LineItem>,
        merchantHint: String?,
        contextText: String
    ): String {
        val itemText = items.joinToString(" ") { it.name + " " + (it.rawText) }
        val itemNorm = normalize(itemText)
        val merchantNorm = normalize(merchantHint ?: "")
        val contextNorm = normalize(contextText)

        data class Score(val categoryId: String, var score: Double)
        val scores = mutableMapOf<String, Double>()

        fun addScore(text: String, weight: Double) {
            for (rule in RULES) {
                val hits = rule.tokens.count { text.contains(it) }
                if (hits > 0) {
                    scores[rule.categoryId] = (scores[rule.categoryId] ?: 0.0) + hits * weight
                }
            }
        }

        addScore(itemNorm, 0.70)
        addScore(merchantNorm, 0.20)
        addScore(contextNorm, 0.10)

        return scores.maxByOrNull { it.value }?.key ?: "other"
    }

    internal fun normalize(s: String): String = Normalizer
        .normalize(s, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace('đ', 'd').replace('Đ', 'D')
        .lowercase()
}
