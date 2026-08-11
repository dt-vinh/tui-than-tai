package com.phuongnn14.tuithantai.capture

/** Reconstructs OCR lines as a semantic receipt table before amount selection. */
object ReceiptTableInterpreter {
    private val moneyCellPattern = Regex(
        """[-−]?\s*(?:[0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]{1,3}(?:\s[0-9]{3})+|[0-9]{4,12}|[0-9]{1,3}\s*[kK])\s*(?:VND|VNĐ|vnđ|đ|d)?"""
    )

    private val totalRoles = listOf(
        "tien can thanh toan" to 145,
        "tong thanh toan" to 140,
        "thanh toan" to 130,
        "tong tien hang" to 70,
        "tong so tien" to 120,
        "tong cong" to 135,
        "phai tra" to 130,
        "tong tien" to 120,
        "amount payable" to 110,
        "balance due" to 105,
        "total due" to 105,
        "grand total" to 95,
        "amount due" to 95,
        "应付金额" to 115,
        "應付金額" to 115,
        "实付金额" to 110,
        "實付金額" to 110,
        "总计" to 100,
        "總計" to 100,
        "合计" to 95,
        "合計" to 95,
        "お支払金額" to 115,
        "支払金額" to 110,
        "請求額" to 105,
        "総合計" to 100,
        "お会計" to 95,
        "총 결제금액" to 115,
        "결제금액" to 110,
        "청구금액" to 105,
        "총액" to 100,
        "합계" to 95,
        "कुल देय" to 115,
        "देय राशि" to 110,
        "भुगतान राशि" to 105,
        "कुल राशि" to 100,
        "total a pagar" to 110,
        "importe total" to 105,
        "monto total" to 105,
        "net à payer" to 110,
        "montant dû" to 105,
        "total à payer" to 105,
        "zu zahlen" to 110,
        "gesamtbetrag" to 105,
        "rechnungsbetrag" to 105,
        "totale da pagare" to 110,
        "importo totale" to 105,
        "valor total" to 105,
        "total pembayaran" to 110,
        "jumlah bayar" to 105,
        "jumlah perlu dibayar" to 105,
        "ยอดชำระ" to 115,
        "ยอดสุทธิ" to 110,
        "รวมทั้งสิ้น" to 105,
        "thanh tien" to 70,
        "total" to 80
    )

    private val documentHeaders = listOf(
        "hoa don thanh toan", "tax invoice", "invoice", "receipt", "发票", "發票",
        "領収書", "レシート", "영수증", "रसीद", "factura", "reçu", "rechnung", "ใบเสร็จ"
    )

    fun resolveFinalAmount(rawText: String): AmountExtractionResult? {
        val lines = rawText.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
        val candidates = lines.mapIndexedNotNull { index, line ->
            val normalized = AmountExtractor.normalize(line)
            if (moneyCells(line).isEmpty() && matchesAny(normalized, documentHeaders)) {
                return@mapIndexedNotNull null
            }
            val role = roleFor(normalized)
                ?: return@mapIndexedNotNull null

            val directCells = moneyCells(line)
            val cells = if (directCells.any { !it.negative }) {
                directCells
            } else {
                lines.drop(index + 1).take(2)
                    .takeWhile { next -> roleFor(AmountExtractor.normalize(next)) == null }
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
                val role = roleFor(normalized)?.first
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

    private fun roleFor(normalizedLine: String): Pair<String, Int>? =
        totalRoles.firstOrNull { (keyword, _) ->
            normalizedLine.contains(AmountExtractor.normalize(keyword))
        }

    private fun matchesAny(normalizedLine: String, keywords: List<String>): Boolean =
        keywords.any { normalizedLine.contains(AmountExtractor.normalize(it)) }

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
