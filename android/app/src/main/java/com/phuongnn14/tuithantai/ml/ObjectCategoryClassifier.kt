package com.phuongnn14.tuithantai.ml

import java.text.Normalizer

data class ObjectCategorySuggestion(
    val categoryId: String,
    val confidence: Float,
    val title: String,
    val matchedLabel: String?
)

object ObjectCategoryClassifier {
    private data class Rule(
        val categoryId: String,
        val tokens: List<String>,
        val title: String
    )

    private val rules = listOf(
        Rule(
            categoryId = "food_and_drink",
            title = "Food",
            tokens = listOf(
                "food", "dish", "meal", "bread", "bakery", "baked goods", "sandwich",
                "fast food", "restaurant", "dessert", "cake", "snack", "fruit",
                "vegetable", "drink", "beverage", "coffee", "tea", "juice", "beer"
            )
        ),
        Rule(
            categoryId = "health",
            title = "Health item",
            tokens = listOf(
                "medicine", "medical", "pharmacy", "drug", "pill", "tablet",
                "capsule", "bandage", "first aid", "health care", "hospital",
                "syringe", "thermometer", "mask"
            )
        ),
        Rule(
            categoryId = "transport",
            title = "Transport",
            tokens = listOf(
                "vehicle", "car", "bus", "train", "motorcycle", "bicycle",
                "taxi", "airplane", "boat", "scooter", "helmet", "gas station"
            )
        ),
        Rule(
            categoryId = "shopping",
            title = "Shopping item",
            tokens = listOf(
                "watch", "clock", "fashion accessory", "jewellery", "jewelry",
                "clothing", "shoe", "bag", "handbag", "wallet", "electronics",
                "mobile phone", "computer", "laptop", "keyboard", "toy",
                "cosmetics", "perfume", "personal care", "tool", "furniture"
            )
        ),
        Rule(
            categoryId = "entertainment",
            title = "Entertainment",
            tokens = listOf(
                "game", "toy", "sports", "movie", "music", "musical instrument",
                "book", "ball", "television"
            )
        ),
        Rule(
            categoryId = "bills",
            title = "Home bill",
            tokens = listOf(
                "home appliance", "kitchen appliance", "washing machine",
                "refrigerator", "air conditioner", "light fixture", "electric meter",
                "water meter", "router"
            )
        )
    )

    fun classify(labels: List<ImageLabelSignal>): ObjectCategorySuggestion {
        if (labels.isEmpty()) return unknown()

        val scores = mutableMapOf<String, Float>()
        val bestLabelByCategory = mutableMapOf<String, ImageLabelSignal>()

        for (label in labels) {
            val normalizedLabel = normalize(label.text)
            for (rule in rules) {
                val hitCount = rule.tokens.count { normalizedLabel.contains(it) }
                if (hitCount <= 0) continue

                val score = label.confidence * hitCount
                scores[rule.categoryId] = (scores[rule.categoryId] ?: 0f) + score
                val currentBest = bestLabelByCategory[rule.categoryId]
                if (currentBest == null || label.confidence > currentBest.confidence) {
                    bestLabelByCategory[rule.categoryId] = label
                }
            }
        }

        val best = scores.maxByOrNull { it.value } ?: return unknown()
        val matchedLabel = bestLabelByCategory[best.key]
        val title = rules.firstOrNull { it.categoryId == best.key }?.title
            ?: matchedLabel?.text
            ?: ""

        return ObjectCategorySuggestion(
            categoryId = best.key,
            confidence = best.value.coerceIn(0f, 1f),
            title = title,
            matchedLabel = matchedLabel?.text
        )
    }

    fun classifyLabelTexts(labels: List<String>): ObjectCategorySuggestion =
        classify(labels.map { ImageLabelSignal(text = it, confidence = 1f) })

    private fun unknown() = ObjectCategorySuggestion(
        categoryId = "other",
        confidence = 0f,
        title = "",
        matchedLabel = null
    )

    private fun normalize(value: String): String =
        Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace('đ', 'd')
}
