package com.phuongnn14.tuithantai.capture

/** Reconstructs OCR lines as a semantic receipt table before amount selection. */
object ReceiptTableInterpreter {
    private val moneyCellPattern = Regex(
        """[-−]?\s*(?:[0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]{1,3}(?:\s[0-9]{3})+|[0-9]{4,12}|[0-9]{1,3}\s*[kK])\s*(?:VND|VNĐ|vnđ|đ|d)?"""
    )

    private val totalRoles = listOf(
        "tien can thanh toan" to 120,
        "tong thanh toan" to 115,
        "thanh toan" to 110,
        "tong tien hang" to 105,
        "tong so tien" to 105,
        "tong cong" to 100,
        "phai tra" to 100,
        "tong tien" to 95,
        "grand total" to 95,
        "amount due" to 95,
        "thanh tien" to 80,
        "total" to 80
    )

    fun resolveFinalAmount(rawText: String): AmountExtractionResult? {
        val lines = rawText.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
        val candidates = lines.mapIndexedNotNull { index, line ->
            val normalized = AmountExtractor.normalize(line)
            if (normalized.contains("hoa don thanh toan")) return@mapIndexedNotNull null
            val role = totalRoles.firstOrNull { (keyword, _) -> normalized.contains(keyword) }
                ?: return@mapIndexedNotNull null

            val directCells = moneyCells(line)
            val cells = if (directCells.any { !it.negative }) {
                directCells
            } else {
                lines.drop(index + 1).take(2)
                    .takeWhile { next -> totalRoles.none { AmountExtractor.normalize(next).contains(it.first) } }
                    .flatMap(::moneyCells)
            }
            val payable = cells.lastOrNull { !it.negative && it.amount > 0L }
                ?: return@mapIndexedNotNull null
            SemanticTotal(
                amount = payable.amount,
                sourceLine = line,
                lineIndex = index,
                semanticPriority = role.second,
                role = role.first
            )
        }

        val best = candidates.maxWithOrNull(
            compareBy<SemanticTotal> { it.semanticPriority }.thenBy { it.lineIndex }
        ) ?: return null
        return AmountExtractionResult(
            amount = best.amount,
            sourceLine = best.sourceLine,
            confidence = 1f,
            reason = "semantic receipt row: ${best.role}; selected last positive money cell"
        )
    }

    fun toMarkdown(rawText: String): String {
        val rows = rawText.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
        if (rows.isEmpty()) return "| # | role | OCR row | money cells |\n|---:|---|---|---|"
        return buildString {
            appendLine("| # | role | OCR row | money cells |")
            appendLine("|---:|---|---|---|")
            rows.forEachIndexed { index, row ->
                val normalized = AmountExtractor.normalize(row)
                val role = totalRoles.firstOrNull { normalized.contains(it.first) }?.first
                    ?: if (moneyCells(row).isNotEmpty()) "item_or_value" else "text"
                val values = moneyCells(row).joinToString(", ") { it.raw.trim() }.ifBlank { "-" }
                appendLine("| ${index + 1} | $role | ${escape(row)} | ${escape(values)} |")
            }
        }.trimEnd()
    }

    private fun moneyCells(line: String): List<MoneyCell> = moneyCellPattern.findAll(line).mapNotNull { match ->
        val raw = match.value.trim()
        val negative = raw.startsWith('-') || raw.startsWith('−')
        val amount = AmountExtractor.parseVndAmount(raw.removePrefix("-").removePrefix("−").trim())
            ?: return@mapNotNull null
        MoneyCell(raw, amount, negative)
    }.toList()

    private fun escape(value: String): String = value.replace("|", "\\|").replace("\n", " ")

    private data class MoneyCell(val raw: String, val amount: Long, val negative: Boolean)

    private data class SemanticTotal(
        val amount: Long,
        val sourceLine: String,
        val lineIndex: Int,
        val semanticPriority: Int,
        val role: String
    )
}
