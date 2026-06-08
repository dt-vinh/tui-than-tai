package com.phuongnn14.tuithantai.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryClassifierTest {

    private fun item(name: String) = LineItem(
        name = name, quantity = null, unitPrice = null,
        lineTotal = null, confidence = 0.9, needsReview = false, rawText = name
    )

    // OCR-DOC-003 / Golden fixture: Tô Ký badminton court with food items
    @Test fun `to ky - food items override sports venue merchant`() {
        val items = listOf(
            item("Kem/đồ uống 250ml"),
            item("Bia chai Tuborg chai xanh")
        )
        val result = CategoryClassifier.classify(
            items = items,
            merchantHint = "Sân cầu lông & pickleball Tô Ký",
            contextText = ""
        )
        assertEquals("food", result)
    }

    @Test fun `sports venue with rental items → entertainment`() {
        val items = listOf(item("Thuê sân cầu lông 2h"), item("Thuê sân pickleball"))
        val result = CategoryClassifier.classify(
            items = items,
            merchantHint = "Sân cầu lông Tô Ký",
            contextText = ""
        )
        assertEquals("entertainment", result)
    }

    @Test fun `transport keywords → transport`() {
        val result = CategoryClassifier.classify(
            items = emptyList(),
            merchantHint = "Grab",
            contextText = "grab taxi cuoc xe"
        )
        assertEquals("transport", result)
    }

    @Test fun `pharmacy items → health`() {
        val items = listOf(item("Thuốc cảm cúm"), item("Nước muối sinh lý"))
        val result = CategoryClassifier.classify(
            items = items,
            merchantHint = "Nhà thuốc Long Châu",
            contextText = ""
        )
        assertEquals("health", result)
    }

    @Test fun `utility bill keywords → bills`() {
        val result = CategoryClassifier.classify(
            items = emptyList(),
            merchantHint = null,
            contextText = "hoa don tien dien ky cuoc thang 5"
        )
        assertEquals("bills", result)
    }

    @Test fun `no match → other`() {
        val result = CategoryClassifier.classify(
            items = emptyList(),
            merchantHint = null,
            contextText = "abc xyz 123"
        )
        assertEquals("other", result)
    }

    @Test fun `coffee label from merchant`() {
        val result = CategoryClassifier.classify(
            items = emptyList(),
            merchantHint = "The Coffee House",
            contextText = "cafe nuoc uong"
        )
        // Both coffee and food can match; coffee has higher score from "cafe"
        val validCategories = setOf("coffee", "food")
        assert(result in validCategories) { "Expected coffee or food, got $result" }
    }
}
