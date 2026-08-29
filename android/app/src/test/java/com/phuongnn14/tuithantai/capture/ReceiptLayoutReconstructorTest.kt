package com.phuongnn14.tuithantai.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptLayoutReconstructorTest {
    @Test
    fun joinsLabelAndAmountUsingTheirVisualRow() {
        val lines = listOf(
            line("Tổng tiền hàng", 10f, 10f, 130f, 30f),
            line("395,000", 240f, 11f, 300f, 31f),
            line("VAT (8%)", 10f, 40f, 90f, 60f),
            line("31,600", 240f, 41f, 300f, 61f),
            line("Tổng cộng", 10f, 70f, 100f, 90f),
            line("426,600", 240f, 71f, 300f, 91f)
        )

        val reconstructed = ReceiptLayoutReconstructor.reconstructPositioned(lines)

        assertTrue(reconstructed.contains("Tổng tiền hàng 395,000"))
        assertTrue(reconstructed.contains("Tổng cộng 426,600"))
        assertEquals(426_600L, ReceiptTableInterpreter.resolveFinalAmount(reconstructed)?.amount)
    }

    @Test
    fun keepsAdjacentReceiptRowsSeparate() {
        val lines = listOf(
            line("Thành tiền", 10f, 10f, 100f, 28f),
            line("246,400", 220f, 10f, 290f, 28f),
            line("Tổng tiền", 10f, 34f, 100f, 52f),
            line("211,120", 220f, 34f, 290f, 52f)
        )

        val rows = ReceiptLayoutReconstructor.reconstructPositioned(lines).lines()

        assertEquals(2, rows.size)
        assertEquals("Thành tiền 246,400", rows[0])
        assertEquals("Tổng tiền 211,120", rows[1])
    }

    @Test
    fun documentPreservesCellsAndColumnOrder() {
        val document = ReceiptLayoutReconstructor.reconstructDocumentPositioned(
            listOf(
                line("Còn", 10f, 10f, 45f, 30f),
                line("lại", 50f, 10f, 75f, 30f),
                line("phải", 80f, 10f, 120f, 30f),
                line("thu", 125f, 10f, 150f, 30f),
                line("510,000", 240f, 10f, 310f, 30f)
            )
        )

        assertEquals(1, document.rows.size)
        assertEquals(5, document.rows.single().cells.size)
        assertEquals("Còn lại phải thu 510,000", document.asText())
        assertEquals(510_000L, ReceiptTableInterpreter.resolveFinalAmount(document)?.amount)
    }

    private fun line(text: String, left: Float, top: Float, right: Float, bottom: Float) =
        ReceiptLayoutReconstructor.PositionedText(text, left, top, right, bottom)
}
