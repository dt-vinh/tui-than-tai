package com.phuongnn14.tuithantai.capture

import com.phuongnn14.tuithantai.ocr.engine.OcrLine
import kotlin.math.abs

/** Rebuilds visual receipt rows from ML Kit line coordinates before semantic parsing. */
object ReceiptLayoutReconstructor {
    data class PositionedText(
        val text: String,
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    ) {
        val centerY: Float get() = (top + bottom) / 2f
        val height: Float get() = (bottom - top).coerceAtLeast(1f)
    }

    fun reconstruct(lines: List<OcrLine>, fallbackText: String): String {
        val positioned = lines.mapNotNull { line ->
            val box = line.boundingBox ?: return@mapNotNull null
            line.text.trim().takeIf { it.isNotEmpty() }?.let {
                PositionedText(it, box.left, box.top, box.right, box.bottom)
            }
        }
        return reconstructPositioned(positioned).takeIf { it.isNotBlank() } ?: fallbackText
    }

    internal fun reconstructPositioned(lines: List<PositionedText>): String {
        if (lines.isEmpty()) return ""
        val rows = mutableListOf<MutableList<PositionedText>>()
        lines.sortedWith(compareBy<PositionedText> { it.centerY }.thenBy { it.left }).forEach { line ->
            val row = rows.minByOrNull { candidate ->
                abs(candidate.map { it.centerY }.average().toFloat() - line.centerY)
            }
            if (row != null && belongsToSameRow(row, line)) row += line else rows += mutableListOf(line)
        }
        return rows
            .sortedBy { row -> row.minOf { it.top } }
            .joinToString("\n") { row ->
                row.sortedBy { it.left }.joinToString(" ") { it.text }
            }
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
