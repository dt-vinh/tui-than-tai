package com.phuongnn14.tuithantai.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObjectCategoryClassifierTest {

    @Test
    fun `watch image labels map to shopping`() {
        val result = ObjectCategoryClassifier.classifyLabelTexts(
            listOf("Watch", "Fashion accessory")
        )

        assertEquals("shopping", result.categoryId)
        assertTrue(result.confidence > 0f)
    }

    @Test
    fun `medicine and bandage labels map to health`() {
        val result = ObjectCategoryClassifier.classifyLabelTexts(
            listOf("Medicine", "Bandage", "First aid")
        )

        assertEquals("health", result.categoryId)
    }

    @Test
    fun `food labels map to food and drink`() {
        val result = ObjectCategoryClassifier.classifyLabelTexts(
            listOf("Bread", "Fast food", "Beverage")
        )

        assertEquals("food_and_drink", result.categoryId)
    }

    @Test
    fun `unknown labels stay other`() {
        val result = ObjectCategoryClassifier.classifyLabelTexts(
            listOf("Sky", "Landscape")
        )

        assertEquals("other", result.categoryId)
        assertEquals(0f, result.confidence)
    }
}
