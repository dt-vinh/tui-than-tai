package com.phuongnn14.tuithantai.ocr

/**
 * Extracts line items from OCR text lines.
 *
 * Handles the common Vietnamese POS receipt format:
 *   [name]  [qty]  [unit_price]  [line_total]
 * or simpler:
 *   [name]  [line_total]
 *
 * Rule: if item name is unclear/empty, set name="" and needsReview=true.
 * Never fills name with placeholder text.
 */
object LineItemParser {

    // Keywords that indicate a summary/total line, not a product line
    private val SKIP_LABELS = setOf(
        "tong", "thanh toan", "vat", "thue", "phi dich vu", "giam gia",
        "chiet khau", "khach dua", "tien thua", "subtotal", "total",
        "discount", "tax", "service", "change", "cash"
    )

    fun parse(lines: List<String>, currency: String): List<LineItem> {
        val defaultCurrency = if (currency == "USD") MoneyParser.Currency.USD else MoneyParser.Currency.VND
        val result = mutableListOf<LineItem>()

        for (line in lines) {
            val item = tryParseLine(line, defaultCurrency) ?: continue
            result.add(item)
        }
        return result
    }

    private fun tryParseLine(line: String, currency: MoneyParser.Currency): LineItem? {
        if (line.length < 3) return null

        // Skip lines that are purely summary/total rows
        val normLine = normalize(line)
        if (SKIP_LABELS.any { normLine.contains(it) }) return null

        // Find all money amounts on the line
        val parts = line.split(Regex("""\s{2,}|\t"""))
        if (parts.size < 2) return null

        // Last part that has a money amount is likely line_total
        val moneyParts = parts.mapIndexedNotNull { i, p ->
            MoneyParser.parse(p.trim(), currency)?.let { i to it }
        }
        if (moneyParts.isEmpty()) return null

        val (totalIdx, totalAmount) = moneyParts.last()
        val nameParts = parts.subList(0, totalIdx).filter {
            MoneyParser.parse(it.trim(), currency) == null
        }
        val rawName = nameParts.joinToString(" ").trim()

        // Detect quantity and unit price if we have 3+ money values
        var qty: Double? = null
        var unitPrice: Double? = null
        if (moneyParts.size >= 2) {
            val firstMoney = moneyParts.first().second.amount
            val lastMoney = totalAmount.amount
            if (lastMoney > 0 && firstMoney > 0 && lastMoney != firstMoney) {
                val ratio = lastMoney / firstMoney
                if (ratio == ratio.toLong().toDouble() && ratio in 1.0..999.0) {
                    qty = ratio
                    unitPrice = firstMoney
                }
            }
        }

        // Don't hallucinate names — leave blank if too short or all-digits
        val confidence = if (rawName.length >= 2 && !rawName.all { it.isDigit() || it in ",./ " }) 0.8 else 0.4
        val needsReview = confidence < 0.6

        return LineItem(
            name = if (rawName.length >= 2 && !rawName.all { it.isDigit() || it in ",./ " }) rawName else "",
            quantity = qty,
            unitPrice = unitPrice,
            lineTotal = totalAmount.amount,
            confidence = confidence,
            needsReview = needsReview,
            rawText = line
        )
    }

    private fun normalize(s: String): String {
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace('đ', 'd').replace('Đ', 'D')
            .lowercase()
    }
}
