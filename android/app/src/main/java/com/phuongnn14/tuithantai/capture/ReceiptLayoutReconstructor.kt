package com.phuongnn14.tuithantai.capture

import com.phuongnn14.tuithantai.ocr.engine.OcrLine
import kotlin.math.abs

data class ReceiptCell(
    val text: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val confidence: Float? = null
)

data class ReceiptRow(val cells: List<ReceiptCell>) {
    val text: String = cells.sortedBy { it.left }.joinToString(" ") { it.text }.trim()
}

data class ReceiptDocument(
    val rows: List<ReceiptRow>,
    val fallbackText: String = ""
) {
    fun asText(): String = rows.joinToString("\n") { it.text }
        .takeIf { it.isNotBlank() }
        ?: fallbackText
}

/** Rebuilds visual receipt rows and cells from ML Kit coordinates before parsing. */
object ReceiptLayoutReconstructor {
    data class PositionedText(
        val text: String,
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val confidence: Float? = null
    ) {
        val centerY: Float get() = (top + bottom) / 2f
        val height: Float get() = (bottom - top).coerceAtLeast(1f)
    }

    fun reconstruct(lines: List<OcrLine>, fallbackText: String): String =
        reconstructDocument(lines, fallbackText).asText()

    fun reconstructDocument(lines: List<OcrLine>, fallbackText: String): ReceiptDocument {
        val positionedElements = lines.flatMap { line ->
            line.elements.mapNotNull { element ->
                val box = element.boundingBox ?: return@mapNotNull null
                element.text.trim().takeIf { it.isNotEmpty() }?.let { text ->
                    PositionedText(
                        text = text,
                        left = box.left,
                        top = box.top,
                        right = box.right,
                        bottom = box.bottom,
                        confidence = element.confidence
                    )
                }
            }
        }
        val positioned = positionedElements.ifEmpty {
            lines.mapNotNull { line ->
                val box = line.boundingBox ?: return@mapNotNull null
                line.text.trim().takeIf { it.isNotEmpty() }?.let { text ->
                    PositionedText(
                        text = text,
                        left = box.left,
                        top = box.top,
                        right = box.right,
                        bottom = box.bottom,
                        confidence = line.confidence
                    )
                }
            }
        }
        return reconstructDocumentPositioned(positioned, fallbackText)
    }

    internal fun reconstructPositioned(lines: List<PositionedText>): String =
        reconstructDocumentPositioned(lines).asText()

    internal fun reconstructDocumentPositioned(
        lines: List<PositionedText>,
        fallbackText: String = ""
    ): ReceiptDocument {
        if (lines.isEmpty()) return ReceiptDocument(emptyList(), fallbackText)
        val rows = mutableListOf<MutableList<PositionedText>>()
        lines.sortedWith(compareBy<PositionedText> { it.centerY }.thenBy { it.left }).forEach { line ->
            val row = rows.minByOrNull { candidate ->
                abs(candidate.map { it.centerY }.average().toFloat() - line.centerY)
            }
            if (row != null && belongsToSameRow(row, line)) row += line else rows += mutableListOf(line)
        }
        val receiptRows = rows.sortedBy { row -> row.minOf { it.top } }.map { row ->
            ReceiptRow(
                row.sortedBy { it.left }.map { cell ->
                    ReceiptCell(
                        text = cell.text,
                        left = cell.left,
                        top = cell.top,
                        right = cell.right,
                        bottom = cell.bottom,
                        confidence = cell.confidence
                    )
                }
            )
        }
        return ReceiptDocument(receiptRows, fallbackText)
    }

    private fun belongsToSameRow(row: List<PositionedText>, line: PositionedText): Boolean {
        val rowTop = row.minOf { it.top }
        val rowBottom = row.maxOf { it.bottom }
        val overlap = (minOf(rowBottom, line.bottom) - maxOf(rowTop, line.top)).coerceAtLeast(0f)
        val overlapRatio = overlap / minOf((rowBottom - rowTop).coerceAtLeast(1f), line.height)
        val centerDistance = abs(row.map { it.centerY }.average().toFloat() - line.centerY)
        val tolerance = maxOf(row.maxOf { it.height }, line.height) * 0.55f
        return overlapRatio >= 0.35f || centerDistance <= tolerance
    }
}
